package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Field node that contains a literal value (string, number, boolean, etc.).
 * Used for static values that don't need generation.
 */
public class LiteralFieldNode implements DslNode {
    private final JsonNode value;

    public LiteralFieldNode(JsonNode value) {
        this.value = value;
    }

    public JsonNode getValue() {
        return value;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitLiteralField(this);
    }
}
