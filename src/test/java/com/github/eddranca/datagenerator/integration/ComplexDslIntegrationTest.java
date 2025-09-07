package com.github.eddranca.datagenerator.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.Generation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

/**
 * Integration tests for complex DSL scenarios including picks, filtering,
 * multiple collections, and advanced reference patterns.
 */
class ComplexDslIntegrationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void testPickWithReferencesAndFiltering() throws Exception {
        JsonNode dsl = mapper.readTree("""
                {
                    "countries": {
                        "count": 5,
                        "tags": ["location"],
                        "item": {
                            "name": {"gen": "country.name"},
                            "code": {"gen": "country.countryCode"},
                            "continent": {"gen": "choice", "options": ["Europe", "Asia", "America", "Africa"]},
                            "population": {"gen": "number", "min": 1000000, "max": 100000000}
                        },
                        "pick": {
                            "firstCountry": 0,
                            "secondCountry": 1,
                            "lastCountry": 4
                        }
                    },
                    "cities": {
                        "count": 10,
                        "item": {
                            "name": {"gen": "address.city"},
                            "countryName": {"ref": "countries[*].name", "filter": [{"ref": "firstCountry.name"}]},
                            "countryCode": {"ref": "countries[*].code"},
                            "population": {"gen": "number", "min": 50000, "max": 10000000}
                        }
                    },
                    "companies": {
                        "count": 15,
                        "item": {
                            "name": {"gen": "company.name"},
                            "headquarters": {"ref": "cities[*].name"},
                            "country": {"ref": "byTag[location]"},
                            "foundedYear": {"gen": "number", "min": 1900, "max": 2023},
                            "employees": {"gen": "number", "min": 10, "max": 50000}
                        }
                    },
                    "users": {
                        "count": 25,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "name.fullName"},
                            "email": {"gen": "internet.emailAddress"},
                            "company": {"ref": "companies[*].name"},
                            "homeCountry": {"ref": "lastCountry.name"},
                            "age": {"gen": "number", "min": 18, "max": 65}
                        }
                    }
                }
                """);

        Generation generation = DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(dsl)
            .generate();

        Map<String, List<JsonNode>> collections = generation.getCollections();

        // Verify all collections exist
        assertThat(collections).containsKeys("countries", "cities", "companies", "users");

        // Verify collection sizes
        assertThat(collections.get("countries")).hasSize(5);
        assertThat(collections.get("cities")).hasSize(10);
        assertThat(collections.get("companies")).hasSize(15);
        assertThat(collections.get("users")).hasSize(25);

        // Verify pick collections contain correct items
        String firstCountryName = collections.get("countries").get(0).get("name").asText();

        // Verify filtering works - cities should not have the first country's name
        List<JsonNode> cities = collections.get("cities");
        assertThat(cities)
            .allSatisfy(city ->
                assertThat(city.get("countryName").asText()).isNotEqualTo(firstCountryName)
            );

