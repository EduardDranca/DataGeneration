package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class SequentialReferenceTest extends ParameterizedGenerationTest {

    @BothImplementations
    void testBasicSequentialReference(String implementationName, boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "users": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 3},
                            "name": {"gen": "choice", "options": ["Alice", "Bob", "Charlie"]}
                        }
                    },
                    "orders": {
                        "count": 9,
                        "item": {
                            "id": {"gen": "uuid"},
                            "userId": {
                                "ref": "users[*].id",
                                "sequential": true
                            }
                        }
                    }
                }
                """);

        IGeneration generation = generateFromDsl(dslNode, memoryOptimized);

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        List<JsonNode> users = collections.get("users");
        List<JsonNode> orders = collections.get("orders");

        assertThat(users).hasSize(3);
        assertThat(orders).hasSize(9);

        // Extract user IDs in order
        int[] userIds = users.stream()
            .mapToInt(user -> user.get("id").intValue())
            .toArray();

        assertThat(orders)
            .extracting(o -> o.get("userId").intValue())
            .as("Orders should cycle through user IDs in round-robin fashion")
            .containsExactlyElementsOf(
                IntStream.range(0, 9)
                    .map(i -> userIds[i % userIds.length])
                    .boxed()
                    .toList()
            );
    }

    @BothImplementations
    void testSequentialReferenceWithFiltering(String implementationName, boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "users": {
                        "count": 4,
                        "item": {
                            "id": {"gen": "uuid"},
                            "type": {"gen": "choice", "options": ["admin", "user", "guest", "banned"]}
                        },
                        "pick": {
                            "bannedUser": 3
                        }
                    },
                    "orders": {
                        "count": 6,
                        "item": {
                            "id": {"gen": "uuid"},
                            "userId": {
                                "ref": "users[*].id",
                                "sequential": true,
                                "filter": [{"ref": "bannedUser.id"}]
                            }
                        }
                    }
                }
                """);

        IGeneration generation = generateFromDsl(dslNode, memoryOptimized);

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        List<JsonNode> users = collections.get("users");
        List<JsonNode> orders = collections.get("orders");

        assertThat(users).hasSize(4);
        assertThat(orders).hasSize(6);

        // Get banned user ID
        String bannedUserId = users.get(3).get("id").asText();

        // Extract non-banned user IDs in order
        String[] validUserIds = users.stream()
            .filter(user -> !user.get("id").asText().equals(bannedUserId))
            .map(user -> user.get("id").asText())
            .toArray(String[]::new);

        assertThat(validUserIds)
            .as("Should have 3 non-banned users")
            .hasSize(3);

        List<String> actualUserIds = orders.stream()
            .map(order -> order.get("userId").asText())
            .toList();

        List<String> expectedUserIds = java.util.stream.IntStream.range(0, 6)
            .mapToObj(i -> validUserIds[i % validUserIds.length])
            .toList();

        assertThat(actualUserIds)
            .as("Orders should cycle through valid user IDs in filtered round-robin fashion")
            .containsExactlyElementsOf(expectedUserIds)
            .doesNotContain(bannedUserId);
    }

    @BothImplementations
    void testRandomVsSequentialReference(String implementationName, boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "users": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 3}
                        }
                    },
                    "orders": {
                        "count": 6,
                        "item": {
                            "id": {"gen": "uuid"},
                            "sequentialUserId": {
                                "ref": "users[*].id",
                                "sequential": true
                            },
                            "randomUserId": {
                                "ref": "users[*].id",
                                "sequential": false
                            }
                        }
                    }
                }
                """);

        IGeneration generation = generateFromDsl(dslNode, memoryOptimized);

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        List<JsonNode> users = collections.get("users");
        List<JsonNode> orders = collections.get("orders");

        assertThat(users).hasSize(3);
        assertThat(orders).hasSize(6);

        // Extract user IDs in order
        int[] userIds = users.stream()
            .mapToInt(user -> user.get("id").intValue())
            .toArray();

        List<Integer> actualSequentialIds = orders.stream()
            .map(order -> order.get("sequentialUserId").intValue())
            .toList();

        List<Integer> expectedSequentialIds = java.util.stream.IntStream.range(0, 6)
            .map(i -> userIds[i % userIds.length])
            .boxed()
            .toList();

        assertThat(actualSequentialIds)
            .as("Sequential references should follow round-robin pattern")
            .containsExactlyElementsOf(expectedSequentialIds);

        List<Integer> actualRandomIds = orders.stream()
            .map(order -> order.get("randomUserId").intValue())
            .toList();

        List<Integer> validUserIdsList = java.util.Arrays.stream(userIds).boxed().toList();

        assertThat(actualRandomIds)
            .as("Random references should all be valid user IDs")
            .isNotEmpty()
            .allSatisfy(randomId -> assertThat(validUserIdsList).contains(randomId));
    }

    @BothImplementations
    void testSequentialReferenceWithTaggedCollections(String implementationName, boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "staff_admins": {
                        "name": "staff",
                        "count": 2,
                        "tags": ["staff"],
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 2},
                            "role": "admin"
                        }
                    },
                    "staff_managers": {
                        "name": "staff",
                        "count": 2,
                        "tags": ["staff"],
                        "item": {
                            "id": {"gen": "number", "min": 3, "max": 4},
                            "role": "manager"
                        }
                    },
                    "tasks": {
                        "count": 8,
                        "item": {
                            "id": {"gen": "uuid"},
                            "assignedTo": {
                                "ref": "byTag[staff].id",
                                "sequential": true
                            }
                        }
                    }
                }
                """);

        IGeneration generation = generateFromDsl(dslNode, memoryOptimized);

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        List<JsonNode> tasks = collections.get("tasks");

        assertThat(tasks).hasSize(8);

        assertThat(tasks)
            .extracting(task -> task.get("assignedTo").intValue())
            .as("All tasks should be assigned to valid staff member IDs (1-4)")
            .allSatisfy(assignedTo -> assertThat(assignedTo).isBetween(1, 4));
    }

    @BothImplementations
    void testSequentialReferenceDefaultBehavior(String implementationName, boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "users": {
                        "count": 2,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 2}
                        }
                    },
                    "orders": {
                        "count": 4,
                        "item": {
                            "id": {"gen": "uuid"},
                            "userId": {
                                "ref": "users[*].id"
                            }
                        }
                    }
                }
                """);

        IGeneration generation = generateFromDsl(dslNode, memoryOptimized);

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        List<JsonNode> orders = collections.get("orders");

        assertThat(orders).hasSize(4);

        // All user IDs should be valid (1 or 2)
        assertThat(orders)
            .extracting(order -> order.get("userId").intValue())
            .as("All user IDs should be valid (1 or 2)")
            .allSatisfy(userId -> assertThat(userId).isIn(1, 2));
    }
}
