package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.github.eddranca.datagenerator.ParameterizedGenerationTest.LegacyApiHelper.asJsonNode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for shadow bindings feature.
 * Shadow bindings allow cross-entity constraints without polluting output.
 */
@DisplayName("Shadow Bindings")
class ShadowBindingsTest extends ParameterizedGenerationTest {

    @Nested
    @DisplayName("Basic Shadow Binding")
    class BasicShadowBinding {

        @BothImplementationsTest
        @DisplayName("shadow binding extracts field from referenced item")
        void shadowBindingExtractsField(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                    "users": {
                        "count": 5,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "name.fullName"}
                        }
                    },
                    "orders": {
                        "count": 10,
                        "item": {
                            "$user": {"ref": "users[*]"},
                            "userId": {"ref": "$user.id"},
                            "userName": {"ref": "$user.name"}
                        }
                    }
                }
                """;

            Generation generation = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = asJsonNode(generation);

            JsonNode orders = result.get("orders");
            assertThat(orders).hasSize(10);

            // Verify shadow binding is NOT in output
            for (JsonNode order : orders) {
                assertThat(order.has("$user")).isFalse();
                assertThat(order.has("userId")).isTrue();
                assertThat(order.has("userName")).isTrue();
            }

            // Verify userId and userName come from the same user
            JsonNode users = result.get("users");
            for (JsonNode order : orders) {
                String userId = order.get("userId").asText();
                String userName = order.get("userName").asText();

                // Find the user with this ID
                boolean found = false;
                for (JsonNode user : users) {
                    if (user.get("id").asText().equals(userId)) {
                        assertThat(user.get("name").asText()).isEqualTo(userName);
                        found = true;
                        break;
                    }
                }
                assertThat(found).isTrue();
            }
        }

        @BothImplementationsTest
        @DisplayName("shadow binding not included in output")
        void shadowBindingNotInOutput(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                    "items": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "sequence"}
                        }
                    },
                    "refs": {
                        "count": 5,
                        "item": {
                            "$item": {"ref": "items[*]"},
                            "itemId": {"ref": "$item.id"}
                        }
                    }
                }
                """;

            Generation generation = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = asJsonNode(generation);

            for (JsonNode ref : result.get("refs")) {
                assertThat(ref.fieldNames()).toIterable().containsExactly("itemId");
            }
        }
    }

    @Nested
    @DisplayName("Shadow Binding in Conditions")
    class ShadowBindingInConditions {

        @BothImplementationsTest
        @DisplayName("shadow binding used in conditional reference")
        void shadowBindingInConditionalReference(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                    "regions": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "sequence"},
                            "name": {"gen": "choice", "options": ["North", "South", "East"]}
                        }
                    },
                    "users": {
                        "count": 10,
                        "item": {
                            "id": {"gen": "uuid"},
                            "regionId": {"ref": "regions[*].id"}
                        }
                    },
                    "products": {
                        "count": 20,
                        "item": {
                            "id": {"gen": "uuid"},
                            "regionId": {"ref": "regions[*].id"}
                        }
                    },
                    "orders": {
                        "count": 30,
                        "item": {
                            "$user": {"ref": "users[*]"},
                            "userId": {"ref": "$user.id"},
                            "productId": {"ref": "products[regionId=$user.regionId].id"}
                        }
                    }
                }
                """;

            Generation generation = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = asJsonNode(generation);

            JsonNode orders = result.get("orders");
            JsonNode users = result.get("users");
            JsonNode products = result.get("products");

            assertThat(orders).hasSize(30);

            // Verify each order's product is from the same region as the user
            for (JsonNode order : orders) {
                String userId = order.get("userId").asText();
                String productId = order.get("productId").asText();

                // Find user's region
                Integer userRegionId = null;
                for (JsonNode user : users) {
                    if (user.get("id").asText().equals(userId)) {
                        userRegionId = user.get("regionId").asInt();
                        break;
                    }
                }
                assertThat(userRegionId).isNotNull();

                // Find product's region
                Integer productRegionId = null;
                for (JsonNode product : products) {
                    if (product.get("id").asText().equals(productId)) {
                        productRegionId = product.get("regionId").asInt();
                        break;
                    }
                }
                assertThat(productRegionId).isNotNull();

                // Verify regions match
                assertThat(productRegionId).isEqualTo(userRegionId);
            }
        }

        @BothImplementationsTest
        @DisplayName("self-exclusion using shadow binding")
        void selfExclusionUsingShadowBinding(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                    "users": {
                        "count": 5,
                        "item": {
                            "id": {"gen": "sequence", "start": 1}
                        }
                    },
                    "friendships": {
                        "count": 20,
                        "item": {
                            "$person": {"ref": "users[*]"},
                            "userId": {"ref": "$person.id"},
                            "friendId": {"ref": "users[id!=$person.id].id"}
                        }
                    }
                }
                """;

            Generation generation = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = asJsonNode(generation);

            JsonNode friendships = result.get("friendships");
            assertThat(friendships).hasSize(20);

            // Verify no self-friendships
            for (JsonNode friendship : friendships) {
                int userId = friendship.get("userId").asInt();
                int friendId = friendship.get("friendId").asInt();
                assertThat(userId).isNotEqualTo(friendId);
            }
        }
    }

    @Nested
    @DisplayName("Multiple Shadow Bindings")
    class MultipleShadowBindings {

        @BothImplementationsTest
        @DisplayName("multiple shadow bindings in same item")
        void multipleShadowBindingsInSameItem(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                    "categories": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "sequence", "start": 1},
                            "name": {"gen": "choice", "options": ["Electronics", "Books", "Clothing"]}
                        }
                    },
                    "warehouses": {
                        "count": 4,
                        "item": {
                            "id": {"gen": "sequence", "start": 100},
                            "categoryId": {"ref": "categories[*].id"}
                        }
                    },
                    "shipments": {
                        "count": 10,
                        "item": {
                            "$category": {"ref": "categories[*]"},
                            "$warehouse": {"ref": "warehouses[categoryId=$category.id]"},
                            "categoryId": {"ref": "$category.id"},
                            "warehouseId": {"ref": "$warehouse.id"}
                        }
                    }
                }
                """;

            Generation generation = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = asJsonNode(generation);

            JsonNode shipments = result.get("shipments");
            JsonNode warehouses = result.get("warehouses");

            assertThat(shipments).hasSize(10);

            // Verify each shipment's warehouse handles the shipment's category
            for (JsonNode shipment : shipments) {
                int categoryId = shipment.get("categoryId").asInt();
                int warehouseId = shipment.get("warehouseId").asInt();

                // Find warehouse and verify it handles this category
                for (JsonNode warehouse : warehouses) {
                    if (warehouse.get("id").asInt() == warehouseId) {
                        assertThat(warehouse.get("categoryId").asInt()).isEqualTo(categoryId);
                        break;
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("error when shadow binding not defined before use")
        void errorWhenShadowBindingNotDefined() {
            String dsl = """
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
                            "userId": {"ref": "$user.id"},
                            "$user": {"ref": "users[*]"}
                        }
                    }
                }
                """;

            assertThatThrownBy(() -> generateFromDsl(dsl, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("$user")
                .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("error when shadow binding reference has no field path")
        void errorWhenShadowBindingHasNoFieldPath() {
            String dsl = """
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
                            "$user": {"ref": "users[*]"},
                            "userId": {"ref": "$user"}
                        }
                    }
                }
                """;

            assertThatThrownBy(() -> generateFromDsl(dsl, false))
                .hasMessageContaining("field path");
        }
    }

    @Nested
    @DisplayName("Nested Field Access")
    class NestedFieldAccess {

        @BothImplementationsTest
        @DisplayName("shadow binding can access nested fields")
        void shadowBindingCanAccessNestedFields(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                    "users": {
                        "count": 5,
                        "item": {
                            "id": {"gen": "uuid"},
                            "profile": {
                                "email": {"gen": "internet.emailAddress"},
                                "settings": {
                                    "theme": {"gen": "choice", "options": ["dark", "light"]}
                                }
                            }
                        }
                    },
                    "notifications": {
                        "count": 10,
                        "item": {
                            "$user": {"ref": "users[*]"},
                            "userId": {"ref": "$user.id"},
                            "email": {"ref": "$user.profile.email"},
                            "theme": {"ref": "$user.profile.settings.theme"}
                        }
                    }
                }
                """;

            Generation generation = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = asJsonNode(generation);

            JsonNode notifications = result.get("notifications");
            JsonNode users = result.get("users");

            assertThat(notifications).hasSize(10);

            // Verify nested field extraction works correctly
            for (JsonNode notification : notifications) {
                String userId = notification.get("userId").asText();
                String email = notification.get("email").asText();
                String theme = notification.get("theme").asText();

                // Find the user and verify fields match
                for (JsonNode user : users) {
                    if (user.get("id").asText().equals(userId)) {
                        assertThat(user.get("profile").get("email").asText()).isEqualTo(email);
                        assertThat(user.get("profile").get("settings").get("theme").asText()).isEqualTo(theme);
                        break;
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Validation Errors")
    class ValidationErrors {

        @Test
        @DisplayName("error when shadow binding definition is not a ref")
        void errorWhenShadowBindingDefinitionIsNotRef() {
            String dsl = """
                {
                    "items": {
                        "count": 5,
                        "item": {
                            "$binding": {"gen": "uuid"}
                        }
                    }
                }
                """;

            assertThatThrownBy(() -> generateFromDsl(dsl, false))
                .hasMessageContaining("shadow binding")
                .hasMessageContaining("ref");
        }

        @Test
        @DisplayName("error when shadow binding field reference has empty binding name")
        void errorWhenShadowBindingFieldReferenceHasEmptyBindingName() {
            String dsl = """
                {
                    "items": {
                        "count": 5,
                        "item": {
                            "id": {"gen": "uuid"}
                        }
                    },
                    "refs": {
                        "count": 5,
                        "item": {
                            "value": {"ref": "$.id"}
                        }
                    }
                }
                """;

            assertThatThrownBy(() -> generateFromDsl(dsl, false))
                .hasMessageContaining("name cannot be empty");
        }

        @Test
        @DisplayName("error when shadow binding field reference has empty field path")
        void errorWhenShadowBindingFieldReferenceHasEmptyFieldPath() {
            String dsl = """
                {
                    "items": {
                        "count": 5,
                        "item": {
                            "id": {"gen": "uuid"}
                        }
                    },
                    "refs": {
                        "count": 5,
                        "item": {
                            "value": {"ref": "$binding."}
                        }
                    }
                }
                """;

            assertThatThrownBy(() -> generateFromDsl(dsl, false))
                .hasMessageContaining("field path cannot be empty");
        }
    }

    @Nested
    @DisplayName("Logical Conditions with Shadow Bindings")
    class LogicalConditionsWithShadowBindings {

        @BothImplementationsTest
        @DisplayName("shadow binding with AND condition")
        void shadowBindingWithAndCondition(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                    "categories": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "sequence", "start": 1},
                            "name": {"gen": "choice", "options": ["A", "B", "C"]}
                        }
                    },
                    "users": {
                        "count": 10,
                        "item": {
                            "id": {"gen": "uuid"},
                            "categoryId": {"ref": "categories[*].id"},
                            "tier": {"gen": "choice", "options": ["gold", "silver", "bronze"]}
                        }
                    },
                    "products": {
                        "count": 30,
                        "item": {
                            "id": {"gen": "uuid"},
                            "categoryId": {"ref": "categories[*].id"},
                            "tier": {"gen": "choice", "options": ["gold", "silver", "bronze"]}
                        }
                    },
                    "matches": {
                        "count": 20,
                        "item": {
                            "$user": {"ref": "users[*]"},
                            "userId": {"ref": "$user.id"},
                            "productId": {"ref": "products[categoryId=$user.categoryId and tier=$user.tier].id"}
                        }
                    }
                }
                """;

            // Use seed for reproducible results - ensures products match user criteria
            Generation generation = generateFromDslWithSeed(dsl, 42L, memoryOptimized);
            JsonNode result = asJsonNode(generation);

            JsonNode matches = result.get("matches");
            assertThat(matches).hasSize(20);

            // Verify all matches have both category and tier matching
            JsonNode users = result.get("users");
            JsonNode products = result.get("products");

            for (JsonNode match : matches) {
                String userId = match.get("userId").asText();
                String productId = match.get("productId").asText();

                // Find user
                JsonNode user = findById(users, userId);
                assertThat(user).isNotNull();

                // Find product
                JsonNode product = findById(products, productId);
                assertThat(product).isNotNull();

                // Verify both category and tier match
                assertThat(product.get("categoryId").asInt()).isEqualTo(user.get("categoryId").asInt());
                assertThat(product.get("tier").asText()).isEqualTo(user.get("tier").asText());
            }
        }

        @BothImplementationsTest
        @DisplayName("shadow binding with OR condition")
        void shadowBindingWithOrCondition(boolean memoryOptimized) throws IOException {
            String dsl = """
                {
                    "users": {
                        "count": 10,
                        "item": {
                            "id": {"gen": "sequence", "start": 1},
                            "role": {"gen": "choice", "options": ["admin", "user", "guest"]}
                        }
                    },
                    "resources": {
                        "count": 20,
                        "item": {
                            "id": {"gen": "uuid"},
                            "ownerId": {"ref": "users[*].id"},
                            "isPublic": {"gen": "boolean", "probability": 0.3}
                        }
                    },
                    "accesses": {
                        "count": 30,
                        "item": {
                            "$user": {"ref": "users[*]"},
                            "userId": {"ref": "$user.id"},
                            "resourceId": {"ref": "resources[ownerId=$user.id or isPublic=true].id"}
                        }
                    }
                }
                """;

            Generation generation = generateFromDsl(dsl, memoryOptimized);
            JsonNode result = asJsonNode(generation);

            JsonNode accesses = result.get("accesses");
            assertThat(accesses).hasSize(30);

            // Verify all accesses are either owned by user or public
            JsonNode resources = result.get("resources");

            for (JsonNode access : accesses) {
                int userId = access.get("userId").asInt();
                String resourceId = access.get("resourceId").asText();

                JsonNode resource = findById(resources, resourceId);
                assertThat(resource).isNotNull();

                int ownerId = resource.get("ownerId").asInt();
                boolean isPublic = resource.get("isPublic").asBoolean();

                // Either owned by user OR public
                assertThat(ownerId == userId || isPublic).isTrue();
            }
        }
    }

    private JsonNode findById(JsonNode collection, String id) {
        for (JsonNode item : collection) {
            if (item.get("id").asText().equals(id)) {
                return item;
            }
        }
        return null;
    }
}
