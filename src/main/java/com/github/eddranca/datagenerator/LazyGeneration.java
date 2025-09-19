package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.github.eddranca.datagenerator.exception.SerializationException;
import com.github.eddranca.datagenerator.visitor.LazyItemProxy;

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

/**
 * Memory-optimized implementation of IGeneration that uses lazy evaluation
 * for efficient memory usage with large datasets.
 * 
 * <p>Key features:
 * <ul>
 *   <li><strong>Lazy field generation</strong> - Only referenced fields are initially materialized</li>
 *   <li><strong>On-demand materialization</strong> - Other fields generated when accessed</li>
 *   <li><strong>Memory-efficient streaming</strong> - Process items without caching</li>
 *   <li><strong>Hierarchical lazy loading</strong> - Nested objects are also optimized</li>
 * </ul>
 * 
 * <p>This implementation can reduce memory usage by up to 99% for large datasets
 * where only a subset of fields are actually used.
 * 
 * <p>Usage example:
 * <pre>{@code
 * Generation result = DslDataGenerator.create()
 *     .withMemoryOptimization()
 *     .fromJsonString(dsl)
 *     .generate();
 * 
 * // Memory-efficient streaming
 * result.streamSqlInserts("users")
 *     .forEach(sql -> database.execute(sql));
 * }</pre>
 */
public class LazyGeneration implements IGeneration {
    private static final Logger logger = Logger.getLogger(LazyGeneration.class.getName());
    private final Map<String, List<LazyItemProxy>> lazyCollections;
    private final ObjectMapper mapper = new ObjectMapper();

    LazyGeneration(Map<String, List<LazyItemProxy>> lazyCollectionsMap) {
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.lazyCollections = new HashMap<>(lazyCollectionsMap);
    }

    /**
     * Returns the lazy collections for internal use.
     * This is package-private for use by the data generator.
     */
    Map<String, List<LazyItemProxy>> getLazyCollections() {
        return lazyCollections;
    }

    @Override
    public Set<String> getCollectionNames() {
        return lazyCollections.keySet();
    }

    @Override
    public int getCollectionSize(String collectionName) {
        List<LazyItemProxy> collection = lazyCollections.get(collectionName);
        if (collection == null) {
            throw new IllegalArgumentException("Collection '" + collectionName + "' not found");
        }
        return collection.size();
    }


    






    @Override
    public Map<String, String> asSqlInserts() {
        return asSqlInserts((String[]) null);
    }

    @Override
    public Map<String, String> asSqlInserts(String... collectionNames) {
        Map<String, String> sqlInserts = new HashMap<>();

        // Create a set of collection names to include (null means include all)
        Set<String> includeCollections = null;
        if (collectionNames != null && collectionNames.length > 0) {
            includeCollections = new HashSet<>(Arrays.asList(collectionNames));
        }

        for (Map.Entry<String, List<LazyItemProxy>> entry : lazyCollections.entrySet()) {
            String tableName = entry.getKey();

            // Skip this collection if it's not in the include list
            if (includeCollections != null && !includeCollections.contains(tableName)) {
                continue;
            }

            List<LazyItemProxy> rows = entry.getValue();
            StringBuilder collectionInserts = new StringBuilder();

            for (LazyItemProxy lazyRow : rows) {
                // Get materialized copy for SQL generation
                JsonNode materializedRow = lazyRow.getMaterializedCopy();
                collectionInserts.append(generateSqlInsert(tableName, materializedRow)).append("\n");
            }
            sqlInserts.put(tableName, collectionInserts.toString());
        }
        return sqlInserts;
    }

    @Override
    public Stream<String> streamSqlInserts(String collectionName) {
        List<LazyItemProxy> collection = lazyCollections.get(collectionName);
        if (collection == null) {
            throw new IllegalArgumentException("Collection '" + collectionName + "' not found");
        }

        return collection.stream()
            .map(lazyItem -> {
                // Get materialized copy for each item independently during streaming
                JsonNode materializedItem = lazyItem.getMaterializedCopy();
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
}