package com.github.eddranca.datagenerator;

import com.github.eddranca.datagenerator.exception.DslValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RangeReferencesTest {

    @Test
    @DisplayName("Should support fixed inclusive range [0:99]")
    void shouldSupportFixedRange() throws Exception {
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

        Generation generation = DslDataGenerator.create()
                .withSeed(123L)
                .fromJsonString(dsl)
                .generate();

        generation.streamJsonNodes("betaFeatures").forEach(item -> {
            int id = item.get("userId").asInt();
            assertThat(id).isBetween(1, 100);
        });
    }

    @Test
    @DisplayName("Should support open end range [10:]")
    void shouldSupportOpenEndRange() throws Exception {
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

        Generation generation = DslDataGenerator.create()
                .withSeed(456L)
                .fromJsonString(dsl)
                .generate();

        generation.streamJsonNodes("orders").forEach(item -> {
            int id = item.get("userId").asInt();
            assertThat(id).isBetween(11, 20);
        });
    }

    @Test
    @DisplayName("Should support open start range [:10]")
    void shouldSupportOpenStartRange() throws Exception {
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

        Generation generation = DslDataGenerator.create()
                .withSeed(789L)
                .fromJsonString(dsl)
                .generate();

        generation.streamJsonNodes("samples").forEach(item -> {
            int id = item.get("userId").asInt();
            assertThat(id).isBetween(1, 11);
        });
    }

    @Test
    @DisplayName("Should support negative range [-10:-1] as last 10 items")
    void shouldSupportNegativeRange() throws Exception {
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

        Generation generation = DslDataGenerator.create()
                .withSeed(101112L)
                .fromJsonString(dsl)
                .generate();

        generation.streamJsonNodes("audience").forEach(item -> {
            int id = item.get("userId").asInt();
            assertThat(id).isBetween(91, 100);
        });
    }

    @Test
    @DisplayName("Should reject multiple colons in range")
    void shouldRejectMultipleColons() {
        String dsl = """
                {
                  "users": {"count": 5, "item": {"id": {"gen": "sequence", "start": 1}}},
                  "orders": {"count": 5, "item": {"userId": {"ref": "users[0:5:10].id"}}}
                }
                """;

        assertThatThrownBy(() -> DslDataGenerator.create()
                .withSeed(1L)
                .fromJsonString(dsl)
                .generate())
                .isInstanceOf(DslValidationException.class)
                .hasMessageContaining("invalid range format")
                .hasMessageContaining("single colon");
    }

    @Test
    @DisplayName("Should reject non-numeric range start")
    void shouldRejectNonNumericRangeStart() {
        String dsl = """
                {
                  "users": {"count": 5, "item": {"id": {"gen": "sequence", "start": 1}}},
                  "orders": {"count": 5, "item": {"userId": {"ref": "users[abc:10].id"}}}
                }
                """;

        assertThatThrownBy(() -> DslDataGenerator.create()
                .withSeed(1L)
                .fromJsonString(dsl)
                .generate())
                .isInstanceOf(DslValidationException.class)
                .hasMessageContaining("invalid range start")
                .hasMessageContaining("abc");
    }

    @Test
    @DisplayName("Should reject non-numeric range end")
    void shouldRejectNonNumericRangeEnd() {
        String dsl = """
                {
                  "users": {"count": 5, "item": {"id": {"gen": "sequence", "start": 1}}},
                  "orders": {"count": 5, "item": {"userId": {"ref": "users[0:xyz].id"}}}
                }
                """;

        assertThatThrownBy(() -> DslDataGenerator.create()
                .withSeed(1L)
                .fromJsonString(dsl)
                .generate())
                .isInstanceOf(DslValidationException.class)
                .hasMessageContaining("invalid range end")
                .hasMessageContaining("xyz");
    }

    @Test
    @DisplayName("Should reject invalid non-numeric index")
    void shouldRejectInvalidIndex() {
        String dsl = """
                {
                  "users": {"count": 5, "item": {"id": {"gen": "sequence", "start": 1}}},
                  "orders": {"count": 5, "item": {"userId": {"ref": "users[invalid].id"}}}
                }
                """;

        assertThatThrownBy(() -> DslDataGenerator.create()
                .withSeed(1L)
                .fromJsonString(dsl)
                .generate())
                .isInstanceOf(DslValidationException.class)
                .hasMessageContaining("invalid index format")
                .hasMessageContaining("invalid");
    }

    @Test
    @DisplayName("Should accept full range with just colon")
    void shouldAcceptFullRange() throws Exception {
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

        Generation generation = DslDataGenerator.create()
                .withSeed(999L)
                .fromJsonString(dsl)
                .generate();

        generation.streamJsonNodes("samples").forEach(item -> {
            int id = item.get("userId").asInt();
            assertThat(id).isBetween(1, 10);
        });
    }
}
