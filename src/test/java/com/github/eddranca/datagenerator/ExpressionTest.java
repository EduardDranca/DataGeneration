package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.exception.DslValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpressionTest extends ParameterizedGenerationTest {

    @Nested
    class BasicExpressions extends ParameterizedGenerationTest {

        @BothImplementationsTest
        void testLiteralExpression(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "items": {
                    "count": 3,
                    "item": {
                      "label": {"expr": "hello world"}
                    }
                  }
                }
                """;
            Generation gen = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = createLegacyJsonNode(gen);
            JsonNode items = result.get("items");

            for (JsonNode item : items) {
                assertThat(item.get("label").asText()).isEqualTo("hello world");
            }
        }

        @BothImplementationsTest
        void testSelfReferenceExpression(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {
                      "firstName": {"gen": "name.firstName"},
                      "lastName": {"gen": "name.lastName"},
                      "email": {"expr": "${this.firstName}.${this.lastName}@example.com"}
                    }
                  }
                }
                """;
            Generation gen = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = createLegacyJsonNode(gen);
            JsonNode users = result.get("users");

            for (JsonNode user : users) {
                String firstName = user.get("firstName").asText();
                String lastName = user.get("lastName").asText();
                String email = user.get("email").asText();
                assertThat(email).isEqualTo(firstName + "." + lastName + "@example.com");
            }
        }
    }

    @Nested
    class FunctionExpressions extends ParameterizedGenerationTest {

        @BothImplementationsTest
        void testLowercaseFunction(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "users": {
                    "count": 3,
                    "item": {
                      "firstName": {"gen": "name.firstName"},
                      "lower": {"expr": "lowercase(${this.firstName})"}
                    }
                  }
                }
                """;
            Generation gen = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = createLegacyJsonNode(gen);

            for (JsonNode user : result.get("users")) {
                String firstName = user.get("firstName").asText();
                assertThat(user.get("lower").asText()).isEqualTo(firstName.toLowerCase());
            }
        }

        @BothImplementationsTest
        void testUppercaseFunction(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "users": {
                    "count": 3,
                    "item": {
                      "name": {"gen": "name.firstName"},
                      "upper": {"expr": "uppercase(${this.name})"}
                    }
                  }
                }
                """;
            Generation gen = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = createLegacyJsonNode(gen);

            for (JsonNode user : result.get("users")) {
                String name = user.get("name").asText();
                assertThat(user.get("upper").asText()).isEqualTo(name.toUpperCase());
            }
        }

        @BothImplementationsTest
        void testTrimFunction(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "items": {
                    "count": 3,
                    "item": {
                      "padded": {"expr": "trim(  hello  )"}
                    }
                  }
                }
                """;
            Generation gen = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = createLegacyJsonNode(gen);

            for (JsonNode item : result.get("items")) {
                assertThat(item.get("padded").asText()).isEqualTo("hello");
            }
        }

        @BothImplementationsTest
        void testSubstringFunction(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "items": {
                    "count": 3,
                    "item": {
                      "id": {"gen": "uuid"},
                      "shortId": {"expr": "substring(${this.id}, 0, 8)"}
                    }
                  }
                }
                """;
            Generation gen = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = createLegacyJsonNode(gen);

            for (JsonNode item : result.get("items")) {
                String id = item.get("id").asText();
                assertThat(item.get("shortId").asText()).isEqualTo(id.substring(0, 8));
            }
        }

        @BothImplementationsTest
        void testNestedFunctions(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "users": {
                    "count": 3,
                    "item": {
                      "firstName": {"gen": "name.firstName"},
                      "lastName": {"gen": "name.lastName"},
                      "email": {"expr": "lowercase(${this.firstName}.${this.lastName}@example.com)"}
                    }
                  }
                }
                """;
            Generation gen = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = createLegacyJsonNode(gen);

            for (JsonNode user : result.get("users")) {
                String firstName = user.get("firstName").asText();
                String lastName = user.get("lastName").asText();
                String expected = (firstName + "." + lastName + "@example.com").toLowerCase();
                assertThat(user.get("email").asText()).isEqualTo(expected);
            }
        }
    }

    @Nested
    class CrossCollectionExpressions extends ParameterizedGenerationTest {

        @BothImplementationsTest
        void testPickReferenceInExpression(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "uuid"},
                      "name": {"gen": "name.firstName"}
                    },
                    "pick": {
                      "admin": 0
                    }
                  },
                  "logs": {
                    "count": 3,
                    "item": {
                      "message": {"expr": "Action by ${admin.name}"}
                    }
                  }
                }
                """;
            Generation gen = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = createLegacyJsonNode(gen);
            String adminName = result.get("users").get(0).get("name").asText();

            for (JsonNode log : result.get("logs")) {
                assertThat(log.get("message").asText()).isEqualTo("Action by " + adminName);
            }
        }
    }

    @Nested
    class ShadowBindingExpressions extends ParameterizedGenerationTest {

        @BothImplementationsTest
        void testShadowBindingInExpression(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "uuid"},
                      "firstName": {"gen": "name.firstName"},
                      "lastName": {"gen": "name.lastName"}
                    }
                  },
                  "emails": {
                    "count": 5,
                    "item": {
                      "$user": {"ref": "users[*]"},
                      "userId": {"ref": "$user.id"},
                      "email": {"expr": "lowercase(${$user.firstName}.${$user.lastName}@company.com)"}
                    }
                  }
                }
                """;
            Generation gen = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = createLegacyJsonNode(gen);

            for (JsonNode emailEntry : result.get("emails")) {
                String email = emailEntry.get("email").asText();
                assertThat(email).contains("@company.com");
                assertThat(email).isEqualTo(email.toLowerCase());
            }
        }
    }

    @Nested
    class CustomFunctionExpressions extends ParameterizedGenerationTest {

        @BothImplementationsTest
        void testCustomExpressionFunction(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "articles": {
                    "count": 3,
                    "item": {
                      "title": {"gen": "lorem.sentence"},
                      "slug": {"expr": "slug(${this.title})"}
                    }
                  }
                }
                """;
            DslDataGenerator.Builder builder = DslDataGenerator.create()
                .withSeed(123L)
                .withExpressionFunction("slug", (value, args) ->
                    value.toLowerCase().replaceAll("\\s+", "-").replaceAll("[^a-z0-9-]", ""));
            if (memoryOptimized) {
                builder = builder.withMemoryOptimization();
            }
            Generation gen = builder.fromJsonString(dsl).generate();
            JsonNode result = createLegacyJsonNode(gen);

            for (JsonNode article : result.get("articles")) {
                String slug = article.get("slug").asText();
                assertThat(slug).doesNotContain(" ");
                assertThat(slug).isEqualTo(slug.toLowerCase());
            }
        }
    }

    @Nested
    class ValidationTests {

        @Test
        void testExprMustBeString() {
            String dsl = """
                {
                  "items": {
                    "count": 1,
                    "item": {
                      "bad": {"expr": 123}
                    }
                  }
                }
                """;
            assertThatThrownBy(() -> DslDataGenerator.create().withSeed(1L)
                .fromJsonString(dsl).generate())
                .isInstanceOf(DslValidationException.class);
        }

        @Test
        void testUnknownFunctionFails() {
            String dsl = """
                {
                  "items": {
                    "count": 1,
                    "item": {
                      "bad": {"expr": "nonexistent(hello)"}
                    }
                  }
                }
                """;
            assertThatThrownBy(() -> DslDataGenerator.create().withSeed(1L)
                .fromJsonString(dsl).generate())
                .isInstanceOf(DslValidationException.class);
        }

        @Test
        void testConflictingKeywords() {
            String dsl = """
                {
                  "items": {
                    "count": 1,
                    "item": {
                      "bad": {"expr": "hello", "gen": "uuid"}
                    }
                  }
                }
                """;
            assertThatThrownBy(() -> DslDataGenerator.create().withSeed(1L)
                .fromJsonString(dsl).generate())
                .isInstanceOf(DslValidationException.class);
        }
    }

    @Nested
    class NullHandling extends ParameterizedGenerationTest {

        @BothImplementationsTest
        void testNullValueRendersAsNullString(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "items": {
                    "count": 3,
                    "item": {
                      "value": null,
                      "label": {"expr": "prefix-${this.value}-suffix"}
                    }
                  }
                }
                """;
            Generation gen = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = createLegacyJsonNode(gen);

            for (JsonNode item : result.get("items")) {
                assertThat(item.get("label").asText()).isEqualTo("prefix-null-suffix");
            }
        }
    }

    @Nested
    class CollectionReferenceExpressions extends ParameterizedGenerationTest {

        @BothImplementationsTest
        void testArrayFieldReferenceInExpression(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "uuid"},
                      "name": {"gen": "name.firstName"}
                    }
                  },
                  "greetings": {
                    "count": 10,
                    "item": {
                      "message": {"expr": "Hello ${users[*].name}!"}
                    }
                  }
                }
                """;
            Generation gen = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = createLegacyJsonNode(gen);

            // Collect all user names for validation
            java.util.Set<String> userNames = new java.util.HashSet<>();
            for (JsonNode user : result.get("users")) {
                userNames.add(user.get("name").asText());
            }

            for (JsonNode greeting : result.get("greetings")) {
                String message = greeting.get("message").asText();
                assertThat(message).startsWith("Hello ");
                assertThat(message).endsWith("!");
                String name = message.substring(6, message.length() - 1);
                assertThat(userNames).contains(name);
            }
        }

        @BothImplementationsTest
        void testIndexedReferenceInExpression(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "uuid"},
                      "name": {"gen": "name.firstName"}
                    }
                  },
                  "logs": {
                    "count": 3,
                    "item": {
                      "message": {"expr": "First user: ${users[0].name}"}
                    }
                  }
                }
                """;
            Generation gen = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = createLegacyJsonNode(gen);
            String firstName = result.get("users").get(0).get("name").asText();

            for (JsonNode log : result.get("logs")) {
                assertThat(log.get("message").asText()).isEqualTo("First user: " + firstName);
            }
        }

        @BothImplementationsTest
        void testNestedPathInExpression(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                  "users": {
                    "count": 3,
                    "item": {
                      "id": {"gen": "uuid"},
                      "profile": {
                        "displayName": {"gen": "name.firstName"}
                      }
                    }
                  },
                  "messages": {
                    "count": 3,
                    "item": {
                      "text": {"expr": "From ${users[0].profile.displayName}"}
                    }
                  }
                }
                """;
            Generation gen = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = createLegacyJsonNode(gen);
            String displayName = result.get("users").get(0).get("profile").get("displayName").asText();

            for (JsonNode msg : result.get("messages")) {
                assertThat(msg.get("text").asText()).isEqualTo("From " + displayName);
            }
        }
    }
}
