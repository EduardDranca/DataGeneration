package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for lazy streaming generation functionality.
 * Verifies that items are generated on-demand during streaming.
 */
class LazyStreamingTest {

  @Test
  void testLazyStreamingWithReferences() throws Exception {
    String dsl = """
        {
          "users": {
            "count": 50,
            "item": {
              "id": {"gen": "uuid"},
              "name": {"gen": "name.firstName"},
              "email": {"gen": "internet.emailAddress"},
              "bio": {"gen": "lorem", "options": {"length": 100}}
            }
          },
          "posts": {
            "count": 30,
            "item": {
              "id": {"gen": "uuid"},
              "title": {"gen": "lorem", "options": {"length": 50}},
              "authorId": {"ref": "users[*].id"},
              "authorName": {"ref": "users[*].name"}
            }
          }
        }
        """;

    // Test with memory optimization enabled
    IGeneration generation = DslDataGenerator.create()
        .withMemoryOptimization()
        .fromJsonString(dsl)
        .generate();

    // Verify structure
    assertTrue(generation.getCollections().containsKey("users"));
    assertTrue(generation.getCollections().containsKey("posts"));
    assertEquals(50, generation.getCollections().get("users").size());
    assertEquals(30, generation.getCollections().get("posts").size());
  }

  @Test
  void testLazyStreamingWithoutOptimization() throws Exception {
    String dsl = """
        {
          "users": {
            "count": 3,
            "item": {
              "id": {"gen": "uuid"},
              "name": {"gen": "name.firstName"},
              "email": {"gen": "internet.emailAddress"}
            }
          }
        }
        """;

    // Test without memory optimization (default)
    IGeneration generation = DslDataGenerator.create()
        .fromJsonString(dsl)
        .generate();

    // Should work normally
    assertEquals(3, generation.getCollections().get("users").size());

    // Test streaming
    List<String> sqlStatements = generation.streamSqlInserts("users")
        .collect(Collectors.toList());

    assertEquals(3, sqlStatements.size());

    for (String sql : sqlStatements) {
      assertTrue(sql.contains("INSERT INTO users"));
      assertTrue(sql.contains("id"));
      assertTrue(sql.contains("name"));
      assertTrue(sql.contains("email"));
    }
  }

  @Test
  public void testMemoryOptimizationWithSelectiveFieldGeneration() throws Exception {
    String dsl = """
        {
          "users": {
            "count": 5,
            "item": {
              "id": {"gen": "uuid"},
              "name": {"gen": "name.firstName"},
              "email": {"gen": "internet.emailAddress"},
              "bio": {"gen": "lorem", "options": {"length": 200}},
              "address": {"gen": "address.fullAddress"}
            }
          },
          "posts": {
            "count": 20,
            "item": {
              "id": {"gen": "uuid"},
              "title": {"gen": "lorem", "options": {"length": 50}},
              "content": {"gen": "lorem", "options": {"length": 500}},
              "authorId": {"ref": "users[*].id"},
              "authorName": {"ref": "users[*].name"}
            }
          }
        }
        """;

    // Test with memory optimization - demonstrates streaming capability with large
    // datasets
    IGeneration optimizedGeneration = DslDataGenerator.create()
        .withMemoryOptimization()
        .fromJsonString(dsl)
        .generate();

    // Verify basic structure
    assertEquals(20, optimizedGeneration.getCollections().get("posts").size());

    optimizedGeneration.streamSqlInserts("posts")
        .forEach(sql -> {
          assertTrue(sql.contains("INSERT INTO posts"));
          assertTrue(sql.contains("authorId"));
          assertTrue(sql.contains("authorName"));
        });
  }

