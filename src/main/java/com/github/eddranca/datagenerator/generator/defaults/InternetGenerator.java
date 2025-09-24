package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import net.datafaker.Faker;

import java.util.Map;
import java.util.function.Supplier;

public class InternetGenerator implements Generator {
    private final Faker faker;
    private final ObjectMapper mapper;

    public InternetGenerator(Faker faker) {
        this.faker = faker;
        this.mapper = new ObjectMapper();
    }

    @Override
    public JsonNode generate(JsonNode options) {
        return mapper.valueToTree(
            Map.of("emailAddress", faker.internet().emailAddress(),
                "domainName", faker.internet().domainName(),
                "url", faker.internet().url(),
                "username", faker.credentials().username()));
    }

    @Override
    public Map<String, Supplier<JsonNode>> getFieldSuppliers(JsonNode options) {
        return Map.of(
            "emailAddress", () -> mapper.valueToTree(faker.internet().emailAddress()),
            "domainName", () -> mapper.valueToTree(faker.internet().domainName()),
            "url", () -> mapper.valueToTree(faker.internet().url()),
            "username", () -> mapper.valueToTree(faker.credentials().username()));
    }
}
