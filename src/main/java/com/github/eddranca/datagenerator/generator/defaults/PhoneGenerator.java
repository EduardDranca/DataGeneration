package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import net.datafaker.Faker;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PhoneGenerator implements Generator {
    private final Faker faker;
    private final ObjectMapper mapper;

    public PhoneGenerator(Faker faker) {
        this.faker = faker;
        this.mapper = new ObjectMapper();
    }

    @Override
    public JsonNode generate(JsonNode options) {
        if (options == null) {
            // Default: return a phone number
            return mapper.valueToTree(faker.phoneNumber().phoneNumber());
        }

        String format = options.has("format") ? options.get("format").asText() : "default";
        
        switch (format.toLowerCase()) {
            case "international":
                return mapper.valueToTree(faker.phoneNumber().phoneNumber());
            case "cell":
            case "mobile":
                return mapper.valueToTree(faker.phoneNumber().cellPhone());
            case "extension":
                return mapper.valueToTree(faker.phoneNumber().extension());
            default:
                return mapper.valueToTree(faker.phoneNumber().phoneNumber());
        }
    }

    @Override
    public Map<String, Supplier<JsonNode>> getFieldSuppliers(JsonNode options) {
        return Map.of(
                "phoneNumber", () -> mapper.valueToTree(faker.phoneNumber().phoneNumber()),
                "cellPhone", () -> mapper.valueToTree(faker.phoneNumber().cellPhone()),
                "extension", () -> mapper.valueToTree(faker.phoneNumber().extension())
        );
    }

    @Override
    public JsonNode generateWithFilter(JsonNode options, List<JsonNode> filterValues) {
        return generate(options);
    }
}
