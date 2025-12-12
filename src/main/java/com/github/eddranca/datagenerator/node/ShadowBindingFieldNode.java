package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.visitor.AbstractGenerationContext;

import java.util.List;
import java.util.Optional;

/**
 * Node representing a reference to a field within a shadow binding.
 * Used for references like {"ref": "$user.id"} which extract a field
 * from a previously defined shadow binding.
 */
public class ShadowBindingFieldNode extends AbstractReferenceNode {
    private final String bindingName;
    private final String fieldPath;

    public ShadowBindingFieldNode(String bindingName, String fieldPath, List<FilterNode> filters, boolean sequential) {
        super(filters, sequential);
        this.bindingName = bindingName;
        this.fieldPath = fieldPath;
    }

    public String getBindingName() {
        return bindingName;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    @Override
    public String getReferenceString() {
        return bindingName + "." + fieldPath;
    }

    @Override
    public Optional<String> getCollectionName() {
        // Shadow binding field references don't directly reference a collection
        return Optional.empty();
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitShadowBindingField(this);
    }

    @Override
    public JsonNode resolve(AbstractGenerationContext<?> context, JsonNode currentItem, List<JsonNode> filterValues) {
        // Resolution is handled by DataGenerationVisitor which has access to shadow bindings
        throw new UnsupportedOperationException(
            "ShadowBindingFieldNode.resolve() should not be called directly. " +
            "Use DataGenerationVisitor.visitShadowBindingField() instead."
        );
    }
}
