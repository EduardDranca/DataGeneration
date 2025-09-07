package com.github.eddranca.datagenerator.generator;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.function.Supplier;

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

    JsonNode generate(JsonNode options);

    /**
     * Generates data with filtering support. Custom generators can override this
     * to implement native filtering (e.g., database queries with WHERE clauses).
     * Default implementation delegates to generate() and ignores filter values.
     *
     * @param options      the generation options
     * @param filterValues values to exclude from generation (null if no filtering)
     * @return generated value that is not in the filter list
     */
    default JsonNode generateWithFilter(JsonNode options, java.util.List<JsonNode> filterValues) {
        return generate(options);
    }

    /**
     * Generates data and extracts a value at the specified path.
     * For example, if the generator produces {"firstName": "John", "lastName": "Doe"},
     * calling generateAtPath(options, "firstName") would return "John".
     * <p>
     * Default implementation generates the full object and extracts the path,
     * but generators can override this for more efficient path-specific generation.
     *
     * @param options the generation options
     * @param path    the dot-separated path to extract (e.g., "firstName", "address.street")
     * @return the value at the specified path, or null if path doesn't exist
     */
    default JsonNode generateAtPath(JsonNode options, String path) {
        JsonNode fullObject = generate(options);
        return extractPath(fullObject, path);
    }

    /**
     * Generates data at a specific path with filtering support.
     *
     * @param options      the generation options
     * @param path         the dot-separated path to extract
     * @param filterValues values to exclude from generation (null if no filtering)
     * @return generated value at path that is not in the filter list
     */
    default JsonNode generateAtPathWithFilter(JsonNode options, String path, java.util.List<JsonNode> filterValues) {
        JsonNode fullObject = generateWithFilter(options, filterValues);
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

    /**
     * Returns a map of field suppliers for lazy field generation.
     * This allows generators to provide field-specific suppliers that are only
     * evaluated when that specific field is requested, avoiding unnecessary computation.
     * <p>
     * Default implementation returns null, indicating no lazy suppliers are available.
     * Generators should override this method to provide efficient field-specific generation.
     *
     * @param options the generation options
     * @return a map of field names to suppliers, or null if not supported
     */
    default Map<String, Supplier<JsonNode>> getFieldSuppliers(JsonNode options) {
        return null;
    }
}
