package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import net.datafaker.Faker;

import java.util.Map;
import java.util.function.Supplier;

public class CompanyGenerator implements Generator {
    private final Faker faker;
    private final ObjectMapper mapper;

    public CompanyGenerator(Faker faker) {
        this.faker = faker;
        this.mapper = new ObjectMapper();
    }

    @Override
    public JsonNode generate(JsonNode options) {
        return mapper.valueToTree(
            Map.of("name", faker.company().name(),
                "industry", faker.company().industry(),
                "profession", faker.company().profession(),
                "buzzword", faker.company().buzzword())
        );
    }

    @Override
    public Map<String, Supplier<JsonNode>> getFieldSuppliers(JsonNode options) {
        return Map.of(
            "name", () -> mapper.valueToTree(faker.company().name()),
            "industry", () -> mapper.valueToTree(faker.company().industry()),
            "profession", () -> mapper.valueToTree(faker.company().profession()),
            "buzzword", () -> mapper.valueToTree(faker.company().buzzword())
        );
    }
}
