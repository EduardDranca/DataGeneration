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
class PickReferenceNodeTest {

    @Mock
    private EagerGenerationContext mockContext;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void testPickReferenceWithoutField() {
        PickReferenceNode node = new PickReferenceNode("userPick", null, List.of(), false);

        assertThat(node.hasFieldName()).isFalse();
        assertThat(node.isSequential()).isFalse();
        assertThat(node.getReferenceString()).isEqualTo("userPick");
    }

    @Test
    void testPickReferenceWithField() {
        PickReferenceNode node = new PickReferenceNode("userPick", "name", List.of(), true);

        assertThat(node.hasFieldName()).isTrue();
        assertThat(node.isSequential()).isTrue();
        assertThat(node.getReferenceString()).isEqualTo("userPick.name");
    }

    @Test
    void testResolvePickWithoutField() {
        PickReferenceNode node = new PickReferenceNode("userPick", null, List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode pickValue = mapper.createObjectNode().put("name", "John").put("age", 30);

        when(mockContext.getNamedPick("userPick")).thenReturn(pickValue);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result).isEqualTo(pickValue);
    }

    @Test
    void testResolvePickWithField() {
        PickReferenceNode node = new PickReferenceNode("userPick", "name", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode pickValue = mapper.createObjectNode().put("name", "John").put("age", 30);

        when(mockContext.getNamedPick("userPick")).thenReturn(pickValue);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.asText()).isEqualTo("John");
    }

    @Test
    void testResolvePickWithNonExistentPick() {
        PickReferenceNode node = new PickReferenceNode("nonExistentPick", null, List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();

        when(mockContext.getNamedPick("nonExistentPick")).thenReturn(null);
        when(mockContext.getMapper()).thenReturn(mapper);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.isNull()).isTrue();
    }

    @Test
    void testResolvePickWithNonExistentField() {
        PickReferenceNode node = new PickReferenceNode("userPick", "nonExistentField", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode pickValue = mapper.createObjectNode().put("name", "John").put("age", 30);

        when(mockContext.getNamedPick("userPick")).thenReturn(pickValue);
        when(mockContext.getMapper()).thenReturn(mapper);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.isNull()).isTrue();
    }

    @Test
    void testResolvePickWithNullPickValue() {
        PickReferenceNode node = new PickReferenceNode("userPick", "name", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode nullPickValue = mapper.nullNode();

        when(mockContext.getNamedPick("userPick")).thenReturn(nullPickValue);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.isNull()).isTrue();
    }

    @Test
    void testResolvePickWithArrayValue() {
        PickReferenceNode node = new PickReferenceNode("arrayPick", null, List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode arrayValue = mapper.createArrayNode().add("item1").add("item2");

        when(mockContext.getNamedPick("arrayPick")).thenReturn(arrayValue);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.isArray()).isTrue();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).asText()).isEqualTo("item1");
        assertThat(result.get(1).asText()).isEqualTo("item2");
    }

    @Test
    void testConstructorWithFilters() {
        FilterNode filter = new FilterNode(new LiteralFieldNode(mapper.valueToTree("test")));
        List<FilterNode> filters = List.of(filter);

        PickReferenceNode node = new PickReferenceNode("userPick", "name", filters, true);

        assertThat(node.getFilters()).hasSize(1);
        assertThat(node.getFilters().get(0)).isEqualTo(filter);
        assertThat(node.isSequential()).isTrue();
    }
}
