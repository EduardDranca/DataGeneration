package com.github.eddranca.datagenerator;

import com.github.eddranca.datagenerator.exception.DslValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for validation of conditional reference syntax.
 * Ensures clear error messages for invalid conditional syntax.
 */
@DisplayName("Conditional Reference Validation")
class ConditionalReferenceValidationTest {

    @Test
    @DisplayName("Should reject conditional reference with missing closing bracket")
    void shouldRejectMissingClosingBracket() {
        String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "status": {"gen": "choice", "options": ["active", "inactive"]}
                    }
                  },
                  "orders": {
                    "count": 10,
                    "item": {
                      "userId": {"ref": "users[status='active'.id"}
                    }
                  }
                }
                """;

        assertThatThrownBy(() -> DslDataGenerator.create()
                .withSeed(123L)
                .fromJsonString(dsl)
                .generate())
                .isInstanceOf(DslValidationException.class)
                .hasMessageContaining("unclosed bracket");
    }

    @Test
    @DisplayName("Should reject conditional reference with empty condition")
    void shouldRejectEmptyCondition() {
        String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "status": {"gen": "choice", "options": ["active", "inactive"]}
                    }
                  },
                  "orders": {
                    "count": 10,
                    "item": {
                      "userId": {"ref": "users[].id"}
                    }
                  }
                }
                """;

        assertThatThrownBy(() -> DslDataGenerator.create()
                .withSeed(123L)
                .fromJsonString(dsl)
                .generate())
                .isInstanceOf(DslValidationException.class)
                .hasMessageContaining("empty index");
    }

    @Test
    @DisplayName("Should reject conditional reference with invalid operator")
    void shouldRejectInvalidOperator() {
        String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "status": {"gen": "choice", "options": ["active", "inactive"]}
                    }
                  },
                  "orders": {
                    "count": 10,
                    "item": {
                      "userId": {"ref": "users[status~'active'].id"}
                    }
                  }
                }
                """;

        assertThatThrownBy(() -> DslDataGenerator.create()
                .withSeed(123L)
                .fromJsonString(dsl)
                .generate())
                .isInstanceOf(DslValidationException.class)
                .hasMessageContaining("invalid index format");
    }

    @Test
    @DisplayName("Should reject conditional reference with empty field name")
    void shouldRejectEmptyFieldName() {
        String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "status": {"gen": "choice", "options": ["active", "inactive"]}
                    }
                  },
                  "orders": {
                    "count": 10,
                    "item": {
                      "userId": {"ref": "users[='active'].id"}
                    }
                  }
                }
                """;

        assertThatThrownBy(() -> DslDataGenerator.create()
                .withSeed(123L)
                .fromJsonString(dsl)
                .generate())
                .isInstanceOf(DslValidationException.class)
                .hasMessageContaining("empty field name");
    }

    @Test
    @DisplayName("Should reject conditional reference with malformed AND operator")
    void shouldRejectMalformedAndOperator() {
        String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "age": {"gen": "number", "min": 18, "max": 70},
                      "status": {"gen": "choice", "options": ["active", "inactive"]}
                    }
                  },
                  "orders": {
                    "count": 10,
                    "item": {
                      "userId": {"ref": "users[age>=21 AND status='active'].id"}
                    }
                  }
                }
                """;

        // Should fail because AND must be lowercase 'and'
        assertThatThrownBy(() -> DslDataGenerator.create()
                .withSeed(123L)
                .fromJsonString(dsl)
                .generate())
                .isInstanceOf(DslValidationException.class)
                .hasMessageContaining("logical operators must be lowercase");
    }

    @Test
    @DisplayName("Should reject conditional reference with malformed OR operator")
    void shouldRejectMalformedOrOperator() {
        String dsl = """
                {
                  "products": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "featured": {"gen": "boolean"},
                      "rating": {"gen": "float", "min": 1.0, "max": 5.0, "decimals": 1}
                    }
                  },
                  "promotions": {
                    "count": 3,
                    "item": {
                      "productId": {"ref": "products[featured=true OR rating>=4.5].id"}
                    }
                  }
                }
                """;

        // Should fail because OR must be lowercase 'or'
        assertThatThrownBy(() -> DslDataGenerator.create()
                .withSeed(123L)
                .fromJsonString(dsl)
                .generate())
                .isInstanceOf(DslValidationException.class)
                .hasMessageContaining("logical operators must be lowercase");
    }

    @Test
    @DisplayName("Should reject conditional reference to undeclared collection")
    void shouldRejectUndeclaredCollection() {
        String dsl = """
                {
                  "orders": {
                    "count": 10,
                    "item": {
                      "userId": {"ref": "users[status='active'].id"}
                    }
                  }
                }
                """;

        assertThatThrownBy(() -> DslDataGenerator.create()
                .withSeed(123L)
                .fromJsonString(dsl)
                .generate())
                .isInstanceOf(DslValidationException.class)
                .hasMessageContaining("undeclared collection")
                .hasMessageContaining("users");
    }

    @Test
    @DisplayName("Should provide clear error for numeric comparison on non-numeric field")
    void shouldProvideErrorForNonNumericComparison() {
        String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "name": {"gen": "name.fullName"}
                    }
                  },
                  "filtered": {
                    "count": 3,
                    "item": {
                      "userId": {"ref": "users[name>100].id"}
                    }
                  }
                }
                """;

        // This should fail at generation time when trying to compare string with number
        assertThatThrownBy(() -> DslDataGenerator.create()
                .withSeed(123L)
                .fromJsonString(dsl)
                .generate())
                .hasMessageContaining("numeric comparison");
    }

    @Test
    @DisplayName("Should accept valid conditional reference with all operators")
    void shouldAcceptValidConditionalReferences() throws Exception {
        String dsl = """
                {
                  "users": {
                    "count": 10,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "age": {"gen": "number", "min": 18, "max": 70},
                      "status": {"gen": "choice", "options": ["active", "inactive"]},
                      "balance": {"gen": "float", "min": 0, "max": 1000, "decimals": 2}
                    }
                  },
                  "test1": {
                    "count": 2,
                    "item": {
                      "userId": {"ref": "users[status='active'].id"}
                    }
                  },
                  "test2": {
                    "count": 2,
                    "item": {
                      "userId": {"ref": "users[age>=21].id"}
                    }
                  },
                  "test3": {
                    "count": 2,
                    "item": {
                      "userId": {"ref": "users[balance>100].id"}
                    }
                  },
                  "test4": {
                    "count": 2,
                    "item": {
                      "userId": {"ref": "users[age>=21 and status='active'].id"}
                    }
                  },
                  "test5": {
                    "count": 2,
                    "item": {
                      "userId": {"ref": "users[status='active' or balance>500].id"}
                    }
                  }
                }
                """;

        // Should not throw any exception
        Generation result = DslDataGenerator.create()
                .withSeed(123L)
                .fromJsonString(dsl)
                .generate();

        assertThat(result.getCollectionNames()).contains("users", "test1", "test2", "test3", "test4", "test5");
    }
}
