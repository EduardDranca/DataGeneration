package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;

import java.util.Map;

public class CountryGenerator implements Generator {
    @Override
    public JsonNode generate(GeneratorContext context) {
        Faker faker = context.faker();
        ObjectMapper mapper = context.mapper();
        return mapper.valueToTree(
            Map.of("name", faker.country().name(),
                "countryCode", faker.country().countryCode2(),
                "capital", faker.country().capital(),
                "currency", faker.country().currency(),
                "currencyCode", faker.country().currencyCode())
        );
    }
}
