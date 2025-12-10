package com.github.eddranca.datagenerator;

import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for range references combined with sequential referencing.
 * Sequential referencing cycles through the range in order rather than randomly selecting.
 */
class RangeSequentialReferenceTest extends ParameterizedGenerationTest {

    @BothImplementationsTest
    @DisplayName("Should cycle through fixed range [0:4] sequentially")
    void shouldCycleThroughFixedRangeSequentially(boolean memoryOptimized) throws Exception {
        String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "sequence", "start": 1}
                    }
                  },
                  "orders": {
                    "count": 15,
                    "item": {
                      "userId": {
                        "ref": "users[0:4].id",
                        "sequential": true
                      }
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 123L, memoryOptimized);

        List<Integer> userIds = new ArrayList<>();
        generation.streamJsonNodes("orders").forEach(item -> {
            userIds.add(item.get("userId").asInt());
        });

        // Should cycle through users 1-5 three times
        assertThat(userIds).hasSize(15);
        assertThat(userIds).containsExactly(
                1, 2, 3, 4, 5,  // First cycle
                1, 2, 3, 4, 5,  // Second cycle
                1, 2, 3, 4, 5   // Third cycle
        );
    }

    @BothImplementationsTest
    @DisplayName("Should cycle through open end range [2:] sequentially")
    void shouldCycleThroughOpenEndRangeSequentially(boolean memoryOptimized) throws Exception {
        String dsl = """
                {
                  "users": {
                    "count": 10,
                    "item": {
                      "id": {"gen": "sequence", "start": 1}
                    }
                  },
                  "tasks": {
                    "count": 16,
                    "item": {
                      "assignedTo": {
                        "ref": "users[2:].id",
                        "sequential": true
                      }
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 456L, memoryOptimized);

        List<Integer> assignedIds = new ArrayList<>();
        generation.streamJsonNodes("tasks").forEach(item -> {
            assignedIds.add(item.get("assignedTo").asInt());
        });

        // Should cycle through users 3-10 (8 users) twice
        assertThat(assignedIds).hasSize(16);
        assertThat(assignedIds).containsExactly(
                3, 4, 5, 6, 7, 8, 9, 10,  // First cycle
                3, 4, 5, 6, 7, 8, 9, 10   // Second cycle
        );
    }

    @BothImplementationsTest
    @DisplayName("Should cycle through open start range [:3] sequentially")
    void shouldCycleThroughOpenStartRangeSequentially(boolean memoryOptimized) throws Exception {
        String dsl = """
                {
                  "users": {
                    "count": 10,
                    "item": {
                      "id": {"gen": "sequence", "start": 1}
                    }
                  },
                  "premiumFeatures": {
                    "count": 12,
                    "item": {
                      "userId": {
                        "ref": "users[:3].id",
                        "sequential": true
                      }
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 789L, memoryOptimized);

        List<Integer> userIds = new ArrayList<>();
        generation.streamJsonNodes("premiumFeatures").forEach(item -> {
            userIds.add(item.get("userId").asInt());
        });

        // Should cycle through users 1-4 (indices 0-3) three times
        assertThat(userIds).hasSize(12);
        assertThat(userIds).containsExactly(
                1, 2, 3, 4,  // First cycle
                1, 2, 3, 4,  // Second cycle
                1, 2, 3, 4   // Third cycle
        );
    }

    @BothImplementationsTest
    @DisplayName("Should cycle through negative range [-5:-1] sequentially")
    void shouldCycleThroughNegativeRangeSequentially(boolean memoryOptimized) throws Exception {
        String dsl = """
                {
                  "users": {
                    "count": 20,
                    "item": {
                      "id": {"gen": "sequence", "start": 1}
                    }
                  },
                  "vipAccess": {
                    "count": 15,
                    "item": {
                      "userId": {
                        "ref": "users[-5:-1].id",
                        "sequential": true
                      }
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 101112L, memoryOptimized);

        List<Integer> userIds = new ArrayList<>();
        generation.streamJsonNodes("vipAccess").forEach(item -> {
            userIds.add(item.get("userId").asInt());
        });

        // Should cycle through last 5 users (16-20) three times
        assertThat(userIds).hasSize(15);
        assertThat(userIds).containsExactly(
                16, 17, 18, 19, 20,  // First cycle
                16, 17, 18, 19, 20,  // Second cycle
                16, 17, 18, 19, 20   // Third cycle
        );
    }

    @BothImplementationsTest
    @DisplayName("Should cycle through full range [:] sequentially")
    void shouldCycleThroughFullRangeSequentially(boolean memoryOptimized) throws Exception {
        String dsl = """
                {
                  "categories": {
                    "count": 3,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "name": {"gen": "choice", "options": ["Electronics", "Books", "Clothing"]}
                    }
                  },
                  "products": {
                    "count": 9,
                    "item": {
                      "categoryId": {
                        "ref": "categories[:].id",
                        "sequential": true
                      }
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 131415L, memoryOptimized);

        List<Integer> categoryIds = new ArrayList<>();
        generation.streamJsonNodes("products").forEach(item -> {
            categoryIds.add(item.get("categoryId").asInt());
        });

        // Should cycle through all 3 categories three times
        assertThat(categoryIds).hasSize(9);
        assertThat(categoryIds).containsExactly(
                1, 2, 3,  // First cycle
                1, 2, 3,  // Second cycle
                1, 2, 3   // Third cycle
        );
    }

    @BothImplementationsTest
    @DisplayName("Should handle single item range [2:2] sequentially")
    void shouldHandleSingleItemRangeSequentially(boolean memoryOptimized) throws Exception {
        String dsl = """
                {
                  "users": {
                    "count": 10,
                    "item": {
                      "id": {"gen": "sequence", "start": 1}
                    }
                  },
                  "specialOrders": {
                    "count": 5,
                    "item": {
                      "userId": {
                        "ref": "users[2:2].id",
                        "sequential": true
                      }
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 161718L, memoryOptimized);

        List<Integer> userIds = new ArrayList<>();
        generation.streamJsonNodes("specialOrders").forEach(item -> {
            userIds.add(item.get("userId").asInt());
        });

        // Should always reference user 3 (index 2)
        assertThat(userIds).hasSize(5);
        assertThat(userIds).containsOnly(3);
    }

    @BothImplementationsTest
    @DisplayName("Should cycle through range with nested field access sequentially")
    void shouldCycleThroughRangeWithNestedFieldSequentially(boolean memoryOptimized) throws Exception {
        String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {
                      "profile": {
                        "userId": {"gen": "sequence", "start": 100}
                      }
                    }
                  },
                  "orders": {
                    "count": 10,
                    "item": {
                      "userId": {
                        "ref": "users[0:2].profile.userId",
                        "sequential": true
                      }
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 192021L, memoryOptimized);

        List<Integer> userIds = new ArrayList<>();
        generation.streamJsonNodes("orders").forEach(item -> {
            userIds.add(item.get("userId").asInt());
        });

        // Should cycle through first 3 users (indices 0-2)
        assertThat(userIds).hasSize(10);
        assertThat(userIds).containsExactly(
                100, 101, 102,  // First cycle
                100, 101, 102,  // Second cycle
                100, 101, 102,  // Third cycle
                100              // Partial fourth cycle
        );
    }

    @BothImplementationsTest
    @DisplayName("Should work with range sequential reference in lazy mode")
    void shouldWorkWithRangeSequentialInLazyMode(boolean memoryOptimized) throws Exception {
        String dsl = """
                {
                  "users": {
                    "count": 4,
                    "item": {
                      "id": {"gen": "sequence", "start": 1}
                    }
                  },
                  "orders": {
                    "count": 12,
                    "item": {
                      "userId": {
                        "ref": "users[0:3].id",
                        "sequential": true
                      }
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 222324L, memoryOptimized);

        List<Integer> userIds = new ArrayList<>();
        generation.streamJsonNodes("orders").forEach(item -> {
            userIds.add(item.get("userId").asInt());
        });

        // Should cycle through users 1-4 three times
        assertThat(userIds).hasSize(12);
        assertThat(userIds).containsExactly(
                1, 2, 3, 4,  // First cycle
                1, 2, 3, 4,  // Second cycle
                1, 2, 3, 4   // Third cycle
        );
    }

    @BothImplementationsTest
    @DisplayName("Should distribute items evenly across range with sequential")
    void shouldDistributeItemsEvenlyAcrossRange(boolean memoryOptimized) throws Exception {
        String dsl = """
                {
                  "managers": {
                    "count": 3,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "name": {"gen": "name.fullName"}
                    }
                  },
                  "employees": {
                    "count": 30,
                    "item": {
                      "id": {"gen": "sequence", "start": 100},
                      "managerId": {
                        "ref": "managers[:].id",
                        "sequential": true
                      }
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 252627L, memoryOptimized);

        List<Integer> managerIds = new ArrayList<>();
        generation.streamJsonNodes("employees").forEach(item -> {
            managerIds.add(item.get("managerId").asInt());
        });

        // Each manager should have exactly 10 employees
        assertThat(managerIds).hasSize(30);
        long manager1Count = managerIds.stream().filter(id -> id == 1).count();
        long manager2Count = managerIds.stream().filter(id -> id == 2).count();
        long manager3Count = managerIds.stream().filter(id -> id == 3).count();

        assertThat(manager1Count).isEqualTo(10);
        assertThat(manager2Count).isEqualTo(10);
        assertThat(manager3Count).isEqualTo(10);
    }

    @BothImplementationsTest
    @DisplayName("Should handle partial cycle at end of range sequential reference")
    void shouldHandlePartialCycleAtEnd(boolean memoryOptimized) throws Exception {
        String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "sequence", "start": 1}
                    }
                  },
                  "orders": {
                    "count": 7,
                    "item": {
                      "userId": {
                        "ref": "users[0:2].id",
                        "sequential": true
                      }
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 282930L, memoryOptimized);

        List<Integer> userIds = new ArrayList<>();
        generation.streamJsonNodes("orders").forEach(item -> {
            userIds.add(item.get("userId").asInt());
        });

        // Should cycle through users 1-3 twice, then start third cycle
        assertThat(userIds).hasSize(7);
        assertThat(userIds).containsExactly(
                1, 2, 3,  // First cycle
                1, 2, 3,  // Second cycle
                1         // Partial third cycle
        );
    }
}
