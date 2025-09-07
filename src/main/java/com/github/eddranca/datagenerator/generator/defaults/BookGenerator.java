package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import net.datafaker.Faker;

import java.util.Map;
import java.util.function.Supplier;

public class BookGenerator implements Generator {
    private final Faker faker;
    private final ObjectMapper mapper;

    public BookGenerator(Faker faker) {
        this.faker = faker;
        this.mapper = new ObjectMapper();
    }

    @Override
    public JsonNode generate(JsonNode options) {
        return mapper.valueToTree(
                Map.of("title", faker.book().title(),
                        "author", faker.book().author(),
                        "publisher", faker.book().publisher(),
                        "genre", faker.book().genre())
        );
    }

    @Override
    public Map<String, Supplier<JsonNode>> getFieldSuppliers(JsonNode options) {
        return Map.of(
                "title", () -> mapper.valueToTree(faker.book().title()),
                "author", () -> mapper.valueToTree(faker.book().author()),
                "publisher", () -> mapper.valueToTree(faker.book().publisher()),
                "genre", () -> mapper.valueToTree(faker.book().genre())
        );
    }
}
