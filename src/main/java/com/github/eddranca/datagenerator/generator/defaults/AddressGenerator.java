package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import net.datafaker.Faker;

import java.util.Map;
import java.util.function.Supplier;

public class AddressGenerator implements Generator {
    private final Faker faker;
    private final ObjectMapper mapper;

    public AddressGenerator(Faker faker) {
        this.faker = faker;
        this.mapper = new ObjectMapper();
    }

    @Override
    public JsonNode generate(JsonNode options) {
        return mapper.valueToTree(
                Map.of(
                        "streetAddress", faker.address().streetAddress(),
                        "city", faker.address().city(),
                        "state", faker.address().state(),
                        "zipCode", faker.address().zipCode(),
                        "country", faker.address().country(),
                        "countryCode", faker.address().countryCode(),
                        "fullAddress", faker.address().fullAddress()));
    }

    @Override
    public Map<String, Supplier<JsonNode>> getFieldSuppliers(JsonNode options) {
        return Map.of(
                "streetAddress", () -> mapper.valueToTree(faker.address().streetAddress()),
                "city", () -> mapper.valueToTree(faker.address().city()),
                "state", () -> mapper.valueToTree(faker.address().state()),
                "zipCode", () -> mapper.valueToTree(faker.address().zipCode()),
                "country", () -> mapper.valueToTree(faker.address().country()),
                "countryCode", () -> mapper.valueToTree(faker.address().countryCode()),
                "fullAddress", () -> mapper.valueToTree(faker.address().fullAddress()));
    }
}
