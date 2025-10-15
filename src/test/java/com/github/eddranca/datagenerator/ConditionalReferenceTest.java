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
}
