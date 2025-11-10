package com.github.eddranca.datagenerator.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.exception.SerializationException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for generating SQL INSERT statements from JsonNode data.
 * Supports field projections and data type-aware formatting.
 */
public final class SqlInsertGenerator {
    private static final Logger logger = Logger.getLogger(SqlInsertGenerator.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    private SqlInsertGenerator() {
        // Utility class
    }

    /**
     * Generates a single SQL INSERT statement for an item.
     *
     * @param tableName the name of the table
     * @param item      the JsonNode representing the item data
     * @return SQL INSERT statement
     * @throws SerializationException if JSON serialization fails for complex objects
     */
    public static String generateSqlInsert(String tableName, JsonNode item) {
        return generateSqlInsert(tableName, item, null);
    }

    /**
     * Generates a single SQL INSERT statement for an item with projection support.
     *
     * @param tableName  the name of the table
     * @param item       the JsonNode representing the item data
     * @param projection optional projection for field filtering and type mapping
     * @return SQL INSERT statement
     * @throws SerializationException if JSON serialization fails for complex objects
     */
    public static String generateSqlInsert(String tableName, JsonNode item, SqlProjection projection) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" (");

        StringJoiner columns = new StringJoiner(", ");
        StringJoiner values = new StringJoiner(", ");

        // Track complex fields found in this item
        Set<String> complexFields = new HashSet<>();

        Iterator<Map.Entry<String, JsonNode>> fields = item.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode val = field.getValue();

            // Skip fields not in projection
            if (projection != null && !projection.shouldIncludeField(fieldName)) {
                continue;
            }

            columns.add(fieldName);

            // Get SQL type for this field if specified
            String sqlType = projection != null ? projection.getFieldType(fieldName) : null;

            if (val.isNull()) {
                values.add("NULL");
            } else if (val.isNumber()) {
                values.add(formatNumericValue(val, sqlType));
            } else if (val.isBoolean()) {
                values.add(formatBooleanValue(val, sqlType));
            } else if (val.isTextual()) {
                values.add(formatTextValue(val.asText(), sqlType));
            } else if (val.isObject() || val.isArray()) {
                // Handle complex objects by converting to JSON string
                complexFields.add(fieldName);

                try {
                    String jsonString = mapper.writeValueAsString(val);
                    values.add(formatTextValue(jsonString, sqlType));
                } catch (JsonProcessingException e) {
                    logger.log(Level.SEVERE,
                        "Failed to serialize complex object to JSON for table ''{0}'', field ''{1}''",
                        new Object[]{tableName, fieldName});
                    throw new SerializationException("Failed to serialize complex object to JSON", e);
                }
            } else {
                // Fallback for any other JsonNode types
                values.add(formatTextValue(val.asText(), sqlType));
            }
        }

        sql.append(columns).append(") VALUES (").append(values).append(");");

        // Log complex fields once per table with all fields listed
        if (!complexFields.isEmpty()) {
            logger.log(Level.WARNING,
                "Complex objects detected in table ''{0}'', fields: {1}. " +
                    "Converting to JSON string representation for SQL insert. " +
                    "Consider using a database with native JSON support for optimal performance.",
                new Object[]{tableName, String.join(", ", complexFields)});
        }

        return sql.toString();
    }

    /**
     * Formats a numeric value based on SQL type.
     */
    private static String formatNumericValue(JsonNode val, String sqlType) {
        return val.asText();
    }

    /**
     * Formats a boolean value based on SQL type.
     */
    private static String formatBooleanValue(JsonNode val, String sqlType) {
        if (sqlType != null) {
            String upperType = sqlType.toUpperCase();
            // Handle different boolean representations
            if (upperType.contains("TINYINT") || upperType.contains("INT")) {
                return val.asBoolean() ? "1" : "0";
            }
        }
        return val.asText();
    }

    /**
     * Formats a text value based on SQL type.
     * Handles special formatting for DATE, TIMESTAMP, and other types.
     */
    private static String formatTextValue(String text, String sqlType) {
        String escaped = text.replace("'", "''");
        
        if (sqlType != null) {
            String upperType = sqlType.toUpperCase();
            
            // For DATE and TIMESTAMP types, use appropriate SQL functions if needed
            if (upperType.contains("DATE") || upperType.contains("TIMESTAMP")) {
                // Keep as string literal - databases will handle conversion
                return "'" + escaped + "'";
            }
        }
        
        return "'" + escaped + "'";
    }
}
