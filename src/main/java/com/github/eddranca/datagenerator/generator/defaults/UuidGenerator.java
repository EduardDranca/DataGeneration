package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import net.datafaker.Faker;

public class UuidGenerator implements Generator {
    private final Faker faker;
    private final ObjectMapper mapper;

    public UuidGenerator(Faker faker) {
        this.faker = faker;
        this.mapper = new ObjectMapper();
    }

    @Override
    public JsonNode generate(JsonNode options) {
        // Create a deterministic UUID using the faker's random
        long mostSigBits = faker.random().nextLong();
        long leastSigBits = faker.random().nextLong();
        java.util.UUID uuid = new java.util.UUID(mostSigBits, leastSigBits);
        return mapper.valueToTree(uuid.toString());
    }
}
