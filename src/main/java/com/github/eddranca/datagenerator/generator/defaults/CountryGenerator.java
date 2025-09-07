package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import net.datafaker.Faker;

import java.util.Map;
import java.util.function.Supplier;

public class CountryGenerator implements Generator {
    private final Faker faker;
    private final ObjectMapper mapper;

    public CountryGenerator(Faker faker) {
        this.faker = faker;
        this.mapper = new ObjectMapper();
    }

    @Override
    public JsonNode generate(JsonNode options) {
        return mapper.valueToTree(
                Map.of("name", faker.country().name(),
                        "countryCode", faker.country().countryCode2(),
                        "capital", faker.country().capital(),
                        "currency", faker.country().currency(),
                        "currencyCode", faker.country().currencyCode())
        );
    }

    @Override
    public Map<String, Supplier<JsonNode>> getFieldSuppliers(JsonNode options) {
        return Map.of(
                "name", () -> mapper.valueToTree(faker.country().name()),
                "countryCode", () -> mapper.valueToTree(faker.country().countryCode2()),
                "capital", () -> mapper.valueToTree(faker.country().capital()),
                "currency", () -> mapper.valueToTree(faker.country().currency()),
                "currencyCode", () -> mapper.valueToTree(faker.country().currencyCode())
        );
    }
}
