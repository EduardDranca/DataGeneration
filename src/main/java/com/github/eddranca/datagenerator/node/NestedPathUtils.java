package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Utility class for handling nested path extraction from JsonNode objects.
 */
public class NestedPathUtils {
    private NestedPathUtils() {
    }

    /**
     * Extracts a field value from a JsonNode, supporting nested paths like "address.street".
     *
     * @param node      the JsonNode to extract from
     * @param fieldPath the dot-separated field path (e.g., "address.street", "profile.social.twitter")
     * @return the value at the specified path, or a missing node if any part of the path is missing
     */
    public static JsonNode extractNestedField(JsonNode node, String fieldPath) {
        if (fieldPath == null || fieldPath.isEmpty()) {
            return node;
        }
        return node.path(fieldPath);
    }
}
