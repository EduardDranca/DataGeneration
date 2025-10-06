package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;

public class NumberGenerator implements Generator {

    @Override
    public JsonNode generate(GeneratorContext context) {
        int min = context.getIntOption("min", Integer.MIN_VALUE);
        int max = context.getIntOption("max", Integer.MAX_VALUE);
        ObjectMapper mapper = context.mapper();
        return mapper.valueToTree(context.faker().number().numberBetween(min, max));
    }
}
