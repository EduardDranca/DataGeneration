package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.exception.FilteringException;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.service.RandomService;

import java.util.ArrayList;
import java.util.List;

/**
 * Generator that handles choice field logic by selecting from pre-generated options.
 * Supports both weighted and unweighted random selection with filtering.
 */
public class ChoiceGenerator implements Generator {

    @Override
    public JsonNode generate(GeneratorContext context) {
        JsonNode options = context.options();
        JsonNode optionsArray = options.get("options");
        List<Double> weights = getWeights(options, optionsArray.size());

        // Use the Faker's random instance for consistency
        RandomService contextRandom = context.faker().random();
        int chosenIndex = chooseWeightedIndex(weights, contextRandom);
        return optionsArray.get(chosenIndex);
    }

    @Override
    public JsonNode generateWithFilter(GeneratorContext context, List<JsonNode> filterValues) {
        JsonNode options = context.options();
        JsonNode optionsArray = options.get("options");

        // If no filtering needed, use regular generate
        if (filterValues == null || filterValues.isEmpty()) {
            return generate(context);
        }

        // Get weights if present, default to 1.0 for each option
        List<Double> originalWeights = getWeights(options, optionsArray.size());

        // Filter out options and maintain corresponding weights
        List<JsonNode> validOptions = new ArrayList<>();
        List<Double> validWeights = new ArrayList<>();

        for (int i = 0; i < optionsArray.size(); i++) {
            JsonNode option = optionsArray.get(i);
            boolean shouldFilter = filterValues.contains(option);

            if (!shouldFilter) {
                validOptions.add(option);
                validWeights.add(originalWeights.get(i));
            }
        }

        // Check if all options were filtered out
        if (validOptions.isEmpty()) {
            throw new FilteringException("All choice options were filtered out");
        }

        // Choose from valid options using their weights
        RandomService contextRandom = context.faker().random();
        int chosenIndex = chooseWeightedIndex(validWeights, contextRandom);
        return validOptions.get(chosenIndex);
    }

    @Override
    public boolean supportsFiltering() {
        return true;
    }

    @Override
    public JsonNode generateAtPath(GeneratorContext context, String path) {
        // Choice generators don't support path extraction
        return generate(context);
    }

    private List<Double> getWeights(JsonNode options, int optionsCount) {
        if (!options.has("weights")) {
            // Default to equal weights of 1.0
            List<Double> defaultWeights = new ArrayList<>();
            for (int i = 0; i < optionsCount; i++) {
                defaultWeights.add(1.0);
            }
            return defaultWeights;
        }

        JsonNode weightsArray = options.get("weights");
        List<Double> weights = new ArrayList<>();
        for (JsonNode weightNode : weightsArray) {
            weights.add(weightNode.asDouble());
        }
        return weights;
    }

    private int chooseWeightedIndex(List<Double> weights, RandomService randomInstance) {
        if (weights.size() == 1) {
            return 0;
        }

        // Calculate total weight
        double totalWeight = weights.stream().mapToDouble(Double::doubleValue).sum();

        // If all weights are equal (or total is 0), use simple random selection
        if (totalWeight == 0.0 || weights.stream().allMatch(w -> w.equals(weights.get(0)))) {
            return randomInstance.nextInt(weights.size());
        }

        // Weighted selection
        double randomValue = randomInstance.nextDouble() * totalWeight;
        double cumulativeWeight = 0.0;

        for (int i = 0; i < weights.size(); i++) {
            cumulativeWeight += weights.get(i);
            if (randomValue <= cumulativeWeight) {
                return i;
            }
        }

        // Fallback to last option (should rarely happen due to floating point precision)
        return weights.size() - 1;
    }
}
