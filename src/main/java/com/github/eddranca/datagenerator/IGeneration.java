package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Interface for generated data that can be output in various formats.
 * Provides both normal and memory-optimized implementations.
 */
public interface IGeneration {
    
    /**
     * Returns the generated data as a JsonNode.
     * This method materializes all data for JSON output.
     */
    JsonNode asJsonNode();
    
    /**
     * Returns the generated data as a JSON string.
     */
    String asJson() throws JsonProcessingException;
    
    /**
     * Returns the collections as a JsonNode for direct access.
     */
    ObjectNode getCollectionsAsJsonNode();
    
    /**
     * Returns the collections as materialized JsonNode lists.
     * This method provides access to the collections for testing and inspection.
     * Note: For memory-optimized implementations, this will materialize all items.
     */
    Map<String, java.util.List<JsonNode>> getCollections();
    
    /**
     * Generates SQL INSERT statements for all collections.
     */
    Map<String, String> asSqlInserts();
    
    /**
     * Generates SQL INSERT statements for specified collections only.
     */
    Map<String, String> asSqlInserts(String... collectionNames);
    
    /**
     * Generates SQL INSERT statements as a stream for the specified collection.
     * This method is memory-efficient for large datasets.
     */
    Stream<String> streamSqlInserts(String collectionName);
}