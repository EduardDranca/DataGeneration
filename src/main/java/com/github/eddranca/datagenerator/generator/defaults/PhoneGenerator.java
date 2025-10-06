package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;

public class PhoneGenerator implements Generator {

    @Override
    public JsonNode generate(GeneratorContext context) {
        Faker faker = context.faker();
        ObjectMapper mapper = context.mapper();
        JsonNode options = context.options();
        if (options == null) {
            // Default: return a phone number
            return mapper.valueToTree(faker.phoneNumber().phoneNumber());
        }

        String format = options.has("format") ? options.get("format").asText() : "default";

        return switch (format.toLowerCase()) {
            case "international" -> mapper.valueToTree(faker.phoneNumber().phoneNumber());
            case "cell", "mobile" -> mapper.valueToTree(faker.phoneNumber().cellPhone());
            case "extension" -> mapper.valueToTree(faker.phoneNumber().extension());
            default -> mapper.valueToTree(faker.phoneNumber().phoneNumber());
        };
    }
}
