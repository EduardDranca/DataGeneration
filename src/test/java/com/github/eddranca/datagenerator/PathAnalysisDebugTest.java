package com.github.eddranca.datagenerator;

import com.github.eddranca.datagenerator.visitor.PathDependencyAnalyzer;
import com.github.eddranca.datagenerator.builder.DslTreeBuilder;
import com.github.eddranca.datagenerator.node.RootNode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

/**
 * Debug test to see what paths are being analyzed
 */
class PathAnalysisDebugTest {

    @Test
    public void debugPathAnalysis() throws Exception {
        String dsl = """
            {
              "users": {
                "count": 2,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.firstName"},
                  "address": {
                    "street": {"gen": "address.streetAddress"},
                    "city": {"gen": "address.city"}
                  }
                }
              },
              "orders": {
                "count": 1,
                "item": {
                  "id": {"gen": "uuid"},
                  "userId": {"ref": "users[*].id"},
                  "shippingStreet": {"ref": "users[*].address.street"}
                }
              }
            }
            """;

        // Parse and analyze
        Generation generation = DslDataGenerator.create()
            .withMemoryOptimization()
            .fromJsonString(dsl)
            .generate();
        
        // We need to access the analyzer through the generation process
        // Let's just check what actually got generated
        System.out.println("=== GENERATED COLLECTIONS ===");
        generation.getCollections().forEach((name, collection) -> {
            System.out.println("Collection: " + name + " (size: " + collection.size() + ")");
        });
        
        
        // Let's check what the first user looks like
        var users = generation.getCollections().get("users");
        if (!users.isEmpty()) {
            System.out.println("=== FIRST USER ===");
            System.out.println(generation.asJsonNode().get("users").get(0).toPrettyString());
        }
        
        var orders = generation.getCollections().get("orders");
        if (!orders.isEmpty()) {
            System.out.println("=== FIRST ORDER ===");
            System.out.println(generation.asJsonNode().get("orders").get(0).toPrettyString());
        }
    }
}