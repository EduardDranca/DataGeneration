package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StaticValueTest {
    private final ObjectMapper mapper = new ObjectMapper();

    // TODO: Rewrite these tests to use streaming API instead of inspecting internal collections
    // These tests were validating static value generation by inspecting collection contents
    
    /*
    @Test
    void testBasicStaticValues() throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "products": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "name.firstName"},
                            "status": "active",
                            "version": 1.0,
                            "enabled": true,
                            "category": null,
                            "tags": ["electronics", "gadget"]
                        }
                    }
                }
                """);

        IGeneration generation = DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(dslNode)
            .generate();

        JsonNode collectionsNode = generation.asJsonNode();
        JsonNode products = collectionsNode.get("products");

        assertThat(products).isNotNull();
        assertThat(products.size()).isEqualTo(3);
        
        for (JsonNode product : products) {
            // Dynamic fields should be present and different
            assertThat(product.get("id")).isNotNull();
            assertThat(product.get("name")).isNotNull();

            // Static fields should be exactly as specified
            assertThat(product.get("status").asText()).isEqualTo("active");
            assertThat(product.get("version").asDouble()).isEqualTo(1.0);
            assertThat(product.get("enabled").asBoolean()).isTrue();
            assertThat(product.get("category").isNull()).isTrue();

            // Array should be preserved
            ArrayNode tags = (ArrayNode) product.get("tags");
            assertThat(tags).hasSize(2);
            List<String> tagList = new ArrayList<>();
            tags.forEach(tag -> tagList.add(tag.asText()));
            assertThat(tagList).containsExactly("electronics", "gadget");
        }
    }

    @Test
    void testComplexStaticValues() throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "orders": {
                        "count": 2,
                        "item": {
                            "id": {"gen": "uuid"},
                            "metadata": {
                                "source": "api",
                                "version": 2,
                                "features": ["tracking", "notifications"],
                                "config": {
                                    "timeout": 30,
                                    "retries": 3
                                }
                            },
                            "constants": {
                                "pi": 3.14159,
                                "enabled": true,
                                "description": null
                            }
                        }
                    }
                }
                """);

        IGeneration generation = DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(dslNode)
            .generate();

        JsonNode collectionsNode = generation.asJsonNode();
        JsonNode orders = collectionsNode.get("orders");

        assertThat(orders).isNotNull();
        assertThat(orders.size()).isEqualTo(2);
        
        for (JsonNode order : orders) {
            // Dynamic field
            assertThat(order.get("id")).isNotNull();

            // Complex nested static object
            ObjectNode metadata = (ObjectNode) order.get("metadata");
            assertThat(metadata.get("source").asText()).isEqualTo("api");
            assertThat(metadata.get("version").asInt()).isEqualTo(2);

            ArrayNode features = (ArrayNode) metadata.get("features");
            assertThat(features).hasSize(2);
            List<String> featureList = new ArrayList<>();
            features.forEach(feature -> featureList.add(feature.asText()));
            assertThat(featureList).containsExactly("tracking", "notifications");

            ObjectNode config = (ObjectNode) metadata.get("config");
            assertThat(config.get("timeout").asInt()).isEqualTo(30);
            assertThat(config.get("retries").asInt()).isEqualTo(3);

            // Another nested static object
            ObjectNode constants = (ObjectNode) order.get("constants");
            assertThat(constants.get("pi").asDouble()).isEqualTo(3.14159);
            assertThat(constants.get("enabled").asBoolean()).isTrue();
            assertThat(constants.get("description").isNull()).isTrue();
        }
    }

    @Test
    void testMixedStaticAndDynamicFields() throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "users": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "name.firstName"},
                            "role": "user",
                            "permissions": ["read", "write"],
                            "settings": {
                                "theme": "dark",
                                "notifications": true,
                                "language": "en"
                            },
                            "email": {"gen": "internet.emailAddress"}
                        }
                    }
                }
                """);

        IGeneration generation = DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(dslNode)
            .generate();

        JsonNode collectionsNode = generation.asJsonNode();
        JsonNode users = collectionsNode.get("users");

        assertThat(users).isNotNull();
        assertThat(users.size()).isEqualTo(3);
        
        for (JsonNode user : users) {
            // Dynamic fields should vary
            assertThat(user.get("id")).isNotNull();
            assertThat(user.get("name")).isNotNull();
            assertThat(user.get("email")).isNotNull();

            // Static fields should be identical across all users
            assertThat(user.get("role").asText()).isEqualTo("user");

            ArrayNode permissions = (ArrayNode) user.get("permissions");
            assertThat(permissions).hasSize(2);
            List<String> permList = new ArrayList<>();
            permissions.forEach(perm -> permList.add(perm.asText()));
            assertThat(permList).containsExactly("read", "write");

            JsonNode settings = user.get("settings");
            assertThat(settings.get("theme").asText()).isEqualTo("dark");
            assertThat(settings.get("notifications").asBoolean()).isTrue();
            assertThat(settings.get("language").asText()).isEqualTo("en");
        }
    }
    */
}
