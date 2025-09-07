package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import net.datafaker.Faker;

public class FloatGenerator implements Generator {
    private final Faker faker;
    private final ObjectMapper mapper;

    public FloatGenerator(Faker faker) {
        this.faker = faker;
        this.mapper = new ObjectMapper();
    }

    @Override
    public JsonNode generate(JsonNode options) {
        int min = options.has("min") ? options.get("min").asInt() : Integer.MIN_VALUE;
        int max = options.has("max") ? options.get("max").asInt() : Integer.MAX_VALUE;
        int decimals = options.has("decimals") ? options.get("decimals").asInt() : 2;

        // Ensure decimals is within reasonable bounds
        decimals = Math.max(0, Math.min(decimals, 10));

        return mapper.valueToTree(faker.number().randomDouble(decimals, min, max));
    }
}
