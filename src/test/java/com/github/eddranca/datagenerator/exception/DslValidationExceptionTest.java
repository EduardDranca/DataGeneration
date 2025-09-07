package com.github.eddranca.datagenerator.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.ValidationError;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DslValidationExceptionTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void testValidationExceptionThrownForInvalidGenerator() throws Exception {
        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "users": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "nonexistent.generator"}
                        }
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors()).isNotEmpty().hasSize(1);

                ValidationError error = exception.getValidationErrors().get(0);
                assertThat(error.toString()).contains("Unknown generator: nonexistent");

                String message = exception.getMessage();
                assertThat(message)
                    .contains("DSL validation failed")
                    .contains("1 error(s)")
                    .contains("Unknown generator: nonexistent");
            });
    }

    @Test
    void testValidationExceptionThrownForMissingItemField() throws Exception {

        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "users": {
                        "count": 2
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors()).isNotEmpty();
                assertThat(exception.getMessage()).contains("missing required 'item' field");
            });
    }

    @Test
    void testValidationExceptionThrownForInvalidChoiceOptions() throws Exception {

        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "items": {
                        "count": 1,
                        "item": {
                            "status": {
                                "gen": "choice"
                            }
                        }
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(e -> {
                DslValidationException exception = (DslValidationException) e;
                assertThat(exception.getValidationErrors()).isNotEmpty();
                assertThat(exception.getMessage()).contains("missing required 'options' array");
            });
    }

    @Test
    void testValidationExceptionThrownForUndeclaredCollectionReference() throws Exception {

        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "users": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "name.firstName"},
                            "country": {"ref": "countries[*].name"}
                        }
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors()).isNotEmpty().hasSize(1);

                ValidationError error = exception.getValidationErrors().get(0);
                assertThat(error.toString()).contains("references undeclared collection or pick: countries");

                String message = exception.getMessage();
                assertThat(message)
                    .contains("DSL validation failed")
                    .contains("1 error(s)")
                    .contains("references undeclared collection or pick: countries");
            });
    }

    @Test
    void testValidationExceptionThrownForUndeclaredTagReference() throws Exception {

        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "users": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "name.firstName"},
                            "randomItem": {"ref": "byTag[nonexistent]"}
                        }
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors()).isNotEmpty().hasSize(1);

                ValidationError error = exception.getValidationErrors().get(0);
                assertThat(error.toString()).contains("references undeclared tag: nonexistent");

                String message = exception.getMessage();
                assertThat(message)
                    .contains("DSL validation failed")
                    .contains("1 error(s)")
                    .contains("references undeclared tag: nonexistent");
            });
    }

    @Test
    void testValidationExceptionThrownForSimpleCollectionReference() throws Exception {

        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "users": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "name.firstName"},
                            "product": {"ref": "products"}
                        }
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors()).isNotEmpty().hasSize(1);

                ValidationError error = exception.getValidationErrors().get(0);
                assertThat(error.toString()).contains("references undeclared collection or pick: products");

                String message = exception.getMessage();
                assertThat(message)
                    .contains("DSL validation failed")
                    .contains("1 error(s)")
                    .contains("references undeclared collection or pick: products");
            });
    }

    @Test
    void testValidationExceptionThrownForIndexedCollectionReference() throws Exception {

        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "users": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "name.firstName"},
                            "firstCountry": {"ref": "countries[0]"}
                        }
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors()).isNotEmpty().hasSize(1);

                ValidationError error = exception.getValidationErrors().get(0);
                assertThat(error.toString()).contains("references undeclared collection or pick: countries");

                String message = exception.getMessage();
                assertThat(message)
                    .contains("DSL validation failed")
                    .contains("1 error(s)")
                    .contains("references undeclared collection or pick: countries");
            });
    }

    @Test
    void testValidationExceptionThrownForMalformedTagReference() throws Exception {

        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "users": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "name.firstName"},
                            "randomItem": {"ref": "byTag["}
                        }
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors()).isNotEmpty().hasSize(1);

                ValidationError error = exception.getValidationErrors().get(0);
                assertThat(error.toString()).contains("malformed byTag reference");

                String message = exception.getMessage();
                assertThat(message)
                    .contains("DSL validation failed")
                    .contains("1 error(s)")
                    .contains("malformed byTag reference");
            });
    }

    @Test
    void testValidationExceptionThrownForEmptyReference() throws Exception {

        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "users": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "name.firstName"},
                            "emptyRef": {"ref": ""}
                        }
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors()).isNotEmpty().hasSize(1);

                ValidationError error = exception.getValidationErrors().get(0);
                assertThat(error.toString()).contains("has empty reference");

                String message = exception.getMessage();
                assertThat(message)
                    .contains("DSL validation failed")
                    .contains("1 error(s)")
                    .contains("has empty reference");
            });
    }

    @Test
    void testValidationExceptionThrownForInvalidFilterType() throws Exception {

        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "countries": {
                        "count": 3,
                        "item": {
                            "name": {"gen": "country.name"},
                            "code": {"gen": "country.countryCode"}
                        }
                    },
                    "users": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "name.firstName"},
                            "country": {
                                "ref": "countries[*].name",
                                "filter": "not_an_array"
                            }
                        }
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors()).isNotEmpty().hasSize(1);

                ValidationError error = exception.getValidationErrors().get(0);
                assertThat(error.toString()).contains("filter must be an array");

                String message = exception.getMessage();
                assertThat(message)
                    .contains("DSL validation failed")
                    .contains("1 error(s)")
                    .contains("filter must be an array");
            });
    }

    @Test
    void testValidationExceptionThrownForInvalidPickIndex() throws Exception {
        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "countries": {
                        "count": 3,
                        "item": {
                            "name": {"gen": "country.name"}
                        },
                        "pick": {
                            "firstCountry": 0,
                            "outOfBounds": 5
                        }
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors()).isNotEmpty().hasSize(1);

                ValidationError error = exception.getValidationErrors().get(0);
                assertThat(error.toString()).contains("index 5 is out of bounds");

                String message = exception.getMessage();
                assertThat(message)
                    .contains("DSL validation failed")
                    .contains("1 error(s)")
                    .contains("index 5 is out of bounds");
            });
    }

    @Test
    void testValidationExceptionThrownForNegativeCount() throws Exception {

        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "users": {
                        "count": -1,
                        "item": {
                            "name": {"gen": "name.firstName"}
                        }
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors()).isNotEmpty().hasSize(1);

                ValidationError error = exception.getValidationErrors().get(0);
                assertThat(error.toString()).contains("count must be non-negative");

                String message = exception.getMessage();
                assertThat(message)
                    .contains("DSL validation failed")
                    .contains("1 error(s)")
                    .contains("count must be non-negative");
            });
    }

    @Test
    void testValidationExceptionThrownForInvalidCollectionStructure() throws Exception {

        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "users": "not_an_object"
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors()).isNotEmpty().hasSize(1);

                ValidationError error = exception.getValidationErrors().get(0);
                assertThat(error.toString()).contains("must be an object");

                String message = exception.getMessage();
                assertThat(message)
                    .contains("DSL validation failed")
                    .contains("1 error(s)")
                    .contains("must be an object");
            });
    }

    @Test
    void testValidationExceptionThrownForInvalidItemStructure() throws Exception {

        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "users": {
                        "count": 2,
                        "item": "not_an_object"
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors()).isNotEmpty().hasSize(1);

                ValidationError error = exception.getValidationErrors().get(0);
                assertThat(error.toString()).contains("Item definition must be an object");

                String message = exception.getMessage();
                assertThat(message)
                    .contains("DSL validation failed")
                    .contains("1 error(s)")
                    .contains("Item definition must be an object");
            });
    }

    @Test
    void testValidReferencesDontThrowException() throws Exception {

        JsonNode validDsl = OBJECT_MAPPER.readTree("""
                {
                    "countries": {
                        "count": 3,
                        "tags": ["country"],
                        "item": {
                            "name": {"gen": "country.name"},
                            "code": {"gen": "country.countryCode"}
                        }
                    },
                    "users": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "name.firstName"},
                            "country": {"ref": "countries[*].name"},
                            "countryByTag": {"ref": "byTag[country]"},
                            "firstCountry": {"ref": "countries[0]"},
                            "selfRef": {"ref": "this.name"}
                        }
                    }
                }
                """);

        // Should not throw any exception
        assertThatCode(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(validDsl)
            .generate())
            .doesNotThrowAnyException();
    }

    @Test
    void testValidDslDoesNotThrowException() throws Exception {

        JsonNode validDsl = OBJECT_MAPPER.readTree("""
                {
                    "users": {
                        "count": 2,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "name.firstName"}
                        }
                    }
                }
                """);

        // Should not throw any exception
        assertThatCode(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(validDsl)
            .generate())
            .doesNotThrowAnyException();
    }

    @Test
    void testValidationExceptionThrownForInvalidTagsType() throws Exception {

        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "users": {
                        "count": 2,
                        "tags": "not_an_array",
                        "item": {
                            "name": {"gen": "name.firstName"}
                        }
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors()).isNotEmpty().hasSize(1);

                ValidationError error = exception.getValidationErrors().get(0);
                assertThat(error.toString()).contains("tags must be an array");

                String message = exception.getMessage();
                assertThat(message)
                    .contains("DSL validation failed")
                    .contains("1 error(s)")
                    .contains("tags must be an array");
            });
    }

    @Test
    void testValidationExceptionThrownForInvalidPickType() throws Exception {

        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "users": {
                        "count": 2,
                        "pick": "not_an_object",
                        "item": {
                            "name": {"gen": "name.firstName"}
                        }
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors()).isNotEmpty().hasSize(1);

                ValidationError error = exception.getValidationErrors().get(0);
                assertThat(error.toString()).contains("pick must be an object");

                String message = exception.getMessage();
                assertThat(message)
                    .contains("DSL validation failed")
                    .contains("1 error(s)")
                    .contains("pick must be an object");
            });
    }

    @Test
    void testValidationExceptionThrownForEmptyChoiceOptions() throws Exception {

        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "items": {
                        "count": 1,
                        "item": {
                            "status": {
                                "gen": "choice",
                                "options": []
                            }
                        }
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors()).isNotEmpty().hasSize(1);

                ValidationError error = exception.getValidationErrors().get(0);
                assertThat(error.toString()).contains("must have at least one valid option");

                String message = exception.getMessage();
                assertThat(message)
                    .contains("DSL validation failed")
                    .contains("1 error(s)")
                    .contains("must have at least one valid option");
            });
    }

    @Test
    void testValidationExceptionThrownForInvalidChoiceOptionsType() throws Exception {

        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "items": {
                        "count": 1,
                        "item": {
                            "status": {
                                "gen": "choice",
                                "options": "not_an_array"
                            }
                        }
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors()).isNotEmpty().hasSize(1);

                ValidationError error = exception.getValidationErrors().get(0);
                assertThat(error.toString()).contains("options must be an array");

                String message = exception.getMessage();
                assertThat(message)
                    .contains("DSL validation failed")
                    .contains("1 error(s)")
                    .contains("options must be an array");
            });
    }

    @Test
    void testMultipleValidationErrors() throws Exception {

        JsonNode invalidDsl = OBJECT_MAPPER.readTree("""
                {
                    "users": {
                        "count": -1,
                        "item": {
                            "name": {"gen": "nonexistent.generator"},
                            "country": {"ref": "undeclared_collection"},
                            "status": {
                                "gen": "choice"
                            }
                        }
                    }
                }
                """);

        assertThatThrownBy(() -> DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(invalidDsl)
            .generate())
            .isInstanceOf(DslValidationException.class)
            .satisfies(ex -> {
                DslValidationException exception = (DslValidationException) ex;
                assertThat(exception.getValidationErrors())
                    .isNotEmpty()
                    .hasSizeGreaterThan(1)
                    .hasSize(4);

                // Verify individual errors using AssertJ's extracting and contains
                assertThat(exception.getValidationErrors())
                    .extracting(ValidationError::toString)
                    .anyMatch(error -> error.contains("count must be non-negative"))
                    .anyMatch(error -> error.contains("Unknown generator: nonexistent"))
                    .anyMatch(error -> error.contains("references undeclared collection or pick: undeclared_collection"))
                    .anyMatch(error -> error.contains("missing required 'options' array"));

                String message = exception.getMessage();
                assertThat(message)
                    .contains("DSL validation failed")
                    .contains("4 error(s)")
                    .contains("count must be non-negative")
                    .contains("Unknown generator: nonexistent")
                    .contains("references undeclared collection or pick: undeclared_collection")
                    .contains("missing required 'options' array");
            });
    }
}
