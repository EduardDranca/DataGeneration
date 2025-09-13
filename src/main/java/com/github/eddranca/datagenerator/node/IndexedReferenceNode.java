package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.visitor.GenerationContext;

import java.util.List;

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

    public IndexedReferenceNode(String collectionName, String index, String fieldName, boolean sequential) {
        super(sequential);
        this.collectionName = collectionName;
        this.index = index;
        this.fieldName = fieldName != null ? fieldName : "";
        this.isWildcardIndex = "*".equals(index);
        this.numericIndex = isWildcardIndex ? null : parseNumericIndex(index);
    }

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

    public String getCollectionName() {
        return collectionName;
    }

    public String getIndex() {
        return index;
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean hasFieldName() {
        return !fieldName.isEmpty();
    }

    public boolean isWildcardIndex() {
        return isWildcardIndex;
    }

    public boolean isNumericIndex() {
        return !isWildcardIndex;
    }

    public Integer getNumericIndex() {
        return numericIndex;
    }

    @Override
    public String getReferenceString() {
        String base = collectionName + "[" + index + "]";
        return hasFieldName() ? base + "." + fieldName : base;
    }

    @Override
    public JsonNode resolve(GenerationContext context, JsonNode currentItem, List<JsonNode> filterValues) {
        List<JsonNode> collection = context.getCollection(collectionName);
        
        if (isWildcardIndex()) {
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
            return hasFieldName() ? selected.path(fieldName) : selected;
        } else {
            // Numeric index - direct access
            if (numericIndex >= collection.size()) {
                return context.getMapper().nullNode();
            }
            
            JsonNode selected = collection.get(numericIndex);
            JsonNode value = hasFieldName() ? selected.path(fieldName) : selected;
            
            // Check filtering for numeric index
            if (filterValues != null && !filterValues.isEmpty() && filterValues.contains(value)) {
                return context.handleFilteringFailure("Indexed reference '" + getReferenceString() + "' value matches filter");
            }
            
            return value;
        }
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitIndexedReference(this);
    }
}