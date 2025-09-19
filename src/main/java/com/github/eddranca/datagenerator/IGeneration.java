package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;


import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Interface for generated data that can be output in various formats.
 * 
 * <p>This interface provides a unified API for both normal and memory-optimized
 * data generation implementations. The memory-optimized implementation uses lazy
 * evaluation to reduce memory usage for large datasets.
 * 
 * <p>Key features:
 * <ul>
 *   <li>JSON output in multiple formats</li>
 *   <li>SQL INSERT statement generation</li>
 *   <li>Memory-efficient streaming for large datasets</li>
 *   <li>Collection access for testing and inspection</li>
 * </ul>
 */
public interface IGeneration {
    
    /**
     * Returns the generated data as a JsonNode tree structure.
     * 
     * <p><strong>Memory Impact:</strong> This method materializes all data in memory,
     * which may consume significant memory for large datasets when using memory optimization.
     * 
     * @return JsonNode containing all generated collections
     */
    JsonNode asJsonNode();
    
    /**
     * Returns the generated data as a formatted JSON string.
     * 
     * <p><strong>Memory Impact:</strong> This method materializes all data in memory
     * before serialization.
     * 
     * @return JSON string representation of all generated data
     * @throws JsonProcessingException if JSON serialization fails
     */
    String asJson() throws JsonProcessingException;
    
    /**
     * Returns the names of all generated collections.
     * 
     * <p>This method provides a way to discover what collections were generated
     * without exposing the internal collection structure.
     * 
     * @return Set of collection names
     */
    Set<String> getCollectionNames();
    
    /**
     * Returns the number of items in the specified collection.
     * 
     * <p>This method provides a way to inspect collection sizes without
     * exposing the internal collection structure.
     * 
     * @param collectionName name of the collection
     * @return number of items in the collection
     * @throws IllegalArgumentException if the collection doesn't exist
     */
    int getCollectionSize(String collectionName);
    
    /**
     * Returns the collections as materialized JsonNode lists.
     * 
     * <p><strong>⚠️ Testing and Legacy Support Only:</strong> This method is primarily
     * intended for testing and backward compatibility. It exposes internal structure
     * and may consume significant memory for large datasets with memory optimization.
     * 
     * <p><strong>Recommended alternatives:</strong>
     * <ul>
     *   <li>Use {@link #asJsonNode()} for JSON output</li>
     *   <li>Use {@link #streamSqlInserts(String)} for SQL generation</li>
     *   <li>Use {@link #getCollectionSize(String)} to check sizes</li>
     * </ul>
     * 
     * @return Map where keys are collection names and values are lists of JsonNode items
     * @deprecated This method exposes internal structure and may be removed in future versions
     */
    @Deprecated
    Map<String, java.util.List<JsonNode>> getCollections();
    
    /**
     * Generates SQL INSERT statements for all collections.
     * 
     * <p>Each collection becomes a table, and each item becomes a row.
     * Complex nested objects are serialized as JSON strings.
     * 
     * @return Map where keys are table names and values are SQL INSERT statements
     */
    Map<String, String> asSqlInserts();
    
    /**
     * Generates SQL INSERT statements for specified collections only.
     * 
     * <p>This method allows selective SQL generation, useful when some collections
     * are used only as reference data.
     * 
     * @param collectionNames names of collections to generate SQL for, or empty for all
     * @return Map where keys are table names and values are SQL INSERT statements
     */
    Map<String, String> asSqlInserts(String... collectionNames);
    
    /**
     * Generates SQL INSERT statements as a stream for memory-efficient processing.
     * 
     * <p><strong>Recommended for large datasets:</strong> This method processes items
     * one at a time without loading everything into memory, making it ideal for
     * large datasets or when using memory optimization.
     * 
     * @param collectionName name of the collection to stream
     * @return Stream of SQL INSERT statements, one per item
     * @throws IllegalArgumentException if the collection doesn't exist
     */
    Stream<String> streamSqlInserts(String collectionName);
}