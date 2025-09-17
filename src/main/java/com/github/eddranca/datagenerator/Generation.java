package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.exception.SerializationException;
import com.github.eddranca.datagenerator.visitor.LazyItemProxy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Generation {
    private static final Logger logger = Logger.getLogger(Generation.class.getName());
    private final Map<String, List<JsonNode>> collections;
    private final ObjectMapper mapper = new ObjectMapper();

    Generation(Map<String, List<JsonNode>> collectionsMap) {
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.collections = new HashMap<>(collectionsMap);
    }

    public Map<String, List<JsonNode>> getCollections() {
        return collections;
    }

    public ObjectNode getCollectionsAsJsonNode() {
        return mapper.valueToTree(collections);
    }

    public JsonNode asJsonNode() {
        // Ensure all lazy items are fully materialized before JSON conversion
        Map<String, List<JsonNode>> materializedCollections = new HashMap<>();

        for (Map.Entry<String, List<JsonNode>> entry : collections.entrySet()) {
            List<JsonNode> collection = entry.getValue();
            List<JsonNode> materializedCollection = new ArrayList<>();

            for (JsonNode item : collection) {
                if (item instanceof LazyItemProxy) {
                    // Get a materialized copy without modifying the original proxy
                    materializedCollection.add(((LazyItemProxy) item).getMaterializedCopy());
                } else {
                    materializedCollection.add(item);
                }
            }

            materializedCollections.put(entry.getKey(), materializedCollection);
        }

        return mapper.valueToTree(materializedCollections);
    }

    public String asJson() throws JsonProcessingException {
        // Use the materialized collections from asJsonNode() for consistency
        return mapper.writeValueAsString(asJsonNode());
    }

    /**
     * Generates SQL INSERT statements for all collections.
     *
     * <p>This method converts the generated data into SQL INSERT statements that can be used
     * to populate database tables. Each collection becomes a table, and each item in the
     * collection becomes a row in that table.</p>
     *
     * <p><strong>Handling of Complex Objects:</strong><br>
     * When a field contains a complex object (nested JSON), it will be serialized to a JSON
     * string representation and inserted as a VARCHAR/TEXT column. This allows for storage
     * of complex data structures in databases that support JSON columns or can store JSON
     * as text. A warning will be logged when complex objects are encountered.</p>
     *
     * <p><strong>Example:</strong><br>
     * A field like <code>{"address": {"street": "123 Main St", "city": "NYC"}}</code>
     * will be converted to <code>'{"street":"123 Main St","city":"NYC"}'</code> in the SQL.</p>
     *
     * @return A map where keys are table names and values are SQL INSERT statements
     * @throws SerializationException if JSON serialization fails for complex objects
     */
    public Map<String, String> asSqlInserts() {
        return asSqlInserts((String[]) null);
    }

    /**
     * Generates SQL INSERT statements for specified collections only.
     *
     * <p>This method allows you to generate SQL INSERT statements for only a subset of
     * collections. This is useful when some collections are used as reference data to
     * generate other collections, but you don't want SQL inserts for all of them.</p>
     *
     * <p><strong>Handling of Complex Objects:</strong><br>
     * When a field contains a complex object (nested JSON), it will be serialized to a JSON
     * string representation and inserted as a VARCHAR/TEXT column. This allows for storage
     * of complex data structures in databases that support JSON columns or can store JSON
     * as text. A warning will be logged when complex objects are encountered.</p>
     *
     * @param collectionNames the names of collections to generate SQL for, or null/empty for all
     * @return A map where keys are table names and values are SQL INSERT statements
     * @throws SerializationException if JSON serialization fails for complex objects
     */
    public Map<String, String> asSqlInserts(String... collectionNames) {
        Map<String, String> sqlInserts = new HashMap<>();

        // Create a set of collection names to include (null means include all)
        Set<String> includeCollections = null;
        if (collectionNames != null && collectionNames.length > 0) {
            includeCollections = new HashSet<>(Arrays.asList(collectionNames));
        }

        for (Map.Entry<String, List<JsonNode>> entry : collections.entrySet()) {
            String tableName = entry.getKey();

            // Skip this collection if it's not in the include list
            if (includeCollections != null && !includeCollections.contains(tableName)) {
                continue;
            }

            List<JsonNode> rows = entry.getValue();
            StringBuilder collectionInserts = new StringBuilder();

            for (JsonNode row : rows) {
                // Get materialized copy without modifying the original proxy
                JsonNode materializedRow = row;
                if (row instanceof LazyItemProxy) {
                    materializedRow = ((LazyItemProxy) row).getMaterializedCopy();
                }
                collectionInserts.append(generateSqlInsert(tableName, materializedRow)).append("\n");
            }
            sqlInserts.put(tableName, collectionInserts.toString());
        }
        return sqlInserts;
    }

    /**
     * Generates SQL INSERT statements as a stream for the specified collection.
     * This method is memory-efficient for large datasets as it generates and processes
     * items one at a time instead of loading everything into memory.
     *
     * @param collectionName the name of the collection to stream
     * @return a stream of SQL INSERT statements
     * @throws IllegalArgumentException if the collection doesn't exist
     */
    public Stream<String> streamSqlInserts(String collectionName) {
        List<JsonNode> collection = collections.get(collectionName);
        if (collection == null) {
            throw new IllegalArgumentException("Collection '" + collectionName + "' not found");
        }

        return collection.stream()
            .map(item -> {
                // Get materialized copy for each item independently during streaming
                JsonNode materializedItem = item;
                if (item instanceof LazyItemProxy) {
                    materializedItem = ((LazyItemProxy) item).getMaterializedCopy();
                }
                return generateSqlInsert(collectionName, materializedItem);
            });
    }

    /**
     * Generates a single SQL INSERT statement for an item.
     */
    private String generateSqlInsert(String tableName, JsonNode item) {
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

    /**
     * Fluent builder for generation operations.
     */
    public static class Builder {
        private final DslDataGenerator generator;
        private final File file;
        private final String jsonString;
        private final JsonNode jsonNode;


        Builder(DslDataGenerator generator, File file) {
            this.generator = generator;
            this.file = file;
            this.jsonString = null;
            this.jsonNode = null;
        }

        Builder(DslDataGenerator generator, String jsonString) {
            this.generator = generator;
            this.file = null;
            this.jsonString = jsonString;
            this.jsonNode = null;
        }

        Builder(DslDataGenerator generator, JsonNode jsonNode) {
            this.generator = generator;
            this.file = null;
            this.jsonString = null;
            this.jsonNode = jsonNode;
        }


        /**
         * Generates the data based on the configured DSL source.
         *
         * @return the generated data
         * @throws IOException                                                        if file reading fails or JSON parsing fails
         * @throws com.github.eddranca.datagenerator.exception.DslValidationException if DSL validation fails
         */
        public Generation generate() throws IOException {
            if (file != null) {
                return generator.generateInternal(file);
            } else if (jsonString != null) {
                return generator.generateInternal(jsonString);
            } else if (jsonNode != null) {
                return generator.generateInternal(jsonNode);
            } else {
                throw new IllegalStateException("No DSL source configured");
            }
        }

        /**
         * Generates the data and returns it as JSON string.
         *
         * @return the generated data as JSON
         * @throws IOException if file reading or JSON serialization fails
         */
        public String generateAsJson() throws IOException {
            return generate().asJson();
        }

        /**
         * Generates the data and returns it as JsonNode.
         *
         * @return the generated data as JsonNode
         * @throws IOException if file reading fails
         */
        public JsonNode generateAsJsonNode() throws IOException {
            return generate().asJsonNode();
        }

        /**
         * Generates the data and returns SQL INSERT statements for all collections.
         *
         * @return a map of table names to SQL INSERT statements
         * @throws IOException if file reading fails
         */
        public Map<String, String> generateAsSql() throws IOException {
            return generate().asSqlInserts();
        }

        /**
         * Generates the data and returns SQL INSERT statements for specified collections only.
         *
         * @param collectionNames the names of collections to generate SQL for
         * @return a map of table names to SQL INSERT statements
         * @throws IOException if file reading fails
         */
        public Map<String, String> generateAsSql(String... collectionNames) throws IOException {
            return generate().asSqlInserts(collectionNames);
        }
    }
}
