package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.generator.defaults.*;
import com.github.eddranca.datagenerator.node.*;
import com.github.eddranca.datagenerator.visitor.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic test for memory optimization functionality.
 */
class MemoryOptimizationBasicTest {

    private GeneratorRegistry registry;
    private GenerationContext context;
    private DataGenerationVisitor visitor;

    @BeforeEach
    void setUp() {
        registry = new GeneratorRegistry();
        
        // Register basic generators
        net.datafaker.Faker faker = new net.datafaker.Faker();
        registry.register("uuid", new UuidGenerator(faker));
        registry.register("name", new NameGenerator(faker));
        registry.register("lorem", new LoremGenerator(faker));
        
        context = new GenerationContext(registry, new Random(12345));
        visitor = new DataGenerationVisitor(context);
    }

    @Test
    void testPathDependencyAnalyzer() {
        // Create a simple DSL structure
        ItemNode userItem = new ItemNode(Map.of(
            "id", new GeneratedFieldNode("uuid", null),
            "name", new GeneratedFieldNode("name", null),
            "email", new GeneratedFieldNode("lorem", null),
            "bio", new GeneratedFieldNode("lorem", null)
        ));
        
        CollectionNode usersCollection = new CollectionNode("users", 10, userItem, List.of(), Map.of(), null);
        
        ItemNode orderItem = new ItemNode(Map.of(
            "id", new GeneratedFieldNode("uuid", null),
            "userId", new SimpleReferenceNode("users", "id", List.of(), false),
            "customerName", new SimpleReferenceNode("users", "name", List.of(), false)
        ));
        
        CollectionNode ordersCollection = new CollectionNode("orders", 5, orderItem, List.of(), Map.of(), null);
        
        RootNode root = new RootNode(12345L);
        root.addCollection("users", usersCollection);
        root.addCollection("orders", ordersCollection);
        
        // Test dependency analysis
        PathDependencyAnalyzer analyzer = new PathDependencyAnalyzer();
        root.accept(analyzer);
        
        Map<String, Set<String>> referencedPaths = analyzer.getReferencedPaths();
        
        System.out.println("Referenced paths: " + referencedPaths);
        
        // Verify analysis results
        assertThat(referencedPaths).containsKey("users");
        Set<String> userPaths = referencedPaths.get("users");
        assertThat(userPaths).contains("id", "name");
        assertThat(userPaths).doesNotContain("email", "bio");
        
        // Test memory optimization
        context.enableMemoryOptimization(referencedPaths);
        assertThat(context.isMemoryOptimizationEnabled()).isTrue();
        
        // Generate data
        JsonNode result = root.accept(visitor);
        
        System.out.println("Generation completed");
        System.out.println("Memory stats: " + context.getMemoryOptimizationStats());
        
        // Basic structure verification
        assertThat(result.has("users")).isTrue();
        assertThat(result.has("orders")).isTrue();
        assertThat(result.get("users").size()).isEqualTo(10);
        assertThat(result.get("orders").size()).isEqualTo(5);
    }

    @Test
    void testLazyItemProxyDirectly() {
        // Test LazyItemProxy directly
        Map<String, DslNode> fields = Map.of(
            "id", new GeneratedFieldNode("uuid", null),
            "name", new GeneratedFieldNode("name", null),
            "email", new GeneratedFieldNode("lorem", null),
            "bio", new GeneratedFieldNode("lorem", null)
        );
        
        Set<String> referencedPaths = Set.of("id", "name");
        
        LazyItemProxy lazyItem = new LazyItemProxy("users", fields, referencedPaths, visitor);
        
        System.out.println("LazyItemProxy created: " + lazyItem);
        System.out.println("Memory stats: " + lazyItem.getMemoryStats());
        
        // Test field access
        JsonNode idValue = lazyItem.get("id");
        JsonNode nameValue = lazyItem.get("name");
        
        assertThat(idValue).isNotNull();
        assertThat(nameValue).isNotNull();
        
        System.out.println("ID: " + idValue);
        System.out.println("Name: " + nameValue);
        
        // Test materialization
        JsonNode fullItem = lazyItem.materializeAll();
        assertThat(fullItem.has("email")).isTrue();
        assertThat(fullItem.has("bio")).isTrue();
        
        System.out.println("Full item: " + fullItem);
    }
}