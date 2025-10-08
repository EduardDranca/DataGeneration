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
        String format = context.getStringOption("format");

        if (format == null) {
            format = "international";
        }

        return switch (format) {
            case "international" -> mapper.valueToTree(faker.phoneNumber().phoneNumber());
            case "cell", "mobile" -> mapper.valueToTree(faker.phoneNumber().cellPhone());
            case "extension" -> mapper.valueToTree(faker.phoneNumber().extension());
            default -> mapper.valueToTree(faker.phoneNumber().phoneNumber());
        };
    }
}
