package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.visitor.LazyItemProxy;

import java.util.List;
import java.util.Map;

/**
 * Memory-optimized implementation of Generation that uses lazy evaluation
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
public class LazyGeneration extends AbstractGeneration<LazyItemProxy> {

    LazyGeneration(Map<String, List<LazyItemProxy>> lazyCollectionsMap) {
        super(lazyCollectionsMap);
    }

    @Override
    protected JsonNode toJsonNode(LazyItemProxy item) {
        // For lazy generation, we need to materialize the proxy
        return item.getMaterializedCopy();
    }


}
