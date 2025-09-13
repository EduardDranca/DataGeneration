package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import net.datafaker.Faker;

import java.util.Map;
import java.util.function.Supplier;

public class FinanceGenerator implements Generator {
    private final Faker faker;
    private final ObjectMapper mapper;

    public FinanceGenerator(Faker faker) {
        this.faker = faker;
        this.mapper = new ObjectMapper();
    }

    @Override
    public JsonNode generate(JsonNode options) {
        return mapper.valueToTree(
            Map.of("iban", faker.finance().iban(),
                "bic", faker.finance().bic(),
                "creditCard", faker.finance().creditCard())
        );
    }

    @Override
    public Map<String, Supplier<JsonNode>> getFieldSuppliers(JsonNode options) {
        return Map.of(
            "iban", () -> mapper.valueToTree(faker.finance().iban()),
            "bic", () -> mapper.valueToTree(faker.finance().bic()),
            "creditCard", () -> mapper.valueToTree(faker.finance().creditCard())
        );
    }
}
