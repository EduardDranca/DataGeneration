package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for conditional reference feature.
 * Tests syntax like: products[category='Electronics'].id
 */
class ConditionalReferenceTest extends ParameterizedGenerationTest {

    @BothImplementationsTest
    void testSimpleConditionalReference(boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                  "products": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "name": {"gen": "lorem.word"},
                      "category": {"gen": "choice", "options": ["Electronics", "Books", "Clothing"]}
                    }
                  },
                  "reviews": {
                    "count": 10,
                    "item": {
                      "productId": {"ref": "products[category='Electronics'].id"}
                    }
                  }
                }
                """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        assertThat(result.has("products")).isTrue();
        assertThat(result.get("products")).hasSize(5);
        assertThat(result.has("reviews")).isTrue();
        assertThat(result.get("reviews")).hasSize(10);

        // Collect Electronics product IDs
        List<Integer> electronicsIds = StreamSupport.stream(result.get("products").spliterator(), false)
                .filter(product -> "Electronics".equals(product.get("category").asText()))
                .map(product -> product.get("id").asInt())
                .toList();

        // Verify all reviews reference Electronics products only
        assertThat(result.get("reviews"))
                .extracting(review -> review.get("productId").asInt())
                .allMatch(electronicsIds::contains);
    }

    @BothImplementationsTest
    void testConditionalReferenceWithFieldExtraction(boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                  "users": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "name": {"gen": "name.fullName"},
                      "role": {"gen": "choice", "options": ["admin", "user"]}
                    }
                  },
                  "auditLogs": {
                    "count": 10,
                    "item": {
                      "adminName": {"ref": "users[role='admin'].name"}
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 456L, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        // Collect admin names
        List<String> adminNames = StreamSupport.stream(result.get("users").spliterator(), false)
                .filter(user -> "admin".equals(user.get("role").asText()))
                .map(user -> user.get("name").asText())
                .toList();

        // Verify all audit logs reference admin names only
        assertThat(result.get("auditLogs"))
                .extracting(log -> log.get("adminName").asText())
                .allMatch(adminNames::contains);
    }

    @BothImplementationsTest
    void testConditionalReferenceWithBooleanCondition(boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                  "users": {
                    "count": 10,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "email": {"gen": "internet.emailAddress"},
                      "isActive": {"gen": "boolean", "probability": 0.7}
                    }
                  },
                  "notifications": {
                    "count": 5,
                    "item": {
                      "userId": {"ref": "users[isActive=true].id"}
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 789L, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        // Collect active user IDs
        List<Integer> activeUserIds = StreamSupport.stream(result.get("users").spliterator(), false)
                .filter(user -> user.get("isActive").asBoolean())
                .map(user -> user.get("id").asInt())
                .toList();

        // Verify all notifications reference active users only
        assertThat(result.get("notifications"))
                .extracting(notification -> notification.get("userId").asInt())
                .allMatch(activeUserIds::contains);
    }

    @BothImplementationsTest
    void testConditionalReferenceWithNotEquals(boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                  "users": {
                    "count": 10,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "name": {"gen": "name.fullName"},
                      "role": {"gen": "choice", "options": ["admin", "user", "guest"]}
                    }
                  },
                  "tasks": {
                    "count": 15,
                    "item": {
                      "assignedTo": {"ref": "users[role!='guest'].id"}
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 999L, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        // Collect all non-guest user IDs
        List<Integer> nonGuestIds = StreamSupport.stream(result.get("users").spliterator(), false)
                .filter(user -> !"guest".equals(user.get("role").asText()))
                .map(user -> user.get("id").asInt())
                .toList();

        // Verify all tasks are assigned to non-guest users
        assertThat(result.get("tasks"))
                .extracting(task -> task.get("assignedTo").asInt())
                .allMatch(nonGuestIds::contains);
    }

    @BothImplementationsTest
    void testConditionalReferenceWithGreaterThan(boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                  "products": {
                    "count": 10,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "price": {"gen": "number", "min": 10, "max": 100}
                    }
                  },
                  "premiumOrders": {
                    "count": 5,
                    "item": {
                      "productId": {"ref": "products[price>50].id"}
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 456L, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        // Collect product IDs with price > 50
        List<Integer> expensiveProductIds = StreamSupport.stream(result.get("products").spliterator(), false)
                .filter(product -> product.get("price").asInt() > 50)
                .map(product -> product.get("id").asInt())
                .toList();

        // Verify all premium orders reference expensive products
        assertThat(result.get("premiumOrders"))
                .extracting(order -> order.get("productId").asInt())
                .allMatch(expensiveProductIds::contains);
    }

    @BothImplementationsTest
    void testConditionalReferenceWithLessThanOrEqual(boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                  "items": {
                    "count": 10,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "quantity": {"gen": "number", "min": 1, "max": 20}
                    }
                  },
                  "lowStockAlerts": {
                    "count": 5,
                    "item": {
                      "itemId": {"ref": "items[quantity<=5].id"}
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 789L, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        // Collect item IDs with quantity <= 5
        List<Integer> lowStockIds = StreamSupport.stream(result.get("items").spliterator(), false)
                .filter(item -> item.get("quantity").asInt() <= 5)
                .map(item -> item.get("id").asInt())
                .toList();

        // Verify all alerts reference low stock items
        assertThat(result.get("lowStockAlerts"))
                .extracting(alert -> alert.get("itemId").asInt())
                .allMatch(lowStockIds::contains);
    }

    @BothImplementationsTest
    void testConditionalReferenceWithAndOperator(boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                  "users": {
                    "count": 10,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "age": {"gen": "number", "min": 18, "max": 70},
                      "status": {"gen": "choice", "options": ["active", "inactive"]}
                    }
                  },
                  "eligibleUsers": {
                    "count": 5,
                    "item": {
                      "userId": {"ref": "users[age>=21 and status='active'].id"}
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 111L, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        // Collect eligible user IDs (age >= 21 AND status = active)
        List<Integer> eligibleIds = StreamSupport.stream(result.get("users").spliterator(), false)
                .filter(user -> user.get("age").asInt() >= 21 && "active".equals(user.get("status").asText()))
                .map(user -> user.get("id").asInt())
                .toList();

        // Verify all eligible users match criteria
        assertThat(result.get("eligibleUsers"))
                .extracting(user -> user.get("userId").asInt())
                .allMatch(eligibleIds::contains);
    }

    @BothImplementationsTest
    void testConditionalReferenceWithOrOperator(boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                  "products": {
                    "count": 10,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "category": {"gen": "choice", "options": ["electronics", "books", "clothing"]},
                      "featured": {"gen": "boolean", "probability": 0.3}
                    }
                  },
                  "promotions": {
                    "count": 5,
                    "item": {
                      "productId": {"ref": "products[category='electronics' or featured=true].id"}
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 222L, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        // Collect product IDs that match criteria (electronics OR featured)
        List<Integer> matchingIds = StreamSupport.stream(result.get("products").spliterator(), false)
                .filter(product -> {
                    boolean isElectronics = "electronics".equals(product.get("category").asText());
                    boolean isFeatured = product.get("featured").asBoolean();
                    return isElectronics || isFeatured;
                })
                .map(product -> product.get("id").asInt())
                .toList();

        // Verify all promotions reference matching products
        assertThat(result.get("promotions"))
                .extracting(promo -> promo.get("productId").asInt())
                .allMatch(matchingIds::contains);
    }

    @BothImplementationsTest
    void testConditionalReferenceWithComplexAndCondition(boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                  "employees": {
                    "count": 10,
                    "item": {
                      "id": {"gen": "sequence", "start": 1},
                      "salary": {"gen": "number", "min": 30000, "max": 150000},
                      "department": {"gen": "choice", "options": ["engineering", "sales", "hr"]},
                      "yearsOfService": {"gen": "number", "min": 0, "max": 20}
                    }
                  },
                  "bonusEligible": {
                    "count": 3,
                    "item": {
                      "employeeId": {"ref": "employees[salary>80000 and yearsOfService>=5 and department='engineering'].id"}
                    }
                  }
                }
                """;

        Generation generation = generateFromDslWithSeed(dsl, 333L, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        // Collect eligible employee IDs
        List<Integer> eligibleIds = StreamSupport.stream(result.get("employees").spliterator(), false)
                .filter(employee -> {
                    int salary = employee.get("salary").asInt();
                    int years = employee.get("yearsOfService").asInt();
                    String dept = employee.get("department").asText();
                    return salary > 80000 && years >= 5 && "engineering".equals(dept);
                })
                .map(employee -> employee.get("id").asInt())
                .toList();

        // Verify all bonus records reference eligible employees
        assertThat(result.get("bonusEligible"))
                .extracting(user -> user.get("employeeId").asInt())
                .allMatch(eligibleIds::contains);
    }
}
