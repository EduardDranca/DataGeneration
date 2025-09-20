package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MultiStepCollectionTest extends ParameterizedGenerationTest {

    @BothImplementations
    void testMultiStepCollectionGeneration(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "admin_users": {
                        "name": "users",
                        "count": 2,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 100},
                            "username": {"gen": "choice", "options": ["admin1", "admin2"]},
                            "role": {"gen": "choice", "options": ["admin"]},
                            "email": {"gen": "internet.emailAddress"}
                        }
                    },
                    "regular_users": {
                        "name": "users",
                        "count": 3,
                        "item": {
                            "id": {"gen": "number", "min": 101, "max": 200},
                            "username": {"gen": "choice", "options": ["user1", "user2", "user3"]},
                            "role": {"gen": "choice", "options": ["user"]},
                            "email": {"gen": "internet.emailAddress"}
                        }
                    },
                    "guest_users": {
                        "name": "users",
                        "count": 1,
                        "item": {
                            "id": {"gen": "number", "min": 201, "max": 300},
                            "username": {"gen": "choice", "options": ["guest1"]},
                            "role": {"gen": "choice", "options": ["guest"]},
                            "email": {"gen": "internet.emailAddress"}
                        }
                    }
                }
                """);

        IGeneration generation = generateFromDsl(dslNode, memoryOptimized);

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);

        // Should have only one collection named "users"
        assertThat(collections)
            .hasSize(1)
            .containsKey("users");

        List<JsonNode> users = collections.get("users");

        // Should have 6 total users (2 admin + 3 regular + 1 guest)
        assertThat(users).hasSize(6);

        // Verify we have the expected roles
        long adminCount = users.stream()
            .mapToLong(user -> "admin".equals(user.get("role").asText()) ? 1 : 0)
            .sum();
        long regularCount = users.stream()
            .mapToLong(user -> "user".equals(user.get("role").asText()) ? 1 : 0)
            .sum();
        long guestCount = users.stream()
            .mapToLong(user -> "guest".equals(user.get("role").asText()) ? 1 : 0)
            .sum();

        assertThat(adminCount).as("Should have 2 admin users").isEqualTo(2);
        assertThat(regularCount).as("Should have 3 regular users").isEqualTo(3);
        assertThat(guestCount).as("Should have 1 guest user").isEqualTo(1);

        assertThat(users)
            .as("All users should have required fields and correct ID ranges")
            .allSatisfy(user -> {
                assertThat(user.get("id")).isNotNull();
                assertThat(user.get("username")).isNotNull();
                assertThat(user.get("role")).isNotNull();
                assertThat(user.get("email")).isNotNull();

                // Verify ID ranges based on role
                int id = user.get("id").intValue();
                String role = user.get("role").asText();

                switch (role) {
                    case "admin":
                        assertThat(id).as("Admin user ID should be 1-100").isBetween(1, 100);
                        break;
                    case "user":
                        assertThat(id).as("Regular user ID should be 101-200").isBetween(101, 200);
                        break;
                    case "guest":
                        assertThat(id).as("Guest user ID should be 201-300").isBetween(201, 300);
                        break;
                }
            });
    }

    @BothImplementations
    void testMultiStepCollectionWithReferences(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "premium_products": {
                        "name": "products",
                        "count": 2,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 100},
                            "name": {"gen": "choice", "options": ["Premium Widget", "Deluxe Gadget"]},
                            "category": {"gen": "choice", "options": ["premium"]},
                            "price": {"gen": "float", "min": 100.0, "max": 500.0, "decimals": 2}
                        }
                    },
                    "basic_products": {
                        "name": "products",
                        "count": 3,
                        "item": {
                            "id": {"gen": "number", "min": 101, "max": 200},
                            "name": {"gen": "choice", "options": ["Basic Widget", "Simple Tool", "Standard Item"]},
                            "category": {"gen": "choice", "options": ["basic"]},
                            "price": {"gen": "float", "min": 10.0, "max": 99.99, "decimals": 2}
                        }
                    },
                    "orders": {
                        "count": 4,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 100},
                            "product_id": {"ref": "products[*].id"},
                            "product_name": {"ref": "products[*].name"},
                            "quantity": {"gen": "number", "min": 1, "max": 5}
                        }
                    }
                }
                """);

        IGeneration generation = generateFromDsl(dslNode, memoryOptimized);

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);

        // Should have products and orders collections
        assertThat(collections)
            .hasSize(2)
            .containsKeys("products", "orders");

        List<JsonNode> products = collections.get("products");
        List<JsonNode> orders = collections.get("orders");

        // Verify products collection merged correctly
        assertThat(products).as("Should have 5 total products (2 premium + 3 basic)").hasSize(5);

        // Count categories
        long premiumCount = products.stream()
            .mapToLong(product -> "premium".equals(product.get("category").asText()) ? 1 : 0)
            .sum();
        long basicCount = products.stream()
            .mapToLong(product -> "basic".equals(product.get("category").asText()) ? 1 : 0)
            .sum();

        assertThat(premiumCount).as("Should have 2 premium products").isEqualTo(2);
        assertThat(basicCount).as("Should have 3 basic products").isEqualTo(3);

        // Verify orders reference products correctly
        assertThat(orders).as("Should have 4 orders").hasSize(4);

        List<Integer> productIds = products.stream()
            .map(product -> product.get("id").intValue())
            .toList();

        assertThat(orders)
            .as("All orders should have required fields and reference existing products")
            .allSatisfy(order -> {
                assertThat(order.get("product_id")).isNotNull();
                assertThat(order.get("product_name")).isNotNull();
                assertThat(order.get("quantity")).isNotNull();

                // Verify the referenced product exists
                int productId = order.get("product_id").intValue();
                assertThat(productIds)
                    .as("Order should reference existing product ID: %d", productId)
                    .contains(productId);
            });
    }

    @BothImplementations
    void testMultiStepCollectionWithTags(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "us_locations": {
                        "name": "locations",
                        "tags": ["location", "us"],
                        "count": 2,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 100},
                            "name": {"gen": "choice", "options": ["New York", "Los Angeles"]},
                            "country": {"gen": "choice", "options": ["US"]}
                        }
                    },
                    "eu_locations": {
                        "name": "locations",
                        "tags": ["location", "eu"],
                        "count": 2,
                        "item": {
                            "id": {"gen": "number", "min": 101, "max": 200},
                            "name": {"gen": "choice", "options": ["London", "Paris"]},
                            "country": {"gen": "choice", "options": ["UK", "FR"]}
                        }
                    },
                    "events": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 100},
                            "name": {"gen": "choice", "options": ["Conference", "Workshop", "Meetup"]},
                            "location_name": {"ref": "byTag[location].name"}
                        }
                    }
                }
                """);

        IGeneration generation = generateFromDsl(dslNode, memoryOptimized);

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);

        // Should have locations and events collections
        assertThat(collections)
            .hasSize(2)
            .containsKeys("locations", "events");

        List<JsonNode> locations = collections.get("locations");
        List<JsonNode> events = collections.get("events");

        // Verify locations collection merged correctly
        assertThat(locations).as("Should have 4 total locations (2 US + 2 EU)").hasSize(4);

        // Verify events reference locations correctly
        assertThat(events).as("Should have 3 events").hasSize(3);

        List<String> locationNames = locations.stream()
            .map(location -> location.get("name").asText())
            .toList();

        assertThat(events)
            .as("All events should reference existing locations")
            .allSatisfy(event -> {
                assertThat(event.get("location_name")).isNotNull();

                // Verify the referenced location exists
                String locationName = event.get("location_name").asText();
                assertThat(locationNames)
                    .as("Event should reference existing location: %s", locationName)
                    .contains(locationName);
            });
    }

    @BothImplementations
    void testReferenceIndividualCollectionSteps(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "us_users": {
                        "name": "users",
                        "count": 2,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 100},
                            "username": {"gen": "choice", "options": ["john_us", "jane_us"]},
                            "region": {"gen": "choice", "options": ["US"]},
                            "email": {"gen": "internet.emailAddress"}
                        }
                    },
                    "eu_users": {
                        "name": "users",
                        "count": 2,
                        "item": {
                            "id": {"gen": "number", "min": 101, "max": 200},
                            "username": {"gen": "choice", "options": ["pierre_eu", "anna_eu"]},
                            "region": {"gen": "choice", "options": ["EU"]},
                            "email": {"gen": "internet.emailAddress"}
                        }
                    },
                    "us_orders": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 100},
                            "user_id": {"ref": "us_users[*].id"},
                            "user_region": {"ref": "us_users[*].region"},
                            "amount": {"gen": "float", "min": 10.0, "max": 100.0, "decimals": 2}
                        },
                        "name": "orders"
                    },
                    "eu_orders": {
                        "count": 2,
                        "item": {
                            "id": {"gen": "number", "min": 101, "max": 200},
                            "user_id": {"ref": "eu_users[*].id"},
                            "user_region": {"ref": "eu_users[*].region"},
                            "amount": {"gen": "float", "min": 50.0, "max": 200.0, "decimals": 2}
                        },
                        "name": "orders"
                    }
                }
                """);

        IGeneration generation = generateFromDsl(dslNode, memoryOptimized);

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);

        // Should have users, orders collections
        assertThat(collections)
            .hasSize(2)
            .containsKeys("users", "orders");

        List<JsonNode> users = collections.get("users");
        List<JsonNode> orders = collections.get("orders");

        // Verify users collection merged correctly
        assertThat(users).as("Should have 4 total users (2 US + 2 EU)").hasSize(4);

        // Count regions in users
        long usUserCount = users.stream()
            .filter(user -> "US".equals(user.get("region").asText()))
            .count();
        long euUserCount = users.stream()
            .filter(user -> "EU".equals(user.get("region").asText()))
            .count();
        assertThat(usUserCount).as("Should have 2 US users").isEqualTo(2);
        assertThat(euUserCount).as("Should have 2 EU users").isEqualTo(2);

        var usOrders = orders.stream()
            .filter(order -> "US".equals(order.get("user_region").asText()))
            .toList();
        var euOrders = orders.stream()
            .filter(order -> "EU".equals(order.get("user_region").asText()))
            .toList();

        assertThat(usOrders)
            .as("Should have 3 US orders")
            .hasSize(3)
            .allSatisfy(order -> {
                assertThat(order.get("user_region").asText())
                    .as("US orders should only reference US users")
                    .isEqualTo("US");

                int userId = order.get("user_id").intValue();
                assertThat(userId)
                    .as("US order should reference US user ID (1-100)")
                    .isBetween(1, 100);
            });

        assertThat(euOrders)
            .as("Should have 2 EU orders")
            .hasSize(2)
            .allSatisfy(order -> {
                assertThat(order.get("user_region").asText())
                    .as("EU orders should only reference EU users")
                    .isEqualTo("EU");

                int userId = order.get("user_id").intValue();
                assertThat(userId)
                    .as("EU order should reference EU user ID (101-200)")
                    .isBetween(101, 200);
            });
    }
}
