package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.ValidationError;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.validation.DslTreeBuildResult;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for reference field validation error paths in ReferenceFieldNodeBuilder.
 * These tests ensure that all validation error messages are properly generated.
 */
class ReferenceFieldValidationTest {
    private ObjectMapper mapper;
    private DslTreeBuilder builder;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        GeneratorRegistry registry = GeneratorRegistry.withDefaultGenerators(new Faker());
        builder = new DslTreeBuilder(registry);
    }

    @Test
    void testReferenceWithNonArrayFilter() throws Exception {
        JsonNode dsl = mapper.readTree("""
            {
                "users": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"}
                    }
                },
                "orders": {
                    "count": 10,
                    "item": {
                        "userId": {
                            "ref": "users[*].id",
                            "filter": "not-an-array"
                        }
                    }
                }
            }
            """);

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
            .extracting(ValidationError::toString)
            .anyMatch(error -> error.contains("filter must be an array"));
    }

    @Test
    void testSpreadFieldWithNonArrayFields() throws Exception {
        JsonNode dsl = mapper.readTree("""
            {
                "templates": {
                    "count": 2,
                    "item": {
                        "name": {"gen": "lorem.word"},
                        "value": {"gen": "number"}
                    }
                },
                "products": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"},
                        "...": {
                            "ref": "templates[*]",
                            "fields": "not-an-array"
                        }
                    }
                }
            }
            """);

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
            .extracting(ValidationError::toString)
            .anyMatch(error -> error.contains("fields must be an array"));
    }

    @Test
    void testSpreadFieldWithEmptyFieldsArray() throws Exception {
        JsonNode dsl = mapper.readTree("""
            {
                "templates": {
                    "count": 2,
                    "item": {
                        "name": {"gen": "lorem.word"},
                        "value": {"gen": "number"}
                    }
                },
                "products": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"},
                        "...": {
                            "ref": "templates[*]",
                            "fields": []
                        }
                    }
                }
            }
            """);

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
            .extracting(ValidationError::toString)
            .anyMatch(error -> error.contains("must have at least one valid field"));
    }

    @Test
    void testSpreadFieldWithNonArrayFilter() throws Exception {
        JsonNode dsl = mapper.readTree("""
            {
                "templates": {
                    "count": 2,
                    "item": {
                        "name": {"gen": "lorem.word"},
                        "value": {"gen": "number"}
                    }
                },
                "products": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"},
                        "...": {
                            "ref": "templates[*]",
                            "filter": "not-an-array"
                        }
                    }
                }
            }
            """);

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
            .extracting(ValidationError::toString)
            .anyMatch(error -> error.contains("filter must be an array"));
    }

    @Test
    void testInvalidSelfReference() throws Exception {
        JsonNode dsl = mapper.readTree("""
            {
                "users": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"},
                        "copy": {"ref": "this."}
                    }
                }
            }
            """);

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
            .extracting(ValidationError::toString)
            .anyMatch(error -> error.contains("has invalid self-reference"));
    }

    @Test
    void testReferenceToUndeclaredCollection() throws Exception {
        JsonNode dsl = mapper.readTree("""
            {
                "orders": {
                    "count": 10,
                    "item": {
                        "userId": {"ref": "users"}
                    }
                }
            }
            """);

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
            .extracting(ValidationError::toString)
            .anyMatch(error -> error.contains("references undeclared collection or pick"));
    }

    @Test
    void testReferenceToUndeclaredPick() throws Exception {
        JsonNode dsl = mapper.readTree("""
            {
                "users": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"}
                    }
                },
                "orders": {
                    "count": 10,
                    "item": {
                        "adminId": {"ref": "admin"}
                    }
                }
            }
            """);

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
            .extracting(ValidationError::toString)
            .anyMatch(error -> error.contains("references undeclared collection or pick"));
    }

    @Test
    void testArrayReferenceToUndeclaredCollection() throws Exception {
        JsonNode dsl = mapper.readTree("""
            {
                "orders": {
                    "count": 10,
                    "item": {
                        "userId": {"ref": "users[*].id"}
                    }
                }
            }
            """);

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
            .extracting(ValidationError::toString)
            .anyMatch(error -> error.contains("references undeclared collection"));
    }

    @Test
    void testArrayReferenceWithEmptyFieldName() throws Exception {
        JsonNode dsl = mapper.readTree("""
            {
                "users": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"}
                    }
                },
                "orders": {
                    "count": 10,
                    "item": {
                        "userId": {"ref": "users[*]."}
                    }
                }
            }
            """);

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
            .extracting(ValidationError::toString)
            .anyMatch(error -> error.contains("has empty field name in array reference"));
    }

    @Test
    void testIndexedReferenceToUndeclaredCollection() throws Exception {
        JsonNode dsl = mapper.readTree("""
            {
                "orders": {
                    "count": 10,
                    "item": {
                        "userId": {"ref": "users[0].id"}
                    }
                }
            }
            """);

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
            .extracting(ValidationError::toString)
            .anyMatch(error -> error.contains("references undeclared collection"));
    }

    @Test
    void testIndexedReferenceWithInvalidIndexFormat() throws Exception {
        JsonNode dsl = mapper.readTree("""
            {
                "users": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"}
                    }
                },
                "orders": {
                    "count": 10,
                    "item": {
                        "userId": {"ref": "users[abc].id"}
                    }
                }
            }
            """);

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
            .extracting(ValidationError::toString)
            .anyMatch(error -> error.contains("has invalid index format"));
    }

    @Test
    void testDotNotationReferenceToUndeclaredBase() throws Exception {
        JsonNode dsl = mapper.readTree("""
            {
                "users": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"}
                    }
                },
                "orders": {
                    "count": 10,
                    "item": {
                        "userName": {"ref": "unknownThing.name"}
                    }
                }
            }
            """);

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
            .extracting(ValidationError::toString)
            .anyMatch(error -> error.contains("references field within collection") && 
                              error.contains("without index"));
    }
}
