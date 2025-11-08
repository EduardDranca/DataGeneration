package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.GeneratedFieldNode;
import com.github.eddranca.datagenerator.node.ObjectFieldNode;
import com.github.eddranca.datagenerator.node.OptionReferenceNode;
import com.github.eddranca.datagenerator.node.SelfReferenceNode;

import java.util.Map;
import java.util.Set;

/**
 * A lazy proxy that only materializes fields on-demand.
 * This allows us to keep only referenced fields in memory during generation
 * and generate the rest when needed for output.
 * <p>
 * Supports nested path materialization for complex object hierarchies.
 * This is a simple POJO that doesn't implement JsonNode for cleaner design.
 */
public class LazyItemProxy extends AbstractLazyProxy {
    private final String collectionName;
    private boolean fullyMaterialized = false;

    public LazyItemProxy(String collectionName,
                         Map<String, DslNode> fieldNodes,
                         Set<String> referencedPaths,
                         DataGenerationVisitor<LazyItemProxy> visitor) {
        super(fieldNodes, referencedPaths, visitor);
        this.collectionName = collectionName;

        // First, materialize fields that are referenced by runtime options
        // This ensures they're available when generating fields that depend on them
        materializeRuntimeOptionDependencies();

        // Then generate only referenced fields immediately
        materializeReferencedFields();
    }

    /**
     * Materializes fields that are referenced by runtime-computed options.
     * This ensures that when a field uses runtime options like {"ref": "this.otherField"},
     * the referenced field is already materialized and available.
     */
    private void materializeRuntimeOptionDependencies() {
        for (Map.Entry<String, DslNode> entry : fieldNodes.entrySet()) {
            DslNode fieldNode = entry.getValue();
            if (fieldNode instanceof com.github.eddranca.datagenerator.node.GeneratedFieldNode genField) {
                if (genField.getOptions().hasRuntimeOptions()) {
                    // This field has runtime options - materialize any self-referenced fields first
                    for (com.github.eddranca.datagenerator.node.OptionReferenceNode optionRef : 
                            genField.getOptions().getRuntimeOptions().values()) {
                        if (optionRef.getReference() instanceof com.github.eddranca.datagenerator.node.SelfReferenceNode selfRef) {
                            // Extract the field name from the self-reference
                            String referencedFieldName = selfRef.getFieldName();
                            // Handle nested paths like "data.baseValue" - just get the first part
                            if (referencedFieldName.contains(".")) {
                                referencedFieldName = referencedFieldName.split("\\.")[0];
                            }
                            // Materialize this field if it exists and hasn't been materialized yet
                            if (fieldNodes.containsKey(referencedFieldName)) {
                                materializeField(referencedFieldName);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean shouldMaterializeField(String fieldName) {
        // If entire object is referenced, materialize everything
        if (referencedPaths.contains("*")) {
            return true;
        }

        // Check if this field or any nested path is referenced
        return referencedPaths.contains(fieldName) || hasReferencesWithPrefix(fieldName);
    }

    @Override
    protected JsonNode generateFieldValue(String fieldName, DslNode fieldNode) {
        // If this is an ObjectFieldNode and has nested references, create a LazyObjectProxy
        if (fieldNode instanceof ObjectFieldNode objectFieldNode && hasReferencesWithPrefix(fieldName)) {
            Set<String> nestedReferences = getReferencesWithPrefix(fieldName);

            LazyObjectProxy lazyObjectProxy = new LazyObjectProxy(
                objectFieldNode.getFields(),
                nestedReferences,
                visitor,
                fieldName);

            // Get the materialized copy for storage in the delegate
            return lazyObjectProxy.getMaterializedCopy();
        } else {
            // Set current item context for self-references
            ObjectNode previousItem = visitor.getCurrentItem();
            try {
                visitor.setCurrentItem(delegate);
                return fieldNode.accept(visitor);
            } finally {
                visitor.setCurrentItem(previousItem);
            }
        }
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
            materializeAll();
            fullyMaterialized = true;
        }

        // Copy all fields from the delegate (including spread fields)
        // For streaming efficiency, we'll use the materialized values directly
        // instead of creating deep copies of LazyObjectProxy instances
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
        if (fullyMaterialized) {
            return delegate.toString();
        } else {
            return String.format("LazyItemProxy{collection=%s, materialized=%d/%d fields}",
                collectionName, delegate.size(), fieldNodes.size());
        }
    }


}
