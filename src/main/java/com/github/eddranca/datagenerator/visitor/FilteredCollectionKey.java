package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.node.Condition;

import java.util.List;
import java.util.Objects;

/**
 * Cache key for filtered collections.
 * <p>
 * This key uniquely identifies a filtered collection based on:
 * - The source collection name
 * - The condition applied (for conditional references)
 * - The filter values (for filtering)
 * - The field name (if extracting a field before filtering)
 * <p>
 * The cache key is designed to be immutable and efficiently hashable.
 * It uses identity-based comparison for Condition objects since they are
 * part of the DSL tree structure and represent unique filtering logic.
 */
public final class FilteredCollectionKey {
    private final String collectionName;
    private final Condition condition; // null for non-conditional references
    private final List<JsonNode> filterValues; // null if no filtering
    private final String fieldName; // empty string if no field extraction
    private final int hashCode;

    /**
     * Creates a cache key for a filtered collection.
     *
     * @param collectionName the name of the source collection
     * @param condition      the condition to apply (null for simple references)
     * @param filterValues   the values to filter out (null if no filtering)
     * @param fieldName      the field to extract (empty string if none)
     */
    public FilteredCollectionKey(String collectionName, Condition condition,
                                 List<JsonNode> filterValues, String fieldName) {
        this.collectionName = collectionName;
        this.condition = condition;
        this.filterValues = filterValues;
        this.fieldName = fieldName != null ? fieldName : "";
        this.hashCode = computeHashCode();
    }

    private int computeHashCode() {
        // Use identity hash for condition since it's part of the DSL tree
        int conditionHash = condition != null ? System.identityHashCode(condition) : 0;
        return Objects.hash(collectionName, conditionHash, filterValues, fieldName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        FilteredCollectionKey that = (FilteredCollectionKey) obj;

        // Use identity comparison for condition (same DSL node = same filtering logic)
        return Objects.equals(collectionName, that.collectionName) &&
               condition == that.condition &&
               Objects.equals(filterValues, that.filterValues) &&
               Objects.equals(fieldName, that.fieldName);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FilteredCollectionKey{");
        sb.append("collection='").append(collectionName).append('\'');
        if (condition != null) {
            sb.append(", condition=").append(condition.toConditionString());
        }
        if (filterValues != null && !filterValues.isEmpty()) {
            sb.append(", filters=").append(filterValues.size());
        }
        if (!fieldName.isEmpty()) {
            sb.append(", field='").append(fieldName).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }
}
