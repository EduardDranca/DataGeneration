package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to verify that pick references maintain referential integrity across multiple collections.
 * When the same pick is referenced in different collections, the referenced field should have
 * the same value in all of them.
 */
class PickReferentialIntegrityTest extends ParameterizedGenerationTest {

    @BothImplementationsTest
    void testPickFieldReferencedInMultipleCollectionsHasSameValue(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
                "users": {
                    "count": 10,
                    "item": {
                        "id": {"gen": "sequence", "start": 1},
                        "name": {"gen": "name.fullName"},
                        "email": {"gen": "internet.emailAddress"}
                    },
                    "pick": {
                        "admin": 0
                    }
                },
                "orders": {
                    "count": 5,
                    "item": {
                        "orderId": {"gen": "sequence", "start": 100},
                        "adminEmail": {"ref": "admin.email"}
                    }
                },
                "auditLogs": {
                    "count": 3,
                    "item": {
                        "logId": {"gen": "sequence", "start": 1000},
                        "adminEmail": {"ref": "admin.email"}
                    }
                }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        // Get the admin's email from the users collection
        String adminEmail = result.get("users").get(0).get("email").asText();

        // Verify all orders reference the same admin email
        JsonNode orders = result.get("orders");
        for (JsonNode order : orders) {
            String orderAdminEmail = order.get("adminEmail").asText();
            assertThat(orderAdminEmail)
                .as("Order %s should reference admin's email", order.get("orderId"))
                .isEqualTo(adminEmail);
        }

        // Verify all audit logs reference the same admin email
        JsonNode auditLogs = result.get("auditLogs");
        for (JsonNode log : auditLogs) {
            String logAdminEmail = log.get("adminEmail").asText();
            assertThat(logAdminEmail)
                .as("Audit log %s should reference admin's email", log.get("logId"))
                .isEqualTo(adminEmail);
        }
    }

    @BothImplementationsTest
    void testPickWithNestedFieldReferencedInMultipleCollections(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
                "users": {
                    "count": 10,
                    "item": {
                        "id": {"gen": "sequence", "start": 1},
                        "profile": {
                            "name": {"gen": "name.fullName"},
                            "contact": {
                                "email": {"gen": "internet.emailAddress"},
                                "phone": {"gen": "phone.phoneNumber"}
                            }
                        }
                    },
                    "pick": {
                        "primaryUser": 0
                    }
                },
                "notifications": {
                    "count": 5,
                    "item": {
                        "notificationId": {"gen": "sequence", "start": 1},
                        "userEmail": {"ref": "primaryUser.profile.contact.email"}
                    }
                },
                "messages": {
                    "count": 3,
                    "item": {
                        "messageId": {"gen": "sequence", "start": 100},
                        "recipientEmail": {"ref": "primaryUser.profile.contact.email"}
                    }
                }
            }
            """;

        Generation generation = generateFromDslWithSeed(dsl, 54321L, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        // Get the primary user's email from the users collection
        String primaryUserEmail = result.get("users").get(0)
            .get("profile").get("contact").get("email").asText();

        // Verify all notifications reference the same email
        JsonNode notifications = result.get("notifications");
        for (JsonNode notification : notifications) {
            String notificationEmail = notification.get("userEmail").asText();
            assertThat(notificationEmail)
                .as("Notification %s should reference primary user's email", 
                    notification.get("notificationId"))
                .isEqualTo(primaryUserEmail);
        }

        // Verify all messages reference the same email
        JsonNode messages = result.get("messages");
        for (JsonNode message : messages) {
            String messageEmail = message.get("recipientEmail").asText();
            assertThat(messageEmail)
                .as("Message %s should reference primary user's email", 
                    message.get("messageId"))
                .isEqualTo(primaryUserEmail);
        }
    }

    @BothImplementationsTest
    void testMultiplePicksReferencedInMultipleCollections(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
                "users": {
                    "count": 10,
                    "item": {
                        "id": {"gen": "sequence", "start": 1},
                        "name": {"gen": "name.fullName"},
                        "role": {"gen": "choice", "options": ["admin", "user"]}
                    },
                    "pick": {
                        "admin": 0,
                        "regularUser": 5
                    }
                },
                "adminActions": {
                    "count": 3,
                    "item": {
                        "actionId": {"gen": "sequence", "start": 1},
                        "adminId": {"ref": "admin.id"},
                        "userId": {"ref": "regularUser.id"}
                    }
                },
                "userActions": {
                    "count": 2,
                    "item": {
                        "actionId": {"gen": "sequence", "start": 100},
                        "adminId": {"ref": "admin.id"},
                        "userId": {"ref": "regularUser.id"}
                    }
                }
            }
            """;

        Generation generation = generateFromDslWithSeed(dsl, 99999L, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        // Get the admin and regular user IDs
        int adminId = result.get("users").get(0).get("id").asInt();
        int regularUserId = result.get("users").get(5).get("id").asInt();

        // Verify all admin actions reference the same IDs
        JsonNode adminActions = result.get("adminActions");
        for (JsonNode action : adminActions) {
            assertThat(action.get("adminId").asInt())
                .as("Admin action should reference admin ID")
                .isEqualTo(adminId);
            assertThat(action.get("userId").asInt())
                .as("Admin action should reference regular user ID")
                .isEqualTo(regularUserId);
        }

        // Verify all user actions reference the same IDs
        JsonNode userActions = result.get("userActions");
        for (JsonNode action : userActions) {
            assertThat(action.get("adminId").asInt())
                .as("User action should reference admin ID")
                .isEqualTo(adminId);
            assertThat(action.get("userId").asInt())
                .as("User action should reference regular user ID")
                .isEqualTo(regularUserId);
        }
    }
}
