package com.github.eddranca.datagenerator.visitor;

import com.github.eddranca.datagenerator.node.ArrayFieldReferenceNode;
import com.github.eddranca.datagenerator.node.CollectionNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.GeneratedFieldNode;
import com.github.eddranca.datagenerator.node.ItemNode;
import com.github.eddranca.datagenerator.node.RootNode;
import com.github.eddranca.datagenerator.node.SimpleReferenceNode;
import com.github.eddranca.datagenerator.node.TagReferenceNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PathDependencyAnalyzerTest {

    private PathDependencyAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new PathDependencyAnalyzer();
    }

    @Test
    void testAnalyzeEmptyRoot() {
        RootNode root = new RootNode(null);
        
        DependencyGraph result = analyzer.analyze(root);
        
        assertThat(result).isNotNull();
        assertThat(result.getCollections()).isEmpty();
        assertThat(result.getDependencies()).isEmpty();
    }

    @Test
    void testAnalyzeSingleCollectionNoDependencies() {
        RootNode root = new RootNode();
        ItemNode item = new ItemNode();
        item.addField("name", new GeneratedFieldNode("name", null));
        CollectionNode collection = new CollectionNode("users", item, 5);
        root.addCollection("users", collection);
        
        DependencyGraph result = analyzer.analyze(root);
        
        assertThat(result.getCollections()).containsExactly("users");
        assertThat(result.getDependencies()).isEmpty();
        assertThat(result.hasCyclicDependency()).isFalse();
    }

    @Test
    void testAnalyzeSimpleReference() {
        RootNode root = new RootNode();
        
        // Create users collection
        ItemNode userItem = new ItemNode();
        userItem.addField("name", new GeneratedFieldNode("name", null));
        CollectionNode users = new CollectionNode("users", userItem, 5);
        root.addCollection("users", users);
        
        // Create orders collection that references users
        ItemNode orderItem = new ItemNode();
        orderItem.addField("userId", new SimpleReferenceNode("users", false));
        CollectionNode orders = new CollectionNode("orders", orderItem, 10);
        root.addCollection("orders", orders);
        
        DependencyGraph result = analyzer.analyze(root);
        
        assertThat(result.getCollections()).containsExactlyInAnyOrder("users", "orders");
        assertThat(result.getDependencies()).hasSize(1);
        assertThat(result.getDependencies()).containsKey("orders");
        assertThat(result.getDependencies().get("orders")).containsExactly("users");
        assertThat(result.hasCyclicDependency()).isFalse();
    }

    @Test
    void testAnalyzeArrayFieldReference() {
        RootNode root = new RootNode();
        
        // Create users collection
        ItemNode userItem = new ItemNode();
        userItem.addField("id", new GeneratedFieldNode("uuid", null));
        userItem.addField("name", new GeneratedFieldNode("name", null));
        CollectionNode users = new CollectionNode("users", userItem, 5);
        root.addCollection("users", users);
        
        // Create orders collection that references user names
        ItemNode orderItem = new ItemNode();
        orderItem.addField("customerName", new ArrayFieldReferenceNode("users", "name", false));
        CollectionNode orders = new CollectionNode("orders", orderItem, 10);
        root.addCollection("orders", orders);
        
        DependencyGraph result = analyzer.analyze(root);
        
        assertThat(result.getDependencies()).containsKey("orders");
        assertThat(result.getDependencies().get("orders")).containsExactly("users");
    }

    @Test
    void testAnalyzeTagReference() {
        RootNode root = new RootNode();
        
        // Create users collection with tags
        ItemNode userItem = new ItemNode();
        userItem.addField("name", new GeneratedFieldNode("name", null));
        CollectionNode users = new CollectionNode("users", userItem, 5);
        users.addTag("people");
        root.addCollection("users", users);
        
        // Create orders collection that references by tag
        ItemNode orderItem = new ItemNode();
        orderItem.addField("customer", new TagReferenceNode("people", null, null, false));
        CollectionNode orders = new CollectionNode("orders", orderItem, 10);
        root.addCollection("orders", orders);
        
        DependencyGraph result = analyzer.analyze(root);
        
        assertThat(result.getDependencies()).containsKey("orders");
        assertThat(result.getDependencies().get("orders")).containsExactly("users");
    }

    @Test
    void testAnalyzeMultipleDependencies() {
        RootNode root = new RootNode();
        
        // Create users collection
        ItemNode userItem = new ItemNode();
        userItem.addField("name", new GeneratedFieldNode("name", null));
        CollectionNode users = new CollectionNode("users", userItem, 5);
        root.addCollection("users", users);
        
        // Create products collection
        ItemNode productItem = new ItemNode();
        productItem.addField("name", new GeneratedFieldNode("productName", null));
        CollectionNode products = new CollectionNode("products", productItem, 20);
        root.addCollection("products", products);
        
        // Create orders collection that references both users and products
        ItemNode orderItem = new ItemNode();
        orderItem.addField("userId", new SimpleReferenceNode("users", false));
        orderItem.addField("productId", new SimpleReferenceNode("products", false));
        CollectionNode orders = new CollectionNode("orders", orderItem, 10);
        root.addCollection("orders", orders);
        
        DependencyGraph result = analyzer.analyze(root);
        
        assertThat(result.getDependencies()).containsKey("orders");
        assertThat(result.getDependencies().get("orders")).containsExactlyInAnyOrder("users", "products");
    }

    @Test
    void testAnalyzeChainedDependencies() {
        RootNode root = new RootNode();
        
        // Create users collection
        ItemNode userItem = new ItemNode();
        userItem.addField("name", new GeneratedFieldNode("name", null));
        CollectionNode users = new CollectionNode("users", userItem, 5);
        root.addCollection("users", users);
        
        // Create orders collection that references users
        ItemNode orderItem = new ItemNode();
        orderItem.addField("userId", new SimpleReferenceNode("users", false));
        CollectionNode orders = new CollectionNode("orders", orderItem, 10);
        root.addCollection("orders", orders);
        
        // Create shipments collection that references orders
        ItemNode shipmentItem = new ItemNode();
        shipmentItem.addField("orderId", new SimpleReferenceNode("orders", false));
        CollectionNode shipments = new CollectionNode("shipments", shipmentItem, 8);
        root.addCollection("shipments", shipments);
        
        DependencyGraph result = analyzer.analyze(root);
        
        assertThat(result.getDependencies()).hasSize(2);
        assertThat(result.getDependencies().get("orders")).containsExactly("users");
        assertThat(result.getDependencies().get("shipments")).containsExactly("orders");
        
        List<String> generationOrder = result.getGenerationOrder();
        assertThat(generationOrder).containsExactly("users", "orders", "shipments");
    }

    @Test
    void testAnalyzeCyclicDependency() {
        RootNode root = new RootNode();
        
        // Create users collection that references orders
        ItemNode userItem = new ItemNode();
        userItem.addField("lastOrderId", new SimpleReferenceNode("orders", false));
        CollectionNode users = new CollectionNode("users", userItem, 5);
        root.addCollection("users", users);
        
        // Create orders collection that references users
        ItemNode orderItem = new ItemNode();
        orderItem.addField("userId", new SimpleReferenceNode("users", false));
        CollectionNode orders = new CollectionNode("orders", orderItem, 10);
        root.addCollection("orders", orders);
        
        DependencyGraph result = analyzer.analyze(root);
        
        assertThat(result.hasCyclicDependency()).isTrue();
        assertThat(result.getCycles()).hasSize(1);
        assertThat(result.getCycles().get(0)).containsExactlyInAnyOrder("users", "orders");
    }

    @Test
    void testAnalyzeComplexCyclicDependency() {
        RootNode root = new RootNode();
        
        // Create A -> B -> C -> A cycle
        ItemNode itemA = new ItemNode();
        itemA.addField("refB", new SimpleReferenceNode("B", false));
        CollectionNode collectionA = new CollectionNode("A", itemA, 5);
        root.addCollection("A", collectionA);
        
        ItemNode itemB = new ItemNode();
        itemB.addField("refC", new SimpleReferenceNode("C", false));
        CollectionNode collectionB = new CollectionNode("B", itemB, 5);
        root.addCollection("B", collectionB);
        
        ItemNode itemC = new ItemNode();
        itemC.addField("refA", new SimpleReferenceNode("A", false));
        CollectionNode collectionC = new CollectionNode("C", itemC, 5);
        root.addCollection("C", collectionC);
        
        DependencyGraph result = analyzer.analyze(root);
        
        assertThat(result.hasCyclicDependency()).isTrue();
        assertThat(result.getCycles()).hasSize(1);
        assertThat(result.getCycles().get(0)).containsExactlyInAnyOrder("A", "B", "C");
    }

    @Test
    void testGetGenerationOrderWithNoDependencies() {
        RootNode root = new RootNode();
        
        ItemNode userItem = new ItemNode();
        userItem.addField("name", new GeneratedFieldNode("name", null));
        CollectionNode users = new CollectionNode("users", userItem, 5);
        root.addCollection("users", users);
        
        ItemNode productItem = new ItemNode();
        productItem.addField("name", new GeneratedFieldNode("productName", null));
        CollectionNode products = new CollectionNode("products", productItem, 20);
        root.addCollection("products", products);
        
        DependencyGraph result = analyzer.analyze(root);
        
        List<String> generationOrder = result.getGenerationOrder();
        assertThat(generationOrder).containsExactlyInAnyOrder("users", "products");
    }

    @Test
    void testGetGenerationOrderThrowsExceptionForCycles() {
        RootNode root = new RootNode();
        
        // Create cyclic dependency
        ItemNode userItem = new ItemNode();
        userItem.addField("orderId", new SimpleReferenceNode("orders", false));
        CollectionNode users = new CollectionNode("users", userItem, 5);
        root.addCollection("users", users);
        
        ItemNode orderItem = new ItemNode();
        orderItem.addField("userId", new SimpleReferenceNode("users", false));
        CollectionNode orders = new CollectionNode("orders", orderItem, 10);
        root.addCollection("orders", orders);
        
        DependencyGraph result = analyzer.analyze(root);
        
        assertThatThrownBy(() -> result.getGenerationOrder())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cyclic dependency detected");
    }

    @Test
    void testDependencyGraphGetters() {
        RootNode root = new RootNode();
        
        ItemNode userItem = new ItemNode();
        userItem.addField("name", new GeneratedFieldNode("name", null));
        CollectionNode users = new CollectionNode("users", userItem, 5);
        root.addCollection("users", users);
        
        DependencyGraph result = analyzer.analyze(root);
        
        assertThat(result.getCollections()).containsExactly("users");
        assertThat(result.getDependencies()).isEmpty();
        assertThat(result.getDependenciesFor("users")).isEmpty();
        assertThat(result.getDependenciesFor("nonexistent")).isEmpty();
    }

    @Test
    void testAnalyzeNullRoot() {
        assertThatThrownBy(() -> analyzer.analyze(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Root node cannot be null");
    }
}