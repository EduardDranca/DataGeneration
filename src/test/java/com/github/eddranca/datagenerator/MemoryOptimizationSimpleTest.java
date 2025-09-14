package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.generator.defaults.*;
import com.github.eddranca.datagenerator.node.*;
import com.github.eddranca.datagenerator.visitor.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for memory optimization that demonstrates
 * selective field generation and lazy materialization.
 */
class MemoryOptimizationSimpleTest {

    private ObjectMapper mapper;
    private GeneratorRegistry registry;
    private GenerationContext context;
    private DataGenerationVisitor visitor;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        registry = new GeneratorRegistry();
        
        // Register only existing generators with faker
        net.datafaker.Faker faker = new net.datafaker.Faker();
        registry.register("uuid", new UuidGenerator(faker));
        registry.register("name", new NameGenerator(faker));
        registry.register("lorem", new LoremGenerator(faker));
        
        context = new GenerationContext(registry, new Random(12345));
        visitor = new DataGenerationVisitor(context);
    }

    @Test
    void testMemoryOptimizationWithSelectiveGeneration() {
        // Create a user collection with many fields
        ItemNode userItem = new ItemNode(Map.of(
            "id", new GeneratedFieldNode("uuid", null),
            "name", new GeneratedFieldNode("name", null),
            "email", new GeneratedFieldNode("name", null), // Using name for simplicity
            "bio", new GeneratedFieldNode("lorem", null),
            "preferences", new GeneratedFieldNode("lorem", null),
            "metadata", new GeneratedFieldNode("lorem", null)
        ));
        
        CollectionNode usersCollection = new CollectionNode("users", 100, userItem, List.of(), Map.of(), null);
        
        // Create an order collection that only references user id and name
        ItemNode orderItem = new ItemNode(Map.of(
            "id", new GeneratedFieldNode("uuid", null),
            "userId", new SimpleReferenceNode("users", "id", List.of(), false),
            "customerName", new SimpleReferenceNode("users", "name", List.of(), false),
            "amount", new GeneratedFieldNode("lorem", null) // Using lorem for simplicity
        ));
        
        CollectionNode ordersCollection = new CollectionNode("orders", 50, orderItem, List.of(), Map.of(), null);
        
        // Create root with both collections
        RootNode root = new RootNode(12345L);
        root.addCollection("users", usersCollection);
        root.addCollection("orders", ordersCollection);
        
        // Analyze dependencies
        PathDependencyAnalyzer analyzer = new PathDependencyAnalyzer();
        root.accept(analyzer);
        
        Map<String, Set<String>> referencedPaths = analyzer.getReferencedPaths();
        
        // Verify dependency analysis found the right references
        assertThat(referencedPaths).containsKey("users");
        Set<String> userPaths = referencedPaths.get("users");
        assertThat(userPaths).contains("id", "name");
        assertThat(userPaths).doesNotContain("email", "bio", "preferences", "metadata");
        
        // Generate without memory optimization (baseline)
        GenerationContext regularContext = new GenerationContext(registry, new Random(12345));
        DataGenerationVisitor regularVisitor = new DataGenerationVisitor(regularContext);
        JsonNode regularResult = root.accept(regularVisitor);
        
        // Generate with memory optimization
        GenerationContext optimizedContext = new GenerationContext(registry, new Random(12345));
        optimizedContext.enableMemoryOptimization(referencedPaths);
        DataGenerationVisitor optimizedVisitor = new DataGenerationVisitor(optimizedContext);
        JsonNode optimizedResult = root.accept(optimizedVisitor);
        
        // Collect memory statistics before materialization
        LazyMaterializer.MemoryStatsSummary memoryStats = LazyMaterializer.collectMemoryStats(optimizedResult);
        System.out.println("Memory stats before materialization: " + memoryStats);
        
        // Materialize all lazy items for final output
        JsonNode fullyMaterialized = LazyMaterializer.materializeAll(optimizedResult);
        
        // Both should produce the same structure after materialization
        assertThat(regularResult.get("users").size()).isEqualTo(fullyMaterialized.get("users").size());
        assertThat(regularResult.get("orders").size()).isEqualTo(fullyMaterialized.get("orders").size());
        
        // Verify references work in both cases
        verifyReferencesWork(regularResult.get("users"), regularResult.get("orders"));
        verifyReferencesWork(fullyMaterialized.get("users"), fullyMaterialized.get("orders"));
        
        // Memory optimization should show significant savings during generation phase
        GenerationContext.MemoryOptimizationStats stats = optimizedContext.getMemoryOptimizationStats();
        
        System.out.println("Memory Optimization Results:");
        System.out.println("Regular generation: All fields materialized immediately");
        System.out.println("Optimized generation: " + stats);
        System.out.println("Memory stats summary: " + memoryStats);
        
        // We should have saved memory during the generation phase
        if (memoryStats.getTotalItems() > 0) {
            assertThat(memoryStats.getOverallMemorySavingsPercentage()).isGreaterThan(0);
        }
    }

    @Test
    void testNoReferencesResultsInMaximumSavings() {
        // Create collections with no cross-references
        ItemNode userItem = new ItemNode(Map.of(
            "id", new GeneratedFieldNode("uuid", null),
            "name", new GeneratedFieldNode("name", null),
            "email", new GeneratedFieldNode("lorem", null), // Using lorem instead of internet
            "bio", new GeneratedFieldNode("lorem", null)
        ));
        
        ItemNode productItem = new ItemNode(Map.of(
            "id", new GeneratedFieldNode("uuid", null),
            "name", new GeneratedFieldNode("name", null),
            "description", new GeneratedFieldNode("lorem", null)
        ));
        
        CollectionNode usersCollection = new CollectionNode("users", 50, userItem, List.of(), Map.of(), null);
        CollectionNode productsCollection = new CollectionNode("products", 30, productItem, List.of(), Map.of(), null);
        
        RootNode root = new RootNode(12345L);
        root.addCollection("users", usersCollection);
        root.addCollection("products", productsCollection);
        
        // Analyze dependencies (should find none)
        PathDependencyAnalyzer analyzer = new PathDependencyAnalyzer();
        root.accept(analyzer);
        
        Map<String, Set<String>> referencedPaths = analyzer.getReferencedPaths();
        assertThat(referencedPaths).isEmpty(); // No cross-references
        
        // Generate with memory optimization
        context.enableMemoryOptimization(referencedPaths);
        JsonNode result = root.accept(visitor);
        
        // Should still produce correct output
        assertThat(result.get("users").size()).isEqualTo(50);
        assertThat(result.get("products").size()).isEqualTo(30);
        
        // Should have maximum memory savings during generation
        GenerationContext.MemoryOptimizationStats stats = context.getMemoryOptimizationStats();
        assertThat(stats.getMemorySavingsPercentage()).isGreaterThan(90); // Nearly 100% savings
        
        System.out.println("No references scenario: " + stats);
    }

    @Test
    void testEntireObjectReferenceResultsInNoSavings() {
        // Create collections where one references entire objects from another
        ItemNode userItem = new ItemNode(Map.of(
            "id", new GeneratedFieldNode("uuid", null),
            "name", new GeneratedFieldNode("name", null),
            "email", new GeneratedFieldNode("lorem", null), // Using lorem instead of internet
            "bio", new GeneratedFieldNode("lorem", null)
        ));
        
        ItemNode profileItem = new ItemNode(Map.of(
            "id", new GeneratedFieldNode("uuid", null),
            "user", new SimpleReferenceNode("users", null, List.of(), false) // Entire user object
        ));
        
        CollectionNode usersCollection = new CollectionNode("users", 50, userItem, List.of(), Map.of(), null);
        CollectionNode profilesCollection = new CollectionNode("profiles", 25, profileItem, List.of(), Map.of(), null);
        
        RootNode root = new RootNode(12345L);
        root.addCollection("users", usersCollection);
        root.addCollection("profiles", profilesCollection);
        
        // Analyze dependencies
        PathDependencyAnalyzer analyzer = new PathDependencyAnalyzer();
        root.accept(analyzer);
        
        Map<String, Set<String>> referencedPaths = analyzer.getReferencedPaths();
        assertThat(referencedPaths.get("users")).contains("*"); // Entire object referenced
        
        // Generate with memory optimization
        context.enableMemoryOptimization(referencedPaths);
        JsonNode result = root.accept(visitor);
        
        // Should work correctly
        assertThat(result.get("users").size()).isEqualTo(50);
        assertThat(result.get("profiles").size()).isEqualTo(25);
        
        // But no memory savings since entire object is needed
        GenerationContext.MemoryOptimizationStats stats = context.getMemoryOptimizationStats();
        assertThat(stats.getMemorySavingsPercentage()).isLessThan(10); // Minimal savings
        
        System.out.println("Entire object reference scenario: " + stats);
    }

    private void verifyReferencesWork(JsonNode users, JsonNode orders) {
        // Verify first order references a valid user
        JsonNode firstOrder = orders.get(0);
        String userId = firstOrder.get("userId").asText();
        String customerName = firstOrder.get("customerName").asText();
        
        // Find the referenced user
        JsonNode referencedUser = null;
        for (JsonNode user : users) {
            if (user.get("id").asText().equals(userId)) {
                referencedUser = user;
                break;
            }
        }
        
        assertThat(referencedUser).isNotNull();
        assertThat(customerName).isEqualTo(referencedUser.get("name").asText());
    }
}