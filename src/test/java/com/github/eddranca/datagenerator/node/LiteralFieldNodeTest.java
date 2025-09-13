package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LiteralFieldNodeTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void testStringLiteralField() {
        JsonNode stringValue = mapper.valueToTree("Hello World");
        LiteralFieldNode node = new LiteralFieldNode(stringValue);

        assertThat(node.getValue()).isEqualTo(stringValue);
        assertThat(node.getValue().asText()).isEqualTo("Hello World");
    }

    @Test
    void testNumberLiteralField() {
        JsonNode numberValue = mapper.valueToTree(42);
        LiteralFieldNode node = new LiteralFieldNode(numberValue);

        assertThat(node.getValue()).isEqualTo(numberValue);
        assertThat(node.getValue().asInt()).isEqualTo(42);
    }

    @Test
    void testBooleanLiteralField() {
        JsonNode booleanValue = mapper.valueToTree(true);
        LiteralFieldNode node = new LiteralFieldNode(booleanValue);

        assertThat(node.getValue()).isEqualTo(booleanValue);
        assertThat(node.getValue().asBoolean()).isTrue();
    }

    @Test
    void testNullLiteralField() {
        JsonNode nullValue = mapper.nullNode();
        LiteralFieldNode node = new LiteralFieldNode(nullValue);

        assertThat(node.getValue()).isEqualTo(nullValue);
        assertThat(node.getValue().isNull()).isTrue();
    }
}
