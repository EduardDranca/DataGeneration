package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceSpreadTest extends ParameterizedGenerationTest {

    @BothImplementations
    void testBasicReferenceSpread(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree(
            """
                {
                    "users": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "choice", "options": ["Alice", "Bob", "Charlie"]},
                            "email": {"gen": "choice", "options": ["alice@test.com", "bob@test.com", "charlie@test.com"]}
                        }
                    },
                    "orders": {
                        "count": 5,
                        "item": {
                            "orderId": {"gen": "uuid"},
                            "...userInfo": {
                                "ref": "users[*]",
                                "fields": ["id", "name"]
                            }
                        }
                    }
                }
                """);

        Generation generation = generateFromDsl(dslNode, memoryOptimized);
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        List<JsonNode> users = collections.get("users");
        List<JsonNode> orders = collections.get("orders");

        assertThat(users).hasSize(3);

        assertThat(orders)
            .as("All orders should have correct spread fields")
            .hasSize(5)
            .allSatisfy(order -> {
                assertThat(order.has("orderId")).as("Order should have orderId").isTrue();
                assertThat(order.has("id")).as("Order should have spread id from user").isTrue();
                assertThat(order.has("name")).as("Order should have spread name from user").isTrue();
                assertThat(order.has("email")).as("Order should NOT have email (not in fields list)").isFalse();

                assertThat(order.get("orderId")).isNotNull();
                assertThat(order.get("id")).isNotNull();
                assertThat(order.get("name")).isNotNull();
            });
    }

    @BothImplementations
    void testReferenceSpreadWithFieldRenaming(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
            {
                "users": {
                    "count": 2,
                    "item": {
                        "id": {"gen": "uuid"},
                        "name": {"gen": "choice", "options": ["Alice", "Bob"]},
                        "email": {"gen": "choice", "options": ["alice@test.com", "bob@test.com"]}
                    }
                },
                "orders": {
                    "count": 4,
                    "item": {
                        "orderId": {"gen": "uuid"},
                        "...userInfo": {
                            "ref": "users[*]",
                            "fields": ["userId:id", "customerName:name"]
                        }
                    }
                }
            }
            """);

        Generation generation = generateFromDsl(dslNode, memoryOptimized);
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        List<JsonNode> orders = collections.get("orders");

        assertThat(orders)
            .as("All orders should have renamed spread fields")
            .hasSize(4)
            .allSatisfy(order -> {
                assertThat(order.has("orderId")).isTrue();
                assertThat(order.has("userId")).isTrue();
                assertThat(order.has("customerName")).isTrue();
                assertThat(order.has("id")).isFalse();
                assertThat(order.has("name")).isFalse();
                assertThat(order.has("email")).isFalse();
                assertThat(order.get("orderId")).isNotNull();
                assertThat(order.get("userId")).isNotNull();
                assertThat(order.get("customerName")).isNotNull();
            });
    }

    @BothImplementations
    void testReferenceSpreadWithoutFieldsArray(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
            {
                "users": {
                    "count": 2,
                    "item": {
                        "id": {"gen": "uuid"},
                        "name": {"gen": "choice", "options": ["Alice", "Bob"]}
                    }
                },
                "orders": {
                    "count": 3,
                    "item": {
                        "orderId": {"gen": "uuid"},
                        "...userInfo": {
                            "ref": "users[*]"
                        }
                    }
                }
            }
            """);

        Generation generation = generateFromDsl(dslNode, memoryOptimized);
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        List<JsonNode> orders = collections.get("orders");

        assertThat(orders)
            .as("All orders should have all user fields when no fields array is specified")
            .hasSize(3)
            .allSatisfy(order -> {
                assertThat(order.has("orderId")).isTrue();
                assertThat(order.has("id")).isTrue();
                assertThat(order.has("name")).isTrue();

                assertThat(order.get("orderId")).isNotNull();
                assertThat(order.get("id")).isNotNull();
                assertThat(order.get("name")).isNotNull();
            });
    }

    @BothImplementations
    void testReferenceSpreadWithSequential(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
            {
                "users": {
                    "count": 2,
                    "item": {
                        "id": {"gen": "uuid"},
                        "name": {"gen": "choice", "options": ["Alice", "Bob"]}
                    }
                },
                "orders": {
                    "count": 6,
                    "item": {
                        "orderId": {"gen": "uuid"},
                        "...userInfo": {
                            "ref": "users[*]",
                            "sequential": true,
                            "fields": ["userId:id", "userName:name"]
                        }
                    }
                }
            }
            """);

        Generation generation = generateFromDsl(dslNode, memoryOptimized);
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        List<JsonNode> users = collections.get("users");
        List<JsonNode> orders = collections.get("orders");

        assertThat(users).hasSize(2);
        assertThat(orders).hasSize(6);

        // Extract user data in order
        String[] userIds = users.stream()
            .map(user -> user.get("id").asText())
            .toArray(String[]::new);
        String[] userNames = users.stream()
            .map(user -> user.get("name").asText())
            .toArray(String[]::new);

        // Check that orders cycle through users sequentially - generate expected cycling pattern
        List<String> expectedUserIds = IntStream.range(0, 6)
            .mapToObj(i -> userIds[i % userIds.length])
            .toList();

        List<String> expectedUserNames = IntStream.range(0, 6)
            .mapToObj(i -> userNames[i % userNames.length])
            .toList();

        assertThat(orders)
            .as("Orders should cycle through users sequentially")
            .hasSize(6)
            .extracting(order -> order.get("userId").asText())
            .isEqualTo(expectedUserIds);

        assertThat(orders)
            .extracting(order -> order.get("userName").asText())
            .isEqualTo(expectedUserNames);
    }

}
