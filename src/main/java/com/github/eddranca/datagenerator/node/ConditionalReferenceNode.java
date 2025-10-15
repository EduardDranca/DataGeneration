package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.visitor.AbstractGenerationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference node that filters collection items based on conditions.
 * Supports syntax like: products[category='Electronics'].id
 */
public class ConditionalReferenceNode extends AbstractReferenceNode {
    private final String collectionName;
    private final String fieldName; // Optional field to extract from the referenced item
    private final List<Condition> conditions; // Conditions to match

    public ConditionalReferenceNode(String collectionName, String fieldName,
                                   List<Condition> conditions,
                                   List<FilterNode> filters, boolean sequential) {
        super(filters, sequential);
        this.collectionName = collectionName;
        this.fieldName = fieldName != null ? fieldName : "";
        this.conditions = new ArrayList<>(conditions);
    }

    public boolean hasFieldName() {
        return !fieldName.isEmpty();
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public List<Condition> getConditions() {
        return new ArrayList<>(conditions);
    }

    @Override
    public String getReferenceString() {
        StringBuilder sb = new StringBuilder(collectionName);
        sb.append("[");
        boolean first = true;
        for (Condition condition : conditions) {
            if (!first) sb.append(" AND ");
            sb.append(condition.toConditionString());
            first = false;
        }
        sb.append("]");
        if (hasFieldName()) {
            sb.append(".").append(fieldName);
        }
        return sb.toString();
    }

    @Override
    public JsonNode resolve(AbstractGenerationContext<?> context, JsonNode currentItem, List<JsonNode> filterValues) {
        // Get the collection
        List<JsonNode> collection = context.getCollection(collectionName);

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
     * Filters the collection based on the conditions.
     * Only items where all conditions match are included.
     */
    private List<JsonNode> applyConditions(List<JsonNode> collection) {
        List<JsonNode> result = new ArrayList<>();
        
        for (JsonNode item : collection) {
            if (matchesAllConditions(item)) {
                result.add(item);
            }
        }
        
        return result;
    }

    /**
     * Checks if an item matches all conditions (AND logic).
     */
    private boolean matchesAllConditions(JsonNode item) {
        for (Condition condition : conditions) {
            if (!condition.matches(item)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitConditionalReference(this);
    }
}
