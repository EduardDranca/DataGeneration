package com.github.eddranca.datagenerator.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;

import java.util.Optional;

/**
 * Context object that provides generators with access to shared resources
 * and configuration needed for data generation.
 * <p>
 * This approach ensures explicit dependency injection and makes it easy
 * to extend the context with additional resources in the future.
 */
public record GeneratorContext(Faker faker, JsonNode options, ObjectMapper mapper) {
    /**
     * Convenience method to get a string option value.
     *
     * @param key the option key
     * @return the string value, or null if not present
     */
    public String getStringOption(String key) {
        return Optional.ofNullable(options)
            .map(opt -> opt.get(key))
            .map(JsonNode::asText)
            .orElse(null);
    }

    /**
     * Convenience method to get an integer option value.
     *
     * @param key          the option key
     * @param defaultValue the default value if not present
     * @return the integer value, or defaultValue if not present
     */
    public int getIntOption(String key, int defaultValue) {
        return Optional.ofNullable(options)
            .map(opt -> opt.get(key))
            .map(node -> node.asInt(defaultValue))
            .orElse(defaultValue);
    }

    /**
     * Convenience method to get a boolean option value.
     *
     * @param key          the option key
     * @param defaultValue the default value if not present
     * @return the boolean value, or defaultValue if not present
     */
    public boolean getBooleanOption(String key, boolean defaultValue) {
        return Optional.ofNullable(options)
            .map(opt -> opt.get(key))
            .map(JsonNode::asBoolean)
            .orElse(defaultValue);
    }

    /**
     * Convenience method to get a double option value.
     *
     * @param key          the option key
     * @param defaultValue the default value if not present
     * @return the double value, or defaultValue if not present
     */
    public double getDoubleOption(String key, double defaultValue) {
        return Optional.ofNullable(options)
            .map(opt -> opt.get(key))
            .map(node -> node.asDouble(defaultValue))
            .orElse(defaultValue);
    }
}
