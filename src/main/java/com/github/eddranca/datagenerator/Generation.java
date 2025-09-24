package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Interface for generated data that can be output in various formats.
 *
 * <p>
 * This interface provides a unified API for both normal and memory-optimized
 * data generation implementations. The memory-optimized implementation uses
 * lazy
 * evaluation to reduce memory usage for large datasets.
 * see {@link LazyGeneration} and {@link EagerGeneration}
 *
 */
public interface Generation {

    /**
     * Returns the names of all generated collections.
     *
     * <p>
     * This method provides a way to discover what collections were generated
     * without exposing the internal collection structure.
     *
     * @return Set of collection names
     */
    Set<String> getCollectionNames();

    /**
     * Returns the number of items in the specified collection.
     *
     * <p>
     * This method provides a way to inspect collection sizes without
     * exposing the internal collection structure.
     *
     * @param collectionName name of the collection
     * @return number of items in the collection
     * @throws IllegalArgumentException if the collection doesn't exist
     */
    int getCollectionSize(String collectionName);

    /**
     * Streams individual JsonNode items from a specific collection.
     *
     * <p>
     * <strong>Memory-efficient:</strong> This method generates items on-demand
     * without loading the entire collection into memory, making it ideal for
     * large datasets or when using memory optimization.
     *
     * @param collectionName name of the collection to stream
     * @return Stream of JsonNode items, one per generated item
     * @throws IllegalArgumentException if the collection doesn't exist
     */
    Stream<JsonNode> streamJsonNodes(String collectionName);

    /**
     * Returns streams of JsonNode items for all collections.
     *
     * <p>
     * Each collection is represented as a separate stream, allowing for
     * memory-efficient processing of multiple collections.
     *
     * @return Map where keys are collection names and values are streams of
     * JsonNode items
     */
    Map<String, Stream<JsonNode>> asJsonNodes();

    /**
     * Returns streams of JsonNode items for specified collections only.
     *
     * <p>
     * This method allows selective streaming, useful when only certain
     * collections are needed.
     *
     * @param collectionNames names of collections to stream, or empty for all
     * @return Map where keys are collection names and values are streams of
     * JsonNode items
     */
    Map<String, Stream<JsonNode>> asJsonNodes(String... collectionNames);

    /**
     * Returns streams of SQL INSERT statements for all collections.
     *
     * <p>
     * Each collection becomes a table, and each item becomes a row.
     * Complex nested objects are serialized as JSON strings.
     * Each stream contains individual INSERT statements for that collection.
     *
     * @return Map where keys are table names and values are streams of SQL INSERT
     * statements
     */
    Map<String, Stream<String>> asSqlInserts();

    /**
     * Returns streams of SQL INSERT statements for specified collections only.
     *
     * <p>
     * This method allows selective SQL generation, useful when some collections
     * are used only as reference data.
     *
     * @param collectionNames names of collections to generate SQL for, or empty for
     *                        all
     * @return Map where keys are table names and values are streams of SQL INSERT
     * statements
     */
    Map<String, Stream<String>> asSqlInserts(String... collectionNames);

    /**
     * Generates SQL INSERT statements as a stream for memory-efficient processing.
     *
     * <p>
     * <strong>Recommended for large datasets:</strong> This method processes items
     * one at a time without loading everything into memory, making it ideal for
     * large datasets or when using memory optimization.
     *
     * @param collectionName name of the collection to stream
     * @return Stream of SQL INSERT statements, one per item
     * @throws IllegalArgumentException if the collection doesn't exist
     */
    Stream<String> streamSqlInserts(String collectionName);

    /**
     * Convenience method to check if a collection exists.
     *
     * @param collectionName name of the collection to check
     * @return true if the collection exists, false otherwise
     */
    default boolean hasCollection(String collectionName) {
        return getCollectionNames().contains(collectionName);
    }
}
