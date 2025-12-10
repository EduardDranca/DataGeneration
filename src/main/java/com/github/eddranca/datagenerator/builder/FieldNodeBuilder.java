package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.node.ArrayFieldNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.LiteralFieldNode;
import com.github.eddranca.datagenerator.node.ObjectFieldNode;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.eddranca.datagenerator.builder.KeyWords.ARRAY;
import static com.github.eddranca.datagenerator.builder.KeyWords.COUNT;
import static com.github.eddranca.datagenerator.builder.KeyWords.GENERATOR;
import static com.github.eddranca.datagenerator.builder.KeyWords.REF;

/**
 * Main field builder that coordinates with specialized builders.
 * Handles basic field types (literal, object, count arrays) and delegates to specialists.
 */
class FieldNodeBuilder implements FieldBuilder {
    private final NodeBuilderContext context;

    public FieldNodeBuilder(NodeBuilderContext context) {
        this.context = context;
    }

    @Override
    public DslNode buildField(String fieldName, JsonNode fieldDef) {
        if (fieldDef.isObject() && fieldDef.has(COUNT)) {
            return buildFieldWithCount(fieldName, fieldDef);
        }

        if (fieldDef.isObject()) {
            validateNoConflictingKeywords(fieldName, fieldDef);
        }

        if (fieldDef.has(GENERATOR)) {
            GeneratedFieldNodeBuilder generatedBuilder = new GeneratedFieldNodeBuilder(context, this);
            return generatedBuilder.buildGeneratorBasedField(fieldName, fieldDef);
        }

        if (fieldDef.has(REF)) {
            ReferenceFieldNodeBuilder referenceBuilder = new ReferenceFieldNodeBuilder(context, this);
            return referenceBuilder.buildReferenceBasedField(fieldName, fieldDef);
        }

        if (fieldDef.has(ARRAY)) {
            ArrayFieldNodeBuilder arrayBuilder = new ArrayFieldNodeBuilder(context, this);
            return arrayBuilder.buildArrayField(fieldName, fieldDef);
        }

        if (fieldDef.isObject()) {
            return buildObjectField(fieldDef);
        }

        return new LiteralFieldNode(fieldDef);
    }

    private void validateNoConflictingKeywords(String fieldName, JsonNode fieldDef) {
        int keywordCount = 0;
        if (fieldDef.has(GENERATOR)) keywordCount++;
        if (fieldDef.has(REF)) keywordCount++;
        if (fieldDef.has(ARRAY)) keywordCount++;

        if (keywordCount > 1) {
            addFieldError(fieldName, "cannot have multiple keywords (gen, ref, array)");
        }
    }

    private DslNode buildObjectField(JsonNode fieldDef) {
        Map<String, DslNode> fields = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> field : fieldDef.properties()) {
            DslNode fieldNode = buildField(field.getKey(), field.getValue());
            if (fieldNode != null) {
                fields.put(field.getKey(), fieldNode);
            }
        }
        return new ObjectFieldNode(fields);
    }

    private ArrayFieldNode buildFieldWithCount(String fieldName, JsonNode fieldDef) {
        int count = validateCountValue(fieldName, fieldDef);
        if (count < 0) {
            return null;
        }

        DslNode itemNode = buildItemNodeFromCountField(fieldName, fieldDef);
        if (itemNode == null) {
            return null;
        }

        return new ArrayFieldNode(count, itemNode);
    }

    private int validateCountValue(String fieldName, JsonNode fieldDef) {
        JsonNode countNode = fieldDef.get(COUNT);
        if (!countNode.isNumber()) {
            addFieldError(fieldName, "count must be a number");
            return -1;
        }

        int count = countNode.asInt();
        if (count < 0) {
            addFieldError(fieldName, "count must be non-negative");
            return -1;
        }
        return count;
    }

    private DslNode buildItemNodeFromCountField(String fieldName, JsonNode fieldDef) {
        ObjectNode itemDef = fieldDef.deepCopy();
        itemDef.remove(COUNT);

        if (itemDef.isEmpty()) {
            addFieldError(fieldName, "with count must have additional field definition");
            return null;
        }

        if (itemDef.size() == 1 && itemDef.has("value")) {
            return new LiteralFieldNode(itemDef.get("value"));
        }

        return buildField("item", itemDef);
    }

    private void addFieldError(String fieldName, String message) {
        context.addError("Field '" + fieldName + "' " + message);
    }
}
