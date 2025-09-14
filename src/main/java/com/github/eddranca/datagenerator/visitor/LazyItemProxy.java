package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.node.DslNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A lazy proxy for JsonNode that only materializes fields on-demand.
 * This allows us to keep only referenced fields in memory during generation
 * and generate the rest when needed for output.
 *
 * Supports nested path materialization for complex object hierarchies.
 */
public class LazyItemProxy extends ObjectNode {
    private final Map<String, DslNode> fieldNodes;
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
        this.fieldNodes = new HashMap<>(fieldNodes);

        // Generate only referenced fields immediately
        materializeReferencedFields();
    }

    /**
     * Materializes only the fields that are referenced by other collections.
     */
    private void materializeReferencedFields() {
        for (String fieldName : fieldNodes.keySet()) {
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
            DslNode fieldNode = fieldNodes.get(fieldName);
            if (fieldNode != null) {
                JsonNode value;

                // If this is an ObjectFieldNode and has nested references, create a
                // LazyObjectProxy
                if (fieldNode instanceof com.github.eddranca.datagenerator.node.ObjectFieldNode
                        && hasNestedReferences(fieldName)) {
                    var objectFieldNode = (com.github.eddranca.datagenerator.node.ObjectFieldNode) fieldNode;
                    Set<String> nestedReferences = getNestedReferences(fieldName);

                    value = new LazyObjectProxy(
                            objectFieldNode.getFields(),
                            nestedReferences,
                            visitor,
                            fieldName);
                } else {
                    // Generate normally for simple fields or non-referenced nested objects
                    value = fieldNode.accept(visitor);
                }

                super.set(fieldName, value);
                materializedFieldNames.add(fieldName);
            }
        }
    }

    /**
     * Checks if there are any referenced paths that go deeper into this field.
     */
    private boolean hasNestedReferences(String fieldName) {
        String prefix = fieldName + ".";
        for (String referencedPath : referencedPaths) {
            if (referencedPath.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all referenced paths that start with the given field name.
     */
    private Set<String> getNestedReferences(String fieldName) {
        Set<String> nestedRefs = new HashSet<>();
        String prefix = fieldName + ".";

        for (String referencedPath : referencedPaths) {
            if (referencedPath.startsWith(prefix)) {
                nestedRefs.add(referencedPath);
            }
        }

        return nestedRefs;
    }

    @Override
    public JsonNode get(String fieldName) {
        // If field is already materialized, return it
        if (materializedFieldNames.contains(fieldName)) {
            return super.get(fieldName);
        }

        // If not materialized and we have a field node, materialize it now
        if (fieldNodes.containsKey(fieldName)) {
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
     * Returns a new ObjectNode with all fields materialized, leaving this proxy
     * unchanged.
     * This is memory-efficient for streaming operations where you need a
     * materialized copy
     * but want to keep the original proxy intact for potential reuse.
     *
     * @return a new ObjectNode with all fields materialized
     */
    public ObjectNode getMaterializedCopy() {
        ObjectNode materializedCopy = JsonNodeFactory.instance.objectNode();

        // First, ensure all fields are materialized in this proxy
        if (!fullyMaterialized) {
            for (String fieldName : fieldNodes.keySet()) {
                materializeField(fieldName);
            }
            fullyMaterialized = true;
        }

        // Copy the already-materialized values to the new node
        // For streaming efficiency, we'll use the materialized values directly
        // instead of creating deep copies of LazyObjectProxy instances
        for (String fieldName : fieldNodes.keySet()) {
            JsonNode value = super.get(fieldName);
            if (value != null) {
                materializedCopy.set(fieldName, value);
            }
        }

        return materializedCopy;
    }

    /**
     * Returns memory usage statistics for this item.
     */
    public MemoryStats getMemoryStats() {
        int totalFields = fieldNodes.size();
        int materializedFields = super.size();
        double efficiency = totalFields > 0 ? (double) materializedFields / totalFields : 1.0;

        return new MemoryStats(totalFields, materializedFields, efficiency, fullyMaterialized);
    }

    @Override
    public String toString() {
        if (fullyMaterialized) {
            return super.toString();
        } else {
            return String.format("LazyItemProxy{collection=%s, materialized=%d/%d fields}",
                    collectionName, super.size(), fieldNodes.size());
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

        public int getTotalFields() {
            return totalFields;
        }

        public int getMaterializedFields() {
            return materializedFields;
        }

        public double getEfficiencyRatio() {
            return efficiencyRatio;
        }

        public boolean isFullyMaterialized() {
            return fullyMaterialized;
        }

        public int getSavedFields() {
            return totalFields - materializedFields;
        }

        public double getMemorySavingsPercentage() {
            if (totalFields == 0)
                return 0.0;
            return (1.0 - efficiencyRatio) * 100.0;
        }

        @Override
        public String toString() {
            return String.format("MemoryStats{total=%d, materialized=%d, saved=%d, savings=%.1f%%}",
                    totalFields, materializedFields, getSavedFields(), getMemorySavingsPercentage());
        }
    }
}
