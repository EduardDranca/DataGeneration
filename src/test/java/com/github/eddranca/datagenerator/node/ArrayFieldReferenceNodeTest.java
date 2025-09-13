package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.visitor.GenerationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArrayFieldReferenceNodeTest {

    @Mock
    private GenerationContext mockContext;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void testArrayFieldReferenceBasicConstructor() {
        ArrayFieldReferenceNode node = new ArrayFieldReferenceNode("users", "name", true);

        assertThat(node.getCollectionName()).isEqualTo("users");
        assertThat(node.getFieldName()).isEqualTo("name");
        assertThat(node.isSequential()).isTrue();
        assertThat(node.getReferenceString()).isEqualTo("users[*].name");
        assertThat(node.getFilters()).isEmpty();
    }

    @Test
    void testArrayFieldReferenceWithFilters() {
        FilterNode filter = new FilterNode(new LiteralFieldNode(mapper.valueToTree("exclude")));
        List<FilterNode> filters = List.of(filter);

        ArrayFieldReferenceNode node = new ArrayFieldReferenceNode("products", "price", filters, false);

        assertThat(node.getCollectionName()).isEqualTo("products");
        assertThat(node.getFieldName()).isEqualTo("price");
        assertThat(node.isSequential()).isFalse();
        assertThat(node.getReferenceString()).isEqualTo("products[*].price");
        assertThat(node.getFilters()).hasSize(1);
        assertThat(node.getFilters().get(0)).isEqualTo(filter);
    }

    @Test
    void testResolveWithoutFiltering() {
        ArrayFieldReferenceNode node = new ArrayFieldReferenceNode("users", "name", false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode user1 = mapper.createObjectNode().put("name", "John").put("age", 30);
        JsonNode user2 = mapper.createObjectNode().put("name", "Jane").put("age", 25);
        List<JsonNode> collection = List.of(user1, user2);

        when(mockContext.getCollection("users")).thenReturn(collection);
        when(mockContext.getElementFromCollection(collection, node, false)).thenReturn(user1);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.asText()).isEqualTo("John");
    }

    @Test
    void testResolveWithFiltering() {
        ArrayFieldReferenceNode node = new ArrayFieldReferenceNode("users", "name", false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode user1 = mapper.createObjectNode().put("name", "John");
        JsonNode user2 = mapper.createObjectNode().put("name", "Jane");
        List<JsonNode> originalCollection = List.of(user1, user2);
        List<JsonNode> filteredCollection = List.of(user2);
        JsonNode filterValue = mapper.valueToTree("John");
        List<JsonNode> filterValues = List.of(filterValue);

        when(mockContext.getCollection("users")).thenReturn(originalCollection);
        when(mockContext.applyFilteringOnField(originalCollection, "name", filterValues)).thenReturn(filteredCollection);
        when(mockContext.getElementFromCollection(filteredCollection, node, false)).thenReturn(user2);

        JsonNode result = node.resolve(mockContext, currentItem, filterValues);

        assertThat(result.asText()).isEqualTo("Jane");
    }

    @Test
    void testResolveWithEmptyCollection() {
        ArrayFieldReferenceNode node = new ArrayFieldReferenceNode("users", "name", false);
        JsonNode currentItem = mapper.createObjectNode();

        when(mockContext.getCollection("users")).thenReturn(List.of());
        when(mockContext.getMapper()).thenReturn(mapper);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.isNull()).isTrue();
    }

    @Test
    void testResolveWithFilteringReturnsFailure() {
        ArrayFieldReferenceNode node = new ArrayFieldReferenceNode("users", "name", false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode user1 = mapper.createObjectNode().put("name", "John");
        List<JsonNode> originalCollection = List.of(user1);
        List<JsonNode> emptyFilteredCollection = List.of();
        JsonNode filterValue = mapper.valueToTree("John");
        List<JsonNode> filterValues = List.of(filterValue);
        JsonNode failureResult = mapper.nullNode();

        when(mockContext.getCollection("users")).thenReturn(originalCollection);
        when(mockContext.applyFilteringOnField(originalCollection, "name", filterValues)).thenReturn(emptyFilteredCollection);
        when(mockContext.handleFilteringFailure("Array field reference 'users[*].name' has no valid values after filtering"))
                .thenReturn(failureResult);

        JsonNode result = node.resolve(mockContext, currentItem, filterValues);

        assertThat(result).isEqualTo(failureResult);
    }

    @Test
    void testResolveWithMissingField() {
        ArrayFieldReferenceNode node = new ArrayFieldReferenceNode("users", "missingField", false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode user = mapper.createObjectNode().put("name", "John");
        List<JsonNode> collection = List.of(user);

        when(mockContext.getCollection("users")).thenReturn(collection);
        when(mockContext.getElementFromCollection(collection, node, false)).thenReturn(user);
        when(mockContext.getMapper()).thenReturn(mapper);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        // Should return null for missing field
        assertThat(result.isNull()).isTrue();
    }
}
