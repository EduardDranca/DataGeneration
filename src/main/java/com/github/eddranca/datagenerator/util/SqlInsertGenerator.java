package com.github.eddranca.datagenerator.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.exception.SerializationException;

import java.util.Iterator;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for generating SQL INSERT statements from JsonNode data.
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
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" (");

        StringJoiner columns = new StringJoiner(", ");
        StringJoiner values = new StringJoiner(", ");

        Iterator<Map.Entry<String, JsonNode>> fields = item.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode val = field.getValue();

            columns.add(fieldName);

            if (val.isNull()) {
                values.add("NULL");
            } else if (val.isNumber()) {
                values.add(val.asText());
            } else if (val.isBoolean()) {
                values.add(val.asText());
            } else if (val.isTextual()) {
                String escaped = val.asText().replace("'", "''");
                values.add("'" + escaped + "'");
            } else if (val.isObject() || val.isArray()) {
                // Handle complex objects by converting to JSON string
                logger.log(Level.WARNING,
                    "Complex object detected in table ''{0}'', field ''{1}''. " +
                        "Converting to JSON string representation for SQL insert. " +
                        "Consider using a database with native JSON support for optimal performance.",
                    new Object[]{tableName, fieldName});

                try {
                    String jsonString = mapper.writeValueAsString(val);
                    String escaped = jsonString.replace("'", "''");
                    values.add("'" + escaped + "'");
                } catch (JsonProcessingException e) {
                    logger.log(Level.SEVERE,
                        "Failed to serialize complex object to JSON for table ''{0}'', field ''{1}''",
                        new Object[]{tableName, fieldName});
                    throw new SerializationException("Failed to serialize complex object to JSON", e);
                }
            } else {
                // Fallback for any other JsonNode types
                String escaped = val.asText().replace("'", "''");
                values.add("'" + escaped + "'");
            }
        }

        sql.append(columns).append(") VALUES (").append(values).append(");");
        return sql.toString();
    }
}
