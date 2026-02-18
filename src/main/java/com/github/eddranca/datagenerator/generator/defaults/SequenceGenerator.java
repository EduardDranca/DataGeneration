package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import com.github.eddranca.datagenerator.generator.GeneratorOptionSpec;

import java.util.IdentityHashMap;
import java.util.Map;

public class SequenceGenerator implements Generator {
    private final Map<JsonNode, Integer> counters = new IdentityHashMap<>();

    @Override
    public GeneratorOptionSpec getOptionSpec() {
        return GeneratorOptionSpec.builder()
            .optional("start", "increment")
            .build();
    }

    @Override
    public JsonNode generate(GeneratorContext context) {
        JsonNode options = context.options();
        int start = context.getIntOption("start", 0);
        int increment = context.getIntOption("increment", 1);

        if (increment == 0) {
            throw new IllegalArgumentException("Sequence increment cannot be zero");
        }

        int current = counters.getOrDefault(options, start);
        int next = current + increment;
        counters.put(options, next);

        return new IntNode(current);
    }
}
