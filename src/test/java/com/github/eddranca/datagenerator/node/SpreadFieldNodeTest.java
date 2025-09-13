package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpreadFieldNodeTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void testSpreadFieldWithBasicFields() {
        JsonNode options = mapper.createObjectNode().put("type", "person");
        List<String> fields = List.of("firstName", "lastName", "age");

        SpreadFieldNode node = new SpreadFieldNode("person", options, fields);

        assertThat(node.getGeneratorName()).isEqualTo("person");
        assertThat(node.getOptions()).isEqualTo(options);
        assertThat(node.getFields()).containsExactly("firstName", "lastName", "age");
    }

    @Test
    void testSpreadFieldWithMappedFields() {
        JsonNode options = mapper.createObjectNode();
        List<String> fields = List.of("name:firstName", "years:age", "email");

        SpreadFieldNode node = new SpreadFieldNode("user", options, fields);

        assertThat(node.getGeneratorName()).isEqualTo("user");
        assertThat(node.getFields()).containsExactly("name:firstName", "years:age", "email");
    }

    @Test
    void testSpreadFieldWithEmptyFields() {
        JsonNode options = mapper.createObjectNode();
        List<String> fields = List.of();

        SpreadFieldNode node = new SpreadFieldNode("generator", options, fields);

        assertThat(node.getFields()).isEmpty();
    }

    @Test
    void testSpreadFieldWithNullOptions() {
        List<String> fields = List.of("field1", "field2");

        SpreadFieldNode node = new SpreadFieldNode("generator", null, fields);

        assertThat(node.getGeneratorName()).isEqualTo("generator");
        assertThat(node.getOptions()).isNull();
        assertThat(node.getFields()).containsExactly("field1", "field2");
    }

    @Test
    void testSpreadFieldWithComplexOptions() {
        JsonNode options = mapper.createObjectNode()
                .put("locale", "en_US")
                .put("seed", 12345);
        List<String> fields = List.of("name", "address");

        SpreadFieldNode node = new SpreadFieldNode("faker", options, fields);

        assertThat(node.getOptions().get("locale").asText()).isEqualTo("en_US");
        assertThat(node.getOptions().get("seed").asInt()).isEqualTo(12345);
    }

    @Test
    void testSpreadFieldIsImmutable() {
        JsonNode options = mapper.createObjectNode();
        List<String> originalFields = new ArrayList<>();
        originalFields.add("field1");

        SpreadFieldNode node = new SpreadFieldNode("generator", options, originalFields);

        // Modify original list - should not affect the node
        originalFields.add("field2");

        assertThat(node.getFields()).hasSize(1);
        assertThat(node.getFields()).containsExactly("field1");
    }
}
