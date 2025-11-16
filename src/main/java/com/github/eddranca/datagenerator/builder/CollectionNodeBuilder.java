package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.node.CollectionNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.ItemNode;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.eddranca.datagenerator.builder.KeyWords.COUNT;
import static com.github.eddranca.datagenerator.builder.KeyWords.ITEM;
import static com.github.eddranca.datagenerator.builder.KeyWords.NAME;
import static com.github.eddranca.datagenerator.builder.KeyWords.PICK;


/**
 * Builder for collection nodes.
 */
class CollectionNodeBuilder {
    private final NodeBuilderContext context;
    private final FieldNodeBuilder fieldBuilder;

    public CollectionNodeBuilder(NodeBuilderContext context, FieldNodeBuilder fieldBuilder) {
        this.context = context;
        this.fieldBuilder = fieldBuilder;
    }

    public CollectionNode buildCollection(String name, JsonNode def) {
        if (!validateCollectionStructure(name, def)) {
            return null;
        }

        int count = validateAndGetCount(name, def);
        ItemNode item = buildItem(def.get(ITEM));
        if (item == null) {
            return null;
        }

        Map<String, Integer> picks = buildCollectionPicks(name, def, count);
        String collectionName = def.has(NAME) ? def.get(NAME).asText() : null;

        return new CollectionNode(name, count, item, picks, collectionName);
    }

    private boolean validateCollectionStructure(String name, JsonNode def) {
        if (!def.isObject()) {
            addCollectionError(name, "must be an object");
            return false;
        }
        if (!def.has(ITEM)) {
            addCollectionError(name, "is missing required 'item' field");
            return false;
        }
        return true;
    }

    private int validateAndGetCount(String name, JsonNode def) {
        int count = def.path(COUNT).asInt(1);
        if (count < 0) {
            addCollectionError(name, "count must be non-negative, got: " + count);
            return 1; // Use default for recovery
        }
        return count;
    }


    private Map<String, Integer> buildCollectionPicks(String name, JsonNode def, int count) {
        Map<String, Integer> picks = new HashMap<>();
        if (def.has(PICK)) {
            JsonNode pickNode = def.get(PICK);
            if (pickNode.isObject()) {
                for (Map.Entry<String, JsonNode> entry : pickNode.properties()) {
                    int index = entry.getValue().asInt();
                    if (index >= count) {
                        addCollectionPickError(name, entry.getKey(), index, count);
                    } else {
                        context.declarePick(entry.getKey());
                        picks.put(entry.getKey(), index);
                    }
                }
            } else {
                addCollectionError(name, "pick must be an object");
            }
        }
        return picks;
    }

    private void addCollectionError(String name, String message) {
        context.addError("Collection '" + name + "' " + message);
    }

    private void addCollectionPickError(String name, String alias, int index, int count) {
        context.addError("Collection '" + name + "' pick alias '" + alias +
            "' index " + index + " is out of bounds (count: " + count + ")");
    }

    private ItemNode buildItem(JsonNode itemDef) {
        if (!itemDef.isObject()) {
            context.addError("Item definition must be an object");
            return null;
        }

        Map<String, DslNode> fields = new LinkedHashMap<>();

        for (Map.Entry<String, JsonNode> entry : itemDef.properties()) {
            String fieldName = entry.getKey();
            JsonNode fieldDef = entry.getValue();

            DslNode field = fieldBuilder.buildField(fieldName, fieldDef);
            if (field != null) {
                fields.put(fieldName, field);
            }
        }

        return new ItemNode(fields);
    }
}
