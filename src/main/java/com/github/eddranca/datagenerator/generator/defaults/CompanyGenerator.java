package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import com.github.eddranca.datagenerator.generator.GeneratorOptionSpec;
import net.datafaker.Faker;

import java.util.Map;

public class CompanyGenerator implements Generator {

    @Override
    public GeneratorOptionSpec getOptionSpec() {
        return GeneratorOptionSpec.strict();
    }

    @Override
    public JsonNode generate(GeneratorContext context) {
        Faker faker = context.faker();
        ObjectMapper mapper = context.mapper();
        return mapper.valueToTree(
            Map.of("name", faker.company().name(),
                "industry", faker.company().industry(),
                "profession", faker.company().profession(),
                "buzzword", faker.company().buzzword())
        );
    }
}
