package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.util.FieldApplicationUtil;

import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class for lazy proxies that provides common field materialization logic.
 * This reduces code duplication between LazyItemProxy and LazyObjectProxy.
 */
abstract class AbstractLazyProxy {
    protected final Map<String, DslNode> fieldNodes;
    protected final Set<String> referencedPaths;
    protected final DataGenerationVisitor<LazyItemProxy> visitor;
    protected final Set<String> materializedFieldNames = new HashSet<>();
    protected final ObjectNode delegate;

    protected AbstractLazyProxy(Map<String, DslNode> fieldNodes,
                                Set<String> referencedPaths,
                                DataGenerationVisitor<LazyItemProxy> visitor) {
        this.fieldNodes = new LinkedHashMap<>(fieldNodes);
        this.referencedPaths = referencedPaths != null ? referencedPaths : Set.of();
        this.visitor = visitor;
        this.delegate = JsonNodeFactory.instance.objectNode();
    }

    /**
     * Materializes only the fields that are referenced by other collections.
     */
    protected void materializeReferencedFields() {
        for (String fieldName : fieldNodes.keySet()) {
            if (shouldMaterializeField(fieldName)) {
                materializeField(fieldName);
            }
        }
    }

    /**
     * Determines if a field should be materialized based on reference analysis.
     * Subclasses can override this for specific path resolution logic.
     */
    protected abstract boolean shouldMaterializeField(String fieldName);

    /**
     * Materializes a specific field if not already materialized.
     */
    protected void materializeField(String fieldName) {
        if (!materializedFieldNames.contains(fieldName)) {
            DslNode fieldNode = fieldNodes.get(fieldName);
            if (fieldNode != null) {
                JsonNode value = generateFieldValue(fieldName, fieldNode);
                // Skip shadow binding fields from output (they start with $)
                if (!fieldName.startsWith("$")) {
                    FieldApplicationUtil.applyFieldToObject(delegate, fieldName, fieldNode, value);
                }
                materializedFieldNames.add(fieldName);
            }
        }
    }

    /**
     * Generates the value for a specific field.
     * Subclasses can override this for specific field generation logic.
     */
    protected abstract JsonNode generateFieldValue(String fieldName, DslNode fieldNode);

    /**
     * Gets all referenced paths that start with the given prefix.
     */
    protected Set<String> getReferencesWithPrefix(String prefix) {
        Set<String> matchingRefs = new HashSet<>();
        String prefixWithDot = prefix + ".";

        for (String referencedPath : referencedPaths) {
            if (referencedPath.startsWith(prefixWithDot)) {
                matchingRefs.add(referencedPath);
            }
        }

        return matchingRefs;
    }

    /**
     * Checks if there are any referenced paths that start with the given prefix.
     */
    protected boolean hasReferencesWithPrefix(String prefix) {
        String prefixWithDot = prefix + ".";
        return referencedPaths.stream().anyMatch(path -> path.startsWith(prefixWithDot));
    }

    /**
     * Materializes all remaining fields.
     */
    public void materializeAll() {
        for (String fieldName : fieldNodes.keySet()) {
            materializeField(fieldName);
        }
    }
}
