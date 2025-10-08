package com.github.eddranca.datagenerator.generator;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface Generator {
    /**
     * Helper method to extract a value from a JsonNode using dot notation.
     */
    static JsonNode extractPath(JsonNode node, String path) {
        if (node == null || path == null || path.isEmpty()) {
            return node;
        }

        String[] parts = path.split("\\.");
        JsonNode current = node;

        for (String part : parts) {
            if (current == null || current.isNull() || !current.has(part)) {
                return null;
            }
            current = current.get(part);
        }

        return current;
    }

    /**
     * Generates data using the provided context.
     * The context contains the Faker instance and generation options.
     *
     * @param context the generation context containing Faker and options
     * @return the generated JsonNode value
     */
    JsonNode generate(GeneratorContext context);

    /**
     * Generates data with filtering support. Custom generators can override this
     * to implement native filtering (e.g., database queries with WHERE clauses).
     * Default implementation delegates to generate() and ignores filter values.
     *
     * @param context      the generation context containing Faker and options
     * @param filterValues values to exclude from generation (null if no filtering)
     * @return generated value that is not in the filter list
     */
    default JsonNode generateWithFilter(GeneratorContext context, List<JsonNode> filterValues) {
        return generate(context);
    }

    /**
     * Generates data and extracts a value at the specified path.
     * For example, if the generator produces {"firstName": "John", "lastName": "Doe"},
     * calling generateAtPath(context, "firstName") would return "John".
     * <p>
     * Default implementation generates the full object and extracts the path,
     * but generators can override this for more efficient path-specific generation.
     *
     * @param context the generation context containing Faker and options
     * @param path    the dot-separated path to extract (e.g., "firstName", "address.street")
     * @return the value at the specified path, or null if path doesn't exist
     */
    default JsonNode generateAtPath(GeneratorContext context, String path) {
        JsonNode fullObject = generate(context);
        return extractPath(fullObject, path);
    }

    /**
     * Generates data at a specific path with filtering support.
     *
     * @param context      the generation context containing Faker and options
     * @param path         the dot-separated path to extract
     * @param filterValues values to exclude from generation (null if no filtering)
     * @return generated value at path that is not in the filter list
     */
    default JsonNode generateAtPathWithFilter(GeneratorContext context, String path, List<JsonNode> filterValues) {
        JsonNode fullObject = generateWithFilter(context, filterValues);
        return extractPath(fullObject, path);
    }

    /**
     * Indicates whether this generator supports native filtering.
     * If false, the GenerationContext will use retry logic.
     *
     * @return true if the generator can handle filter values natively
     */
    default boolean supportsFiltering() {
        return false;
    }
}
