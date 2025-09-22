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
        Generation optimizedGeneration = DslDataGenerator.create()
            .withMemoryOptimization()
            .fromJsonString(dsl)
            .generate();

        // Test without memory optimization for comparison
        Generation fullGeneration = DslDataGenerator.create()
            .fromJsonString(dsl)
            .generate();

        // Both should have the same structure
        assertThat(optimizedGeneration.getCollectionSize("users")).isEqualTo(5);
        assertThat(optimizedGeneration.getCollectionSize("orders")).isEqualTo(3);
        assertThat(fullGeneration.getCollectionSize("users")).isEqualTo(5);
        assertThat(fullGeneration.getCollectionSize("orders")).isEqualTo(3);

        // Get the final JSON streams to verify nested references work
        List<JsonNode> optimizedUsers = optimizedGeneration.streamJsonNodes("users").toList();
        List<JsonNode> optimizedOrders = optimizedGeneration.streamJsonNodes("orders").toList();
        // TODO: rethink this test
        List<JsonNode> fullUsers = fullGeneration.streamJsonNodes("users").toList();
        List<JsonNode> fullOrders = fullGeneration.streamJsonNodes("orders").toList();

        // Verify that orders reference nested user fields correctly
        JsonNode optimizedFirstOrder = optimizedOrders.get(0);

        // Check that all nested references are resolved
        assertThat(optimizedFirstOrder.has("userId")).isTrue();
        assertThat(optimizedFirstOrder.has("shippingStreet")).isTrue();
        assertThat(optimizedFirstOrder.has("customerName")).isTrue();
        assertThat(optimizedFirstOrder.has("socialHandle")).isTrue();

        assertThat(optimizedFirstOrder.get("userId").asText()).isNotNull();
        assertThat(optimizedFirstOrder.get("shippingStreet").asText()).isNotNull();
        assertThat(optimizedFirstOrder.get("customerName").asText()).isNotNull();
        assertThat(optimizedFirstOrder.get("socialHandle").asText()).isNotNull();

        // Verify the values are not null and have reasonable content
        assertThat(optimizedFirstOrder.get("userId").asText()).isNotEmpty();
        assertThat(optimizedFirstOrder.get("shippingStreet").asText()).isNotEmpty();
        assertThat(optimizedFirstOrder.get("customerName").asText()).isNotEmpty();
        assertThat(optimizedFirstOrder.get("socialHandle").asText()).isNotEmpty();

        // Verify that the referenced user has the nested fields materialized
        for (int i = 0; i < optimizedUsers.size(); i++) {
            JsonNode optimizedUser = optimizedUsers.get(i);

            // These fields should be materialized in optimized version because they're
            // referenced
            assertThat(optimizedUser.has("id")).isTrue();
            assertThat(optimizedUser.has("name")).isTrue();
            assertThat(optimizedUser.has("address")).isTrue();
            assertThat(optimizedUser.get("address").has("street")).isTrue();
            assertThat(optimizedUser.has("profile")).isTrue();
            assertThat(optimizedUser.get("profile").has("social")).isTrue();
            assertThat(optimizedUser.get("profile").get("social").has("twitter")).isTrue();

            // Verify the nested values are not null and have content
            assertThat(optimizedUser.get("id").asText()).isNotEmpty();
            assertThat(optimizedUser.get("name").asText()).isNotEmpty();
            assertThat(optimizedUser.get("address").get("street").asText()).isNotEmpty();
            assertThat(optimizedUser.get("profile").get("social").get("twitter").asText()).isNotEmpty();
        }

    }
}
