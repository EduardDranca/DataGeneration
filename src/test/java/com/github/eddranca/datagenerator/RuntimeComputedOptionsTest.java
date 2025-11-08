package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for runtime-computed generator options (Issue #28).
 * Verifies that generator options can reference other field values.
 */
class RuntimeComputedOptionsTest extends ParameterizedGenerationTest {

    @BothImplementationsTest
    void shouldSupportSimpleReferenceInOptions(boolean memoryOptimized) throws IOException {
        String dsl = """
            {
              "employees": {
                "count": 5,
                "item": {
                  "id": {"gen": "uuid"},
                  "startAge": {"gen": "number", "min": 22, "max": 35},
                  "retirementAge": {"gen": "number", "min": {"ref": "this.startAge"}, "max": 65}
                }
              }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        JsonNode employees = result.get("employees");
        assertThat(employees).isNotNull();
        assertThat(employees.isArray()).isTrue();
        assertThat(employees.size()).isEqualTo(5);

        for (JsonNode employee : employees) {
            int startAge = employee.get("startAge").asInt();
            int retirementAge = employee.get("retirementAge").asInt();

            assertThat(startAge).isBetween(22, 35);
            assertThat(retirementAge).isBetween(startAge, 65);
        }
    }

    @BothImplementationsTest
    void shouldSupportMappedReferenceInOptions(boolean memoryOptimized) throws IOException {
        String dsl = """
            {
              "products": {
                "count": 10,
                "item": {
                  "id": {"gen": "uuid"},
                  "category": {"gen": "choice", "options": ["budget", "premium", "luxury"]},
                  "price": {
                    "gen": "float",
                    "min": {"ref": "this.category", "map": {"budget": 10, "premium": 100, "luxury": 1000}},
                    "max": {"ref": "this.category", "map": {"budget": 50, "premium": 500, "luxury": 5000}},
                    "decimals": 2
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

        for (JsonNode product : products) {
            String category = product.get("category").asText();
            double price = product.get("price").asDouble();

            switch (category) {
                case "budget" -> assertThat(price).isBetween(10.0, 50.0);
                case "premium" -> assertThat(price).isBetween(100.0, 500.0);
                case "luxury" -> assertThat(price).isBetween(1000.0, 5000.0);
                default -> throw new AssertionError("Unexpected category: " + category);
            }
        }
    }

    @BothImplementationsTest
    void shouldSupportMultipleRuntimeOptionsInSameField(boolean memoryOptimized) throws IOException {
        String dsl = """
            {
              "ranges": {
                "count": 5,
                "item": {
                  "id": {"gen": "uuid"},
                  "min": {"gen": "number", "min": 1, "max": 50},
                  "max": {"gen": "number", "min": 51, "max": 100},
                  "value": {
                    "gen": "number",
                    "min": {"ref": "this.min"},
                    "max": {"ref": "this.max"}
                  }
                }
              }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        JsonNode ranges = result.get("ranges");
        assertThat(ranges).isNotNull();
        assertThat(ranges.size()).isEqualTo(5);

        for (JsonNode range : ranges) {
            int min = range.get("min").asInt();
            int max = range.get("max").asInt();
            int value = range.get("value").asInt();

            assertThat(min).isBetween(1, 50);
            assertThat(max).isBetween(51, 100);
            assertThat(value).isBetween(min, max);
        }
    }

    @BothImplementationsTest
    void shouldSupportSimpleSelfReferenceInSameObject(boolean memoryOptimized) throws IOException {
        String dsl = """
            {
              "items": {
                "count": 3,
                "item": {
                  "id": {"gen": "sequence", "start": 1},
                  "baseValue": {"gen": "number", "min": 10, "max": 20},
                  "derivedValue": {
                    "gen": "number",
                    "min": {"ref": "this.baseValue"},
                    "max": 100
                  }
                }
              }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        JsonNode items = result.get("items");
        assertThat(items).isNotNull();
        assertThat(items.size()).isEqualTo(3);

        for (JsonNode item : items) {
            int baseValue = item.get("baseValue").asInt();
            int derivedValue = item.get("derivedValue").asInt();

            assertThat(baseValue).isBetween(10, 20);
            assertThat(derivedValue).isBetween(baseValue, 100);
        }
    }

    @BothImplementationsTest
    void shouldSupportSelfReferencesAcrossNestedObjects(boolean memoryOptimized) throws IOException {
        String dsl = """
            {
              "items": {
                "count": 5,
                "item": {
                  "id": {"gen": "uuid"},
                  "baseValue": {"gen": "number", "min": 10, "max": 20},
                  "data": {
                    "multipliedValue": {
                      "gen": "number",
                      "min": {"ref": "this.baseValue"},
                      "max": 100
                    }
                  }
                }
              }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        JsonNode items = result.get("items");
        assertThat(items).isNotNull();
        assertThat(items.size()).isEqualTo(5);

        for (JsonNode item : items) {
            int baseValue = item.get("baseValue").asInt();
            int multipliedValue = item.get("data").get("multipliedValue").asInt();

            assertThat(baseValue).isBetween(10, 20);
            assertThat(multipliedValue).isBetween(baseValue, 100);
        }
    }

    @BothImplementationsTest
    void shouldThrowExceptionWhenMappingValueNotFound(boolean memoryOptimized) {
        String dsl = """
            {
              "items": {
                "count": 1,
                "item": {
                  "category": "unknown",
                  "value": {
                    "gen": "number",
                    "min": {"ref": "this.category", "map": {"budget": 10, "premium": 100}}
                  }
                }
              }
            }
            """;

        // In eager mode, exception is thrown during generation
        // In lazy mode, exception is thrown during materialization
        assertThatThrownBy(() -> {
            Generation generation = generateFromDsl(dsl, memoryOptimized);
            createLegacyJsonNode(generation);
        })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No mapping found for value 'unknown'");
    }

    @BothImplementationsTest
    void shouldSupportRuntimeOptionsWithStaticOptions(boolean memoryOptimized) throws IOException {
        String dsl = """
            {
              "items": {
                "count": 5,
                "item": {
                  "id": {"gen": "sequence", "start": 1},
                  "minValue": {"gen": "number", "min": 1, "max": 10},
                  "maxValue": {"gen": "number", "min": 50, "max": 100},
                  "value": {
                    "gen": "number",
                    "min": {"ref": "this.minValue"},
                    "max": {"ref": "this.maxValue"}
                  }
                }
              }
            }
            """;

        Generation generation = generateFromDsl(dsl, memoryOptimized);
        JsonNode result = createLegacyJsonNode(generation);

        JsonNode items = result.get("items");
        assertThat(items).isNotNull();
        assertThat(items.size()).isEqualTo(5);

        for (JsonNode item : items) {
            int minValue = item.get("minValue").asInt();
            int maxValue = item.get("maxValue").asInt();
            int value = item.get("value").asInt();

            assertThat(minValue).isBetween(1, 10);
            assertThat(maxValue).isBetween(50, 100);
            assertThat(value).isBetween(minValue, maxValue);
        }
    }
}
