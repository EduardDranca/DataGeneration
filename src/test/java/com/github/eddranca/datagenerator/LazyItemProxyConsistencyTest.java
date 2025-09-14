package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

/**
 * Test to check if LazyItemProxy returns consistent values when accessed multiple times
 */
class LazyItemProxyConsistencyTest {

    @Test
    public void testLazyItemProxyConsistency() throws Exception {
        String dsl = """
            {
              "users": {
                "count": 1,
                "item": {
                  "id": {"gen": "uuid"},
                  "profile": {
                    "social": {
                      "twitter": {"gen": "internet.username"}
                    }
                  }
                }
              }
            }
            """;

        Generation generation = DslDataGenerator.create()
            .withSeed(12345L)
            .withMemoryOptimization()
            .fromJsonString(dsl)
            .generate();

        var rawUsers = generation.getCollections().get("users");
        var lazyUser = rawUsers.get(0);
        
        System.out.println("=== CONSISTENCY TEST ===");
        System.out.println("LazyUser type: " + lazyUser.getClass().getSimpleName());
        
        // Access the same nested field multiple times
        JsonNode twitter1 = lazyUser.get("profile").get("social").get("twitter");
        JsonNode twitter2 = lazyUser.get("profile").get("social").get("twitter");
        JsonNode twitter3 = lazyUser.get("profile").get("social").get("twitter");
        
        System.out.println("Twitter access 1: " + twitter1.asText());
        System.out.println("Twitter access 2: " + twitter2.asText());
        System.out.println("Twitter access 3: " + twitter3.asText());
        
        System.out.println("All same: " + (twitter1.asText().equals(twitter2.asText()) && twitter2.asText().equals(twitter3.asText())));
        
        // Also test via JSON conversion
        JsonNode json = generation.asJsonNode();
        JsonNode twitterFromJson = json.get("users").get(0).get("profile").get("social").get("twitter");
        System.out.println("Twitter from JSON: " + twitterFromJson.asText());
        System.out.println("JSON matches direct access: " + twitter1.asText().equals(twitterFromJson.asText()));
    }
}