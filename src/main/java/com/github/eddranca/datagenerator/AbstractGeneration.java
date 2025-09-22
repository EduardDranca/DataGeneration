package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.util.SqlInsertGenerator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Abstract base class for Generation implementations that eliminates code duplication
 * between eager and lazy generation strategies.
 *
 * @param <T> the type of items stored in collections (JsonNode for eager, LazyItemProxy for lazy)
 */
public abstract class AbstractGeneration<T> implements Generation {
    protected final Map<String, List<T>> collections;

    protected AbstractGeneration(Map<String, List<T>> collectionsMap) {
        this.collections = new HashMap<>(collectionsMap);
    }

    /**
     * Converts an item of type T to JsonNode for output.
     * Eager implementation returns the JsonNode as-is.
     * Lazy implementation calls getMaterializedCopy() on the proxy.
     */
    protected abstract JsonNode toJsonNode(T item);

    @Override
    public Set<String> getCollectionNames() {
        return collections.keySet();
    }

    @Override
    public int getCollectionSize(String collectionName) {
        List<T> collection = collections.get(collectionName);
        if (collection == null) {
            throw new IllegalArgumentException("Collection '" + collectionName + "' not found");
        }
        return collection.size();
    }

    @Override
    public Stream<JsonNode> streamJsonNodes(String collectionName) {
        List<T> collection = collections.get(collectionName);
        if (collection == null) {
            throw new IllegalArgumentException("Collection '" + collectionName + "' not found");
        }
        return collection.stream().map(this::toJsonNode);
    }

    @Override
    public Map<String, Stream<JsonNode>> asJsonNodes() {
        Map<String, Stream<JsonNode>> streams = new HashMap<>();
        for (Map.Entry<String, List<T>> entry : collections.entrySet()) {
            streams.put(entry.getKey(), entry.getValue().stream().map(this::toJsonNode));
        }
        return streams;
    }

    @Override
    public Map<String, Stream<JsonNode>> asJsonNodes(String... collectionNames) {
        Map<String, Stream<JsonNode>> streams = new HashMap<>();

        Set<String> includeCollections = null;
        if (collectionNames != null && collectionNames.length > 0) {
            includeCollections = new HashSet<>(Arrays.asList(collectionNames));
        }

        for (Map.Entry<String, List<T>> entry : collections.entrySet()) {
            String collectionName = entry.getKey();
            if (includeCollections == null || includeCollections.contains(collectionName)) {
                streams.put(collectionName, entry.getValue().stream().map(this::toJsonNode));
            }
        }
        return streams;
    }

    @Override
    public Map<String, Stream<String>> asSqlInserts() {
        return asSqlInserts((String[]) null);
    }

    @Override
    public Map<String, Stream<String>> asSqlInserts(String... collectionNames) {
        Map<String, Stream<String>> sqlStreams = new HashMap<>();

        Set<String> includeCollections = null;
        if (collectionNames != null && collectionNames.length > 0) {
            includeCollections = new HashSet<>(Arrays.asList(collectionNames));
        }

        for (Map.Entry<String, List<T>> entry : collections.entrySet()) {
            String tableName = entry.getKey();

            if (includeCollections != null && !includeCollections.contains(tableName)) {
                continue;
            }

            Stream<String> sqlStream = entry.getValue().stream()
                .map(item -> SqlInsertGenerator.generateSqlInsert(tableName, toJsonNode(item)));
            sqlStreams.put(tableName, sqlStream);
        }
        return sqlStreams;
    }

    @Override
    public Stream<String> streamSqlInserts(String collectionName) {
        List<T> collection = collections.get(collectionName);
        if (collection == null) {
            throw new IllegalArgumentException("Collection '" + collectionName + "' not found");
        }

        return collection.stream()
            .map(item -> SqlInsertGenerator.generateSqlInsert(collectionName, toJsonNode(item)));
    }

    /**
     * Fluent builder for generation operations.
     */
    public static class Builder {
        private final DslDataGenerator generator;
        private final File file;
        private final String jsonString;
        private final JsonNode jsonNode;

        Builder(DslDataGenerator generator, File file) {
            this.generator = generator;
            this.file = file;
            this.jsonString = null;
            this.jsonNode = null;
        }

        Builder(DslDataGenerator generator, String jsonString) {
            this.generator = generator;
            this.file = null;
            this.jsonString = jsonString;
            this.jsonNode = null;
        }

        Builder(DslDataGenerator generator, JsonNode jsonNode) {
            this.generator = generator;
            this.file = null;
            this.jsonString = null;
            this.jsonNode = jsonNode;
        }

        /**
         * Generates the data based on the configured DSL source.
         *
         * @return the generated data
         * @throws IOException                                                        if file reading fails or JSON parsing fails
         * @throws com.github.eddranca.datagenerator.exception.DslValidationException if DSL validation fails
         */
        public Generation generate() throws IOException {
            if (file != null) {
                return generator.generateInternal(file);
            } else if (jsonString != null) {
                return generator.generateInternal(jsonString);
            } else if (jsonNode != null) {
                return generator.generateInternal(jsonNode);
            } else {
                throw new IllegalStateException("No DSL source configured");
            }
        }

        /**
         * Generates the data and returns SQL INSERT statement streams for all collections.
         *
         * @return a map of table names to SQL INSERT statement streams
         * @throws IOException if file reading fails
         */
        public Map<String, Stream<String>> generateAsSql() throws IOException {
            return generate().asSqlInserts();
        }

        /**
         * Generates the data and returns SQL INSERT statement streams for specified collections only.
         *
         * @param collectionNames the names of collections to generate SQL for
         * @return a map of table names to SQL INSERT statement streams
         * @throws IOException if file reading fails
         */
        public Map<String, Stream<String>> generateAsSql(String... collectionNames) throws IOException {
            return generate().asSqlInserts(collectionNames);
        }

        /**
         * Generates the data and returns JsonNode streams for all collections.
         *
         * @return a map of collection names to JsonNode streams
         * @throws IOException if file reading fails
         */
        public Map<String, Stream<JsonNode>> generateAsJson() throws IOException {
            return generate().asJsonNodes();
        }

        /**
         * Generates the data and returns JsonNode streams for specified collections only.
         *
         * @param collectionNames the names of collections to generate JSON for
         * @return a map of collection names to JsonNode streams
         * @throws IOException if file reading fails
         */
        public Map<String, Stream<JsonNode>> generateAsJson(String... collectionNames) throws IOException {
            return generate().asJsonNodes(collectionNames);
        }

        /**
         * Generates the data and returns a JsonNode stream for a single collection.
         *
         * @param collectionName the name of the collection to stream
         * @return a stream of JsonNode items
         * @throws IOException              if file reading fails
         * @throws IllegalArgumentException if the collection doesn't exist
         */
        public Stream<JsonNode> streamJsonNodes(String collectionName) throws IOException {
            return generate().streamJsonNodes(collectionName);
        }

        /**
         * Generates the data and returns a SQL INSERT stream for a single collection.
         *
         * @param collectionName the name of the collection to stream
         * @return a stream of SQL INSERT statements
         * @throws IOException              if file reading fails
         * @throws IllegalArgumentException if the collection doesn't exist
         */
        public Stream<String> streamSqlInserts(String collectionName) throws IOException {
            return generate().streamSqlInserts(collectionName);
        }
    }
}
