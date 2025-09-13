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
class TagReferenceNodeTest {

    @Mock
    private GenerationContext mockContext;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void testStaticTagReferenceWithoutField() {
        TagReferenceNode node = new TagReferenceNode("userTag", "", List.of(), true);

        assertThat(node.hasFieldName()).isFalse();
        assertThat(node.isSequential()).isTrue();
        assertThat(node.getReferenceString()).isEqualTo("byTag[userTag]");
    }

    @Test
    void testStaticTagReferenceWithField() {
        TagReferenceNode node = new TagReferenceNode("userTag", "name", List.of(), false);

        assertThat(node.hasFieldName()).isTrue();
        assertThat(node.isSequential()).isFalse();
        assertThat(node.getReferenceString()).isEqualTo("byTag[userTag].name");
    }

    @Test
    void testDynamicTagReference() {
        TagReferenceNode node = new TagReferenceNode("this.category", "name", List.of(), false);

        assertThat(node.hasFieldName()).isTrue();
        assertThat(node.getLocalFieldName()).isEqualTo("category");
        assertThat(node.getReferenceString()).isEqualTo("byTag[this.category].name");
    }

    @Test
    void testResolveStaticTag() {
        TagReferenceNode node = new TagReferenceNode("userTag", "name", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode user1 = mapper.createObjectNode().put("name", "John");
        JsonNode user2 = mapper.createObjectNode().put("name", "Jane");
        List<JsonNode> taggedCollection = List.of(user1, user2);

        when(mockContext.getTaggedCollection("userTag")).thenReturn(taggedCollection);
        when(mockContext.getElementFromCollection(taggedCollection, node, false)).thenReturn(user1);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.asText()).isEqualTo("John");
    }

    @Test
    void testResolveDynamicTag() {
        TagReferenceNode node = new TagReferenceNode("this.category", "name", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode().put("category", "premium");
        JsonNode user1 = mapper.createObjectNode().put("name", "John");
        List<JsonNode> taggedCollection = List.of(user1);

        when(mockContext.getTaggedCollection("premium")).thenReturn(taggedCollection);
        when(mockContext.getElementFromCollection(taggedCollection, node, false)).thenReturn(user1);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.asText()).isEqualTo("John");
    }

    @Test
    void testResolveWithEmptyTaggedCollection() {
        TagReferenceNode node = new TagReferenceNode("emptyTag", "name", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();

        when(mockContext.getTaggedCollection("emptyTag")).thenReturn(List.of());
        when(mockContext.getMapper()).thenReturn(mapper);

        JsonNode result = node.resolve(mockContext, currentItem, null);

        assertThat(result.isNull()).isTrue();
    }

    @Test
    void testResolveWithFiltering() {
        TagReferenceNode node = new TagReferenceNode("userTag", "name", List.of(), false);
        JsonNode currentItem = mapper.createObjectNode();
        JsonNode user1 = mapper.createObjectNode().put("name", "John");
        JsonNode user2 = mapper.createObjectNode().put("name", "Jane");
        List<JsonNode> originalCollection = List.of(user1, user2);
        List<JsonNode> filteredCollection = List.of(user2);
        JsonNode filterValue = mapper.valueToTree("John");
        List<JsonNode> filterValues = List.of(filterValue);

        when(mockContext.getTaggedCollection("userTag")).thenReturn(originalCollection);
        when(mockContext.applyFiltering(originalCollection, "name", filterValues)).thenReturn(filteredCollection);
        when(mockContext.getElementFromCollection(filteredCollection, node, false)).thenReturn(user2);

        JsonNode result = node.resolve(mockContext, currentItem, filterValues);

        assertThat(result.asText()).isEqualTo("Jane");
    }

    @Test
    void testConstructorWithFilters() {
        FilterNode filter = new FilterNode(new LiteralFieldNode(mapper.valueToTree("test")));
        List<FilterNode> filters = List.of(filter);

        TagReferenceNode node = new TagReferenceNode("userTag", "name", filters, true);

        assertThat(node.getFilters()).hasSize(1);
        assertThat(node.getFilters().get(0)).isEqualTo(filter);
        assertThat(node.isSequential()).isTrue();
    }
}
