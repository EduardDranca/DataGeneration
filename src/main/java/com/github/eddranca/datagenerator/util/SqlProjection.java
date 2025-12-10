package com.github.eddranca.datagenerator.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration for SQL generation projections and data type mappings.
 * Allows specifying which fields to include in SQL INSERTs and their SQL data types.
 */
public class SqlProjection {
    private final Set<String> includedFields;
    private final Map<String, String> fieldTypes;

    private SqlProjection(Builder builder) {
        this.includedFields = builder.includedFields != null 
            ? Collections.unmodifiableSet(builder.includedFields) 
            : null;
        this.fieldTypes = builder.fieldTypes != null 
            ? Collections.unmodifiableMap(builder.fieldTypes) 
            : Collections.emptyMap();
    }

    /**
     * Creates a new builder for SqlProjection.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if a field should be included in the SQL output.
     * If no fields are specified, all fields are included.
     */
    public boolean shouldIncludeField(String fieldName) {
        return includedFields == null || includedFields.contains(fieldName);
    }

    /**
     * Gets the SQL data type for a field, or null if not specified.
     */
    public String getFieldType(String fieldName) {
        return fieldTypes.get(fieldName);
    }

    /**
     * Returns true if any field types are specified.
     */
    public boolean hasFieldTypes() {
        return !fieldTypes.isEmpty();
    }

    public static class Builder {
        private Set<String> includedFields;
        private Map<String, String> fieldTypes;

        /**
         * Specifies which fields to include in SQL output.
         * If not called, all fields are included.
         */
        public Builder includeFields(Set<String> fields) {
            this.includedFields = fields;
            return this;
        }

        /**
         * Specifies SQL data types for fields.
         * Used to format values appropriately (e.g., DATE vs VARCHAR).
         */
        public Builder withFieldTypes(Map<String, String> types) {
            this.fieldTypes = types;
            return this;
        }

        /**
         * Adds a single field type mapping.
         */
        public Builder withFieldType(String fieldName, String sqlType) {
            if (this.fieldTypes == null) {
                this.fieldTypes = new HashMap<>();
            }
            this.fieldTypes.put(fieldName, sqlType);
            return this;
        }

        public SqlProjection build() {
            return new SqlProjection(this);
        }
    }
}
