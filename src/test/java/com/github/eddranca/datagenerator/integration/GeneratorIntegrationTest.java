package com.github.eddranca.datagenerator.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.Generation;

import static com.github.eddranca.datagenerator.ParameterizedGenerationTest.LegacyApiHelper.asJsonNode;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for various generators working together in DSL scenarios.
 * Tests the interaction between boolean, lorem, phone, and other generators.
 */
class GeneratorIntegrationTest extends com.github.eddranca.datagenerator.ParameterizedGenerationTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @BothImplementationsTest
    void testMultipleGeneratorsInDsl(boolean memoryOptimized) throws Exception {

        String dsl = """
            {
              "testData": {
                "count": 3,
                "item": {
                  "id": { "gen": "uuid" },
                  "isActive": { "gen": "boolean" },
                  "probability": { "gen": "boolean", "probability": 0.8 },
                  "description": { "gen": "lorem", "sentences": 2 },
                  "keywords": { "gen": "lorem", "words": 5 },
                  "content": { "gen": "lorem", "paragraphs": 1 },
                  "phone": { "gen": "phone" },
                  "cellPhone": { "gen": "phone", "format": "cell" },
                  "extension": { "gen": "phone", "format": "extension" }
                }
              }
            }
            """;

        JsonNode dslNode = mapper.readTree(dsl);

        Generation generation = generateFromDsl(dslNode, memoryOptimized);

        JsonNode result = asJsonNode(generation);

        // Verify structure
        assertThat(result.has("testData")).isTrue();
        JsonNode testData = result.get("testData");
        assertThat(testData.isArray()).isTrue();
        assertThat(testData.size()).isEqualTo(3);

        // Verify each item has all expected fields
        for (JsonNode item : testData) {
            // Boolean fields
            assertThat(item.has("isActive")).isTrue();
            assertThat(item.get("isActive").isBoolean()).isTrue();

            assertThat(item.has("probability")).isTrue();
            assertThat(item.get("probability").isBoolean()).isTrue();

            // Lorem fields
            assertThat(item.has("description")).isTrue();
            assertThat(item.get("description").isTextual()).isTrue();
            assertThat(item.get("description").asText()).isNotEmpty();

            assertThat(item.has("keywords")).isTrue();
            assertThat(item.get("keywords").isTextual()).isTrue();
            assertThat(item.get("keywords").asText()).isNotEmpty();

            assertThat(item.has("content")).isTrue();
            assertThat(item.get("content").isTextual()).isTrue();
            assertThat(item.get("content").asText()).isNotEmpty();

            // Phone fields
            assertThat(item.has("phone")).isTrue();
            assertThat(item.get("phone").isTextual()).isTrue();
            assertThat(item.get("phone").asText()).isNotEmpty();

            assertThat(item.has("cellPhone")).isTrue();
            assertThat(item.get("cellPhone").isTextual()).isTrue();
            assertThat(item.get("cellPhone").asText()).isNotEmpty();

            assertThat(item.has("extension")).isTrue();
            assertThat(item.get("extension").isTextual()).isTrue();
            assertThat(item.get("extension").asText()).isNotEmpty();

            // UUID field (existing generator)
            assertThat(item.has("id")).isTrue();
            assertThat(item.get("id").isTextual()).isTrue();
            assertThat(item.get("id").asText()).isNotEmpty();
        }
    }

    @BothImplementationsTest
    void testBooleanProbabilityDistribution(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
              "booleanTest": {
                "count": 100,
                "item": {
                  "alwaysTrue": { "gen": "boolean", "probability": 1.0 },
                  "alwaysFalse": { "gen": "boolean", "probability": 0.0 },
                  "mostlyTrue": { "gen": "boolean", "probability": 0.9 }
                }
              }
            }
            """;

        JsonNode dslNode = mapper.readTree(dsl);

        Generation generation = generateFromDslWithSeed(dslNode, 456L, memoryOptimized);

        JsonNode result = asJsonNode(generation);
        JsonNode testData = result.get("booleanTest");

        int mostlyTrueCount = 0;

        for (JsonNode item : testData) {
            // alwaysTrue should always be true
            assertThat(item.get("alwaysTrue").asBoolean()).isTrue();

            // alwaysFalse should always be false
            assertThat(item.get("alwaysFalse").asBoolean()).isFalse();

            // Count mostlyTrue distribution
            if (item.get("mostlyTrue").asBoolean()) {
                mostlyTrueCount++;
            }
        }

        // mostlyTrue should be around 90% true (allowing some variance)
        assertThat(mostlyTrueCount)
            .as("Expected ~90 true values, got %d", mostlyTrueCount)
            .isBetween(85, 95);
    }

    @BothImplementationsTest
    void testLoremGeneratorVariations(boolean memoryOptimized) throws Exception {
        // Test simple lorem generation (this works)
        String simpleDsl = """
            {
              "loremTest": {
                "count": 2,
                "item": {
                  "description": { "gen": "lorem", "sentences": 1 },
                  "keywords": { "gen": "lorem", "words": 3 },
                  "content": { "gen": "lorem", "paragraphs": 1 },
                  "id": { "gen": "uuid" }
                }
              }
            }
            """;

        JsonNode dslNode = mapper.readTree(simpleDsl);

        Generation generation = generateFromDslWithSeed(dslNode, 789L, memoryOptimized);

        JsonNode result = asJsonNode(generation);
        JsonNode testData = result.get("loremTest");

        for (JsonNode item : testData) {
            // Should have lorem fields with specific options
            assertThat(item.has("description")).isTrue();
            assertThat(item.get("description").isTextual()).isTrue();
            assertThat(item.get("description").asText()).isNotEmpty();

            assertThat(item.has("keywords")).isTrue();
            assertThat(item.get("keywords").isTextual()).isTrue();
            assertThat(item.get("keywords").asText()).isNotEmpty();

            assertThat(item.has("content")).isTrue();
            assertThat(item.get("content").isTextual()).isTrue();
            assertThat(item.get("content").asText()).isNotEmpty();

            // Should also have the regular id field
            assertThat(item.has("id")).isTrue();
            assertThat(item.get("id").isTextual()).isTrue();
        }
    }

    @BothImplementationsTest
    void testPhoneGeneratorFormats(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
              "contacts": {
                "count": 5,
                "item": {
                  "id": { "gen": "uuid" },
                  "name": { "gen": "name.fullName" },
                  "mainPhone": { "gen": "phone" },
                  "cellPhone": { "gen": "phone", "format": "cell" },
                  "workExtension": { "gen": "phone", "format": "extension" },
                  "isActive": { "gen": "boolean", "probability": 0.8 }
                }
              }
            }
            """;

        JsonNode dslNode = mapper.readTree(dsl);

        Generation generation = generateFromDslWithSeed(dslNode, 999L, memoryOptimized);

        JsonNode result = asJsonNode(generation);
        JsonNode contacts = result.get("contacts");

        assertThat(contacts.size()).isEqualTo(5);

        for (JsonNode contact : contacts) {
            // Basic fields
            assertThat(contact.has("id")).isTrue();
            assertThat(contact.has("name")).isTrue();
            assertThat(contact.has("isActive")).isTrue();

            // Phone fields
            assertThat(contact.has("mainPhone")).isTrue();
            assertThat(contact.get("mainPhone").isTextual()).isTrue();
            assertThat(contact.get("mainPhone").asText()).isNotEmpty();

            assertThat(contact.has("cellPhone")).isTrue();
            assertThat(contact.get("cellPhone").isTextual()).isTrue();
            assertThat(contact.get("cellPhone").asText()).isNotEmpty();

            assertThat(contact.has("workExtension")).isTrue();
            assertThat(contact.get("workExtension").isTextual()).isTrue();
            assertThat(contact.get("workExtension").asText()).isNotEmpty();
            // Extensions should be numeric
            assertThat(contact.get("workExtension").asText()).matches("\\d+");
        }
    }
}
