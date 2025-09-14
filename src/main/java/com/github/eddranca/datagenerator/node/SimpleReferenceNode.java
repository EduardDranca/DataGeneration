package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.visitor.GenerationContext;

import java.util.List;

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
    public String getReferenceString() {
        return hasFieldName() ? collectionName + "." + fieldName : collectionName;
    }

    @Override
    public JsonNode resolve(GenerationContext context, JsonNode currentItem, List<JsonNode> filterValues) {
        // Get the collection
        List<JsonNode> collection = context.getCollection(collectionName);

        // Apply filtering if needed
        if (filterValues != null && !filterValues.isEmpty()) {
            collection = context.applyFiltering(collection, hasFieldName() ? fieldName : "", filterValues);
            if (collection.isEmpty()) {
                return context.handleFilteringFailure("Simple reference '" + getReferenceString() + "' has no valid values after filtering");
            }
        }

        if (collection.isEmpty()) {
            return context.getMapper().nullNode();
        }

        // Select an element
        JsonNode selected = context.getElementFromCollection(collection, this, sequential);

        // Extract field if specified (supporting nested paths)
        return hasFieldName() ? NestedPathUtils.extractNestedField(selected, fieldName) : selected;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitSimpleReference(this);
    }
}
