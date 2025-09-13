package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.visitor.GenerationContext;

import java.util.List;

/**
 * Reference node for array field references like "collection[*].field".
 * This is a specialized case where we want to extract a specific field from
 * a randomly/sequentially selected item in a collection.
 */
public class ArrayFieldReferenceNode extends AbstractReferenceNode {
    private final String collectionName;
    private final String fieldName;

    public ArrayFieldReferenceNode(String collectionName, String fieldName, boolean sequential) {
        super(sequential);
        this.collectionName = collectionName;
        this.fieldName = fieldName;
    }

    public ArrayFieldReferenceNode(String collectionName, String fieldName,
            List<FilterNode> filters, boolean sequential) {
        super(filters, sequential);
        this.collectionName = collectionName;
        this.fieldName = fieldName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String getReferenceString() {
        return collectionName + "[*]." + fieldName;
    }

    @Override
    public JsonNode resolve(GenerationContext context, JsonNode currentItem, List<JsonNode> filterValues) {
        List<JsonNode> collection = context.getCollection(collectionName);

        // Apply filtering based on the field values
        if (filterValues != null && !filterValues.isEmpty()) {
            collection = context.applyFilteringOnField(collection, fieldName, filterValues);
            if (collection.isEmpty()) {
                return context.handleFilteringFailure(
                        "Array field reference '" + getReferenceString() + "' has no valid values after filtering");
            }
        }

        if (collection.isEmpty()) {
            return context.getMapper().nullNode();
        }

        // Select an element and extract the field
        JsonNode selected = context.getElementFromCollection(collection, this, sequential);
        JsonNode fieldValue = selected.path(fieldName);

        return fieldValue.isMissingNode() ? context.getMapper().nullNode() : fieldValue;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitArrayFieldReference(this);
    }
}