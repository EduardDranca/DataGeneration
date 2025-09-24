package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.visitor.EagerGenerationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SelfReferenceNodeTest {

    @Mock
    private EagerGenerationContext mockContext;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void testSelfReferenceProperties() {
        SelfReferenceNode node = new SelfReferenceNode("name", List.of(), true);

        assertThat(node.getFieldName()).isEqualTo("name");
        assertThat(node.isSequential()).isTrue();
        assertThat(node.getReferenceString()).isEqualTo("this.name");
    }

    @Test
    void testResolveWithExistingField() {
        SelfReferenceNode node = new SelfReferenceNode("name", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode()
            .put("name", "John")
            .put("age", 30);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.asText()).isEqualTo("John");
    }

    @Test
    void testResolveWithMissingField() {
        SelfReferenceNode node = new SelfReferenceNode("missingField", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode()
            .put("name", "John")
            .put("age", 30);

        when(mockContext.getMapper()).thenReturn(mapper);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.isNull()).isTrue();
    }

    @Test
    void testResolveWithNestedField() {
        SelfReferenceNode node = new SelfReferenceNode("address", List.of(), false);
        JsonNode address = mapper.createObjectNode().put("city", "New York").put("state", "NY");
        JsonNode currentItem = mapper.createObjectNode()
            .put("name", "John")
            .set("address", address);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.get("city").asText()).isEqualTo("New York");
    }

    @Test
    void testResolveWithNullField() {
        SelfReferenceNode node = new SelfReferenceNode("nullField", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode()
            .put("name", "John")
            .putNull("nullField");

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.isNull()).isTrue();
    }

    @Test
    void testResolveWithArrayField() {
        SelfReferenceNode node = new SelfReferenceNode("tags", List.of(), false);
        JsonNode tags = mapper.createArrayNode().add("tag1").add("tag2");
        JsonNode currentItem = mapper.createObjectNode()
            .put("name", "John")
            .set("tags", tags);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.isArray()).isTrue();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).asText()).isEqualTo("tag1");
        assertThat(result.get(1).asText()).isEqualTo("tag2");
    }

    @Test
    void testResolveWithObjectField() {
        SelfReferenceNode node = new SelfReferenceNode("profile", List.of(), false);
        JsonNode profile = mapper.createObjectNode()
            .put("bio", "Software Developer")
            .put("experience", 5);
        JsonNode currentItem = mapper.createObjectNode()
            .put("name", "John")
            .set("profile", profile);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.isObject()).isTrue();
        assertThat(result.get("bio").asText()).isEqualTo("Software Developer");
        assertThat(result.get("experience").asInt()).isEqualTo(5);
    }

    @Test
    void testResolveWithFilterValues() {
        // Self-references with filter values that match should return failure
        SelfReferenceNode node = new SelfReferenceNode("name", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode().put("name", "John");
        JsonNode filterValue = mapper.valueToTree("John");
        List<JsonNode> filterValues = List.of(filterValue);
        JsonNode failureResult = mapper.nullNode();

        when(mockContext.handleFilteringFailure("Self reference 'this.name' value matches filter"))
            .thenReturn(failureResult);

        JsonNode result = node.resolve(mockContext, currentItem, filterValues);

        assertThat(result).isEqualTo(failureResult);
    }

    @Test
    void testConstructorWithFilters() {
        FilterNode filter = new FilterNode(new LiteralFieldNode(mapper.valueToTree("test")));
        List<FilterNode> filters = List.of(filter);

        SelfReferenceNode node = new SelfReferenceNode("name", filters, true);

        assertThat(node.getFilters()).hasSize(1);
        assertThat(node.getFilters().get(0)).isEqualTo(filter);
        assertThat(node.isSequential()).isTrue();
        assertThat(node.getFieldName()).isEqualTo("name");
    }

    @Test
    void testResolveWithEmptyCurrentItem() {
        SelfReferenceNode node = new SelfReferenceNode("name", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode(); // Empty object

        when(mockContext.getMapper()).thenReturn(mapper);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.isNull()).isTrue();
    }

    @Test
    void testResolveWithComplexNestedPath() {
        SelfReferenceNode node = new SelfReferenceNode("user", List.of(), false);
        JsonNode contact = mapper.createObjectNode().put("email", "john@example.com");
        JsonNode profile = mapper.createObjectNode().set("contact", contact);
        JsonNode user = mapper.createObjectNode().set("profile", profile);
        JsonNode currentItem = mapper.createObjectNode().set("user", user);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.get("profile").get("contact").get("email").asText()).isEqualTo("john@example.com");
    }
}
