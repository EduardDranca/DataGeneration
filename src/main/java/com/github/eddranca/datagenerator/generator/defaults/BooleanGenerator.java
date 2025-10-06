package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;

import java.util.List;

public class BooleanGenerator implements Generator {

    @Override
    public JsonNode generate(GeneratorContext context) {
        ObjectMapper mapper = context.mapper();
        double probability = context.getDoubleOption("probability", 0.5); // Default 50/50

        // Clamp probability between 0.0 and 1.0
        probability = Math.max(0.0, Math.min(1.0, probability));

        boolean result = context.faker().random().nextDouble() < probability;
        return mapper.valueToTree(result);
    }

    @Override
    public JsonNode generateWithFilter(GeneratorContext context, List<JsonNode> filterValues) {
        if (filterValues == null || filterValues.isEmpty()) {
            return generate(context);
        }
        ObjectMapper mapper = context.mapper();

        // Convert filter values to booleans using streams
        boolean filterTrue = filterValues.stream()
            .filter(JsonNode::isBoolean)
            .anyMatch(JsonNode::asBoolean);

        boolean filterFalse = filterValues.stream()
            .filter(JsonNode::isBoolean)
            .anyMatch(node -> !node.asBoolean());

        // If both true and false are filtered, return null
        if (filterTrue && filterFalse) {
            return mapper.nullNode();
        }

        // If only true is filtered, always return false
        if (filterTrue) {
            return mapper.valueToTree(false);
        }

        // If only false is filtered, always return true
        if (filterFalse) {
            return mapper.valueToTree(true);
        }

        // No filtering needed
        return generate(context);
    }

    @Override
    public boolean supportsFiltering() {
        return true;
    }
}
