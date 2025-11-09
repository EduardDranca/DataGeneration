package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.node.ChoiceFieldNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.FilterNode;
import com.github.eddranca.datagenerator.node.GeneratedFieldNode;
import com.github.eddranca.datagenerator.node.GeneratorOptions;
import com.github.eddranca.datagenerator.node.SpreadFieldNode;

import java.util.ArrayList;
import java.util.List;

import static com.github.eddranca.datagenerator.builder.KeyWords.ELLIPSIS;
import static com.github.eddranca.datagenerator.builder.KeyWords.FIELDS;
import static com.github.eddranca.datagenerator.builder.KeyWords.FILTER;
import static com.github.eddranca.datagenerator.builder.KeyWords.GENERATOR;
import static com.github.eddranca.datagenerator.builder.KeyWords.OPTIONS;

/**
 * Builder for generated field nodes (generators, choices, spreads).
 */
class GeneratedFieldNodeBuilder {
    private final NodeBuilderContext context;
    private final FieldBuilder fieldBuilder;
    private final ReferenceFieldNodeBuilder referenceBuilder;

    public GeneratedFieldNodeBuilder(NodeBuilderContext context, FieldBuilder fieldBuilder) {
        this.context = context;
        this.fieldBuilder = fieldBuilder;
        this.referenceBuilder = new ReferenceFieldNodeBuilder(context, fieldBuilder);
    }

    public DslNode buildGeneratorBasedField(String fieldName, JsonNode fieldDef) {
        if (fieldName.startsWith(ELLIPSIS)) {
            return buildSpreadField(fieldName, fieldDef);
        }
        return buildGeneratedField(fieldName, fieldDef);
    }

    private DslNode buildGeneratedField(String fieldName, JsonNode fieldDef) {
        String generatorSpec = fieldDef.get(GENERATOR).asText();

        if ("choice".equals(generatorSpec)) {
            return buildChoiceField(fieldName, fieldDef);
        }

        // Parse generator specification
        GeneratorInfo generatorInfo = parseGeneratorSpec(generatorSpec);

        // Validate generator exists
        if (!validateGenerator(generatorInfo.name)) {
            return null;
        }

        // Build filters if present
        List<FilterNode> filters = buildGeneratedFieldFilters(fieldName, fieldDef);

        // Parse options (may contain runtime references)
        OptionReferenceParser optionParser = new OptionReferenceParser(context, referenceBuilder);
        GeneratorOptions options = optionParser.parseOptions(fieldName, fieldDef);

        return new GeneratedFieldNode(generatorInfo.name, options, generatorInfo.path, filters);
    }

    private GeneratorInfo parseGeneratorSpec(String generatorSpec) {
        // Handle dot notation (e.g., "name.firstName")
        if (generatorSpec.contains(".")) {
            String[] parts = generatorSpec.split("\\.", 2);
            return new GeneratorInfo(parts[0], parts[1]);
        } else {
            return new GeneratorInfo(generatorSpec, null);
        }
    }

    private boolean validateGenerator(String generatorName) {
        if (!context.isGeneratorRegistered(generatorName)) {
            addUnknownGeneratorError(generatorName);
            return false;
        }
        return true;
    }

    private List<FilterNode> buildGeneratedFieldFilters(String fieldName, JsonNode fieldDef) {
        List<FilterNode> filters = new ArrayList<>();
        if (fieldDef.has(FILTER)) {
            JsonNode filtersNode = fieldDef.get(FILTER);
            if (filtersNode.isArray()) {
                for (JsonNode filterNode : filtersNode) {
                    DslNode filterExpression = fieldBuilder.buildField(fieldName + "[filter]", filterNode);
                    if (filterExpression != null) {
                        filters.add(new FilterNode(filterExpression));
                    }
                }
            } else {
                addGeneratedFieldError(fieldName, "filter must be an array");
            }
        }
        return filters;
    }

    private DslNode buildChoiceField(String fieldName, JsonNode fieldDef) {
        if (!validateChoiceFieldStructure(fieldName, fieldDef)) {
            return null;
        }

        List<DslNode> options = buildChoiceOptions(fieldName, fieldDef);
        if (options.isEmpty()) {
            addChoiceFieldError(fieldName, "must have at least one valid option");
            return null; // Keep this as null since we can't create a valid choice without options
        }

        List<FilterNode> filters = buildChoiceFilters(fieldName, fieldDef);

        if (fieldDef.has("weights")) {
            List<Double> weights = buildChoiceWeights(fieldName, fieldDef, options.size());
            if (!weights.isEmpty()) {
                return ChoiceFieldNode.withWeightsAndFilters(options, weights, filters);
            }
            // If weights parsing failed, fall back to uniform distribution
        }

        return filters.isEmpty() ? new ChoiceFieldNode(options) : ChoiceFieldNode.withFilters(options, filters);
    }

