package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class GeneratedFieldNodeTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void testGeneratedFieldWithPath() {
        JsonNode options = mapper.createObjectNode().put("type", "object");
        GeneratedFieldNode node = new GeneratedFieldNode("person", options, "firstName", List.of());

        assertThat(node.getGeneratorName()).isEqualTo("person");
        assertThat(node.getOptions()).isEqualTo(options);
        assertThat(node.getPath()).isEqualTo("firstName");
        assertThat(node.hasPath()).isTrue();
        assertThat(node.hasFilters()).isFalse();
    }

    @Test
    void testGeneratedFieldWithEmptyPath() {
        JsonNode options = mapper.createObjectNode();
        GeneratedFieldNode node = new GeneratedFieldNode("name", options, "", List.of());

        assertThat(node.getPath()).isEqualTo("");
        assertThat(node.hasPath()).isFalse();
    }

    @Test
    void testGeneratedFieldWithFilters() {
        JsonNode options = mapper.createObjectNode();
        FilterNode filter1 = new FilterNode(new LiteralFieldNode(mapper.valueToTree("exclude1")));
        FilterNode filter2 = new FilterNode(new LiteralFieldNode(mapper.valueToTree("exclude2")));
        List<FilterNode> filters = List.of(filter1, filter2);

        GeneratedFieldNode node = new GeneratedFieldNode("name", options, "path", filters);

        assertThat(node.hasFilters()).isTrue();
        assertThat(node.getFilters()).hasSize(2);
        assertThat(node.getFilters()).containsExactly(filter1, filter2);
    }

    @Test
    void testGeneratedFieldWithNullOptions() {
        GeneratedFieldNode node = new GeneratedFieldNode("name", null, null, List.of());

        assertThat(node.getGeneratorName()).isEqualTo("name");
        assertThat(node.getOptions()).isNull();
    }

    @Test
    void testGeneratedFieldWithComplexOptions() {
        JsonNode options = mapper.createObjectNode()
                .put("min", 1)
                .put("max", 100)
                .put("type", "integer");

        GeneratedFieldNode node = new GeneratedFieldNode("number", options, null, List.of());

        assertThat(node.getOptions().get("min").asInt()).isEqualTo(1);
        assertThat(node.getOptions().get("max").asInt()).isEqualTo(100);
        assertThat(node.getOptions().get("type").asText()).isEqualTo("integer");
    }

    @Test
    void testGeneratedFieldIsImmutable() {
        JsonNode options = mapper.createObjectNode();
        FilterNode filter = new FilterNode(new LiteralFieldNode(mapper.valueToTree("test")));
        List<FilterNode> originalFilters = List.of(filter);

        GeneratedFieldNode node = new GeneratedFieldNode("name", options, null, originalFilters);

        // Original list should not affect the node
        assertThat(node.getFilters()).hasSize(1);
        assertThat(node.getFilters().get(0)).isEqualTo(filter);
    }
}
