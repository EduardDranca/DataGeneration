package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import net.datafaker.Faker;

public class StringGenerator implements Generator {
    private static final String DEFAULT_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private final Faker faker;
    private final ObjectMapper mapper;

    public StringGenerator(Faker faker) {
        this.faker = faker;
        this.mapper = new ObjectMapper();
    }

    @Override
    public JsonNode generate(JsonNode options) {
        // Default allowed characters (alphanumeric)

        // Parse configuration options
        String allowedChars = options.has("allowedChars") ? options.get("allowedChars").asText() : DEFAULT_CHARS;
        int maxLength = options.has("maxLength") ? options.get("maxLength").asInt() : 20;
        int minLength = options.has("minLength") ? options.get("minLength").asInt() : 1;

        // Handle length (overrides min/max if specified)
        int length;
        if (options.has("length")) {
            length = options.get("length").asInt(10);
        } else {
            // Ensure minLength doesn't exceed maxLength
            minLength = Math.min(minLength, maxLength);
            length = faker.number().numberBetween(minLength, maxLength + 1);
        }

        // Generate string using allowed characters
        if (!options.has("regex")) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < length; i++) {
                int randomIndex = faker.number().numberBetween(0, allowedChars.length());
                result.append(allowedChars.charAt(randomIndex));
            }
            return mapper.valueToTree(result.toString());
        }

        String regexPattern = options.get("regex").asText();
        return mapper.valueToTree(faker.regexify(regexPattern));
    }
}
