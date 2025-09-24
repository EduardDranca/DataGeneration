package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.exception.DslValidationException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagValidationTest extends ParameterizedGenerationTest {

    @BothImplementationsTest
    void testValidTagSharing(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
            {
                "users": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"},
                        "name": {"gen": "choice", "options": ["Alice", "Bob"]}
                    },
                    "tags": ["people"]
                },
                "customers": {
                    "name": "users",
                    "count": 3,
                    "item": {
                        "id": {"gen": "uuid"},
                        "email": {"gen": "choice", "options": ["alice@test.com", "bob@test.com"]}
                    },
                    "tags": ["people"]
                }
            }
            """);

        // Should succeed because both collections have the same final name "users"
        Generation generation = generateFromDsl(dslNode, memoryOptimized);

        assertThat(generation).isNotNull();
    }

    @Test
    void testInvalidTagSharing() throws IOException {
        JsonNode dslNode = mapper.readTree("""
            {
                "users": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"},
                        "name": {"gen": "choice", "options": ["Alice", "Bob"]}
                    },
                    "tags": ["people"]
                },
                "products": {
                    "count": 3,
                    "item": {
                        "id": {"gen": "uuid"},
                        "name": {"gen": "choice", "options": ["Product A", "Product B"]}
                    },
                    "tags": ["people"]
                }
            }
            """);

        // Should fail because different collections (users vs products) try to use the same tag
        assertThatThrownBy(() -> generateFromDslWithSeed(dslNode, 123L, false))
            .isInstanceOf(DslValidationException.class)
            .hasMessageContaining("Tag 'people' is already declared by collection 'users'")
            .hasMessageContaining("cannot be redeclared by collection 'products'");
    }

    @BothImplementationsTest
    void testValidTagSharingWithCustomNames(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
            {
                "user_data": {
                    "name": "people",
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"},
                        "name": {"gen": "choice", "options": ["Alice", "Bob"]}
                    },
                    "tags": ["humans"]
                },
                "customer_data": {
                    "name": "people",
                    "count": 3,
                    "item": {
                        "id": {"gen": "uuid"},
                        "email": {"gen": "choice", "options": ["alice@test.com", "bob@test.com"]}
                    },
                    "tags": ["humans"]
                }
            }
            """);

        // Should succeed because both collections have the same final name "people"
        Generation generation = generateFromDsl(dslNode, memoryOptimized);

        assertThat(generation).isNotNull();
    }

    @Test
    void testInvalidTagSharingWithCustomNames() throws IOException {
        JsonNode dslNode = mapper.readTree("""
            {
                "user_data": {
                    "name": "people",
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"},
                        "name": {"gen": "choice", "options": ["Alice", "Bob"]}
                    },
                    "tags": ["entities"]
                },
                "product_data": {
                    "name": "items",
                    "count": 3,
                    "item": {
                        "id": {"gen": "uuid"},
                        "name": {"gen": "choice", "options": ["Product A", "Product B"]}
                    },
                    "tags": ["entities"]
                }
            }
            """);

        // Should fail because different final collection names (people vs items) try to use the same tag
        assertThatThrownBy(() -> generateFromDslWithSeed(dslNode, 123L, false))
            .isInstanceOf(DslValidationException.class)
            .hasMessageContaining("Tag 'entities' is already declared by collection 'people'")
            .hasMessageContaining("cannot be redeclared by collection 'items'");
    }

    @Test
    void testMultipleTagsValidation() throws IOException {
        JsonNode dslNode = mapper.readTree("""
            {
                "users": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"},
                        "name": {"gen": "choice", "options": ["Alice", "Bob"]}
                    },
                    "tags": ["people", "active"]
                },
                "products": {
                    "count": 3,
                    "item": {
                        "id": {"gen": "uuid"},
                        "name": {"gen": "choice", "options": ["Product A", "Product B"]}
                    },
                    "tags": ["items", "active"]
                }
            }
            """);

        // Should fail because "active" tag is used by different collections
        assertThatThrownBy(() -> generateFromDslWithSeed(dslNode, 123L, false))
            .isInstanceOf(DslValidationException.class)
            .hasMessageContaining("Tag 'active' is already declared by collection 'users'")
            .hasMessageContaining("cannot be redeclared by collection 'products'");
    }

    @BothImplementationsTest
    void testSingleCollectionMultipleTags(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
            {
                "users": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"},
                        "name": {"gen": "choice", "options": ["Alice", "Bob"]},
                        "type": {"gen": "choice", "options": ["admin", "user"]}
                    },
                    "tags": ["people", "active", "verified"]
                }
            }
            """);

        // Should succeed - single collection can have multiple tags
        Generation generation = generateFromDsl(dslNode, memoryOptimized);

        assertThat(generation).isNotNull();
        assertThat(generation.getCollectionSize("users")).isEqualTo(5);
    }
}
