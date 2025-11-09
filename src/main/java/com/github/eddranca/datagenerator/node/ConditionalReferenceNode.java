package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.visitor.AbstractGenerationContext;

import java.util.List;
import java.util.Optional;

/**
 * Reference node that filters collection items based on conditions.
 * Supports syntax like: products[category='Electronics'].id
 */
public class ConditionalReferenceNode extends AbstractReferenceNode {
    private final String collectionName;
    private final String fieldName; // Optional field to extract from the referenced item
    private final Condition condition; // Condition to match

    public ConditionalReferenceNode(String collectionName, String fieldName,
                                   Condition condition,
                                   List<FilterNode> filters, boolean sequential) {
        super(filters, sequential);
        this.collectionName = collectionName;
        this.fieldName = fieldName != null ? fieldName : "";
        this.condition = condition;
    }

    public boolean hasFieldName() {
        return !fieldName.isEmpty();
    }

    @Override
    public Optional<String> getCollectionName() {
        return Optional.of(collectionName);
    }

    public String getCollectionNameString() {
        return collectionName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Condition getCondition() {
        return condition;
    }

    @Override
    public String getReferenceString() {
        String ref = collectionName + "[" + condition.toConditionString() + "]";
        return hasFieldName() ? ref + "." + fieldName : ref;
    }

    @Override
    public JsonNode resolve(AbstractGenerationContext<?> context, JsonNode currentItem, List<JsonNode> filterValues) {
        // Use cached filtered collection (handles both condition and filter values)
        List<JsonNode> filteredCollection = context.getFilteredCollection(
            getCollectionNameString(),
            condition,
            filterValues,
            hasFieldName() ? fieldName : ""
        );

        if (filteredCollection.isEmpty()) {
            if (filterValues != null && !filterValues.isEmpty()) {
                return context.handleFilteringFailure("Conditional reference '" + getReferenceString() + "' has no valid values after filtering");
            } else {
                return context.handleFilteringFailure("Conditional reference '" + getReferenceString() + "' matched no items");
            }
        }

        // Select an element
        JsonNode selected = context.getElementFromCollection(filteredCollection, this, sequential);

        // Extract field if specified (supporting nested paths)
        return hasFieldName() ? extractNestedField(selected, fieldName) : selected;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitConditionalReference(this);
    }
}