    private boolean validateChoiceFieldStructure(String fieldName, JsonNode fieldDef) {
        if (!fieldDef.has(OPTIONS)) {
            addChoiceFieldError(fieldName, "is missing required 'options' array");
            return false;
        }
        if (!fieldDef.get(OPTIONS).isArray()) {
            addChoiceFieldError(fieldName, "options must be an array");
            return false;
        }
        return true;
    }

    private List<DslNode> buildChoiceOptions(String fieldName, JsonNode fieldDef) {
        List<DslNode> options = new ArrayList<>();
        JsonNode optionsNode = fieldDef.get(OPTIONS);
        for (JsonNode optionNode : optionsNode) {
            DslNode option = fieldBuilder.buildField(fieldName + "[option]", optionNode);
            if (option != null) {
                options.add(option);
            }
        }
        return options;
    }

    private List<FilterNode> buildChoiceFilters(String fieldName, JsonNode fieldDef) {
        List<FilterNode> filters = new ArrayList<>();
        if (fieldDef.has(FILTER)) {
            JsonNode filtersNode = fieldDef.get(FILTER);
            if (filtersNode.isArray()) {
                for (JsonNode filterNode : filtersNode) {
                    DslNode filterExpression = fieldBuilder.buildField(fieldName + "[filter]", filterNode);
                    if (filterExpression != null) {
                        filters.add(new FilterNode(filterExpression));
                    }
                }
            } else {
                addChoiceFieldError(fieldName, "filter must be an array");
            }
        }
        return filters;
    }

    private List<Double> buildChoiceWeights(String fieldName, JsonNode fieldDef, int optionsCount) {
        JsonNode weightsNode = fieldDef.get("weights");

        if (!validateWeightsStructure(fieldName, weightsNode, optionsCount)) {
            // Return empty list to indicate no weights (uniform distribution)
            return new ArrayList<>();
        }

        return parseWeights(fieldName, weightsNode);

    }

    private boolean validateWeightsStructure(String fieldName, JsonNode weightsNode, int optionsCount) {
        if (!weightsNode.isArray()) {
            addChoiceFieldError(fieldName, "weights must be an array");
            return false;
        }

        if (weightsNode.size() != optionsCount) {
            addChoiceFieldError(fieldName, "weights array must have the same size as options array");
            return false;
        }

        return true;
    }

    private List<Double> parseWeights(String fieldName, JsonNode weightsNode) {
        List<Double> weights = new ArrayList<>();

        for (int i = 0; i < weightsNode.size(); i++) {
            Double weight = parseWeight(fieldName, weightsNode.get(i), i);
            if (weight == null) {
                // Skip invalid weights but continue processing others
                continue;
            }
            weights.add(weight);
        }

        return weights;
    }

    private Double parseWeight(String fieldName, JsonNode weightNode, int index) {
        if (!weightNode.isNumber()) {
            addChoiceFieldWeightError(fieldName, index, "must be a number");
            return null;
        }

        double weight = weightNode.asDouble();
        if (weight <= 0) {
            addChoiceFieldWeightError(fieldName, index, "must be positive");
            return null;
        }

        return Math.round(weight * 100.0) / 100.0;
    }

    private DslNode buildSpreadField(String fieldName, JsonNode fieldDef) {
        String generatorName = fieldDef.get(GENERATOR).asText();

        // Validate generator exists
        if (!context.isGeneratorRegistered(generatorName)) {
            addUnknownGeneratorError(generatorName);
            return null;
        }

        // Fields array is optional - if not provided, all generator fields will be used
        List<String> fields = new ArrayList<>();
        if (fieldDef.has(FIELDS)) {
            JsonNode fieldsNode = fieldDef.get(FIELDS);
            if (!fieldsNode.isArray()) {
                addSpreadFieldError(fieldName, "fields must be an array");
                return null;
            }

            for (JsonNode fieldNode : fieldsNode) {
                fields.add(fieldNode.asText());
            }

            if (fields.isEmpty()) {
                addSpreadFieldError(fieldName, "must have at least one field when fields array is provided");
                // Allow empty fields - this will spread all available fields from the generator
            }
        }
        // If fields is empty, it means use all available fields from the generator

        return new SpreadFieldNode(generatorName, fieldDef, fields);
    }

    private void addGeneratedFieldError(String fieldName, String message) {
        context.addError("Generated field '" + fieldName + "' " + message);
    }

    private void addChoiceFieldError(String fieldName, String message) {
        context.addError("Choice field '" + fieldName + "' " + message);
    }

    private void addChoiceFieldWeightError(String fieldName, int index, String message) {
        context.addError("Choice field '" + fieldName + "' weight at index " + index + " " + message);
    }

    private void addSpreadFieldError(String fieldName, String message) {
        context.addError("Spread field '" + fieldName + "' " + message);
    }

    private void addUnknownGeneratorError(String generatorName) {
        context.addError("Unknown generator: " + generatorName);
    }

    private static class GeneratorInfo {
        final String name;
        final String path;

        GeneratorInfo(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }
}
