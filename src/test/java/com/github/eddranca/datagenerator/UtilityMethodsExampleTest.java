package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Example test demonstrating how to use the utility methods for easier test migration.
 * This shows how tests can be converted from the old API to the new streaming API
 * with minimal changes using the utility methods.
 */
class UtilityMethodsExampleTest extends ParameterizedGenerationTest {

    @BothImplementations
    void testLegacyJsonNodeFormat(String implementationName, boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                    "users": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "name.firstName"}
                        }
                    },
                    "products": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "commerce.productName"},
                            "price": {"gen": "number", "min": 10, "max": 100}
                        }
                    }
                }
                """;

        IGeneration generation = generateFromDsl(dsl, memoryOptimized);

        // OLD WAY: generation.asJsonNode()
        // NEW WAY: Use utility method to get the same format
        JsonNode collectionsNode = createLegacyJsonNode(generation);

        assertThat(collectionsNode).isNotNull();
        assertThat(collectionsNode.has("users")).isTrue();
        assertThat(collectionsNode.has("products")).isTrue();

        JsonNode users = collectionsNode.get("users");
        JsonNode products = collectionsNode.get("products");

        assertThat(users).isNotNull();
        assertThat(users.size()).isEqualTo(3);
        assertThat(products).isNotNull();
        assertThat(products.size()).isEqualTo(2);

        // Verify structure of individual items
        for (JsonNode user : users) {
            assertThat(user.has("id")).isTrue();
            assertThat(user.has("name")).isTrue();
        }

        for (JsonNode product : products) {
            assertThat(product.has("name")).isTrue();
            assertThat(product.has("price")).isTrue();
        }
    }

    @BothImplementations
    void testLegacyJsonStringFormat(String implementationName, boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                    "items": {
                        "count": 2,
                        "item": {
                            "type": "test",
                            "value": {"gen": "number", "min": 1, "max": 10}
                        }
                    }
                }
                """;

        IGeneration generation = generateFromDsl(dsl, memoryOptimized);

        // OLD WAY: generation.asJson()
        // NEW WAY: Use utility method to get the same format
        String jsonString = createLegacyJsonString(generation);

        assertThat(jsonString).isNotNull();
        assertThat(jsonString).contains("\"items\"");
        assertThat(jsonString).contains("\"type\" : \"test\"");
    }

    @BothImplementations
    void testLegacySqlFormat(String implementationName, boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                    "users": {
                        "count": 2,
                        "item": {
                            "name": "John",
                            "age": 25
                        }
                    }
                }
                """;

        IGeneration generation = generateFromDsl(dsl, memoryOptimized);

        // OLD WAY: generation.asSqlInserts() returned Map<String, String>
        // NEW WAY: Use utility method to get the same format
        Map<String, String> sqlMap = collectAllSqlInserts(generation);

        assertThat(sqlMap).hasSize(1);
        assertThat(sqlMap).containsKey("users");
        
        String usersSql = sqlMap.get("users");
        assertThat(usersSql).contains("INSERT INTO users");
        assertThat(usersSql).contains("John");
        assertThat(usersSql).contains("25");
        
        // Should have 2 INSERT statements joined by newlines
        String[] statements = usersSql.split("\n");
        assertThat(statements).hasSize(2);
        assertThat(statements[0]).startsWith("INSERT INTO users");
        assertThat(statements[1]).startsWith("INSERT INTO users");
    }

    @BothImplementations
    void testCollectAllJsonNodesFormat(String implementationName, boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                    "orders": {
                        "count": 2,
                        "item": {
                            "id": {"gen": "uuid"},
                            "status": "pending"
                        }
                    }
                }
                """;

        IGeneration generation = generateFromDsl(dsl, memoryOptimized);

        // NEW UTILITY: Get collections as Map<String, List<JsonNode>>
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);

        assertThat(collections).hasSize(1);
        assertThat(collections).containsKey("orders");
        
        List<JsonNode> orders = collections.get("orders");
        assertThat(orders).hasSize(2);
        
        for (JsonNode order : orders) {
            assertThat(order.has("id")).isTrue();
            assertThat(order.get("status").asText()).isEqualTo("pending");
        }
    }
}