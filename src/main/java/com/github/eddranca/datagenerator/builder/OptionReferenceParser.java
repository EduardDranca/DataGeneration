package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.node.AbstractReferenceNode;
import com.github.eddranca.datagenerator.node.ChoiceFieldNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.GeneratedFieldNode;
import com.github.eddranca.datagenerator.node.GeneratorOptionNode;
import com.github.eddranca.datagenerator.node.GeneratorOptions;
import com.github.eddranca.datagenerator.node.OptionReferenceNode;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.eddranca.datagenerator.builder.KeyWords.GENERATOR;
import static com.github.eddranca.datagenerator.builder.KeyWords.MAP;
import static com.github.eddranca.datagenerator.builder.KeyWords.REF;

/**
 * Parses generator options that may contain runtime-computed references.
 * <p>
 * This parser examines generator options and separates them into:
 * <ul>
 *   <li>Static options: Values known at parse time (numbers, strings, etc.)</li>
 *   <li>Runtime options: References to other fields that must be resolved during generation</li>
 * </ul>
 * <p>
 * The parser recognizes runtime references by the presence of a "ref" key in the option value.
 * It delegates reference parsing to {@link ReferenceFieldNodeBuilder} to ensure consistent
 * reference handling across the system.
 *
 * @see GeneratorOptions
 * @see OptionReferenceNode
 */
class OptionReferenceParser {
    private static final String ERROR_IN_FIELD = "' in field '";

    private final NodeBuilderContext context;
    private final ReferenceFieldNodeBuilder referenceBuilder;

    public OptionReferenceParser(NodeBuilderContext context, ReferenceFieldNodeBuilder referenceBuilder) {
        this.context = context;
        this.referenceBuilder = referenceBuilder;
    }

    /**
     * Parses generator options, extracting any runtime-computed references.
     * Returns a GeneratorOptions object containing both static and runtime options.
     */
    public GeneratorOptions parseOptions(String fieldName, JsonNode optionsNode) {
        if (optionsNode == null || !optionsNode.isObject()) {
            return new GeneratorOptions(optionsNode);
        }

        ObjectNode staticOptions = ((ObjectNode) optionsNode).objectNode();
        Map<String, OptionReferenceNode> runtimeOptions = new HashMap<>();
        Map<String, GeneratorOptionNode> generatorOptions = new HashMap<>();

        for (Map.Entry<String, JsonNode> entry : optionsNode.properties()) {
            String optionKey = entry.getKey();
            JsonNode optionValue = entry.getValue();

            if (isRuntimeReference(optionValue)) {
                OptionReferenceNode optionRef = parseOptionReference(fieldName, optionKey, optionValue);
                if (optionRef != null) {
                    runtimeOptions.put(optionKey, optionRef);
                }
            } else if (isRuntimeGenerator(optionValue)) {
                GeneratorOptionNode generatorOption = parseGeneratorOption(fieldName, optionKey, optionValue);
                if (generatorOption != null) {
                    generatorOptions.put(optionKey, generatorOption);
                }
            } else {
                staticOptions.set(optionKey, optionValue);
            }
        }

        return new GeneratorOptions(staticOptions, runtimeOptions, generatorOptions);
    }

    private boolean isRuntimeReference(JsonNode value) {
        return value.isObject() && value.has(REF);
    }

    private boolean isRuntimeGenerator(JsonNode value) {
        return value.isObject() && value.has(GENERATOR);
    }

    private OptionReferenceNode parseOptionReference(String fieldName, String optionKey, JsonNode refNode) {
        try {
            String refString = refNode.get(REF).asText();

            // Build the reference node
            AbstractReferenceNode reference = referenceBuilder.buildReferenceNode(
                fieldName + "." + optionKey,
                refString
            );

            if (reference == null) {
                context.addError("Invalid reference '" + refString + "' in option '" + optionKey + "' of field '" + fieldName + "'");
                return null;
            }

            // Check for mapping
            if (refNode.has(MAP)) {
                JsonNode mapNode = refNode.get(MAP);
                if (!mapNode.isObject()) {
                    context.addError("Option '" + optionKey + ERROR_IN_FIELD + fieldName +
                        "' has invalid map - must be an object");
                    return null;
                }

                Map<String, JsonNode> valueMap = mapNode.propertyStream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                return new OptionReferenceNode(reference, valueMap);
            }

            return new OptionReferenceNode(reference);
        } catch (Exception e) {
            context.addError("Failed to parse runtime option '" + optionKey + ERROR_IN_FIELD + fieldName + "': " + e.getMessage());
            return null;
        }
    }

    private GeneratorOptionNode parseGeneratorOption(String fieldName, String optionKey, JsonNode generatorNode) {
        try {
            // Create a GeneratedFieldNode from the generator definition
            // The generatorNode should be a complete field definition (e.g., {"gen": "choice", "options": [1,2,3]})
            FieldNodeBuilder fieldBuilder = new FieldNodeBuilder(context);
            GeneratedFieldNodeBuilder generatedBuilder = new GeneratedFieldNodeBuilder(context, fieldBuilder);
            DslNode generatorField = generatedBuilder.buildGeneratorBasedField(fieldName + "." + optionKey, generatorNode);

            if (generatorField instanceof GeneratedFieldNode generatedFieldNode) {
                return new GeneratorOptionNode(generatedFieldNode);
            } else if (generatorField instanceof ChoiceFieldNode choiceFieldNode) {
                // Handle choice fields which are also valid generator options
                return new GeneratorOptionNode(choiceFieldNode);
            } else {
                context.addError("Failed to create generator option '" + optionKey + ERROR_IN_FIELD + fieldName + "' - unexpected node type: " + (generatorField != null ? generatorField.getClass().getSimpleName() : "null"));
                return null;
            }
        } catch (Exception e) {
            context.addError("Failed to parse generator option '" + optionKey + ERROR_IN_FIELD + fieldName + "': " + e.getMessage());
            return null;
        }
    }
}
