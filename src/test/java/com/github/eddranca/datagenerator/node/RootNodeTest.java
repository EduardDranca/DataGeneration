package com.github.eddranca.datagenerator.node;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RootNodeTest {

    @Mock
    private CollectionNode mockCollection1;

    @Mock
    private CollectionNode mockCollection2;

    @Mock
    private ItemNode mockItem;

    @Test
    void testRootNodeWithSeed() {
        Long seed = 12345L;
        RootNode node = new RootNode(seed);

        assertThat(node.getSeed()).isEqualTo(seed);
        assertThat(node.getCollections()).isEmpty();
    }

    @Test
    void testRootNodeWithNullSeed() {
        RootNode node = new RootNode(null);

        assertThat(node.getSeed()).isNull();
        assertThat(node.getCollections()).isEmpty();
    }

    @Test
    void testAddSingleCollection() {
        RootNode node = new RootNode(123L);
        node.addCollection("users", mockCollection1);

        assertThat(node.getCollections()).hasSize(1);
        assertThat(node.getCollections()).containsEntry("users", mockCollection1);
    }

    @Test
    void testAddMultipleCollections() {
        RootNode node = new RootNode(123L);
        node.addCollection("users", mockCollection1);
        node.addCollection("products", mockCollection2);

        assertThat(node.getCollections()).hasSize(2);
        assertThat(node.getCollections()).containsEntry("users", mockCollection1);
        assertThat(node.getCollections()).containsEntry("products", mockCollection2);
    }

    @Test
    void testCollectionsPreserveOrder() {
        RootNode node = new RootNode(123L);
        node.addCollection("first", mockCollection1);
        node.addCollection("second", mockCollection2);

        assertThat(node.getCollections().keySet()).containsExactly("first", "second");
    }

    @Test
    void testOverwriteCollection() {
        RootNode node = new RootNode(123L);
        node.addCollection("users", mockCollection1);
        node.addCollection("users", mockCollection2); // Overwrite

        assertThat(node.getCollections()).hasSize(1);
        assertThat(node.getCollections()).containsEntry("users", mockCollection2);
    }

    @Test
    void testGetCollectionsReturnsDirectReference() {
        RootNode node = new RootNode(123L);
        node.addCollection("users", mockCollection1);

        // The returned map should be the actual internal map
        assertThat(node.getCollections()).containsKey("users");

        // Adding to the node should reflect in the returned map
        node.addCollection("products", mockCollection2);
        assertThat(node.getCollections()).hasSize(2);
    }

    @Test
    void testRootNodeWithComplexCollections() {
        RootNode node = new RootNode(999L);

        // Create a real collection for more comprehensive testing
        CollectionNode realCollection = new CollectionNode(
                "users",
                10,
                mockItem,
                List.of("tag1"),
                new HashMap<>(),
                null
        );

        node.addCollection("users", realCollection);

        assertThat(node.getCollections().get("users").getName()).isEqualTo("users");
        assertThat(node.getCollections().get("users").getCount()).isEqualTo(10);
    }
}
