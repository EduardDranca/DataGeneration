package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.util.SqlInsertGenerator;

import java.util.*;
import java.util.function.Function;
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
}