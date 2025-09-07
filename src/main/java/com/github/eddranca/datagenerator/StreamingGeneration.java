package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.eddranca.datagenerator.node.CollectionNode;
import com.github.eddranca.datagenerator.visitor.DataGenerationVisitor;
import com.github.eddranca.datagenerator.visitor.GenerationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Streaming generation that produces SQL inserts on-demand while keeping
 * reference collections in memory for dependency resolution.
 */
public class StreamingGeneration {
    private static final Logger logger = Logger.getLogger(StreamingGeneration.class.getName());
    private final GenerationContext context;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, CollectionNode> collections;

    StreamingGeneration(GenerationContext context, Map<String, CollectionNode> collections) {
        this.context = context;
        this.collections = collections;
    }

    /**
     * Streams SQL INSERT statements for the specified collection.
     * Reference collections are generated first and kept in memory.
     *
     * @param collectionName the collection to stream
     * @return stream of SQL INSERT statements
     */
    public Stream<String> streamSqlInserts(String collectionName) {
        CollectionNode targetCollection = collections.get(collectionName);
        if (targetCollection == null) {
            throw new IllegalArgumentException("Collection '" + collectionName + "' not found");
        }

        // First, generate all reference collections that this collection might depend on
        generateReferenceCollections(collectionName);

        // Now stream the target collection
        return generateCollectionStream(targetCollection)
                .map(item -> generateSqlInsert(targetCollection.getCollectionName(), item));
    }

    /**
     * Streams SQL INSERT statements for multiple collections in dependency order.
     *
     * @param collectionNames the collections to stream
     * @return stream of SQL INSERT statements with collection name prefixes
     */
    public Stream<String> streamSqlInserts(String... collectionNames) {
        if (collectionNames == null || collectionNames.length == 0) {
            collectionNames = collections.keySet().toArray(new String[0]);
        }

        // Generate reference collections first
        for (String collectionName : collectionNames) {
            generateReferenceCollections(collectionName);
        }

        // Stream all requested collections
        return Arrays.stream(collectionNames)
                .flatMap(collectionName -> {
                    CollectionNode collection = collections.get(collectionName);
                    if (collection == null) {
                        logger.warning("Collection '" + collectionName + "' not found, skipping");
                        return Stream.empty();
                    }
                    return generateCollectionStream(collection)
                            .map(item -> generateSqlInsert(collection.getCollectionName(), item));
                });
    }

    /**
     * Generates reference collections that might be needed by the target collection.
     * This is a simplified approach - in a more sophisticated implementation,
     * we would analyze dependencies and generate only what's needed.
     */
    private void generateReferenceCollections(String targetCollectionName) {
        // For now, generate all collections except the target one
        // This ensures all references are available
        for (Map.Entry<String, CollectionNode> entry : collections.entrySet()) {
            String collectionName = entry.getKey();
            CollectionNode collection = entry.getValue();

            // Skip the target collection - we'll stream that
            if (collectionName.equals(targetCollectionName)) {
                continue;
            }

            // Check if this collection is already generated
            if (context.getCollection(collection.getCollectionName()) != null) {
                continue;
            }

            // Generate this collection fully for references
            generateFullCollection(collection);
        }
    }

    /**
     * Generates a complete collection and registers it for references.
     */
    private void generateFullCollection(CollectionNode collection) {
        DataGenerationVisitor visitor = new DataGenerationVisitor(context);
        visitor.visitCollection(collection);
    }

    /**
     * Generates a stream of items for a collection without storing them.
     */
    private Stream<JsonNode> generateCollectionStream(CollectionNode collection) {
        DataGenerationVisitor visitor = new DataGenerationVisitor(context);

        return Stream.generate(() -> collection.getItem().accept(visitor))
                .limit(collection.getCount())
                .peek(item -> {
                    // Register item for references as it's generated
                    registerItemForReferences(collection, item);
                });
    }

    /**
     * Registers a generated item for reference purposes without storing the full collection.
     */
    private void registerItemForReferences(CollectionNode node, JsonNode item) {
        // Add to named collection for references
        List<JsonNode> namedCollection = context.getCollection(node.getCollectionName());
        if (namedCollection == null) {
            List<JsonNode> newCollection = new ArrayList<>();
            context.registerCollection(node.getCollectionName(), newCollection);
            namedCollection = context.getCollection(node.getCollectionName());
        }
        namedCollection.add(item);

        // Add to DSL key collection if different
        if (!node.getName().equals(node.getCollectionName())) {
            List<JsonNode> keyCollection = context.getCollection(node.getName());
            if (keyCollection == null) {
                List<JsonNode> newCollection = new ArrayList<>();
                context.registerReferenceCollection(node.getName(), newCollection);
                keyCollection = context.getCollection(node.getName());
            }
            keyCollection.add(item);
        }

        // Add to tagged collections
        for (String tag : node.getTags()) {
            List<JsonNode> taggedCollection = context.getTaggedCollection(tag);
            if (taggedCollection == null) {
                List<JsonNode> newCollection = new ArrayList<>();
                context.registerTaggedCollection(tag, newCollection);
                taggedCollection = context.getTaggedCollection(tag);
            }
            taggedCollection.add(item);
        }

        // Handle picks
        List<JsonNode> finalCollection = context.getCollection(node.getCollectionName());
        int currentIndex = finalCollection.size() - 1; // Index of the item we just added
        for (Map.Entry<String, Integer> pick : node.getPicks().entrySet()) {
            String alias = pick.getKey();
            int pickIndex = pick.getValue();
            if (pickIndex == currentIndex) {
                context.registerPick(alias, item);
            }
        }
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
                    throw new RuntimeException("Failed to serialize complex object to JSON", e);
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
