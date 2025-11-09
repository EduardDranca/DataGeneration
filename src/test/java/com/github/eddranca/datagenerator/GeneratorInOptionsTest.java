package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for using generators in runtime computed options.
 * Verifies that generator options can reference other field values.
 */
class GeneratorInOptionsTest extends ParameterizedGenerationTest {

    @BothImplementationsTest
    void shouldSupportGeneratorInOptions(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
              "items": {
                "count": 5,
                "item": {
                  "id": {"gen": "sequence", "start": 1},
                  "length": {"gen": "choice", "options": [10, 23, 1, 29]},
                  "value": {
                    "gen": "string",
                    "length": {"gen": "choice", "options": [5, 10, 15]}
                  }
                }
              }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        JsonNode items = result.get("items");
        assertThat(items).isNotNull();
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isEqualTo(5);

        assertThat(items).allSatisfy(item -> {
            String value = item.get("value").asText();
            int length = item.get("length").asInt();

            // The string length should be one of the choice options: 5, 10, or 15
            assertThat(value.length()).isIn(5, 10, 15);
            assertThat(length).isIn(10, 23, 1, 29);
        });
    }

    @BothImplementationsTest
    void shouldSupportGeneratorInOptionsWithReferences(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
              "items": {
                "count": 5,
                "item": {
                  "id": {"gen": "sequence", "start": 1},
                  "baseLength": {"gen": "number", "min": 5, "max": 10},
                  "value": {
                    "gen": "string",
                    "length": {"ref": "this.baseLength"}
                  }
                }
              }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        JsonNode items = result.get("items");
        assertThat(items).isNotNull();
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isEqualTo(5);

        assertThat(items).allSatisfy(item -> {
            String value = item.get("value").asText();
            int baseLength = item.get("baseLength").asInt();

            // The string length should match the baseLength
            assertThat(value).hasSize(baseLength);
            assertThat(baseLength).isBetween(5, 10);
        });
    }

    @BothImplementationsTest
    void shouldSupportComplexGeneratorInOptions(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
              "products": {
                "count": 10,
                "item": {
                  "id": {"gen": "uuid"},
                  "category": {"gen": "choice", "options": ["short", "medium", "long"]},
                  "name": {
                    "gen": "string",
                    "length": {
                      "gen": "choice",
                      "options": [5, 10, 15],
                      "weights": [0.5, 0.3, 0.2]
                    }
                  }
                }
              }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        JsonNode products = result.get("products");
        assertThat(products).isNotNull();
        assertThat(products.isArray()).isTrue();
        assertThat(products.size()).isEqualTo(10);

        assertThat(products).allSatisfy(product -> {
            String name = product.get("name").asText();
            String category = product.get("category").asText();

            // The name length should be one of the choice options
            assertThat(name.length()).isIn(5, 10, 15);
            assertThat(category).isIn("short", "medium", "long");
        });
    }
}
