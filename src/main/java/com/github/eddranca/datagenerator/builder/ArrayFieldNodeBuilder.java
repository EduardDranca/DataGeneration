package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.node.ArrayFieldNode;
import com.github.eddranca.datagenerator.node.DslNode;

import static com.github.eddranca.datagenerator.builder.KeyWords.*;

/**
 * Builder for array field nodes.
 */
// TODO: Most of these classes should be package-private, we don't need to expose most of them publicly
public class ArrayFieldNodeBuilder {
    private final NodeBuilderContext context;
    private final FieldBuilder fieldBuilder;

    public ArrayFieldNodeBuilder(NodeBuilderContext context, FieldBuilder fieldBuilder) {
        this.context = context;
        this.fieldBuilder = fieldBuilder;
    }

    public DslNode buildArrayField(String fieldName, JsonNode fieldDef) {
        JsonNode arrayDef = fieldDef.get(ARRAY);

        if (!validateArrayDefinition(fieldName, arrayDef)) {
            return null;
        }

        // Build the item node
        DslNode itemNode = fieldBuilder.buildField(fieldName + "[item]", arrayDef.get(ITEM));
        if (itemNode == null) {
            return null;
        }

        return createArrayFieldNode(fieldName, arrayDef, itemNode);
    }

    private boolean validateArrayDefinition(String fieldName, JsonNode arrayDef) {
        if (!arrayDef.isObject()) {
            addArrayFieldError(fieldName, "array definition must be an object");
            return false;
        }

        if (!arrayDef.has(ITEM)) {
            addArrayFieldError(fieldName, "must have an 'item' definition");
            return false;
        }

        // Parse size configuration
        boolean hasSize = arrayDef.has(SIZE);
        boolean hasMinMax = arrayDef.has(MIN_SIZE) || arrayDef.has(MAX_SIZE);

        if (hasSize && hasMinMax) {
            addArrayFieldError(fieldName, "cannot have both 'size' and 'minSize/maxSize'");
            return false;
        }

        if (!hasSize && !hasMinMax) {
            addArrayFieldError(fieldName, "must have either 'size' or 'minSize/maxSize'");
            return false;
        }

        return true;
    }

    private DslNode createArrayFieldNode(String fieldName, JsonNode arrayDef, DslNode itemNode) {
        boolean hasSize = arrayDef.has(SIZE);

        if (hasSize) {
            return createFixedSizeArrayNode(fieldName, arrayDef, itemNode);
        } else {
            return createRangeSizeArrayNode(fieldName, arrayDef, itemNode);
        }
    }

    private DslNode createFixedSizeArrayNode(String fieldName, JsonNode arrayDef, DslNode itemNode) {
        int size = arrayDef.get(SIZE).asInt();
        if (size < 0) {
            context.addError("Array field '" + fieldName + "' size must be non-negative");
            return null;
        }
        return new ArrayFieldNode(size, itemNode);
    }

    private DslNode createRangeSizeArrayNode(String fieldName, JsonNode arrayDef, DslNode itemNode) {
        int minSize = arrayDef.path(MIN_SIZE).asInt(0);
        int maxSize = arrayDef.path(MAX_SIZE).asInt(10);

        if (minSize < 0) {
            context.addError("Array field '" + fieldName + "' minSize must be non-negative");
            return null;
        }

        if (maxSize < minSize) {
            context.addError("Array field '" + fieldName + "' maxSize must be >= minSize");
            return null;
        }

        return new ArrayFieldNode(minSize, maxSize, itemNode);
    }

    private void addArrayFieldError(String fieldName, String message) {
        context.addError("Array field '" + fieldName + "' " + message);
    }
}
