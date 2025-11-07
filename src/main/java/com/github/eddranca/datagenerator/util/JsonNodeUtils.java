package com.github.eddranca.datagenerator.util;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Utility methods for working with JsonNode objects.
 */
public class JsonNodeUtils {
    
    private JsonNodeUtils() {
        // Utility class
    }

    /**
     * Extracts a nested field from a JsonNode using dot notation.
     * 
     * @param node the node to extract from
     * @param fieldPath the field path (e.g., "address.city")
     * @return the extracted node, or a missing node if not found
     */
    public static JsonNode extractNestedField(JsonNode node, String fieldPath) {
        if (fieldPath == null || fieldPath.isEmpty()) {
            return node;
        }

        String[] parts = fieldPath.split("\\.");
        JsonNode current = node;
        for (String part : parts) {
            if (current.isMissingNode() || current.isNull()) {
                return current;
            }
            current = current.path(part);
        }
        return current;
    }
}
