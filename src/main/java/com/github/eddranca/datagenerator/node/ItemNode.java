package com.github.eddranca.datagenerator.node;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Node representing an item definition within a collection.
 * Contains all field definitions for the item.
 */
public class ItemNode implements DslNode {
    private final Map<String, DslNode> fields;

    public ItemNode(Map<String, DslNode> fields) {
        this.fields = new LinkedHashMap<>(fields);
    }

    public Map<String, DslNode> getFields() {
        return fields;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitItem(this);
    }
}
