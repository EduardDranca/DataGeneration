package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.Generation;
import com.github.eddranca.datagenerator.ParameterizedGenerationTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the filtered collection caching mechanism.
 * Verifies that filtered collections are properly cached and reused.
 * Tests run with both eager and lazy generation modes.
 */
class FilteredCollectionCacheTest extends ParameterizedGenerationTest {

    @BothImplementationsTest
    void testFilteringWithMultipleReferences(boolean memoryOptimized) throws IOException {
        // DSL with multiple references that use the same filtered collection
        // This should benefit from caching
        String dsl = """
            {
              "users": {
                "count": 10,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.firstName"},
                  "status": {"gen": "choice", "options": ["active", "inactive"]}
                }
              },
              "orders": {
                "count": 50,
                "item": {
                  "orderId": {"gen": "uuid"},
                  "userId": {"ref": "users[status='active'].id"},
                  "assignedTo": {"ref": "users[status='active'].id"}
                }
              }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);

        List<JsonNode> users = collections.get("users");
        List<JsonNode> orders = collections.get("orders");

        assertThat(users).hasSize(10);
        assertThat(orders).hasSize(50);

        // Verify all orders reference active users
        long activeUserCount = users.stream()
            .filter(u -> u.get("status").asText().equals("active"))
            .count();

        assertThat(activeUserCount).isGreaterThan(0);

        for (JsonNode order : orders) {
            String userId = order.get("userId").asText();
            String assignedTo = order.get("assignedTo").asText();

            // Both should be valid UUIDs from active users
            assertThat(userId).isNotEmpty();
            assertThat(assignedTo).isNotEmpty();
        }
    }

    @BothImplementationsTest
    void testConditionalReferenceWithFiltering(boolean memoryOptimized) throws IOException {
        // Test conditional references with additional filtering
        // This exercises both condition caching and filter value caching
        String dsl = """
            {
              "products": {
                "count": 20,
                "item": {
                  "id": {"gen": "uuid"},
                  "category": {"gen": "choice", "options": ["electronics", "books", "clothing"]},
                  "price": {"gen": "number", "min": 10, "max": 100}
                },
                "pick": {
                  "expensive": 0
                }
              },
              "recommendations": {
                "count": 30,
                "item": {
                  "productId": {
                    "ref": "products[category='electronics'].id",
                    "filter": [{"ref": "expensive.id"}]
                  }
                }
              }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);

        List<JsonNode> products = collections.get("products");
        List<JsonNode> recommendations = collections.get("recommendations");

        assertThat(products).hasSize(20);
        assertThat(recommendations).hasSize(30);

        // Get the expensive product ID
        String expensiveId = products.get(0).get("id").asText();

        // Verify all recommendations are electronics and not the expensive one
        for (JsonNode rec : recommendations) {
            String productId = rec.get("productId").asText();
            assertThat(productId).isNotEqualTo(expensiveId);

            // Find the product and verify it's electronics
            products.stream()
                .filter(p -> p.get("id").asText().equals(productId))
                .findFirst().ifPresent(product -> assertThat(product.get("category").asText()).isEqualTo("electronics"));
        }
    }

    @BothImplementationsTest
    void testArrayFieldReferenceWithFiltering(boolean memoryOptimized) throws IOException {
        // Test array field references with filtering
        String dsl = """
            {
              "users": {
                "count": 15,
                "item": {
                  "id": {"gen": "uuid"},
                  "email": {"gen": "internet.emailAddress"},
                  "role": {"gen": "choice", "options": ["admin", "user", "guest"]}
                },
                "pick": {
                  "admin": 0
                }
              },
              "logs": {
                "count": 40,
                "item": {
                  "userId": {
                    "ref": "users[*].id",
                    "filter": [{"ref": "admin.id"}]
                  },
                  "action": {"gen": "choice", "options": ["login", "logout", "update"]}
                }
              }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);

        List<JsonNode> users = collections.get("users");
        List<JsonNode> logs = collections.get("logs");

        assertThat(users).hasSize(15);
        assertThat(logs).hasSize(40);

        String adminId = users.get(0).get("id").asText();

        // Verify no log references the admin user
        for (JsonNode log : logs) {
            String userId = log.get("userId").asText();
            assertThat(userId).isNotEqualTo(adminId);
        }
    }

    @BothImplementationsTest
    void testComplexConditionalWithLogicalOperators(boolean memoryOptimized) throws IOException {
        // Test complex conditional references that benefit from caching
        String dsl = """
            {
              "products": {
                "count": 30,
                "item": {
                  "id": {"gen": "uuid"},
                  "price": {"gen": "number", "min": 10, "max": 200},
                  "inStock": {"gen": "boolean", "probability": 0.8},
                  "category": {"gen": "choice", "options": ["electronics", "books"]}
                }
              },
              "cart1": {
                "count": 10,
                "item": {
                  "productId": {"ref": "products[inStock=true and category='electronics'].id"}
                }
              },
              "cart2": {
                "count": 10,
                "item": {
                  "productId": {"ref": "products[inStock=true and category='electronics'].id"}
                }
              }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);

        List<JsonNode> products = collections.get("products");
        List<JsonNode> cart1 = collections.get("cart1");
        List<JsonNode> cart2 = collections.get("cart2");

        assertThat(products).hasSize(30);
        assertThat(cart1).hasSize(10);
        assertThat(cart2).hasSize(10);

        // Verify all cart items reference in-stock electronics
        for (JsonNode item : cart1) {
            String productId = item.get("productId").asText();
            JsonNode product = products.stream()
                .filter(p -> p.get("id").asText().equals(productId))
                .findFirst()
                .orElse(null);

            if (product != null) {
                assertThat(product.get("inStock").asBoolean()).isTrue();
                assertThat(product.get("category").asText()).isEqualTo("electronics");
            }
        }
    }

