package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;

import java.util.Map;

public class AddressGenerator implements Generator {

    @Override
    public JsonNode generate(GeneratorContext context) {
        Faker faker = context.faker();
        ObjectMapper mapper = context.mapper();
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
}
