package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;

public class StringGenerator implements Generator {
    private static final String DEFAULT_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Override
    public JsonNode generate(GeneratorContext context) {
        Faker faker = context.faker();
        ObjectMapper mapper = context.mapper();

        // Parse configuration options
        String allowedChars = context.getStringOption("allowedChars");
        if (allowedChars == null) allowedChars = DEFAULT_CHARS;

        int maxLength = context.getIntOption("maxLength", 20);
        int minLength = context.getIntOption("minLength", 1);

        if (minLength < 0) {
            throw new IllegalArgumentException("minLength cannot be negative");
        }

        int length;
        JsonNode options = context.options();
        if (options != null && options.has("length")) {
            length = context.getIntOption("length", 10);
            if (length < 0) {
                throw new IllegalArgumentException("length cannot be negative");
            }
        } else {
            minLength = Math.min(minLength, maxLength);
            length = faker.number().numberBetween(minLength, maxLength + 1);
        }

        // Generate string using allowed characters
        String regexPattern = context.getStringOption("regex");
        if (regexPattern == null) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < length; i++) {
                int randomIndex = faker.number().numberBetween(0, allowedChars.length());
                result.append(allowedChars.charAt(randomIndex));
            }
            return mapper.valueToTree(result.toString());
        }

        return mapper.valueToTree(faker.regexify(regexPattern));
    }
}
