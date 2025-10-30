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
    private final String index; // Either a number, a range, or "*"
    private final String fieldName; // Optional field to extract from the referenced item
    private final boolean isWildcardIndex;
    private final boolean isRangeIndex;
    private final Integer numericIndex; // Parsed numeric index, null for wildcards or ranges
    private final Integer rangeStartRaw; // May be null (open start)
    private final Integer rangeEndRaw;   // May be null (open end)

    public IndexedReferenceNode(String collectionName, String index, String fieldName,
                                List<FilterNode> filters, boolean sequential) {
        super(filters, sequential);
        this.collectionName = collectionName;
        this.index = index;
        this.fieldName = fieldName != null ? fieldName : "";
        this.isWildcardIndex = "*".equals(index);
        
        // Determine if this is a range syntax using colon separator
        // Range patterns: "0:99", "10:", ":99", "-10:-1", ":"
        boolean looksLikeRange = index.contains(":");
        
        this.isRangeIndex = !isWildcardIndex && looksLikeRange;

        if (isWildcardIndex) {
            this.numericIndex = null;
            this.rangeStartRaw = null;
            this.rangeEndRaw = null;
        } else if (isRangeIndex) {
            // Parse range using colon separator
            String[] parts = index.split(":", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid range format: " + index);
            }
            
            Integer start = null;
            Integer end = null;
            
            // Parse start (empty string means open start)
            if (!parts[0].isEmpty()) {
                start = Integer.parseInt(parts[0]);
            }
            
            // Parse end (empty string means open end)
            if (!parts[1].isEmpty()) {
                end = Integer.parseInt(parts[1]);
            }
            
            this.rangeStartRaw = start;
            this.rangeEndRaw = end;
            this.numericIndex = null;
        } else {
            this.numericIndex = parseNumericIndex(index);
            this.rangeStartRaw = null;
            this.rangeEndRaw = null;
        }
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
        } else if (isRangeIndex) {
            return resolveRangeIndex(context, collection, filterValues);
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

    private JsonNode resolveRangeIndex(AbstractGenerationContext<?> context, List<JsonNode> collection, List<JsonNode> filterValues) {
        int size = collection.size();
        if (size == 0) {
            return context.getMapper().nullNode();
        }

        // Resolve start
        Integer start;
        if (rangeStartRaw == null) {
            start = 0;
        } else if (rangeStartRaw < 0) {
            start = size + rangeStartRaw; // negative from end
        } else {
            start = rangeStartRaw;
        }

        // Resolve end (inclusive)
        Integer end;
        if (rangeEndRaw == null) {
            end = size - 1;
        } else if (rangeEndRaw < 0) {
            end = size + rangeEndRaw; // negative from end
        } else {
            end = rangeEndRaw;
        }

        // Clamp and validate
        if (start < 0) start = 0;
        if (end < 0) return context.getMapper().nullNode();
        if (start > size - 1) return context.getMapper().nullNode();
        if (end > size - 1) end = size - 1;
        if (start > end) return context.getMapper().nullNode();

        List<JsonNode> sub = collection.subList(start, end + 1);

        // Apply filtering within the range
        if (filterValues != null && !filterValues.isEmpty()) {
            sub = context.applyFiltering(sub, hasFieldName() ? fieldName : "", filterValues);
            if (sub.isEmpty()) {
                return context.handleFilteringFailure("Indexed reference '" + getReferenceString() + "' has no valid values after filtering");
            }
        }

        if (sub.isEmpty()) {
            return context.getMapper().nullNode();
        }

        JsonNode selected = context.getElementFromCollection(sub, this, sequential);
        return hasFieldName() ? extractNestedField(selected, fieldName) : selected;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitIndexedReference(this);
    }
}
