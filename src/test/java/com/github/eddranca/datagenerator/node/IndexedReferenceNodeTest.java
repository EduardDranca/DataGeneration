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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexedReferenceNodeTest {

    @Mock
    private EagerGenerationContext mockContext;

    @Mock
    private DslNodeVisitor<String> mockVisitor;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void testNumericIndexWithoutField() {
        IndexedReferenceNode node = new IndexedReferenceNode("users", "0", null, List.of(), false);

        assertThat(node.hasFieldName()).isFalse();
        assertThat(node.isWildcardIndex()).isFalse();
        assertThat(node.isSequential()).isFalse();
        assertThat(node.getReferenceString()).isEqualTo("users[0]");
    }

    @Test
    void testNumericIndexWithField() {
        IndexedReferenceNode node = new IndexedReferenceNode("users", "1", "name", List.of(), true);

        assertThat(node.hasFieldName()).isTrue();
        assertThat(node.isWildcardIndex()).isFalse();
        assertThat(node.isSequential()).isTrue();
        assertThat(node.getReferenceString()).isEqualTo("users[1].name");
    }

    @Test
    void testWildcardIndexWithoutField() {
        IndexedReferenceNode node = new IndexedReferenceNode("users", "*", null, List.of(), false);

        assertThat(node.hasFieldName()).isFalse();
        assertThat(node.isWildcardIndex()).isTrue();
        assertThat(node.isSequential()).isFalse();
        assertThat(node.getReferenceString()).isEqualTo("users[*]");
    }

    @Test
    void testWildcardIndexWithField() {
        IndexedReferenceNode node = new IndexedReferenceNode("users", "*", "name", List.of(), false);

        assertThat(node.hasFieldName()).isTrue();
        assertThat(node.isWildcardIndex()).isTrue();
        assertThat(node.getReferenceString()).isEqualTo("users[*].name");
    }

    @Test
    void testInvalidNumericIndex() {
        assertThatThrownBy(() -> new IndexedReferenceNode("users", "invalid", null, List.of(), false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid numeric index: invalid");
    }

    @Test
    void testResolveNumericIndexWithoutField() {
        IndexedReferenceNode node = new IndexedReferenceNode("users", "0", null, List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode user1 = mapper.createObjectNode().put("name", "John");
        JsonNode user2 = mapper.createObjectNode().put("name", "Jane");
        List<JsonNode> collection = List.of(user1, user2);

        when(mockContext.getCollection("users")).thenReturn(collection);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result).isEqualTo(user1);
    }

    @Test
    void testResolveNumericIndexWithField() {
        IndexedReferenceNode node = new IndexedReferenceNode("users", "1", "name", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode user1 = mapper.createObjectNode().put("name", "John");
        JsonNode user2 = mapper.createObjectNode().put("name", "Jane");
        List<JsonNode> collection = List.of(user1, user2);

        when(mockContext.getCollection("users")).thenReturn(collection);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.asText()).isEqualTo("Jane");
    }

    @Test
    void testResolveNumericIndexOutOfBounds() {
        IndexedReferenceNode node = new IndexedReferenceNode("users", "5", "name", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode user1 = mapper.createObjectNode().put("name", "John");
        List<JsonNode> collection = List.of(user1);

        when(mockContext.getCollection("users")).thenReturn(collection);
        when(mockContext.getMapper()).thenReturn(mapper);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.isNull()).isTrue();
    }

    @Test
    void testResolveWildcardIndex() {
        IndexedReferenceNode node = new IndexedReferenceNode("users", "*", "name", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode user1 = mapper.createObjectNode().put("name", "John");
        JsonNode user2 = mapper.createObjectNode().put("name", "Jane");
        List<JsonNode> collection = List.of(user1, user2);

        when(mockContext.getCollection("users")).thenReturn(collection);
        when(mockContext.getElementFromCollection(collection, node, false)).thenReturn(user1);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.asText()).isEqualTo("John");
    }

    @Test
    void testResolveWildcardIndexWithFiltering() {
        IndexedReferenceNode node = new IndexedReferenceNode("users", "*", "name", List.of(), false);
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
    void testResolveNumericIndexWithFiltering() {
        IndexedReferenceNode node = new IndexedReferenceNode("users", "0", "name", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode user1 = mapper.createObjectNode().put("name", "John");
        List<JsonNode> collection = List.of(user1);
        JsonNode filterValue = mapper.valueToTree("John");
        List<JsonNode> filterValues = List.of(filterValue);
        JsonNode failureResult = mapper.nullNode();

        when(mockContext.getCollection("users")).thenReturn(collection);
        when(mockContext.handleFilteringFailure("Indexed reference 'users[0].name' value matches filter"))
            .thenReturn(failureResult);

        JsonNode result = node.resolve(mockContext, currentItem, filterValues);

        assertThat(result).isEqualTo(failureResult);
    }

    @Test
    void testConstructorWithFilters() {
        FilterNode filter = new FilterNode(new LiteralFieldNode(mapper.valueToTree("test")));
        List<FilterNode> filters = List.of(filter);

        IndexedReferenceNode node = new IndexedReferenceNode("users", "*", "name", filters, true);

        assertThat(node.getFilters()).hasSize(1);
        assertThat(node.getFilters().get(0)).isEqualTo(filter);
        assertThat(node.isSequential()).isTrue();
    }
}
