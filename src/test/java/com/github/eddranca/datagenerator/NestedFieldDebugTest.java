package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

/**
 * Debug test to compare nested field generation with and without memory optimization
 */
class NestedFieldDebugTest {

    @Test
    public void compareNestedFieldGeneration() throws Exception {
        String dsl = """
            {
              "users": {
                "count": 2,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.firstName"},
                  "profile": {
                    "social": {
                      "twitter": {"gen": "internet.username"},
                      "linkedin": {"gen": "internet.username"}
                    }
                  }
                }
              },
              "posts": {
                "count": 1,
                "item": {
                  "id": {"gen": "uuid"},
                  "socialHandle": {"ref": "users[*].profile.social.twitter"}
                }
              }
            }
            """;

        System.out.println("=== WITHOUT MEMORY OPTIMIZATION ===");
        Generation normalGeneration = DslDataGenerator.create()
            .withSeed(12345L)  // Fixed seed for comparison
            .fromJsonString(dsl)
            .generate();
        
        JsonNode normalJson = normalGeneration.asJsonNode();
        JsonNode normalUser = normalJson.get("users").get(0);
        JsonNode normalPost = normalJson.get("posts").get(0);
        
        System.out.println("Normal user profile.social.twitter: " + normalUser.get("profile").get("social").get("twitter").asText());
        System.out.println("Normal post socialHandle: " + normalPost.get("socialHandle").asText());
        System.out.println("MATCH: " + normalUser.get("profile").get("social").get("twitter").asText().equals(normalPost.get("socialHandle").asText()));
        
        System.out.println("\n=== WITH MEMORY OPTIMIZATION ===");
        Generation optimizedGeneration = DslDataGenerator.create()
            .withSeed(12345L)  // Same seed for comparison
            .withMemoryOptimization()
            .fromJsonString(dsl)
            .generate();
        
        JsonNode optimizedJson = optimizedGeneration.asJsonNode();
        JsonNode optimizedUser = optimizedJson.get("users").get(0);
        JsonNode optimizedPost = optimizedJson.get("posts").get(0);
        
        System.out.println("Optimized user profile.social.twitter: " + optimizedUser.get("profile").get("social").get("twitter").asText());
        System.out.println("Optimized post socialHandle: " + optimizedPost.get("socialHandle").asText());
        System.out.println("MATCH: " + optimizedUser.get("profile").get("social").get("twitter").asText().equals(optimizedPost.get("socialHandle").asText()));
        
        // Debug: Check what's in the raw collections before JSON conversion
        System.out.println("\n=== RAW COLLECTIONS DEBUG ===");
        var rawUsers = optimizedGeneration.getCollections().get("users");
        var rawPosts = optimizedGeneration.getCollections().get("posts");
        
        System.out.println("Raw user type: " + rawUsers.get(0).getClass().getSimpleName());
        System.out.println("Raw post type: " + rawPosts.get(0).getClass().getSimpleName());
        
        // Check if the user LazyItemProxy has the twitter field already materialized
        if (rawUsers.get(0) instanceof com.github.eddranca.datagenerator.visitor.LazyItemProxy) {
            var lazyUser = (com.github.eddranca.datagenerator.visitor.LazyItemProxy) rawUsers.get(0);
            System.out.println("User LazyItemProxy memory stats: " + lazyUser.getMemoryStats());
            
            // Try to access the twitter field directly from the proxy
            var profileNode = lazyUser.get("profile");
            System.out.println("Profile node type: " + (profileNode != null ? profileNode.getClass().getSimpleName() : "null"));
            if (profileNode != null) {
                var socialNode = profileNode.get("social");
                System.out.println("Social node type: " + (socialNode != null ? socialNode.getClass().getSimpleName() : "null"));
                if (socialNode != null) {
                    var twitterNode = socialNode.get("twitter");
                    System.out.println("Twitter from LazyItemProxy: " + (twitterNode != null ? twitterNode.asText() : "null"));
                }
            }
        }
    }
}