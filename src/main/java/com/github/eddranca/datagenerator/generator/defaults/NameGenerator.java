package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;

import java.util.Map;

public class NameGenerator implements Generator {

    @Override
    public JsonNode generate(GeneratorContext context) {
        Faker faker = context.faker();
        ObjectMapper mapper = context.mapper();
        return mapper.valueToTree(
            Map.of("firstName", faker.name().firstName(),
                "lastName", faker.name().lastName(),
                "fullName", faker.name().fullName(),
                "prefix", faker.name().prefix(),
                "suffix", faker.name().suffix(),
                "title", faker.name().title())
        );
    }
}
