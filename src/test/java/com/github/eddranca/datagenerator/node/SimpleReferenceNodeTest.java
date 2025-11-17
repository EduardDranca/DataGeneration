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
class SimpleReferenceNodeTest {

    @Mock
    private EagerGenerationContext mockContext;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void testSimpleReferenceWithoutField() {
        SimpleReferenceNode node = new SimpleReferenceNode("users", null, List.of(), true);

        assertThat(node.hasFieldName()).isFalse();
        assertThat(node.isSequential()).isTrue();
        assertThat(node.getReferenceString()).isEqualTo("users");
    }

    @Test
    void testSimpleReferenceWithField() {
        SimpleReferenceNode node = new SimpleReferenceNode("users", "name", List.of(), false);

        assertThat(node.hasFieldName()).isTrue();
        assertThat(node.isSequential()).isFalse();
        assertThat(node.getReferenceString()).isEqualTo("users.name");
    }

    @Test
    void testResolveWithoutField() {
        SimpleReferenceNode node = new SimpleReferenceNode("users", null, List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode user1 = mapper.createObjectNode().put("name", "John").put("age", 30);
        JsonNode user2 = mapper.createObjectNode().put("name", "Jane").put("age", 25);
        List<JsonNode> collection = List.of(user1, user2);

        when(mockContext.getCollection("users")).thenReturn(collection);
        when(mockContext.getElementFromCollection(collection, node, false)).thenReturn(user1);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result).isEqualTo(user1);
    }

    @Test
    void testResolveWithField() {
        SimpleReferenceNode node = new SimpleReferenceNode("users", "name", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode user = mapper.createObjectNode().put("name", "John").put("age", 30);
        List<JsonNode> collection = List.of(user);

        when(mockContext.getCollection("users")).thenReturn(collection);
        when(mockContext.getElementFromCollection(collection, node, false)).thenReturn(user);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.asText()).isEqualTo("John");
    }

    @Test
    void testResolveWithEmptyCollection() {
        SimpleReferenceNode node = new SimpleReferenceNode("users", "name", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();

        when(mockContext.getCollection("users")).thenReturn(List.of());
        when(mockContext.getMapper()).thenReturn(mapper);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.isNull()).isTrue();
    }

    @Test
    void testResolveWithFiltering() {
        SimpleReferenceNode node = new SimpleReferenceNode("users", "name", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode user1 = mapper.createObjectNode().put("name", "John");
        JsonNode user2 = mapper.createObjectNode().put("name", "Jane");
        List<JsonNode> filteredCollection = List.of(user2);
        JsonNode filterValue = mapper.valueToTree("John");
        List<JsonNode> filterValues = List.of(filterValue);

        when(mockContext.getFilteredCollection("users", null, filterValues, "name")).thenReturn(filteredCollection);
        when(mockContext.getElementFromCollection(filteredCollection, node, false)).thenReturn(user2);

        JsonNode result = node.resolve(mockContext, currentItem, filterValues);

        assertThat(result.asText()).isEqualTo("Jane");
    }

    @Test
    void testResolveWithFilteringAndNoField() {
        SimpleReferenceNode node = new SimpleReferenceNode("users", null, List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode user = mapper.valueToTree("Jane");
        List<JsonNode> filteredCollection = List.of(user);
        JsonNode filterValue = mapper.valueToTree("John");
        List<JsonNode> filterValues = List.of(filterValue);

        when(mockContext.getFilteredCollection("users", null, filterValues, "")).thenReturn(filteredCollection);
        when(mockContext.getElementFromCollection(filteredCollection, node, false)).thenReturn(user);

        JsonNode result = node.resolve(mockContext, currentItem, filterValues);

        assertThat(result.asText()).isEqualTo("Jane");
    }

    @Test
    void testResolveWithFilteringReturnsFailureForEmptyResult() {
        SimpleReferenceNode node = new SimpleReferenceNode("users", "name", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        List<JsonNode> emptyFilteredCollection = List.of();
        JsonNode filterValue = mapper.valueToTree("John");
        List<JsonNode> filterValues = List.of(filterValue);
        JsonNode failureResult = mapper.nullNode();

        when(mockContext.getFilteredCollection("users", null, filterValues, "name")).thenReturn(emptyFilteredCollection);
        when(mockContext.handleFilteringFailure("Simple reference 'users.name' has no valid values after filtering"))
            .thenReturn(failureResult);

        JsonNode result = node.resolve(mockContext, currentItem, filterValues);

        assertThat(result).isEqualTo(failureResult);
    }

    @Test
    void testConstructorWithFilters() {
        FilterNode filter = new FilterNode(new LiteralFieldNode(mapper.valueToTree("test")));
        List<FilterNode> filters = List.of(filter);

        SimpleReferenceNode node = new SimpleReferenceNode("users", "name", filters, true);

        assertThat(node.getFilters()).hasSize(1);
        assertThat(node.getFilters().get(0)).isEqualTo(filter);
        assertThat(node.isSequential()).isTrue();
    }

    @Test
    void testResolveWithMissingField() {
        SimpleReferenceNode node = new SimpleReferenceNode("users", "missingField", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode user = mapper.createObjectNode().put("name", "John");
        List<JsonNode> collection = List.of(user);

        when(mockContext.getCollection("users")).thenReturn(collection);
        when(mockContext.getElementFromCollection(collection, node, false)).thenReturn(user);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        // Should return missing node for non-existent field
        assertThat(result.isMissingNode()).isTrue();
    }
}
