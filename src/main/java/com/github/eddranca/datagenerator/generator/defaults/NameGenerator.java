package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import net.datafaker.Faker;

import java.util.Map;
import java.util.function.Supplier;

public class NameGenerator implements Generator {
    private final Faker faker;
    private final ObjectMapper mapper;

    public NameGenerator(Faker faker) {
        this.faker = faker;
        this.mapper = new ObjectMapper();
    }

    @Override
    public JsonNode generate(JsonNode options) {
        return mapper.valueToTree(
            Map.of("firstName", faker.name().firstName(),
                "lastName", faker.name().lastName(),
                "fullName", faker.name().fullName(),
                "prefix", faker.name().prefix(),
                "suffix", faker.name().suffix(),
                "title", faker.name().title())
        );
    }

    @Override
    public Map<String, Supplier<JsonNode>> getFieldSuppliers(JsonNode options) {
        return Map.of(
            "firstName", () -> mapper.valueToTree(faker.name().firstName()),
            "lastName", () -> mapper.valueToTree(faker.name().lastName()),
            "fullName", () -> mapper.valueToTree(faker.name().fullName()),
            "prefix", () -> mapper.valueToTree(faker.name().prefix()),
            "suffix", () -> mapper.valueToTree(faker.name().suffix()),
            "title", () -> mapper.valueToTree(faker.name().title())
        );
    }
}
