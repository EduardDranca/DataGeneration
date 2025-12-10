package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.visitor.AbstractGenerationContext;

import java.util.List;
import java.util.Optional;

/**
 * Reference node for simple collection references like "users" or "collection.field".
 * This handles direct references to collections or fields within collections.
 */
public class SimpleReferenceNode extends AbstractReferenceNode {
    private final String collectionName;
    private final String fieldName; // Optional field to extract from the referenced item

    public SimpleReferenceNode(String collectionName, String fieldName,
                               List<FilterNode> filters, boolean sequential) {
        super(filters, sequential);
        this.collectionName = collectionName;
        this.fieldName = fieldName != null ? fieldName : "";
    }

    public boolean hasFieldName() {
        return !fieldName.isEmpty();
    }

    @Override
    public Optional<String> getCollectionName() {
        return Optional.of(collectionName);
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String getReferenceString() {
        return hasFieldName() ? collectionName + "." + fieldName : collectionName;
    }

    @Override
    public JsonNode resolve(AbstractGenerationContext<?> context, JsonNode currentItem, List<JsonNode> filterValues) {
        // Use cached filtered collection if filtering is needed
        List<JsonNode> collection;
        if (filterValues != null && !filterValues.isEmpty()) {
            collection = context.getFilteredCollection(collectionName, null, filterValues, hasFieldName() ? fieldName : "");
            if (collection.isEmpty()) {
                return context.handleFilteringFailure("Simple reference '" + getReferenceString() + "' has no valid values after filtering");
            }
        } else {
            // No filtering needed, get collection directly
            collection = context.getCollection(collectionName);
        }

        if (collection.isEmpty()) {
            return context.getMapper().nullNode();
        }

        // Select an element
        JsonNode selected = context.getElementFromCollection(collection, this, sequential);

        // Extract field if specified (supporting nested paths)
        return hasFieldName() ? extractNestedField(selected, fieldName) : selected;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitSimpleReference(this);
    }
}
