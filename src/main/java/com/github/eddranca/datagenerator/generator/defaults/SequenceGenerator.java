package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.github.eddranca.datagenerator.generator.Generator;

import java.util.IdentityHashMap;
import java.util.Map;

public class SequenceGenerator implements Generator {
    private final Map<JsonNode, Integer> counters;

    public SequenceGenerator() {
        this.counters = new IdentityHashMap<>();
    }

    @Override
    public JsonNode generate(JsonNode options) {
        int start = options.has("start") ? options.get("start").asInt() : 0;
        int increment = options.has("increment") ? options.get("increment").asInt() : 1;

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
