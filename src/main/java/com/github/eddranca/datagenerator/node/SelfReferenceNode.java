package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.visitor.AbstractGenerationContext;

import java.util.List;
import java.util.Optional;

import static com.github.eddranca.datagenerator.builder.KeyWords.THIS_PREFIX;

/**
 * Reference node for self-references like "this.field".
 * These references point to fields within the current item being generated.
 */
public class SelfReferenceNode extends AbstractReferenceNode {
    private final String fieldName;

    public SelfReferenceNode(String fieldName, List<FilterNode> filters, boolean sequential) {
        super(filters, sequential);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public Optional<String> getCollectionName() {
        return Optional.empty();
    }

    @Override
    public String getReferenceString() {
        return THIS_PREFIX + fieldName;
    }

    @Override
    public JsonNode resolve(AbstractGenerationContext<?> context, JsonNode currentItem, List<JsonNode> filterValues) {
        if (currentItem == null) {
            return context.getMapper().nullNode();
        }

        JsonNode value = extractNestedField(currentItem, fieldName);

        // Self-references typically don't need filtering since they reference the current item
        // But if filtering is requested, we can check if the value matches any filter
        if (filterValues != null && !filterValues.isEmpty() && filterValues.contains(value)) {
            return context.handleFilteringFailure("Self reference '" + getReferenceString() + "' value matches filter");
        }

        return value.isMissingNode() ? context.getMapper().nullNode() : value;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitSelfReference(this);
    }
}
