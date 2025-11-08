package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.visitor.AbstractGenerationContext;

import java.util.ArrayList;
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
        // Get the collection
        List<JsonNode> collection = context.getCollection(getCollectionNameString());

        // Apply conditional filtering
        List<JsonNode> filteredCollection = applyConditions(collection);

        // Apply additional filters if needed
        if (filterValues != null && !filterValues.isEmpty()) {
            filteredCollection = context.applyFiltering(filteredCollection, hasFieldName() ? fieldName : "", filterValues);
            if (filteredCollection.isEmpty()) {
                return context.handleFilteringFailure("Conditional reference '" + getReferenceString() + "' has no valid values after filtering");
            }
        }

        if (filteredCollection.isEmpty()) {
            return context.handleFilteringFailure("Conditional reference '" + getReferenceString() + "' matched no items");
        }

        // Select an element
        JsonNode selected = context.getElementFromCollection(filteredCollection, this, sequential);

        // Extract field if specified (supporting nested paths)
        return hasFieldName() ? extractNestedField(selected, fieldName) : selected;
    }

    /**
     * Filters the collection based on the condition.
     * Only items that match the condition are included.
     */
    private List<JsonNode> applyConditions(List<JsonNode> collection) {
        List<JsonNode> result = new ArrayList<>();
        
        for (JsonNode item : collection) {
            if (condition.matches(item)) {
                result.add(item);
            }
        }
        
        return result;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitConditionalReference(this);
    }
}
