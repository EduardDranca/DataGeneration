package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
        Generation generation = DslDataGenerator.create()
            .withMemoryOptimization()
            .fromJsonString(dsl)
            .generate();

        // Verify structure
        assertThat(generation.getCollectionNames()).contains("users", "posts");
        assertThat(generation.getCollectionSize("users")).isEqualTo(50);
        assertThat(generation.getCollectionSize("posts")).isEqualTo(30);
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
        Generation generation = DslDataGenerator.create()
            .fromJsonString(dsl)
            .generate();

        // Should work normally
        assertThat(generation.getCollectionSize("users")).isEqualTo(3);

        // Test streaming
        List<String> sqlStatements = generation.streamSqlInserts("users").toList();

        assertThat(sqlStatements).hasSize(3);

        for (String sql : sqlStatements) {
            assertThat(sql).contains("INSERT INTO users", "id", "name", "email");
        }
    }

    @Test
    void testMemoryOptimizationWithSelectiveFieldGeneration() throws Exception {
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
        Generation optimizedGeneration = DslDataGenerator.create()
            .withMemoryOptimization()
            .fromJsonString(dsl)
            .generate();

        // Verify basic structure
        assertThat(optimizedGeneration.getCollectionSize("posts")).isEqualTo(20);

        optimizedGeneration.streamSqlInserts("posts")
            .forEach(sql -> {
                assertThat(sql).contains("INSERT INTO posts", "authorId", "authorName");
            });
    }

    @Test
    void testNestedPathReferences() throws Exception {
        String dsl = """
            {
              "users": {
                "count": 3,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.firstName"},
                  "address": {
                    "street": {"gen": "address.streetAddress"},
                    "city": {"gen": "address.city"}
                  },
                  "profile": {
                    "social": {
                      "twitter": {"gen": "internet.username"}
                    }
                  }
                }
              },
              "orders": {
                "count": 2,
                "item": {
                  "id": {"gen": "uuid"},
                  "userId": {"ref": "users[*].id"},
                  "shippingStreet": {"ref": "users[*].address.street"},
                  "socialHandle": {"ref": "users[*].profile.social.twitter"}
                }
              }
            }
            """;

        // Test with memory optimization - only referenced nested fields should be generated
        Generation generation = DslDataGenerator.create()
            .withMemoryOptimization()
            .fromJsonString(dsl)
            .generate();

        // Verify basic structure
        assertThat(generation.getCollectionSize("users")).isEqualTo(3);
        assertThat(generation.getCollectionSize("orders")).isEqualTo(2);

        // Verify that orders have all nested references resolved
        List<JsonNode> orders = generation.streamJsonNodes("orders").toList();
        JsonNode firstOrder = orders.get(0);

        assertThat(firstOrder.get("userId").asText()).isNotEmpty();
        assertThat(firstOrder.get("shippingStreet").asText()).isNotEmpty();
        assertThat(firstOrder.get("socialHandle").asText()).isNotEmpty();

        // Verify that users have the referenced nested fields materialized
        List<JsonNode> users = generation.streamJsonNodes("users").toList();
        JsonNode firstUser = users.get(0);

        assertThat(firstUser.has("id")).isTrue();
        assertThat(firstUser.path("address").path("street").asText()).isNotEmpty();
        assertThat(firstUser.path("profile").path("social").path("twitter").asText()).isNotEmpty();
    }
}
