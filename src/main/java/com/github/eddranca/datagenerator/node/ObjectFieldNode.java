package com.github.eddranca.datagenerator.node;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Field node that generates nested objects with multiple fields.
 * Similar to ItemNode but represents a nested object within an item.
 */
public class ObjectFieldNode implements DslNode {
    private final Map<String, DslNode> fields;

    public ObjectFieldNode(Map<String, DslNode> fields) {
        this.fields = new LinkedHashMap<>(fields);
    }


    public Map<String, DslNode> getFields() {
        return fields;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitObjectField(this);
    }
}
