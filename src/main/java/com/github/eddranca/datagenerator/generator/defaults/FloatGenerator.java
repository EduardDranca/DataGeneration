package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import com.github.eddranca.datagenerator.generator.GeneratorOptionSpec;

public class FloatGenerator implements Generator {

    @Override
    public GeneratorOptionSpec getOptionSpec() {
        return GeneratorOptionSpec.builder()
            .optional("min", "max", "decimals")
            .build();
    }

    @Override
    public JsonNode generate(GeneratorContext context) {
        ObjectMapper mapper = context.mapper();
        int min = context.getIntOption("min", Integer.MIN_VALUE);
        int max = context.getIntOption("max", Integer.MAX_VALUE);
        int decimals = context.getIntOption("decimals", 2);

        // Ensure decimals is within reasonable bounds
        decimals = Math.max(0, Math.min(decimals, 10));

        return mapper.valueToTree(context.faker().number().randomDouble(decimals, min, max));
    }
}
