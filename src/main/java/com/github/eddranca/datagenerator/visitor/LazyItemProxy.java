package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.ObjectFieldNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A lazy proxy that only materializes fields on-demand.
 * This allows us to keep only referenced fields in memory during generation
 * and generate the rest when needed for output.
 *
 * Supports nested path materialization for complex object hierarchies.
 * This is a simple POJO that doesn't implement JsonNode for cleaner design.
 */
public class LazyItemProxy {
    private final Map<String, DslNode> fieldNodes;
    private final Set<String> referencedPaths;
    private final String collectionName;
    private final DataGenerationVisitor visitor;
    private final Set<String> materializedFieldNames = new HashSet<>();
    private final ObjectNode delegate;
    private boolean fullyMaterialized = false;

    public LazyItemProxy(String collectionName,
            Map<String, DslNode> fieldNodes,
            Set<String> referencedPaths,
            DataGenerationVisitor visitor) {
        this.collectionName = collectionName;
        this.referencedPaths = referencedPaths;
        this.visitor = visitor;
        this.fieldNodes = new HashMap<>(fieldNodes);
        this.delegate = JsonNodeFactory.instance.objectNode();

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
                // LazyObjectProxy and get its materialized copy
                if (fieldNode instanceof ObjectFieldNode objectFieldNode && hasNestedReferences(fieldName)) {
                    Set<String> nestedReferences = getNestedReferences(fieldName);

                    LazyObjectProxy lazyObjectProxy = new LazyObjectProxy(
                            objectFieldNode.getFields(),
                            nestedReferences,
                            visitor,
                            fieldName);
                    
                    // Get the materialized copy for storage in the delegate
                    value = lazyObjectProxy.getMaterializedCopy();
                } else {
                    // Generate normally for simple fields or non-referenced nested objects
                    value = fieldNode.accept(visitor);
                }

                delegate.set(fieldName, value);
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
        // Similar to get() but returns missing node instead of null
        JsonNode result = get(fieldName);
        return result != null ? result : delegate.missingNode();
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
            JsonNode value = delegate.get(fieldName);
            if (value != null) {
                materializedCopy.set(fieldName, value);
            }
        }

        return materializedCopy;
    }

    @Override
    public String toString() {
        if (fullyMaterialized) {
            return delegate.toString();
        } else {
            return String.format("LazyItemProxy{collection=%s, materialized=%d/%d fields}",
                    collectionName, delegate.size(), fieldNodes.size());
        }
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
