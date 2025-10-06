package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;

import java.util.IdentityHashMap;
import java.util.Map;

public class SequenceGenerator implements Generator {
    private final Map<JsonNode, Integer> counters = new IdentityHashMap<>();

    @Override
    public JsonNode generate(GeneratorContext context) {
        JsonNode options = context.options();
        int start = context.getIntOption("start", 0);
        int increment = context.getIntOption("increment", 1);

        // Get the current counter value for this specific options node
        // Each field using a sequence generator will have its own options node,
        // so this ensures each field has its own sequence
        int current = counters.getOrDefault(options, start);

        // Calculate the next value in the sequence
        int next = current + increment;

        // Update the counter for this options node
        counters.put(options, next);

        // Return the current value (before incrementing)
        return new IntNode(current);
    }
}
