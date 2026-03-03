package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the expression feature at the intersections with other DSL features.
 * Focused on combinations most likely to break rather than exhaustive coverage.
 */
class ExpressionIntegrationTest extends ParameterizedGenerationTest {

    /**
     * expr + filtering on the same item.
     * The filtered ref must be resolved and written to currentItem before the expr runs.
     */
    @Nested
    class ExpressionWithFiltering extends ParameterizedGenerationTest {

        @BothImplementationsTest
        void testExprReferencesFilteredFieldOnSameItem(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {"id": {"gen": "uuid"}, "name": {"gen": "name.firstName"}},
                    "pick": {"admin": 0}
                  },
                  "orders": {
                    "count": 5,
                    "item": {
                      "userId": {"ref": "users[*].id", "filter": [{"ref": "admin.id"}]},
                      "label": {"expr": "Order for ${this.userId}"}
                    }
                  }
                }
                """;
            JsonNode result = createLegacyJsonNode(generateFromDsl(dsl, memoryOptimized));
            String adminId = result.get("users").get(0).get("id").asText();

            for (JsonNode order : result.get("orders")) {
                String userId = order.get("userId").asText();
                assertThat(userId).isNotEqualTo(adminId);
                // expr must see the already-resolved userId on currentItem
                assertThat(order.get("label").asText()).isEqualTo("Order for " + userId);
            }
        }
    }

    /**
     * expr referencing a field whose value is an array.
     * The evaluator renders non-scalar JsonNode via toString() — verify it doesn't blow up
     * and produces something deterministic.
     */
    @Nested
    class ExpressionWithArrayField extends ParameterizedGenerationTest {

        @BothImplementationsTest
        void testExprReferencingArrayFieldRendersStably(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "items": {
                    "count": 3,
                    "item": {
                      "tags": {"array": {"size": 2, "item": {"gen": "lorem.word"}}},
                      "summary": {"expr": "tags:${this.tags}"}
                    }
                  }
                }
                """;
            JsonNode result = createLegacyJsonNode(generateFromDsl(dsl, memoryOptimized));

            for (JsonNode item : result.get("items")) {
                String summary = item.get("summary").asText();
                // Must not throw and must start with the literal prefix
                assertThat(summary).startsWith("tags:");
                assertThat(summary).isNotEqualTo("tags:");
            }
        }

        @BothImplementationsTest
        void testExprInsideArrayItem(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "users": {
                    "count": 3,
                    "item": {
                      "firstName": {"gen": "name.firstName"},
                      "greetings": {
                        "array": {
                          "size": 2,
                          "item": {"expr": "Hello ${this.firstName}"}
                        }
                      }
                    }
                  }
                }
                """;
            JsonNode result = createLegacyJsonNode(generateFromDsl(dsl, memoryOptimized));

            for (JsonNode user : result.get("users")) {
                String firstName = user.get("firstName").asText();
                for (JsonNode greeting : user.get("greetings")) {
                    assertThat(greeting.asText()).isEqualTo("Hello " + firstName);
                }
            }
        }
    }

    /**
     * expr + seeding — identical seeds must produce identical expr output.
     */
    @Nested
    class ExpressionWithSeeding {

        @Test
        void testSameSeedProducesSameExprOutput() throws IOException {
            String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {
                      "firstName": {"gen": "name.firstName"},
                      "lastName": {"gen": "name.lastName"},
                      "email": {"expr": "lowercase(${this.firstName}.${this.lastName}@example.com)"}
                    }
                  }
                }
                """;
            Generation gen1 = DslDataGenerator.create().withSeed(42L).fromJsonString(dsl).generate();
            Generation gen2 = DslDataGenerator.create().withSeed(42L).fromJsonString(dsl).generate();

            JsonNode result1 = ParameterizedGenerationTest.LegacyApiHelper.asJsonNode(gen1);
            JsonNode result2 = ParameterizedGenerationTest.LegacyApiHelper.asJsonNode(gen2);

            for (int i = 0; i < 5; i++) {
                assertThat(result1.get("users").get(i).get("email").asText())
                    .isEqualTo(result2.get("users").get(i).get("email").asText());
            }
        }
    }

    /**
     * expr + spread on the same item.
     * Spread fields are merged into the item object; expr must still resolve this.* correctly.
     */
    @Nested
    class ExpressionWithSpread extends ParameterizedGenerationTest {

        @BothImplementationsTest
        void testExprCoexistsWithSpreadOnSameItem(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "users": {
                    "count": 3,
                    "item": {
                      "...": {"gen": "name"},
                      "email": {"expr": "lowercase(${this.firstName}.${this.lastName}@example.com)"}
                    }
                  }
                }
                """;
            JsonNode result = createLegacyJsonNode(generateFromDsl(dsl, memoryOptimized));

            for (JsonNode user : result.get("users")) {
                // Spread should have populated firstName/lastName
                assertThat(user.has("firstName")).isTrue();
                assertThat(user.has("lastName")).isTrue();
                String expected = (user.get("firstName").asText() + "." + user.get("lastName").asText() + "@example.com").toLowerCase();
                assertThat(user.get("email").asText()).isEqualTo(expected);
            }
        }
    }

    /**
     * expr where one this.* field is itself computed by another expr.
     * Chained expressions — the second expr must see the already-written value of the first.
     */
    @Nested
    class ExpressionChaining extends ParameterizedGenerationTest {

        @BothImplementationsTest
        void testExprReferencingAnotherExprField(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "users": {
                    "count": 3,
                    "item": {
                      "firstName": {"gen": "name.firstName"},
                      "lastName": {"gen": "name.lastName"},
                      "fullName": {"expr": "${this.firstName} ${this.lastName}"},
                      "greeting": {"expr": "Hello, ${this.fullName}!"}
                    }
                  }
                }
                """;
            JsonNode result = createLegacyJsonNode(generateFromDsl(dsl, memoryOptimized));

            for (JsonNode user : result.get("users")) {
                String firstName = user.get("firstName").asText();
                String lastName = user.get("lastName").asText();
                String fullName = user.get("fullName").asText();
                assertThat(fullName).isEqualTo(firstName + " " + lastName);
                assertThat(user.get("greeting").asText()).isEqualTo("Hello, " + fullName + "!");
            }
        }
    }

    /**
     * expr + shadow binding where the binding resolves a whole object, not just a scalar.
     * The expr accesses a nested field path through the binding.
     */
    @Nested
    class ExpressionWithShadowBindingObject extends ParameterizedGenerationTest {

        @BothImplementationsTest
        void testExprWithShadowBindingNestedField(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "uuid"},
                      "profile": {
                        "displayName": {"gen": "name.firstName"},
                        "department": {"gen": "lorem.word"}
                      }
                    }
                  },
                  "notifications": {
                    "count": 5,
                    "item": {
                      "$user": {"ref": "users[*]"},
                      "message": {"expr": "Hi ${$user.profile.displayName} from ${$user.profile.department}"}
                    }
                  }
                }
                """;
            JsonNode result = createLegacyJsonNode(generateFromDsl(dsl, memoryOptimized));

            java.util.Map<String, JsonNode> usersById = new java.util.HashMap<>();
            for (JsonNode user : result.get("users")) {
                usersById.put(user.get("id").asText(), user);
            }

            for (JsonNode notif : result.get("notifications")) {
                String msg = notif.get("message").asText();
                assertThat(msg).startsWith("Hi ");
                assertThat(msg).contains(" from ");
            }
        }
    }
}
