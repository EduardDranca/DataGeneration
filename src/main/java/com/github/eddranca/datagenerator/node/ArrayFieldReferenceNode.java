package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.visitor.AbstractGenerationContext;

import java.util.List;
import java.util.Optional;

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

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public Optional<String> getCollectionName() {
        return Optional.of(collectionName);
    }

    @Override
    public String getReferenceString() {
        return collectionName + "[*]." + fieldName;
    }

    @Override
    public JsonNode resolve(AbstractGenerationContext<?> context, JsonNode currentItem, List<JsonNode> filterValues) {
        // Use cached filtered collection for array field references
        List<JsonNode> collection = context.getFilteredCollectionForArrayField(collectionName, fieldName, filterValues);

        if (collection.isEmpty()) {
            if (filterValues != null && !filterValues.isEmpty()) {
                return context.handleFilteringFailure(
                    "Array field reference '" + getReferenceString() + "' has no valid values after filtering");
            }
            return context.getMapper().nullNode();
        }

        // Select an element and extract the field (supporting nested paths)
        JsonNode selected = context.getElementFromCollection(collection, this, sequential);
        JsonNode fieldValue = extractNestedField(selected, fieldName);

        return fieldValue.isMissingNode() ? context.getMapper().nullNode() : fieldValue;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitArrayFieldReference(this);
    }
}
