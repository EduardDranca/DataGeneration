package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.util.SqlInsertGenerator;
import com.github.eddranca.datagenerator.visitor.LazyItemProxy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Memory-optimized implementation of IGeneration that uses lazy evaluation
 * for efficient memory usage with large datasets.
 *
 * <p>Key features:
 * <ul>
 *   <li><strong>Lazy field generation</strong> - Only referenced fields are initially materialized</li>
 *   <li><strong>On-demand materialization</strong> - Other fields generated when accessed</li>
 *   <li><strong>Memory-efficient streaming</strong> - Process items without caching</li>
 *   <li><strong>Hierarchical lazy loading</strong> - Nested objects are also optimized</li>
 * </ul>
 *
 * <p>This implementation can reduce memory usage by up to 99% for large datasets
 * where only a subset of fields are actually used.
 *
 * <p>Usage example:
 * <pre>{@code
 * Generation result = DslDataGenerator.create()
 *     .withMemoryOptimization()
 *     .fromJsonString(dsl)
 *     .generate();
 *
 * // Memory-efficient streaming
 * result.streamSqlInserts("users")
 *     .forEach(sql -> database.execute(sql));
 * }</pre>
 */
public class LazyGeneration implements Generation {
    private final Map<String, List<LazyItemProxy>> lazyCollections;

    LazyGeneration(Map<String, List<LazyItemProxy>> lazyCollectionsMap) {
        this.lazyCollections = new HashMap<>(lazyCollectionsMap);
    }

    @Override
    public Set<String> getCollectionNames() {
        return lazyCollections.keySet();
    }

    @Override
    public int getCollectionSize(String collectionName) {
        List<LazyItemProxy> collection = lazyCollections.get(collectionName);
        if (collection == null) {
            throw new IllegalArgumentException("Collection '" + collectionName + "' not found");
        }
        return collection.size();
    }

    @Override
    public Stream<JsonNode> streamJsonNodes(String collectionName) {
        List<LazyItemProxy> collection = lazyCollections.get(collectionName);
        if (collection == null) {
            throw new IllegalArgumentException("Collection '" + collectionName + "' not found");
        }
        return collection.stream().map(LazyItemProxy::getMaterializedCopy);
    }

    @Override
    public Map<String, Stream<JsonNode>> asJsonNodes() {
        Map<String, Stream<JsonNode>> streams = new HashMap<>();
        for (Map.Entry<String, List<LazyItemProxy>> entry : lazyCollections.entrySet()) {
            Stream<JsonNode> jsonStream = entry.getValue().stream()
                .map(LazyItemProxy::getMaterializedCopy);
            streams.put(entry.getKey(), jsonStream);
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

        for (Map.Entry<String, List<LazyItemProxy>> entry : lazyCollections.entrySet()) {
            String collectionName = entry.getKey();
            if (includeCollections == null || includeCollections.contains(collectionName)) {
                Stream<JsonNode> jsonStream = entry.getValue().stream()
                    .map(LazyItemProxy::getMaterializedCopy);
                streams.put(collectionName, jsonStream);
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

        for (Map.Entry<String, List<LazyItemProxy>> entry : lazyCollections.entrySet()) {
            String tableName = entry.getKey();

            if (includeCollections != null && !includeCollections.contains(tableName)) {
                continue;
            }

            Stream<String> sqlStream = entry.getValue().stream()
                .map(lazyItem -> {
                    JsonNode materializedItem = lazyItem.getMaterializedCopy();
                    return SqlInsertGenerator.generateSqlInsert(tableName, materializedItem);
                });
            sqlStreams.put(tableName, sqlStream);
        }
        return sqlStreams;
    }

    @Override
    public Stream<String> streamSqlInserts(String collectionName) {
        List<LazyItemProxy> collection = lazyCollections.get(collectionName);
        if (collection == null) {
            throw new IllegalArgumentException("Collection '" + collectionName + "' not found");
        }

        return collection.stream()
            .map(lazyItem -> {
                // Get materialized copy for each item independently during streaming
                JsonNode materializedItem = lazyItem.getMaterializedCopy();
                return SqlInsertGenerator.generateSqlInsert(collectionName, materializedItem);
            });
    }


}
