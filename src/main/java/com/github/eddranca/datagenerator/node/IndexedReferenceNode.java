package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.visitor.AbstractGenerationContext;

import java.util.List;
import java.util.Optional;

/**
 * Reference node for indexed references like "collection[0]" or "collection[*]".
 * Handles both specific numeric indices and wildcard indices.
 */
public class IndexedReferenceNode extends AbstractReferenceNode {
    private final String collectionName;
    private final String index; // Either a number or "*"
    private final String fieldName; // Optional field to extract from the referenced item
    private final boolean isWildcardIndex;
    private final Integer numericIndex; // Parsed numeric index, null for wildcards

    public IndexedReferenceNode(String collectionName, String index, String fieldName,
                                List<FilterNode> filters, boolean sequential) {
        super(filters, sequential);
        this.collectionName = collectionName;
        this.index = index;
        this.fieldName = fieldName != null ? fieldName : "";
        this.isWildcardIndex = "*".equals(index);
        this.numericIndex = isWildcardIndex ? null : parseNumericIndex(index);
    }

    private Integer parseNumericIndex(String index) {
        try {
            return Integer.parseInt(index);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric index: " + index);
        }
    }

    public boolean hasFieldName() {
        return !fieldName.isEmpty();
    }

    public boolean isWildcardIndex() {
        return isWildcardIndex;
    }

    @Override
    public Optional<String> getCollectionName() {
        return Optional.of(collectionName);
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getIndex() {
        return index;
    }

    @Override
    public String getReferenceString() {
        String base = collectionName + "[" + index + "]";
        return hasFieldName() ? base + "." + fieldName : base;
    }

    @Override
    public JsonNode resolve(AbstractGenerationContext<?> context, JsonNode currentItem, List<JsonNode> filterValues) {
        List<JsonNode> collection = context.getCollection(collectionName);

        if (isWildcardIndex()) {
            return resolveWildcardIndex(context, collection, filterValues);
        } else {
            return resolveNumericIndex(context, collection, filterValues);
        }
    }

    private JsonNode resolveWildcardIndex(AbstractGenerationContext<?> context, List<JsonNode> collection, List<JsonNode> filterValues) {
        // Apply filtering for wildcard index
        if (filterValues != null && !filterValues.isEmpty()) {
            collection = context.applyFiltering(collection, hasFieldName() ? fieldName : "", filterValues);
            if (collection.isEmpty()) {
                return context.handleFilteringFailure("Indexed reference '" + getReferenceString() + "' has no valid values after filtering");
            }
        }

        if (collection.isEmpty()) {
            return context.getMapper().nullNode();
        }

        // Select an element
        JsonNode selected = context.getElementFromCollection(collection, this, sequential);
        return hasFieldName() ? extractNestedField(selected, fieldName) : selected;
    }

    private JsonNode resolveNumericIndex(AbstractGenerationContext<?> context, List<JsonNode> collection, List<JsonNode> filterValues) {
        // Numeric index - direct access
        if (numericIndex >= collection.size()) {
            return context.getMapper().nullNode();
        }

        JsonNode selected = collection.get(numericIndex);
        JsonNode value = hasFieldName() ? extractNestedField(selected, fieldName) : selected;

        // Check filtering for numeric index
        if (filterValues != null && !filterValues.isEmpty() && filterValues.contains(value)) {
            return context.handleFilteringFailure("Indexed reference '" + getReferenceString() + "' value matches filter");
        }

        return value;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitIndexedReference(this);
    }
}
