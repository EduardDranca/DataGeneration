package com.github.eddranca.datagenerator;

import com.github.eddranca.datagenerator.exception.DslValidationException;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RangeReferencesTest extends ParameterizedGenerationTest {

    @BothImplementationsTest
    @DisplayName("Should support fixed inclusive range [0:99]")
    void shouldSupportFixedRange(boolean memoryOptimized) throws Exception {
        String dsl = """
                {
                  "users": {
                    "count": 100,
                    "item": {
                      "id": {"gen": "sequence", "start": 1}
                    }
                  },
                  "betaFeatures": {
                    "count": 50,
                    "item": {
                      "userId": {"ref": "users[0:99].id"}
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 123L, memoryOptimized);

        generation.streamJsonNodes("betaFeatures").forEach(item -> {
            int id = item.get("userId").asInt();
            assertThat(id).isBetween(1, 100);
        });
    }

    @BothImplementationsTest
    @DisplayName("Should support open end range [10:]")
    void shouldSupportOpenEndRange(boolean memoryOptimized) throws Exception {
        String dsl = """
                {
                  "users": {
                    "count": 20,
                    "item": {
                      "id": {"gen": "sequence", "start": 1}
                    }
                  },
                  "orders": {
                    "count": 20,
                    "item": {
                      "userId": {"ref": "users[10:].id"}
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 456L, memoryOptimized);

        generation.streamJsonNodes("orders").forEach(item -> {
            int id = item.get("userId").asInt();
            assertThat(id).isBetween(11, 20);
        });
    }

    @BothImplementationsTest
    @DisplayName("Should support open start range [:10]")
    void shouldSupportOpenStartRange(boolean memoryOptimized) throws Exception {
        String dsl = """
                {
                  "users": {
                    "count": 30,
                    "item": {
                      "id": {"gen": "sequence", "start": 1}
                    }
                  },
                  "samples": {
                    "count": 10,
                    "item": {
                      "userId": {"ref": "users[:10].id"}
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 789L, memoryOptimized);

        generation.streamJsonNodes("samples").forEach(item -> {
            int id = item.get("userId").asInt();
            assertThat(id).isBetween(1, 11);
        });
    }

    @BothImplementationsTest
    @DisplayName("Should support negative range [-10:-1] as last 10 items")
    void shouldSupportNegativeRange(boolean memoryOptimized) throws Exception {
        String dsl = """
                {
                  "users": {
                    "count": 100,
                    "item": {
                      "id": {"gen": "sequence", "start": 1}
                    }
                  },
                  "audience": {
                    "count": 20,
                    "item": {
                      "userId": {"ref": "users[-10:-1].id"}
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 101112L, memoryOptimized);

        generation.streamJsonNodes("audience").forEach(item -> {
            int id = item.get("userId").asInt();
            assertThat(id).isBetween(91, 100);
        });
    }

    @BothImplementationsTest
    @DisplayName("Should reject multiple colons in range")
    void shouldRejectMultipleColons(boolean memoryOptimized) {
        String dsl = """
                {
                  "users": {"count": 5, "item": {"id": {"gen": "sequence", "start": 1}}},
                  "orders": {"count": 5, "item": {"userId": {"ref": "users[0:5:10].id"}}}
                }
                """;

        assertThatThrownBy(() -> generateFromDslWithSeed(dsl, 1L, memoryOptimized))
                .isInstanceOf(DslValidationException.class)
                .hasMessageContaining("invalid range format")
                .hasMessageContaining("single colon");
    }

    @BothImplementationsTest
    @DisplayName("Should reject non-numeric range start")
    void shouldRejectNonNumericRangeStart(boolean memoryOptimized) {
        String dsl = """
                {
                  "users": {"count": 5, "item": {"id": {"gen": "sequence", "start": 1}}},
                  "orders": {"count": 5, "item": {"userId": {"ref": "users[abc:10].id"}}}
                }
                """;

        assertThatThrownBy(() -> generateFromDslWithSeed(dsl, 1L, memoryOptimized))
                .isInstanceOf(DslValidationException.class)
                .hasMessageContaining("invalid range start")
                .hasMessageContaining("abc");
    }

    @BothImplementationsTest
    @DisplayName("Should reject non-numeric range end")
    void shouldRejectNonNumericRangeEnd(boolean memoryOptimized) {
        String dsl = """
                {
                  "users": {"count": 5, "item": {"id": {"gen": "sequence", "start": 1}}},
                  "orders": {"count": 5, "item": {"userId": {"ref": "users[0:xyz].id"}}}
                }
                """;

        assertThatThrownBy(() -> generateFromDslWithSeed(dsl, 1L, memoryOptimized))
                .isInstanceOf(DslValidationException.class)
                .hasMessageContaining("invalid range end")
                .hasMessageContaining("xyz");
    }

    @BothImplementationsTest
    @DisplayName("Should reject invalid non-numeric index")
    void shouldRejectInvalidIndex(boolean memoryOptimized) {
        String dsl = """
                {
                  "users": {"count": 5, "item": {"id": {"gen": "sequence", "start": 1}}},
                  "orders": {"count": 5, "item": {"userId": {"ref": "users[invalid].id"}}}
                }
                """;

        assertThatThrownBy(() -> generateFromDslWithSeed(dsl, 1L, memoryOptimized))
                .isInstanceOf(DslValidationException.class)
                .hasMessageContaining("invalid index format")
                .hasMessageContaining("invalid");
    }

    @BothImplementationsTest
    @DisplayName("Should accept full range with just colon")
    void shouldAcceptFullRange(boolean memoryOptimized) throws Exception {
        String dsl = """
                {
                  "users": {
                    "count": 10,
                    "item": {
                      "id": {"gen": "sequence", "start": 1}
                    }
                  },
                  "samples": {
                    "count": 5,
                    "item": {
                      "userId": {"ref": "users[:].id"}
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 999L, memoryOptimized);

        generation.streamJsonNodes("samples").forEach(item -> {
            int id = item.get("userId").asInt();
            assertThat(id).isBetween(1, 10);
        });
    }
}