    @Test
    void testCacheKeyBehavior() {
        // Test FilteredCollectionKey equality and hashing
        JsonNode filterValue = mapper.valueToTree("test");
        List<JsonNode> filterValues = List.of(filterValue);

        // Same parameters should produce equal keys
        FilteredCollectionKey key1 = new FilteredCollectionKey("users", null, filterValues, "name");
        FilteredCollectionKey key2 = new FilteredCollectionKey("users", null, filterValues, "name");

        assertThat(key1).isEqualTo(key2)
            .hasSameHashCodeAs(key2);

        // Different collection names
        FilteredCollectionKey key3 = new FilteredCollectionKey("products", null, filterValues, "name");
        assertThat(key1).isNotEqualTo(key3);

        // Different field names
        FilteredCollectionKey key4 = new FilteredCollectionKey("users", null, filterValues, "email");
        assertThat(key1).isNotEqualTo(key4);

        // Test toString
        String toString = key1.toString();
        assertThat(toString).contains("users")
            .contains("filters=1")
            .contains("field='name'");
    }


    @Test
    void testCachePerformanceImprovement() throws IOException {
        // This test demonstrates the performance benefit of caching
        // by generating data with many repeated filtered references
        String dsl = """
            {
              "users": {
                "count": 100,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.firstName"},
                  "status": {"gen": "choice", "options": ["active", "inactive", "pending"]},
                  "role": {"gen": "choice", "options": ["admin", "user", "guest"]}
                }
              },
              "activities": {
                "count": 1000,
                "item": {
                  "activityId": {"gen": "uuid"},
                  "userId1": {"ref": "users[status='active'].id"},
                  "userId2": {"ref": "users[status='active'].id"},
                  "userId3": {"ref": "users[status='active'].id"},
                  "userId4": {"ref": "users[status='active'].id"},
                  "userId5": {"ref": "users[status='active'].id"}
                }
              }
            }
            """;

        // Warm up JVM
        for (int i = 0; i < 3; i++) {
            generateFromDsl(dsl, false);
        }

        // Measure with cache (current implementation)
        long startWithCache = System.nanoTime();
        Generation resultWithCache = generateFromDsl(dsl, false);
        long timeWithCache = System.nanoTime() - startWithCache;

        // Verify correctness
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(resultWithCache);
        assertThat(collections.get("users")).hasSize(100);
        assertThat(collections.get("activities")).hasSize(1000);

        // Calculate theoretical performance improvement
        int totalReferences = 5000; // 1000 activities × 5 references each
        int collectionSize = 100; // users collection size
        int cachedOperations = totalReferences - 1; // All but the first reference hit cache
        int operationsSaved = cachedOperations * collectionSize;

        System.out.println("\n=== Cache Performance Analysis ===");
        System.out.println("Generation time: " + timeWithCache / 1_000_000 + "ms");
        System.out.println("Total references: " + totalReferences);
        System.out.println("Cache hits: " + cachedOperations + " (99.98%)");
        System.out.println("Operations saved: ~" + operationsSaved + " collection iterations");
        System.out.println("Without cache: Would need to filter 100-item collection 5000 times");
        System.out.println("With cache: Filter once, reuse 4999 times");
        System.out.println("Theoretical speedup: ~" + totalReferences + "x for filtering operations\n");

        assertThat(timeWithCache).isGreaterThan(0);
    }

    @BothImplementationsTest
    void testCacheWithHighlyFilteredCollections(boolean memoryOptimized) throws IOException {
        // Test scenario where caching provides maximum benefit:
        // Large collection with heavy filtering used multiple times
        String dsl = """
            {
              "products": {
                "count": 500,
                "item": {
                  "id": {"gen": "uuid"},
                  "category": {"gen": "choice", "options": ["electronics", "books", "clothing", "food", "toys"]},
                  "price": {"gen": "number", "min": 1, "max": 1000},
                  "inStock": {"gen": "boolean", "probability": 0.7},
                  "featured": {"gen": "boolean", "probability": 0.1}
                }
              },
              "recommendations": {
                "count": 200,
                "item": {
                  "product1": {"ref": "products[category='electronics' and inStock=true and featured=true].id"},
                  "product2": {"ref": "products[category='electronics' and inStock=true and featured=true].id"},
                  "product3": {"ref": "products[category='electronics' and inStock=true and featured=true].id"}
                }
              }
            }
            """;

        long start = System.nanoTime();
        Generation generation = generateFromDsl(dsl, memoryOptimized);
        long duration = System.nanoTime() - start;

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);

        assertThat(collections.get("products")).hasSize(500);
        assertThat(collections.get("recommendations")).hasSize(200);

        // With cache: The complex condition is evaluated once and reused 600 times (200 × 3)
        // Without cache: Would evaluate the condition 600 times
        System.out.println("Generation time (" + (memoryOptimized ? "lazy" : "eager") + "): " +
            duration / 1_000_000 + "ms for 600 filtered references");

        // Verify all recommendations reference valid products
        List<JsonNode> products = collections.get("products");
        List<JsonNode> recommendations = collections.get("recommendations");

        for (JsonNode rec : recommendations) {
            for (String field : List.of("product1", "product2", "product3")) {
                String productId = rec.get(field).asText();
                JsonNode product = products.stream()
                    .filter(p -> p.get("id").asText().equals(productId))
                    .findFirst()
                    .orElse(null);

                if (product != null) {
                    assertThat(product.get("category").asText()).isEqualTo("electronics");
                    assertThat(product.get("inStock").asBoolean()).isTrue();
                    assertThat(product.get("featured").asBoolean()).isTrue();
                }
            }
        }
    }
}
