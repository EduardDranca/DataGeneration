package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;

/**
 * Utility class for materializing lazy items during output generation.
 * This ensures all fields are generated before creating final JSON or SQL output.
 */
public class LazyMaterializer {
    
    /**
     * Materializes all lazy items in a JsonNode tree recursively.
     * This should be called before generating final output (JSON, SQL, etc.).
     */
    public static JsonNode materializeAll(JsonNode node) {
        if (node == null) {
            return null;
        }
        
        if (node instanceof LazyItemProxy) {
            // Materialize the lazy proxy
            LazyItemProxy lazyProxy = (LazyItemProxy) node;
            return lazyProxy.materializeAll();
        } else if (node.isObject()) {
            // Recursively materialize object fields
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<String> fieldNames = objectNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldValue = objectNode.get(fieldName);
                JsonNode materializedValue = materializeAll(fieldValue);
                if (materializedValue != fieldValue) {
                    objectNode.set(fieldName, materializedValue);
                }
            }
            return objectNode;
        } else if (node.isArray()) {
            // Recursively materialize array elements
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode element = arrayNode.get(i);
                JsonNode materializedElement = materializeAll(element);
                if (materializedElement != element) {
                    arrayNode.set(i, materializedElement);
                }
            }
            return arrayNode;
        } else {
            // Primitive values don't need materialization
            return node;
        }
    }
    
    /**
     * Collects memory statistics from all lazy items in a JsonNode tree.
     */
    public static MemoryStatsSummary collectMemoryStats(JsonNode node) {
        MemoryStatsSummary summary = new MemoryStatsSummary();
        collectMemoryStatsRecursive(node, summary);
        return summary;
    }
    
    private static void collectMemoryStatsRecursive(JsonNode node, MemoryStatsSummary summary) {
        if (node == null) {
            return;
        }
        
        if (node instanceof LazyItemProxy) {
            LazyItemProxy lazyProxy = (LazyItemProxy) node;
            LazyItemProxy.MemoryStats stats = lazyProxy.getMemoryStats();
            summary.addStats(stats);
        } else if (node.isObject()) {
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                collectMemoryStatsRecursive(elements.next(), summary);
            }
        } else if (node.isArray()) {
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                collectMemoryStatsRecursive(elements.next(), summary);
            }
        }
    }
    
    /**
     * Summary of memory statistics across multiple lazy items.
     */
    public static class MemoryStatsSummary {
        private int totalItems = 0;
        private int totalFieldsPossible = 0;
        private int totalFieldsMaterialized = 0;
        private int fullyMaterializedItems = 0;
        
        public void addStats(LazyItemProxy.MemoryStats stats) {
            totalItems++;
            totalFieldsPossible += stats.getTotalFields();
            totalFieldsMaterialized += stats.getMaterializedFields();
            if (stats.isFullyMaterialized()) {
                fullyMaterializedItems++;
            }
        }
        
        public int getTotalItems() { return totalItems; }
        public int getTotalFieldsPossible() { return totalFieldsPossible; }
        public int getTotalFieldsMaterialized() { return totalFieldsMaterialized; }
        public int getFullyMaterializedItems() { return fullyMaterializedItems; }
        
        public double getOverallMemorySavingsPercentage() {
            if (totalFieldsPossible == 0) return 0.0;
            return ((double)(totalFieldsPossible - totalFieldsMaterialized) / totalFieldsPossible) * 100.0;
        }
        
        public int getTotalFieldsSaved() {
            return totalFieldsPossible - totalFieldsMaterialized;
        }
        
        @Override
        public String toString() {
            return String.format(
                "MemoryStatsSummary{items=%d, totalFields=%d, materialized=%d, saved=%d, savings=%.1f%%, fullyMaterialized=%d}",
                totalItems, totalFieldsPossible, totalFieldsMaterialized, 
                getTotalFieldsSaved(), getOverallMemorySavingsPercentage(), fullyMaterializedItems
            );
        }
    }
}