package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import net.datafaker.Faker;

public class NumberGenerator implements Generator {
    private final Faker faker;
    private final ObjectMapper mapper;

    public NumberGenerator(Faker faker) {
        this.faker = faker;
        this.mapper = new ObjectMapper();
    }

    @Override
    public JsonNode generate(JsonNode options) {
        int min = options.has("min") ? options.get("min").asInt() : Integer.MIN_VALUE;
        int max = options.has("max") ? options.get("max").asInt() : Integer.MAX_VALUE;
        return mapper.valueToTree(faker.number().numberBetween(min, max));
    }
}
