package com.github.eddranca.datagenerator.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.IGeneration;
import com.github.eddranca.datagenerator.Generation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class ArrayIntegrationTest {

    @Test
    void testComplexArrayScenario() throws Exception {
        String dsl = """
                {
                    "users": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "name.firstName"},
                            "email": {"gen": "internet.emailAddress"},
                            "skills": {
                                "array": {
                                    "minSize": 2,
                                    "maxSize": 4,
                                    "item": {"gen": "choice", "options": ["Java", "Python", "JavaScript", "Go", "Rust"]}
                                }
                            },
                            "projects": {
                                "array": {
                                    "size": 2,
                                    "item": {
                                        "name": {"gen": "company.name"},
                                        "status": {"gen": "choice", "options": ["active", "completed", "on-hold"]},
                                        "tags": {
                                            "array": {
                                                "minSize": 1,
                                                "maxSize": 3,
                                                "item": {"gen": "choice", "options": ["urgent", "backend", "frontend", "mobile", "web"]}
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                """;

        IGeneration generation = DslDataGenerator.create()
            .withSeed(12345L) // For deterministic testing
            .fromJsonString(dsl)
            .generate();

        JsonNode result = generation.asJsonNode();

        // Verify structure
        assertThat(result.has("users")).isTrue();
        JsonNode users = result.get("users");
        assertThat(users.isArray()).isTrue();

        // Verify each user has required fields and proper array structures
        assertThat(users)
            .hasSize(3)
            .allSatisfy(user -> {
                // Basic fields
                assertThat(user.has("id")).isTrue();
                assertThat(user.has("name")).isTrue();
                assertThat(user.has("email")).isTrue();
                assertThat(user.get("email").asText()).contains("@");

                // Skills array
                assertThat(user.has("skills")).isTrue();
                JsonNode skills = user.get("skills");
                assertThat(skills.isArray()).isTrue();
                assertThat(skills).hasSizeBetween(2, 4);

                // Projects array
                assertThat(user.has("projects")).isTrue();
                JsonNode projects = user.get("projects");
                assertThat(projects.isArray()).isTrue();
                assertThat(projects).hasSize(2);

                // Verify each project structure
                assertThat(projects).allSatisfy(project -> {
                    assertThat(project.has("name")).isTrue();
                    assertThat(project.has("status")).isTrue();
                    assertThat(project.has("tags")).isTrue();

                    assertThat(project.get("status").asText()).isIn("active", "completed", "on-hold");

                    // Project tags array
                    JsonNode projectTags = project.get("tags");
                    assertThat(projectTags.isArray()).isTrue();
                    assertThat(projectTags).hasSizeBetween(1, 3);
                });
            });
    }

    @Test
    void testArrayWithReferences() throws Exception {
        String dsl = """
                {
                    "categories": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "choice", "options": ["Technology", "Business", "Design"]}
                        }
                    },
                    "articles": {
                        "count": 2,
                        "item": {
                            "title": {"gen": "company.name"},
                            "categoryIds": {
                                "array": {
                                    "minSize": 1,
                                    "maxSize": 2,
                                    "item": {"ref": "categories[*].id"}
                                }
                            }
                        }
                    }
                }
                """;

        IGeneration generation = DslDataGenerator.create()
            .withSeed(54321L)
            .fromJsonString(dsl)
            .generate();

        JsonNode result = generation.asJsonNode();

        // Extract category IDs for validation
        JsonNode categories = result.get("categories");
        String[] categoryIds = StreamSupport.stream(categories.spliterator(), false)
            .map(category -> category.get("id").asText())
            .toArray(String[]::new);

        // Verify articles
        JsonNode articles = result.get("articles");

        assertThat(articles)
            .hasSize(2)
            .allSatisfy(article -> {
                assertThat(article.has("categoryIds")).isTrue();

                JsonNode categoryIdsArray = article.get("categoryIds");
                assertThat(categoryIdsArray.isArray()).isTrue();
                assertThat(categoryIdsArray).hasSizeBetween(1, 2);

                // Verify each category ID exists in the categories collection
                assertThat(categoryIdsArray).allSatisfy(categoryIdNode ->
                    assertThat(categoryIds).contains(categoryIdNode.asText())
                );
            });
    }

    @Nested
    class CountSyntaxIntegrationTest {

        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
            objectMapper = new ObjectMapper();
        }

        @Test
        void testCountSyntaxWithChoiceGenerator() throws Exception {
            String dsl = """
                    {
                      "users": {
                        "count": 2,
                        "item": {
                          "name": {"gen": "name.firstName"},
                          "tags": {
                            "gen": "choice",
                            "options": ["tech", "business", "personal"],
                            "count": 3
                          }
                        }
                      }
                    }
                    """;

            JsonNode dslNode = objectMapper.readTree(dsl);
            IGeneration generation = DslDataGenerator.create()
                .withSeed(123L)
                .fromJsonNode(dslNode)
                .generate();

            JsonNode collectionsNode = generation.asJsonNode();
            JsonNode usersArray = collectionsNode.get("users");

            assertThat(usersArray)
                .hasSize(2)
                .allSatisfy(user -> {
                    assertThat(user.has("name")).isTrue();
                    assertThat(user.has("tags")).isTrue();

                    JsonNode tags = user.get("tags");
                    assertThat(tags.isArray()).isTrue();
                    assertThat(tags).hasSize(3);

                    // Verify all tags are from the expected values
                    assertThat(tags).allSatisfy(tag ->
                        assertThat(tag.asText()).isIn("tech", "business", "personal")
                    );
                });
        }

        @Test
        void testCountSyntaxWithLiteralValues() throws Exception {
            String dsl = """
                    {
                      "messages": {
                        "count": 1,
                        "item": {
                          "id": {"gen": "uuid"},
                          "repeated_greeting": {
                            "value": "Hello World",
                            "count": 4
                          }
                        }
                      }
                    }
                    """;

            JsonNode dslNode = objectMapper.readTree(dsl);
            IGeneration generation = DslDataGenerator.create()
                .withSeed(123L)
                .fromJsonNode(dslNode)
                .generate();

            JsonNode collectionsNode = generation.asJsonNode();
            JsonNode messagesArray = collectionsNode.get("messages");

            assertThat(messagesArray).hasSize(1);

            JsonNode message = messagesArray.get(0);
            assertThat(message.has("id")).isTrue();
            assertThat(message.has("repeated_greeting")).isTrue();

            JsonNode greetings = message.get("repeated_greeting");
            assertThat(greetings.isArray()).isTrue();

            // Verify all greetings are the expected literal value
            assertThat(greetings)
                .hasSize(4)
                .allSatisfy(greeting ->
                    assertThat(greeting.asText()).isEqualTo("Hello World")
                );
        }

        @Test
        void testCountSyntaxWithComplexObjects() throws Exception {
            String dsl = """
                    {
                      "companies": {
                        "count": 1,
                        "item": {
                          "name": {"gen": "company.name"},
                          "employees": {
                            "name": {"gen": "name.firstName"},
                            "department": {
                              "gen": "choice",
                              "options": ["Engineering", "Marketing", "Sales"]
                            },
                            "count": 3
                          }
                        }
                      }
                    }
                    """;

            JsonNode dslNode = objectMapper.readTree(dsl);
            IGeneration generation = DslDataGenerator.create()
                .withSeed(123L)
                .fromJsonNode(dslNode)
                .generate();

            JsonNode collectionsNode = generation.asJsonNode();
            JsonNode companiesArray = collectionsNode.get("companies");

            assertThat(companiesArray).hasSize(1);

            JsonNode company = companiesArray.get(0);
            assertThat(company.has("name")).isTrue();
            assertThat(company.has("employees")).isTrue();

            JsonNode employees = company.get("employees");
            assertThat(employees.isArray()).isTrue();

            // Verify each employee has the expected structure
            assertThat(employees)
                .hasSize(3)
                .allSatisfy(employee -> {
                    assertThat(employee.has("name")).isTrue();
                    assertThat(employee.has("department")).isTrue();
                    assertThat(employee.get("department").asText()).isIn("Engineering", "Marketing", "Sales");
                });
        }

        @Test
        void testCountZeroGeneratesEmptyArray() throws Exception {
            String dsl = """
                    {
                      "users": {
                        "count": 1,
                        "item": {
                          "name": {"gen": "name.firstName"},
                          "empty_tags": {
                            "gen": "choice",
                            "options": ["tag1", "tag2"],
                            "count": 0
                          }
                        }
                      }
                    }
                    """;

            JsonNode dslNode = objectMapper.readTree(dsl);
            IGeneration generation = DslDataGenerator.create()
                .withSeed(123L)
                .fromJsonNode(dslNode)
                .generate();

            JsonNode collectionsNode = generation.asJsonNode();
            JsonNode usersArray = collectionsNode.get("users");

            assertThat(usersArray).hasSize(1);

            JsonNode user = usersArray.get(0);
            assertThat(user.has("name")).isTrue();
            assertThat(user.has("empty_tags")).isTrue();

            JsonNode emptyTags = user.get("empty_tags");
            assertThat(emptyTags.isArray()).isTrue();
            assertThat(emptyTags).isEmpty();
        }

        @Test
        void testCountSyntaxWithNestedObjects() throws Exception {
            String dsl = """
                    {
                      "projects": {
                        "count": 1,
                        "item": {
                          "name": {"gen": "company.name"},
                          "tasks": {
                            "title": {"gen": "company.buzzword"},
                            "assignees": {
                              "gen": "name.firstName",
                              "count": 2
                            },
                            "count": 3
                          }
                        }
                      }
                    }
                    """;

            JsonNode dslNode = objectMapper.readTree(dsl);
            IGeneration generation = DslDataGenerator.create()
                .withSeed(123L)
                .fromJsonNode(dslNode)
                .generate();

            JsonNode collectionsNode = generation.asJsonNode();
            JsonNode projectsArray = collectionsNode.get("projects");

            assertThat(projectsArray).hasSize(1);

            JsonNode project = projectsArray.get(0);
            assertThat(project.has("name")).isTrue();
            assertThat(project.has("tasks")).isTrue();

            JsonNode tasks = project.get("tasks");
            assertThat(tasks.isArray()).isTrue();

            // Verify each task has the expected structure
            assertThat(tasks)
                .hasSize(3)
                .allSatisfy(task -> {
                    assertThat(task.has("title")).isTrue();
                    assertThat(task.has("assignees")).isTrue();

                    JsonNode assignees = task.get("assignees");
                    assertThat(assignees.isArray()).isTrue();
                    assertThat(assignees).hasSize(2);

                    // Verify assignees are strings (names)
                    assertThat(assignees).allSatisfy(assignee -> {
                        assertThat(assignee.isTextual()).isTrue();
                        assertThat(assignee.asText()).isNotEmpty();
                    });
                });
        }
    }
}
