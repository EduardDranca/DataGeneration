package com.github.eddranca.datagenerator.node;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CollectionNodeTest {

    @Mock
    private ItemNode mockItem;

    @Test
    void testCollectionNodeWithAllParameters() {
        List<String> tags = List.of("tag1", "tag2");
        Map<String, Integer> picks = Map.of("first", 0, "last", 9);

        CollectionNode node = new CollectionNode("users", 10, mockItem, tags, picks, "customUsers");

        assertThat(node.getName()).isEqualTo("users");
        assertThat(node.getCount()).isEqualTo(10);
        assertThat(node.getItem()).isEqualTo(mockItem);
        assertThat(node.getTags()).containsExactly("tag1", "tag2");
        assertThat(node.getPicks()).containsEntry("first", 0);
        assertThat(node.getPicks()).containsEntry("last", 9);
        assertThat(node.getCollectionName()).isEqualTo("customUsers");
    }

    @Test
    void testCollectionNodeWithDefaultCollectionName() {
        List<String> tags = List.of();
        Map<String, Integer> picks = new HashMap<>();

        CollectionNode node = new CollectionNode("users", 5, mockItem, tags, picks, null);

        assertThat(node.getName()).isEqualTo("users");
        assertThat(node.getCollectionName()).isEqualTo("users"); // Should default to name
    }

    @Test
    void testCollectionNodeWithEmptyTagsAndPicks() {
        List<String> tags = List.of();
        Map<String, Integer> picks = new HashMap<>();

        CollectionNode node = new CollectionNode("products", 3, mockItem, tags, picks, "items");

        assertThat(node.getTags()).isEmpty();
        assertThat(node.getPicks()).isEmpty();
    }

    @Test
    void testCollectionNodeIsImmutable() {
        List<String> originalTags = List.of("tag1");
        Map<String, Integer> originalPicks = new HashMap<>();
        originalPicks.put("first", 0);

        CollectionNode node = new CollectionNode("users", 10, mockItem, originalTags, originalPicks, null);

        // Modify original collections
        originalPicks.put("second", 1);

        // Node should not be affected
        assertThat(node.getPicks()).hasSize(1);
        assertThat(node.getPicks()).containsOnlyKeys("first");
    }
}
