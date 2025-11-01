package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        List<Integer> electronicsIds = new ArrayList<>();
        result.get("products").forEach(product -> {
            if ("Electronics".equals(product.get("category").asText())) {
                electronicsIds.add(product.get("id").asInt());
            }
        });

        // Verify all reviews reference Electronics products only
        List<Integer> reviewProductIds = new ArrayList<>();
        result.get("reviews").forEach(review -> reviewProductIds.add(review.get("productId").asInt()));

        assertThat(reviewProductIds).allMatch(electronicsIds::contains);
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
        List<String> adminNames = new ArrayList<>();
        result.get("users").forEach(user -> {
            if ("admin".equals(user.get("role").asText())) {
                adminNames.add(user.get("name").asText());
            }
        });

        // Verify all audit logs reference admin names only
        List<String> logAdminNames = new ArrayList<>();
        result.get("auditLogs").forEach(log -> logAdminNames.add(log.get("adminName").asText()));

        assertThat(logAdminNames).allMatch(adminNames::contains);
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
        List<Integer> activeUserIds = new ArrayList<>();
        result.get("users").forEach(user -> {
            if (user.get("isActive").asBoolean()) {
                activeUserIds.add(user.get("id").asInt());
            }
        });

        // Verify all notifications reference active users only
        List<Integer> notificationUserIds = new ArrayList<>();
        result.get("notifications")
                .forEach(notification -> notificationUserIds.add(notification.get("userId").asInt()));

        assertThat(notificationUserIds).allMatch(activeUserIds::contains);
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
        List<Integer> nonGuestIds = new ArrayList<>();
        result.get("users").forEach(user -> {
            if (!"guest".equals(user.get("role").asText())) {
                nonGuestIds.add(user.get("id").asInt());
            }
        });

        // Verify all tasks are assigned to non-guest users
        List<Integer> taskUserIds = new ArrayList<>();
        result.get("tasks").forEach(task -> taskUserIds.add(task.get("assignedTo").asInt()));

        assertThat(taskUserIds).allMatch(nonGuestIds::contains);
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
        List<Integer> expensiveProductIds = new ArrayList<>();
        result.get("products").forEach(product -> {
            if (product.get("price").asInt() > 50) {
                expensiveProductIds.add(product.get("id").asInt());
            }
        });

        // Verify all premium orders reference expensive products
        List<Integer> orderProductIds = new ArrayList<>();
        result.get("premiumOrders").forEach(order -> orderProductIds.add(order.get("productId").asInt()));

        assertThat(orderProductIds).allMatch(expensiveProductIds::contains);
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
        List<Integer> lowStockIds = new ArrayList<>();
        result.get("items").forEach(item -> {
            if (item.get("quantity").asInt() <= 5) {
                lowStockIds.add(item.get("id").asInt());
            }
        });

        // Verify all alerts reference low stock items
        List<Integer> alertItemIds = new ArrayList<>();
        result.get("lowStockAlerts").forEach(alert -> alertItemIds.add(alert.get("itemId").asInt()));

        assertThat(alertItemIds).allMatch(lowStockIds::contains);
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
        List<Integer> eligibleIds = new ArrayList<>();
        result.get("users").forEach(user -> {
            if (user.get("age").asInt() >= 21 && "active".equals(user.get("status").asText())) {
                eligibleIds.add(user.get("id").asInt());
            }
        });

        // Verify all eligible users match criteria
        List<Integer> selectedIds = new ArrayList<>();
        result.get("eligibleUsers").forEach(user -> selectedIds.add(user.get("userId").asInt()));

        assertThat(selectedIds).allMatch(eligibleIds::contains);
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
        List<Integer> matchingIds = new ArrayList<>();
        result.get("products").forEach(product -> {
            boolean isElectronics = "electronics".equals(product.get("category").asText());
            boolean isFeatured = product.get("featured").asBoolean();
            if (isElectronics || isFeatured) {
                matchingIds.add(product.get("id").asInt());
            }
        });

        // Verify all promotions reference matching products
        List<Integer> promoProductIds = new ArrayList<>();
        result.get("promotions").forEach(promo -> promoProductIds.add(promo.get("productId").asInt()));

        assertThat(promoProductIds).allMatch(matchingIds::contains);
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
        List<Integer> eligibleIds = new ArrayList<>();
        result.get("employees").forEach(employee -> {
            int salary = employee.get("salary").asInt();
            int years = employee.get("yearsOfService").asInt();
            String dept = employee.get("department").asText();
            if (salary > 80000 && years >= 5 && "engineering".equals(dept)) {
                eligibleIds.add(employee.get("id").asInt());
            }
        });

        // TODO: don't use for each if possible, use assertj
        // Verify all bonus records reference eligible employees
        List<Integer> bonusEmployeeIds = new ArrayList<>();
        result.get("bonusEligible").forEach(user -> bonusEmployeeIds.add(user.get("employeeId").asInt()));

        assertThat(bonusEmployeeIds).allMatch(eligibleIds::contains);
    }
}
