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
 * A lazy proxy for nested object fields that only materializes referenced sub-fields.
 * This enables hierarchical lazy generation where nested objects are also lazy.
 * This is a simple POJO that doesn't implement JsonNode for cleaner design.
 */
public class LazyObjectProxy {
    private final Map<String, DslNode> fieldNodes;
    private final Set<String> referencedPaths;
    private final DataGenerationVisitor visitor;
    private final Set<String> materializedFieldNames = new HashSet<>();
    private final String objectPath; // For debugging
    private final ObjectNode delegate;

    public LazyObjectProxy(Map<String, DslNode> fieldNodes,
                          Set<String> referencedPaths,
                          DataGenerationVisitor visitor,
                          String objectPath) {
        this.fieldNodes = new HashMap<>(fieldNodes);
        this.referencedPaths = referencedPaths != null ? referencedPaths : Set.of();
        this.visitor = visitor;
        this.objectPath = objectPath;
        this.delegate = JsonNodeFactory.instance.objectNode();

        // Materialize only referenced fields immediately
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
        String currentPath = objectPath.isEmpty() ? fieldName : objectPath + "." + fieldName;

        for (String referencedPath : referencedPaths) {
            if (referencedPath.equals(currentPath) || referencedPath.startsWith(currentPath + ".")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Materializes a specific field if not already materialized.
     */
    private void materializeField(String fieldName) {
        if (!materializedFieldNames.contains(fieldName)) {
            DslNode fieldNode = fieldNodes.get(fieldName);
            if (fieldNode != null) {
                JsonNode value;

                // If this field node represents a nested object and has sub-references,
                // create another LazyObjectProxy for it
                if (fieldNode instanceof com.github.eddranca.datagenerator.node.ObjectFieldNode) {
                    String nestedPath = objectPath.isEmpty() ? fieldName : objectPath + "." + fieldName;
                    Set<String> nestedReferences = getNestedReferences(nestedPath);

                    if (!nestedReferences.isEmpty()) {
                        // Create a lazy proxy for the nested object and get its materialized copy
                        var objectFieldNode = (com.github.eddranca.datagenerator.node.ObjectFieldNode) fieldNode;
                        LazyObjectProxy nestedLazyProxy = new LazyObjectProxy(
                            objectFieldNode.getFields(),
                            nestedReferences,
                            visitor,
                            nestedPath
                        );
                        value = nestedLazyProxy.getMaterializedCopy();
                    } else {
                        // No nested references, generate normally
                        value = fieldNode.accept(visitor);
                    }
                } else {
                    // Simple field, generate normally
                    value = fieldNode.accept(visitor);
                }

                delegate.set(fieldName, value);
                materializedFieldNames.add(fieldName);
            }
        }
    }

    /**
     * Gets all referenced paths that start with the given nested path.
     */
    private Set<String> getNestedReferences(String nestedPath) {
        Set<String> nestedRefs = new HashSet<>();
        String prefix = nestedPath + ".";

        for (String referencedPath : referencedPaths) {
            if (referencedPath.startsWith(prefix)) {
                nestedRefs.add(referencedPath);
            }
        }

        return nestedRefs;
    }

    public JsonNode get(String fieldName) {
        // If field is already materialized, return it
        if (materializedFieldNames.contains(fieldName)) {
            return delegate.get(fieldName);
        }

        // If not materialized and we have a field node, materialize it now
        if (fieldNodes.containsKey(fieldName)) {
            materializeField(fieldName);
            return delegate.get(fieldName);
        }

        // Field doesn't exist
        return null;
    }

    public JsonNode path(String fieldName) {
        JsonNode result = get(fieldName);
        return result != null ? result : delegate.missingNode();
    }

    /**
     * Materializes all remaining fields for complete object generation.
     */
    public void materializeAll() {
        for (String fieldName : fieldNodes.keySet()) {
            materializeField(fieldName);
        }
    }

    /**
     * Returns a new ObjectNode with all fields materialized.
     */
    public ObjectNode getMaterializedCopy() {
        ObjectNode materializedCopy = JsonNodeFactory.instance.objectNode();

        // Ensure all fields are materialized first
        materializeAll();

        // Copy all materialized values (they're already JsonNode objects in the delegate)
        for (String fieldName : fieldNodes.keySet()) {
            JsonNode value = delegate.get(fieldName);
            if (value != null) {
                materializedCopy.set(fieldName, value);
            }
        }

        return materializedCopy;
    }

    @Override
    public String toString() {
        return String.format("LazyObjectProxy{path=%s, materialized=%d/%d fields}",
            objectPath, materializedFieldNames.size(), fieldNodes.size());
    }

    /**
     * Checks if a field exists (either materialized or available to materialize).
     */
    public boolean has(String fieldName) {
        return fieldNodes.containsKey(fieldName) || delegate.has(fieldName);
    }

    /**
     * Returns the number of materialized fields.
     */
    public int size() {
        return delegate.size();
    }

    /**
     * Returns true if no fields are materialized yet.
     */
    public boolean isEmpty() {
        return delegate.isEmpty();
    }
}
