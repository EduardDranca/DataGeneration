package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.node.ArrayFieldNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.LiteralFieldNode;
import com.github.eddranca.datagenerator.node.ObjectFieldNode;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.eddranca.datagenerator.builder.KeyWords.*;

/**
 * Builder for field nodes (literal, object, array with count syntax).
 */
public class FieldNodeBuilder {
    private final NodeBuilderContext context;
    private final GeneratedFieldNodeBuilder generatedFieldBuilder;
    private final ReferenceFieldNodeBuilder referenceFieldBuilder;
    private final ArrayFieldNodeBuilder arrayFieldBuilder;

    public FieldNodeBuilder(NodeBuilderContext context) {
        this.context = context;
        this.generatedFieldBuilder = new GeneratedFieldNodeBuilder(context, this);
        this.referenceFieldBuilder = new ReferenceFieldNodeBuilder(context, this);
        this.arrayFieldBuilder = new ArrayFieldNodeBuilder(context, this);
    }

    public DslNode buildField(String fieldName, JsonNode fieldDef) {
        // Check for count field first - this creates arrays using the shorthand syntax
        if (fieldDef.isObject() && fieldDef.has(COUNT)) {
            return buildFieldWithCount(fieldName, fieldDef);
        }

        if (fieldDef.has(GENERATOR)) {
            return generatedFieldBuilder.buildGeneratorBasedField(fieldName, fieldDef);
        }

        if (fieldDef.has(REFERENCE)) {
            return referenceFieldBuilder.buildReferenceBasedField(fieldName, fieldDef);
        }

        if (fieldDef.has(ARRAY)) {
            return arrayFieldBuilder.buildArrayField(fieldName, fieldDef);
        }

        if (fieldDef.isObject()) {
            return buildObjectField(fieldDef);
        }

        return new LiteralFieldNode(fieldDef);
    }

    private DslNode buildObjectField(JsonNode fieldDef) {
        Map<String, DslNode> fields = new LinkedHashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = fieldDef.fields(); it.hasNext(); ) {
            var field = it.next();
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
            context.addError("Field '" + fieldName + "' count must be a number");
            return -1;
        }

        int count = countNode.asInt();
        if (count < 0) {
            context.addError("Field '" + fieldName + "' count must be non-negative");
            return -1;
        }
        return count;
    }

    private DslNode buildItemNodeFromCountField(String fieldName, JsonNode fieldDef) {
        ObjectNode itemDef = fieldDef.deepCopy();
        itemDef.remove(COUNT);

        if (itemDef.isEmpty()) {
            context.addError("Field '" + fieldName + "' with count must have additional field definition");
            return null;
        }

        if (itemDef.size() == 1 && itemDef.has("value")) {
            return new LiteralFieldNode(itemDef.get("value"));
        }

        return buildField("item", itemDef);
    }
}