package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.builder.DslTreeBuilder;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.node.ArrayFieldNode;
import com.github.eddranca.datagenerator.node.CollectionNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.ItemNode;
import com.github.eddranca.datagenerator.node.ObjectFieldNode;
import com.github.eddranca.datagenerator.node.RootNode;
import com.github.eddranca.datagenerator.validation.DslTreeBuildResult;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArrayFieldTest extends ParameterizedGenerationTest {

    @BothImplementations
    void testFixedSizeArray(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
                "users": {
                    "count": 2,
                    "item": {
                        "tags": {
                            "array": {
                                "size": 3,
                                "item": "tag"
                            }
                        }
                    }
                }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        List<JsonNode> users = generation.streamJsonNodes("users").toList();

        assertThat(users)
            .hasSize(2)
            .allSatisfy(user -> {
                assertThat(user.has("tags")).isTrue();
                JsonNode tags = user.get("tags");
                assertThat(tags.isArray()).isTrue();
                assertThat(tags).hasSize(3);

                assertThat(tags).allSatisfy(tag ->
                    assertThat(tag.asText()).isEqualTo("tag")
                );
            });
    }

    @BothImplementations
    void testVariableSizeArray(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
                "users": {
                    "count": 1,
                    "item": {
                        "scores": {
                            "array": {
                                "minSize": 2,
                                "maxSize": 5,
                                "item": 100
                            }
                        }
                    }
                }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        List<JsonNode> users = generation.streamJsonNodes("users").toList();
        JsonNode user = users.get(0);

        assertThat(user.has("scores")).isTrue();
        JsonNode scores = user.get("scores");
        assertThat(scores.isArray()).isTrue();

        assertThat(scores)
            .hasSizeBetween(2, 5)
            .allSatisfy(score ->
                assertThat(score.asInt()).isEqualTo(100)
            );
    }

    @BothImplementations
    void testArrayWithGeneratedItems(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
                "users": {
                    "count": 1,
                    "item": {
                        "numbers": {
                            "array": {
                                "size": 3,
                                "item": {
                                    "gen": "number",
                                    "min": 1,
                                    "max": 10
                                }
                            }
                        }
                    }
                }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        List<JsonNode> users = generation.streamJsonNodes("users").toList();
        JsonNode user = users.get(0);
        JsonNode numbers = user.get("numbers");

        assertThat(numbers.isArray()).isTrue();

        assertThat(numbers)
            .hasSize(3)
            .allSatisfy(number -> {
                assertThat(number.isNumber()).isTrue();
                assertThat(number.asInt()).isBetween(1, 10);
            });
    }

    @BothImplementations
    void testArrayWithObjectItems(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
                "users": {
                    "count": 1,
                    "item": {
                        "contacts": {
                            "array": {
                                "size": 2,
                                "item": {
                                    "type": "email",
                                    "value": {
                                        "gen": "internet.emailAddress"
                                    }
                                }
                            }
                        }
                    }
                }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        List<JsonNode> users = generation.streamJsonNodes("users").toList();
        JsonNode user = users.get(0);
        JsonNode contacts = user.get("contacts");

        assertThat(contacts.isArray()).isTrue();

        assertThat(contacts)
            .hasSize(2)
            .allSatisfy(contact -> {
                assertThat(contact.isObject()).isTrue();
                assertThat(contact.has("type")).isTrue();
                assertThat(contact.has("value")).isTrue();
                assertThat(contact.get("type").asText()).isEqualTo("email");
                assertThat(contact.get("value").asText()).contains("@");
            });
    }

    @BothImplementations
    void testEmptyArray(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
                "users": {
                    "count": 1,
                    "item": {
                        "emptyList": {
                            "array": {
                                "size": 0,
                                "item": "value"
                            }
                        }
                    }
                }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        List<JsonNode> users = generation.streamJsonNodes("users").toList();
        JsonNode user = users.get(0);
        JsonNode emptyList = user.get("emptyList");

        assertThat(emptyList.isArray()).isTrue();
        assertThat(emptyList).isEmpty();
    }

    @Nested
    class CountSyntaxTest {

        private ObjectMapper objectMapper;
        private DslTreeBuilder builder;

        @BeforeEach
        void setUp() {
            objectMapper = new ObjectMapper();
            GeneratorRegistry registry = GeneratorRegistry.withDefaultGenerators(new Faker());
            builder = new DslTreeBuilder(registry);
        }

        @Test
        void testBasicCountWithGenerator() throws Exception {
            String dsl = """
                {
                  "users": {
                    "count": 1,
                    "item": {
                      "tags": {
                        "gen": "choice",
                        "options": ["tech", "business", "personal"],
                        "count": 3
                      }
                    }
                  }
                }
                """;

            JsonNode jsonNode = objectMapper.readTree(dsl);
            DslTreeBuildResult result = builder.build(jsonNode);

            assertThat(result).isNotNull();
            assertThat(result.hasErrors()).isFalse();

            RootNode root = result.getTree();
            CollectionNode usersCollection = root.getCollections().get("users");
            ItemNode item = usersCollection.getItem();
            DslNode tagsField = item.getFields().get("tags");

            assertThat(tagsField).isInstanceOf(ArrayFieldNode.class);
            ArrayFieldNode arrayNode = (ArrayFieldNode) tagsField;
            assertThat(arrayNode.getSize()).isEqualTo(3);
            assertThat(arrayNode.hasFixedSize()).isTrue();
        }

        @Test
        void testCountWithLiteralValue() throws Exception {
            String dsl = """
                {
                  "users": {
                    "count": 1,
                    "item": {
                      "repeated_message": {
                        "value": "Hello World",
                        "count": 5
                      }
                    }
                  }
                }
                """;

            JsonNode jsonNode = objectMapper.readTree(dsl);
            DslTreeBuildResult result = builder.build(jsonNode);

            assertThat(result).isNotNull();
            assertThat(result.hasErrors()).isFalse();

            RootNode root = result.getTree();
            CollectionNode usersCollection = root.getCollections().get("users");
            ItemNode item = usersCollection.getItem();
            DslNode messageField = item.getFields().get("repeated_message");

            assertThat(messageField).isInstanceOf(ArrayFieldNode.class);
            ArrayFieldNode arrayNode = (ArrayFieldNode) messageField;
            assertThat(arrayNode.getSize()).isEqualTo(5);
            assertThat(arrayNode.hasFixedSize()).isTrue();
        }

        @Test
        void testCountZero() throws Exception {
            String dsl = """
                {
                  "users": {
                    "count": 1,
                    "item": {
                      "empty_tags": {
                        "gen": "choice",
                        "options": ["a", "b", "c"],
                        "count": 0
                      }
                    }
                  }
                }
                """;

            JsonNode jsonNode = objectMapper.readTree(dsl);
            DslTreeBuildResult result = builder.build(jsonNode);

            assertThat(result).isNotNull();
            assertThat(result.hasErrors()).isFalse();

            RootNode root = result.getTree();
            CollectionNode usersCollection = root.getCollections().get("users");
            ItemNode item = usersCollection.getItem();
            DslNode tagsField = item.getFields().get("empty_tags");

            assertThat(tagsField).isInstanceOf(ArrayFieldNode.class);
            ArrayFieldNode arrayNode = (ArrayFieldNode) tagsField;
            assertThat(arrayNode.getSize()).isZero();
            assertThat(arrayNode.hasFixedSize()).isTrue();
        }

        @ParameterizedTest
        @CsvSource({
            "-1, count must be non-negative",
            "\"three\", count must be a number"
        })
        void testInvalidCountErrors(String countValue, String expectedError) throws Exception {
            String dsl = """
                {
                  "users": {
                    "count": 1,
                    "item": {
                      "invalid_tags": {
                        "gen": "choice",
                        "options": ["a", "b", "c"],
                        "count": %s
                      }
                    }
                  }
                }
                """.formatted(countValue);

            JsonNode jsonNode = objectMapper.readTree(dsl);
            DslTreeBuildResult result = builder.build(jsonNode);

            assertThat(result).isNotNull();
            assertThat(result.hasErrors()).isTrue();
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0).toString()).contains(expectedError);
        }

        @Test
        void testCountWithoutAdditionalFieldsError() throws Exception {
            String dsl = """
                {
                  "users": {
                    "count": 1,
                    "item": {
                      "invalid_field": {
                        "count": 3
                      }
                    }
                  }
                }
                """;

            JsonNode jsonNode = objectMapper.readTree(dsl);
            DslTreeBuildResult result = builder.build(jsonNode);

            assertThat(result).isNotNull();
            assertThat(result.hasErrors()).isTrue();
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0).toString()).contains("with count must have additional field definition");
        }

        @Test
        void testCountWithComplexGenerator() throws Exception {
            String dsl = """
                {
                  "companies": {
                    "count": 1,
                    "item": {
                      "employees": {
                        "name": {
                          "gen": "choice",
                          "options": ["Alice", "Bob", "Charlie"]
                        },
                        "age": {
                          "gen": "number",
                          "min": 18,
                          "max": 65
                        },
                        "count": 2
                      }
                    }
                  }
                }
                """;

            JsonNode jsonNode = objectMapper.readTree(dsl);
            DslTreeBuildResult result = builder.build(jsonNode);

            assertThat(result).isNotNull();
            assertThat(result.hasErrors()).isFalse();

            RootNode root = result.getTree();
            CollectionNode companiesCollection = root.getCollections().get("companies");
            ItemNode item = companiesCollection.getItem();
            DslNode employeesField = item.getFields().get("employees");

            assertThat(employeesField).isInstanceOf(ArrayFieldNode.class);
            ArrayFieldNode arrayNode = (ArrayFieldNode) employeesField;
            assertThat(arrayNode.getSize()).isEqualTo(2);
            assertThat(arrayNode.hasFixedSize()).isTrue();

            // The item should be an object with name and age fields
            DslNode itemNode = arrayNode.getItemNode();
            assertThat(itemNode).isInstanceOf(ObjectFieldNode.class);
        }
    }
}
