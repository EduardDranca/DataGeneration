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
        JsonNode optionsNode = mapper.createObjectNode().put("type", "object");
        GeneratorOptions options = new GeneratorOptions(optionsNode);
        GeneratedFieldNode node = new GeneratedFieldNode("person", options, "firstName", List.of());

        assertThat(node.getGeneratorName()).isEqualTo("person");
        assertThat(node.getOptions().getStaticOptions()).isEqualTo(optionsNode);
        assertThat(node.getPath()).isEqualTo("firstName");
        assertThat(node.hasPath()).isTrue();
        assertThat(node.hasFilters()).isFalse();
    }

    @Test
    void testGeneratedFieldWithEmptyPath() {
        JsonNode optionsNode = mapper.createObjectNode();
        GeneratorOptions options = new GeneratorOptions(optionsNode);
        GeneratedFieldNode node = new GeneratedFieldNode("name", options, "", List.of());

        assertThat(node.getPath()).isEmpty();
        assertThat(node.hasPath()).isFalse();
    }

    @Test
    void testGeneratedFieldWithFilters() {
        JsonNode optionsNode = mapper.createObjectNode();
        GeneratorOptions options = new GeneratorOptions(optionsNode);
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
        GeneratorOptions options = new GeneratorOptions(null);
        GeneratedFieldNode node = new GeneratedFieldNode("name", options, null, List.of());

        assertThat(node.getGeneratorName()).isEqualTo("name");
        assertThat(node.getOptions().getStaticOptions()).isNull();
    }

    @Test
    void testGeneratedFieldWithComplexOptions() {
        JsonNode optionsNode = mapper.createObjectNode()
            .put("min", 1)
            .put("max", 100)
            .put("type", "integer");
        GeneratorOptions options = new GeneratorOptions(optionsNode);

        GeneratedFieldNode node = new GeneratedFieldNode("number", options, null, List.of());

        assertThat(node.getOptions().getStaticOptions().get("min").asInt()).isEqualTo(1);
        assertThat(node.getOptions().getStaticOptions().get("max").asInt()).isEqualTo(100);
        assertThat(node.getOptions().getStaticOptions().get("type").asText()).isEqualTo("integer");
    }

    @Test
    void testGeneratedFieldIsImmutable() {
        JsonNode optionsNode = mapper.createObjectNode();
        GeneratorOptions options = new GeneratorOptions(optionsNode);
        FilterNode filter = new FilterNode(new LiteralFieldNode(mapper.valueToTree("test")));
        List<FilterNode> originalFilters = List.of(filter);

        GeneratedFieldNode node = new GeneratedFieldNode("name", options, null, originalFilters);

        // Original list should not affect the node
        assertThat(node.getFilters()).hasSize(1);
        assertThat(node.getFilters().get(0)).isEqualTo(filter);
    }
}
