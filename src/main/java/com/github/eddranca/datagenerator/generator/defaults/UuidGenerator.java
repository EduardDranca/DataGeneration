package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;

import java.util.UUID;

public class UuidGenerator implements Generator {

    @Override
    public JsonNode generate(GeneratorContext context) {
        // Create a deterministic UUID using the context's faker random
        Faker faker = context.faker();
        ObjectMapper mapper = context.mapper();
        long mostSigBits = faker.random().nextLong();
        long leastSigBits = faker.random().nextLong();
        UUID uuid = new UUID(mostSigBits, leastSigBits);
        return mapper.valueToTree(uuid.toString());
    }
}