  @Test
  public void testNestedPathReferences() throws Exception {
    String dsl = """
        {
          "users": {
            "count": 5,
            "item": {
              "id": {"gen": "uuid"},
              "name": {"gen": "name.firstName"},
              "email": {"gen": "internet.emailAddress"},
              "address": {
                "street": {"gen": "address.streetAddress"},
                "city": {"gen": "address.city"},
                "zipCode": {"gen": "address.zipCode"},
                "country": {"gen": "address.country"}
              },
              "profile": {
                "bio": {"gen": "lorem", "options": {"length": 100}},
                "website": {"gen": "internet.url"},
                "social": {
                  "twitter": {"gen": "internet.username"},
                  "linkedin": {"gen": "internet.username"}
                }
              }
            }
          },
          "orders": {
            "count": 3,
            "item": {
              "id": {"gen": "uuid"},
              "userId": {"ref": "users[*].id"},
              "shippingStreet": {"ref": "users[*].address.street"},
              "customerName": {"ref": "users[*].name"},
              "socialHandle": {"ref": "users[*].profile.social.twitter"}
            }
          }
        }
        """;

    // Test with memory optimization - only referenced nested fields should be
    // generated
    IGeneration optimizedGeneration = DslDataGenerator.create()
        .withMemoryOptimization()
        .fromJsonString(dsl)
        .generate();

    // Test without memory optimization for comparison
    IGeneration fullGeneration = DslDataGenerator.create()
        .fromJsonString(dsl)
        .generate();

    // Both should have the same structure
    assertEquals(5, optimizedGeneration.getCollections().get("users").size());
    assertEquals(3, optimizedGeneration.getCollections().get("orders").size());
    assertEquals(5, fullGeneration.getCollections().get("users").size());
    assertEquals(3, fullGeneration.getCollections().get("orders").size());

    // Get the final JSON to verify nested references work
    JsonNode optimizedJson = optimizedGeneration.asJsonNode();
    JsonNode fullJson = fullGeneration.asJsonNode();

    // Verify that orders reference nested user fields correctly
    JsonNode optimizedFirstOrder = optimizedJson.get("orders").get(0);

    // Check that all nested references are resolved
    assertTrue(optimizedFirstOrder.has("userId"));
    assertTrue(optimizedFirstOrder.has("shippingStreet"));
    assertTrue(optimizedFirstOrder.has("customerName"));
    assertTrue(optimizedFirstOrder.has("socialHandle"));

    assertNotNull(optimizedFirstOrder.get("userId").asText());
    assertNotNull(optimizedFirstOrder.get("shippingStreet").asText());
    assertNotNull(optimizedFirstOrder.get("customerName").asText());
    assertNotNull(optimizedFirstOrder.get("socialHandle").asText());

    // Verify the values are not null and have reasonable content
    assertFalse(optimizedFirstOrder.get("userId").asText().isEmpty());
    assertFalse(optimizedFirstOrder.get("shippingStreet").asText().isEmpty());
    assertFalse(optimizedFirstOrder.get("customerName").asText().isEmpty());
    assertFalse(optimizedFirstOrder.get("socialHandle").asText().isEmpty());

    // Verify that the referenced user has the nested fields materialized
    JsonNode optimizedUsers = optimizedJson.get("users");
    JsonNode fullUsers = fullJson.get("users");

    for (int i = 0; i < optimizedUsers.size(); i++) {
      JsonNode optimizedUser = optimizedUsers.get(i);

      // These fields should be materialized in optimized version because they're
      // referenced
      assertTrue(optimizedUser.has("id"));
      assertTrue(optimizedUser.has("name"));
      assertTrue(optimizedUser.has("address"));
      assertTrue(optimizedUser.get("address").has("street"));
      assertTrue(optimizedUser.has("profile"));
      assertTrue(optimizedUser.get("profile").has("social"));
      assertTrue(optimizedUser.get("profile").get("social").has("twitter"));

      // Verify the nested values are not null and have content
      assertFalse(optimizedUser.get("id").asText().isEmpty());
      assertFalse(optimizedUser.get("name").asText().isEmpty());
      assertFalse(optimizedUser.get("address").get("street").asText().isEmpty());
      assertFalse(optimizedUser.get("profile").get("social").get("twitter").asText().isEmpty());
    }

  }
}
