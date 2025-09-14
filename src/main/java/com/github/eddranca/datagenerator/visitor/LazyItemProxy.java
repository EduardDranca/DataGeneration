package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.node.DslNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A lazy proxy for JsonNode that only materializes fields on-demand.
 * This allows us to keep only referenced fields in memory during generation
 * and generate the rest when needed for output.
 * 
 * Supports nested path materialization for complex object hierarchies.
 */
public class LazyItemProxy extends ObjectNode {
    private final Map<String, Supplier<JsonNode>> fieldSuppliers;
    private final Set<String> referencedPaths;
    private final String collectionName;
    private final DataGenerationVisitor visitor;
    private final Set<String> materializedFieldNames = new HashSet<>();
    private boolean fullyMaterialized = false;
    
    public LazyItemProxy(String collectionName, 
                        Map<String, DslNode> fieldNodes,
                        Set<String> referencedPaths,
                        DataGenerationVisitor visitor) {
        super(JsonNodeFactory.instance);
        this.collectionName = collectionName;
        this.referencedPaths = referencedPaths;
        this.visitor = visitor;
        this.fieldSuppliers = createFieldSuppliers(fieldNodes);
        
        // Generate only referenced fields immediately
        materializeReferencedFields();
    }
    
    /**
     * Creates suppliers for all fields in the item.
     */
    private Map<String, Supplier<JsonNode>> createFieldSuppliers(Map<String, DslNode> fieldNodes) {
        Map<String, Supplier<JsonNode>> suppliers = new HashMap<>();
        
        for (Map.Entry<String, DslNode> entry : fieldNodes.entrySet()) {
            String fieldName = entry.getKey();
            DslNode fieldNode = entry.getValue();
            
            // Create lazy supplier that will generate the field when called
            suppliers.put(fieldName, () -> fieldNode.accept(visitor));
        }
        
        return suppliers;
    }
    
    /**
     * Materializes only the fields that are referenced by other collections.
     */
    private void materializeReferencedFields() {
        for (String fieldName : fieldSuppliers.keySet()) {
            if (shouldMaterializeField(fieldName)) {
                materializeField(fieldName);
            }
        }
    }
    
    /**
     * Determines if a field should be materialized based on reference analysis.
     */
    private boolean shouldMaterializeField(String fieldName) {
        // If entire object is referenced, materialize everything
        if (referencedPaths.contains("*")) {
            return true;
        }
        
        // Check if this field or any nested path is referenced
        for (String referencedPath : referencedPaths) {
            if (referencedPath.equals(fieldName) || referencedPath.startsWith(fieldName + ".")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Materializes a specific field if not already materialized.
     */
    private void materializeField(String fieldName) {
        // Check if already materialized using our own tracking
        if (!materializedFieldNames.contains(fieldName)) {
            Supplier<JsonNode> supplier = fieldSuppliers.get(fieldName);
            if (supplier != null) {
                JsonNode value = supplier.get();
                super.set(fieldName, value);
                materializedFieldNames.add(fieldName);
            }
        }
    }
    
    @Override
    public JsonNode get(String fieldName) {
        // If field is already materialized, return it
        if (materializedFieldNames.contains(fieldName)) {
            return super.get(fieldName);
        }
        
        // If not materialized and we have a supplier, materialize it now
        if (fieldSuppliers.containsKey(fieldName)) {
            materializeField(fieldName);
            return super.get(fieldName);
        }
        
        // Field doesn't exist
        return null;
    }
    
    @Override
    public JsonNode path(String fieldName) {
        // Similar to get() but returns missing node instead of null
        JsonNode result = get(fieldName);
        return result != null ? result : missingNode();
    }
    
    /**
     * Materializes all remaining fields for complete object generation.
     * This should be called during output generation (JSON/SQL).
     */
    public JsonNode materializeAll() {
        if (!fullyMaterialized) {
            // Generate any remaining fields
            for (String fieldName : fieldSuppliers.keySet()) {
                materializeField(fieldName);
            }
            fullyMaterialized = true;
        }
        return this;
    }
    
    /**
     * Returns true if this proxy has been fully materialized.
     */
    public boolean isFullyMaterialized() {
        return fullyMaterialized;
    }
    
    /**
     * Returns memory usage statistics for this item.
     */
    public MemoryStats getMemoryStats() {
        int totalFields = fieldSuppliers.size();
        int materializedFields = super.size();
        double efficiency = totalFields > 0 ? (double) materializedFields / totalFields : 1.0;
        
        return new MemoryStats(totalFields, materializedFields, efficiency, fullyMaterialized);
    }
    
    /**
     * Gets the value at a specific nested path, materializing as needed.
     * This supports deep path access like "address.street".
     */
    public JsonNode getAtPath(String path) {
        if (path == null || path.isEmpty()) {
            return this;
        }
        
        String[] parts = path.split("\\.", 2);
        String firstPart = parts[0];
        
        // Get the first level field (materializing if needed)
        JsonNode firstLevel = get(firstPart);
        
        if (parts.length == 1) {
            return firstLevel;
        } else {
            // Navigate deeper into the path
            String remainingPath = parts[1];
            if (firstLevel != null && firstLevel.isObject()) {
                return firstLevel.path(remainingPath);
            } else {
                return missingNode();
            }
        }
    }
    
    @Override
    public String toString() {
        if (fullyMaterialized) {
            return super.toString();
        } else {
            return String.format("LazyItemProxy{collection=%s, materialized=%d/%d fields}", 
                collectionName, super.size(), fieldSuppliers.size());
        }
    }
    
    /**
     * Memory usage statistics for a lazy item.
     */
    public static class MemoryStats {
        private final int totalFields;
        private final int materializedFields;
        private final double efficiencyRatio;
        private final boolean fullyMaterialized;
        
        public MemoryStats(int totalFields, int materializedFields, 
                          double efficiencyRatio, boolean fullyMaterialized) {
            this.totalFields = totalFields;
            this.materializedFields = materializedFields;
            this.efficiencyRatio = efficiencyRatio;
            this.fullyMaterialized = fullyMaterialized;
        }
        
        public int getTotalFields() { return totalFields; }
        public int getMaterializedFields() { return materializedFields; }
        public double getEfficiencyRatio() { return efficiencyRatio; }
        public boolean isFullyMaterialized() { return fullyMaterialized; }
        
        public int getSavedFields() {
            return totalFields - materializedFields;
        }
        
        public double getMemorySavingsPercentage() {
            if (totalFields == 0) return 0.0;
            return (1.0 - efficiencyRatio) * 100.0;
        }
        
        @Override
        public String toString() {
            return String.format("MemoryStats{total=%d, materialized=%d, saved=%d, savings=%.1f%%}",
                totalFields, materializedFields, getSavedFields(), getMemorySavingsPercentage());
        }
    }
}