        // Verify users reference the last country
        List<JsonNode> users = collections.get("users");
        String lastCountryName = collections.get("countries").get(4).get("name").asText();
        assertThat(users)
            .allSatisfy(user ->
                assertThat(user.get("homeCountry").asText()).isEqualTo(lastCountryName)
            );
    }

    @Test
    void testLargeScaleDataGeneration() throws Exception {
        JsonNode dsl = mapper.readTree("""
                {
                    "regions": {
                        "count": 5,
                        "tags": ["geo"],
                        "item": {
                            "name": {"gen": "choice", "options": ["North", "South", "East", "West", "Central"]},
                            "code": {"gen": "string", "length": 3}
                        }
                    },
                    "countries": {
                        "count": 25,
                        "item": {
                            "name": {"gen": "country.name"},
                            "code": {"gen": "country.countryCode"},
                            "region": {"ref": "regions[*].name"}
                        }
                    },
                    "states": {
                        "count": 100,
                        "item": {
                            "name": {"gen": "address.state"},
                            "country": {"ref": "countries[*].name"},
                            "population": {"gen": "number", "min": 100000, "max": 50000000}
                        }
                    },
                    "cities": {
                        "count": 500,
                        "item": {
                            "name": {"gen": "address.city"},
                            "state": {"ref": "states[*].name"},
                            "country": {"ref": "countries[*].name"},
                            "population": {"gen": "number", "min": 10000, "max": 10000000}
                        }
                    },
                    "companies": {
                        "count": 200,
                        "item": {
                            "name": {"gen": "company.name"},
                            "headquarters": {"ref": "cities[*].name"},
                            "industry": {"gen": "choice", "options": ["Tech", "Finance", "Healthcare", "Manufacturing", "Retail"]},
                            "employees": {"gen": "number", "min": 50, "max": 100000}
                        }
                    },
                    "users": {
                        "count": 1000,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "name.fullName"},
                            "email": {"gen": "internet.emailAddress"},
                            "company": {"ref": "companies[*].name"},
                            "city": {"ref": "cities[*].name"},
                            "age": {"gen": "number", "min": 18, "max": 70}
                        }
                    }
                }
                """);

        Generation generation = DslDataGenerator.create()
            .withSeed(789L)
            .fromJsonNode(dsl)
            .generate();

        Map<String, List<JsonNode>> collections = generation.getCollections();

        // Verify all collections have correct sizes
        assertThat(collections.get("regions")).hasSize(5);
        assertThat(collections.get("countries")).hasSize(25);
        assertThat(collections.get("states")).hasSize(100);
        assertThat(collections.get("cities")).hasSize(500);
        assertThat(collections.get("companies")).hasSize(200);
        assertThat(collections.get("users")).hasSize(1000);

        // Verify data integrity - all references should be resolved
        List<JsonNode> users = collections.get("users");
        assertThat(users)
            .allSatisfy(user -> {
                assertThat(user.get("company")).isNotNull();
                assertThat(user.get("city")).isNotNull();
                assertThat(user.get("name")).isNotNull();
                assertThat(user.get("email")).isNotNull();
            });

        // Verify JSON serialization works for large datasets
        String json = generation.asJson();
        assertThat(json)
            .isNotNull()
            .hasSizeGreaterThan(100000); // Should be substantial JSON
    }

    @Test
    void testComplexFilteringWithMultipleLevels() throws Exception {
        JsonNode dsl = mapper.readTree("""
                {
                    "categories": {
                        "count": 4,
                        "item": {
                            "name": {"gen": "choice", "options": ["Electronics", "Books", "Clothing", "Home"]},
                            "priority": {"gen": "choice", "options": ["high", "medium", "low"]}
                        },
                        "pick": {
                            "highPriorityCategory": 0,
                            "lowPriorityCategory": 3
                        }
                    },
                    "products": {
                        "count": 20,
                        "item": {
                            "name": {"gen": "string", "length": 12},
                            "category": {"ref": "categories[*].name", "filter": [{"ref": "highPriorityCategory.name"}]},
                            "price": {"gen": "number", "min": 10, "max": 1000},
                            "inStock": {"gen": "choice", "options": [true, false]}
                        }
                    },
                    "orders": {
                        "count": 50,
                        "item": {
                            "id": {"gen": "uuid"},
                            "product": {"ref": "products[*].name"},
                            "quantity": {"gen": "number", "min": 1, "max": 10},
                            "customerEmail": {"gen": "internet.emailAddress"},
                            "status": {"gen": "choice", "options": ["pending", "shipped", "delivered", "cancelled"]}
                        }
                    },
                    "reviews": {
                        "count": 30,
                        "item": {
                            "orderId": {"ref": "orders[*].id"},
                            "rating": {"gen": "number", "min": 1, "max": 5},
                            "comment": {"gen": "string", "length": 50},
                            "verified": {"gen": "choice", "options": [true, false]},
                            "productCategory": {"ref": "lowPriorityCategory.name"}
                        }
                    }
                }
                """);

        Generation generation = DslDataGenerator.create()
            .withSeed(999L)
            .fromJsonNode(dsl)
            .generate();

        Map<String, List<JsonNode>> collections = generation.getCollections();

        // Verify filtering works correctly
        String highPriorityCategoryName = collections.get("categories").get(0).get("name").asText();
        String lowPriorityCategoryName = collections.get("categories").get(3).get("name").asText();

        // Products should not have the high priority category (filtered out)
        List<JsonNode> products = collections.get("products");
        assertThat(products)
            .allSatisfy(product ->
                assertThat(product.get("category").asText()).isNotEqualTo(highPriorityCategoryName)
            );

        // Reviews should all reference the low priority category
        List<JsonNode> reviews = collections.get("reviews");
        assertThat(reviews)
            .allSatisfy(review ->
                assertThat(review.get("productCategory").asText()).isEqualTo(lowPriorityCategoryName)
            );

        // Verify all orders have valid UUIDs and references
        List<JsonNode> orders = collections.get("orders");
        assertThat(orders)
            .allSatisfy(order -> {
                assertThat(order.get("id"))
                    .isNotNull()
                    .asString()
                    .hasSizeGreaterThan(30); // UUID should be substantial length
                assertThat(order.get("product")).isNotNull();
                assertThat(order.get("customerEmail")).isNotNull();
            });
    }

    @Test
    void testDynamicTagReferencesWithThisField() throws Exception {
        JsonNode dsl = mapper.readTree("""
                {
                    "users": {
                        "count": 10,
                        "item": {
                            "name": {"gen": "name.firstName"},
                            "role": {"gen": "choice", "options": ["admin", "user", "guest"]},
                            "department": {"gen": "choice", "options": ["engineering", "marketing", "sales"]}
                        }
                    },
                    "admin_resources": {
                        "count": 5,
                        "tags": ["admin"],
                        "item": {
                            "name": {"gen": "string", "length": 10},
                            "type": {"gen": "choice", "options": ["server", "database", "application"]}
                        }
                    },
                    "user_resources": {
                        "count": 8,
                        "tags": ["user"],
                        "item": {
                            "name": {"gen": "string", "length": 8},
                            "type": {"gen": "choice", "options": ["document", "report", "dashboard"]}
                        }
                    },
                    "guest_resources": {
                        "count": 3,
                        "tags": ["guest"],
                        "item": {
                            "name": {"gen": "string", "length": 6},
                            "type": {"gen": "choice", "options": ["faq", "help", "tutorial"]}
                        }
                    },
                    "permissions": {
                        "count": 15,
                        "item": {
                            "userId": {"ref": "users[*].name"},
                            "userRole": {"ref": "users[*].role"},
                            "allowedResource": {"ref": "byTag[this.userRole]"},
                            "accessLevel": {"gen": "choice", "options": ["read", "write", "execute"]}
                        }
                    }
                }
                """);

        Generation generation = DslDataGenerator.create()
            .withSeed(111L)
            .fromJsonNode(dsl)
            .generate();

        Map<String, List<JsonNode>> collections = generation.getCollections();

        // Verify all collections exist and have correct sizes
        assertThat(collections.get("users")).hasSize(10);
        assertThat(collections.get("admin_resources")).hasSize(5);
        assertThat(collections.get("user_resources")).hasSize(8);
        assertThat(collections.get("guest_resources")).hasSize(3);
        assertThat(collections.get("permissions")).hasSize(15);

        // Verify dynamic tag references work correctly
        List<JsonNode> permissions = collections.get("permissions");
        assertThat(permissions)
            .allSatisfy(permission -> {
                String userRole = permission.get("userRole").asText();
                assertThat(userRole)
                    .as("User role should be valid")
                    .isIn("admin", "user", "guest");

                // The allowedResource should come from the collection tagged with the user's role
                assertThat(permission.get("allowedResource")).isNotNull();
            });
    }

    @Test
    void testSeedConsistencyWithComplexStructures() throws Exception {
        JsonNode dsl = mapper.readTree("""
                {
                    "base_data": {
                        "count": 5,
                        "tags": ["base"],
                        "item": {
                            "id": {"gen": "uuid"},
                            "value": {"gen": "number", "min": 1, "max": 100}
                        },
                        "pick": {
                            "first": 0,
                            "last": 4
                        }
                    },
                    "derived_data": {
                        "count": 10,
                        "item": {
                            "baseRef": {"ref": "base_data[*].id"},
                            "filteredRef": {"ref": "base_data[*].value", "filter": [{"ref": "first.value"}]},
                            "tagRef": {"ref": "byTag[base]"},
                            "computed": {
                                "nested": {"gen": "string", "length": 8},
                                "choice": {"gen": "choice", "options": ["a", "b", "c"]}
                            }
                        }
                    }
                }
                """);

        // Generate with same seed twice
        Generation generation1 = DslDataGenerator.create()
            .withSeed(222L)
            .fromJsonNode(dsl)
            .generate();

        Generation generation2 = DslDataGenerator.create()
            .withSeed(222L)
            .fromJsonNode(dsl)
            .generate();

        // Results should be identical
        assertThat(generation1.asJson()).isEqualTo(generation2.asJson());

        // Verify structure is correct
        Map<String, List<JsonNode>> collections1 = generation1.getCollections();
        assertThat(collections1.get("base_data")).hasSize(5);
        assertThat(collections1.get("derived_data")).hasSize(10);
    }

    @Test
    void testCsvGeneratorUsersIntegration() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String csvPath = "src/test/resources/test-users.csv";

        JsonNode dslNode = mapper.readTree(String.format("""
                {
                    "users_from_csv": {
                        "count": 3,
                        "item": {
                            "...user": {
                                "gen": "csv",
                                "file": "%s",
                                "sequential": true
                            }
                        }
                    }
                }
                """, csvPath));

        Generation generation = DslDataGenerator.create()
            .withSeed(42L)
            .fromJsonNode(dslNode)
            .generate();

        Map<String, List<JsonNode>> collections = generation.getCollections();
        List<JsonNode> users = collections.get("users_from_csv");

        assertThat(users)
            .isNotNull()
            .hasSize(3);

        assertThat(users)
            .extracting(
                u -> u.path("id").asText(),
                u -> u.path("name").asText(),
                u -> u.path("email").asText()
            )
            .containsExactly(
                tuple("1", "John Doe", "john.doe@example.com"),
                tuple("2", "Jane Doe", "jane.doe@example.com"),
                tuple("1", "John Doe", "john.doe@example.com")
            );
    }

    @Test
    void testCsvGeneratorPickFieldIntegration() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String csvPath = "src/test/resources/test-users.csv";

        // TODO: This should work with just "item": { "gen": "csv", ... } but currently requires the nested 'user' object
        JsonNode dslNode = mapper.readTree(String.format("""
                {
                    "users_picked": {
                        "count": 3,
                        "item": {
                            "user": {
                                "gen": "csv",
                                "file": "%s",
                                "sequential": true
                            }
                        }
                    }
                }
                """, csvPath));

        Generation generation = DslDataGenerator.create()
            .withSeed(42L)
            .fromJsonNode(dslNode)
            .generate();

        Map<String, List<JsonNode>> collections = generation.getCollections();
        List<JsonNode> users = collections.get("users_picked");

        assertThat(users)
            .isNotNull()
            .hasSize(3);

        // Validate values are picked from the nested 'source' object and that sequential wraps
        JsonNode user0 = users.get(0).get("user");
        assertThat(user0.get("id").asText()).isEqualTo("1");
        assertThat(user0.get("name").asText()).isEqualTo("John Doe");
        assertThat(user0.get("email").asText()).isEqualTo("john.doe@example.com");

        JsonNode user1 = users.get(1).get("user");
        assertThat(user1.get("id").asText()).isEqualTo("2");
        assertThat(user1.get("name").asText()).isEqualTo("Jane Doe");
        assertThat(user1.get("email").asText()).isEqualTo("jane.doe@example.com");

        // third should wrap to first
        JsonNode user2 = users.get(2).get("user");
        assertThat(user2.get("id").asText()).isEqualTo("1");
        assertThat(user2.get("name").asText()).isEqualTo("John Doe");
        assertThat(user2.get("email").asText()).isEqualTo("john.doe@example.com");
    }
}
