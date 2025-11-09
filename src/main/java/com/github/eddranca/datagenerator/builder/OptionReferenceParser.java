package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.node.AbstractReferenceNode;
import com.github.eddranca.datagenerator.node.GeneratorOptions;
import com.github.eddranca.datagenerator.node.OptionReferenceNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

        Iterator<Map.Entry<String, JsonNode>> fields = optionsNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String optionKey = entry.getKey();
            JsonNode optionValue = entry.getValue();

            if (isRuntimeReference(optionValue)) {
                OptionReferenceNode optionRef = parseOptionReference(fieldName, optionKey, optionValue);
                if (optionRef != null) {
                    runtimeOptions.put(optionKey, optionRef);
                }
            } else {
                staticOptions.set(optionKey, optionValue);
            }
        }

        return new GeneratorOptions(staticOptions, runtimeOptions);
    }

    private boolean isRuntimeReference(JsonNode value) {
        return value.isObject() && value.has(REF);
    }

    private OptionReferenceNode parseOptionReference(String fieldName, String optionKey, JsonNode refNode) {
        String refString = refNode.get(REF).asText();
        
        // Build the reference node
        AbstractReferenceNode reference = referenceBuilder.buildReferenceNode(
            fieldName + "." + optionKey, 
            refString
        );

        if (reference == null) {
            return null;
        }

        // Check for mapping
        if (refNode.has(MAP)) {
            JsonNode mapNode = refNode.get(MAP);
            if (!mapNode.isObject()) {
                context.addError("Option '" + optionKey + "' in field '" + fieldName + 
                    "' has invalid map - must be an object");
                return null;
            }

            Map<String, JsonNode> valueMap = parseValueMap(mapNode);
            return new OptionReferenceNode(reference, valueMap);
        }

        return new OptionReferenceNode(reference);
    }

    private Map<String, JsonNode> parseValueMap(JsonNode mapNode) {
        Map<String, JsonNode> valueMap = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = mapNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            valueMap.put(entry.getKey(), entry.getValue());
        }
        return valueMap;
    }
}
