package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.node.DslNode;

import java.util.Map;
import java.util.Set;

/**
 * A lazy proxy for nested object fields that only materializes referenced sub-fields.
 * This enables hierarchical lazy generation where nested objects are also lazy.
 * This is a simple POJO that doesn't implement JsonNode for cleaner design.
 */
public class LazyObjectProxy extends AbstractLazyProxy {
    private final String objectPath; // For debugging and path resolution

    public LazyObjectProxy(Map<String, DslNode> fieldNodes,
                          Set<String> referencedPaths,
                          DataGenerationVisitor visitor,
                          String objectPath) {
        super(fieldNodes, referencedPaths, visitor);
        this.objectPath = objectPath;

        // Materialize only referenced fields immediately
        materializeReferencedFields();
    }

    @Override
    protected boolean shouldMaterializeField(String fieldName) {
        // If entire object is referenced, materialize everything
        if (referencedPaths.contains("*")) {
            return true;
        }

        // Check if this field or any nested path is referenced
        String currentPath = buildFieldPath(fieldName);
        return referencedPaths.contains(currentPath) || hasReferencesWithPrefix(currentPath);
    }

    @Override
    protected JsonNode generateFieldValue(String fieldName, DslNode fieldNode) {
        // If this field node represents a nested object and has sub-references,
        // create another LazyObjectProxy for it
        if (fieldNode instanceof com.github.eddranca.datagenerator.node.ObjectFieldNode) {
            String nestedPath = buildFieldPath(fieldName);
            Set<String> nestedReferences = getReferencesWithPrefix(nestedPath);

            if (!nestedReferences.isEmpty()) {
                // Create a lazy proxy for the nested object and get its materialized copy
                var objectFieldNode = (com.github.eddranca.datagenerator.node.ObjectFieldNode) fieldNode;
                LazyObjectProxy nestedLazyProxy = new LazyObjectProxy(
                    objectFieldNode.getFields(),
                    nestedReferences,
                    visitor,
                    nestedPath
                );
                return nestedLazyProxy.getMaterializedCopy();
            }
        }

        // Simple field or no nested references, generate normally
        return fieldNode.accept(visitor);
    }

    /**
     * Builds the full path for a field within this object's context.
     */
    private String buildFieldPath(String fieldName) {
        return objectPath.isEmpty() ? fieldName : objectPath + "." + fieldName;
    }



    /**
     * Returns a new ObjectNode with all fields materialized.
     */
    public ObjectNode getMaterializedCopy() {
        ObjectNode materializedCopy = JsonNodeFactory.instance.objectNode();

        // Ensure all fields are materialized first
        materializeAll();

        // Copy all fields from the delegate (including spread fields)
        delegate.fieldNames().forEachRemaining(fieldName -> {
            JsonNode value = delegate.get(fieldName);
            if (value != null) {
                materializedCopy.set(fieldName, value);
            }
        });

        return materializedCopy;
    }

    @Override
    public String toString() {
        return String.format("LazyObjectProxy{path=%s, materialized=%d/%d fields}",
            objectPath, materializedFieldNames.size(), fieldNodes.size());
    }


}
