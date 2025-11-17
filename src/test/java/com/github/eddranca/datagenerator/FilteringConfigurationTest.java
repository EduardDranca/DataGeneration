package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.exception.FilteringException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilteringConfigurationTest extends ParameterizedGenerationTest {

    @BothImplementationsTest
    void testDefaultFilteringBehaviorReturnsNull(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
            {
                "items": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"},
                        "restrictedChoice": {
                            "gen": "choice",
                            "options": ["A", "B"],
                            "filter": ["A", "B"]
                        }
                    }
                }
            }
            """);

        Generation generation = generateFromDsl(dslNode, memoryOptimized);
        List<JsonNode> items = generation.streamJsonNodes("items").toList();

        assertThat(items).hasSize(5);

        assertThat(items)
            .extracting(item -> item.get("restrictedChoice"))
            .as("Should be null when all options are filtered (default behavior)")
            .allSatisfy(choice -> assertThat(choice.isNull()).isTrue());
    }

    @BothImplementationsTest
    void testThrowExceptionFilteringBehaviorForGenerators(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
            {
                "items": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"},
                        "restrictedChoice": {
                            "gen": "choice",
                            "options": ["A", "B"],
                            "filter": ["A", "B"]
                        }
                    }
                }
            }
            """);

        assertThatThrownBy(() -> generateAndConsumeWithThrowBehavior(dslNode, memoryOptimized))
            .isInstanceOf(FilteringException.class)
            .hasMessageContaining("All choice options were filtered out");
    }

    @BothImplementationsTest
    void testThrowExceptionFilteringBehaviorForReferences(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
            {
                "reference_data": {
                    "count": 2,
                    "item": {
                        "name": {"gen": "choice", "options": ["Alice", "Bob"]}
                    }
                },
                "filtered_items": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"},
                        "name": {
                            "ref": "reference_data[*].name",
                            "filter": ["Alice", "Bob"]
                        }
                    }
                }
            }
            """);

        assertThatThrownBy(() -> generateAndConsumeCollection(dslNode, memoryOptimized, "filtered_items"))
            .isInstanceOf(FilteringException.class)
            .hasMessageContaining("has no valid values after filtering");
    }

    @BothImplementationsTest
    void testCustomMaxFilteringRetries(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
            {
                "items": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"},
                        "number": {
                            "gen": "number",
                            "min": 1,
                            "max": 3,
                            "filter": [1]
                        }
                    }
                }
            }
            """);

        // With only 2 possible values (1, 2) and filtering out 1,
        // we should be able to generate 2 consistently
        Generation generation = createGenerator(memoryOptimized)
            .withMaxFilteringRetries(10) // Lower retry count
            .fromJsonNode(dslNode)
            .generate();

        List<JsonNode> items = generation.streamJsonNodes("items").toList();

        assertThat(items).hasSize(5);

        assertThat(items)
            .extracting(item -> item.get("number"))
            .as("All numbers should be non-null and equal to 2")
            .allSatisfy(number -> {
                assertThat(number).isNotNull();
                assertThat(number.intValue()).isEqualTo(2);
            });
    }

    @Test
    void testMaxFilteringRetriesValidation() {
        assertThatThrownBy(() -> DslDataGenerator.create().withMaxFilteringRetries(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Max filtering retries must be positive");

        assertThatThrownBy(() -> DslDataGenerator.create().withMaxFilteringRetries(-5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Max filtering retries must be positive");
    }

    @Test
    void testFilteringBehaviorNullHandling() {
        // Should not throw exception when null is passed
        DslDataGenerator generator = DslDataGenerator.create()
            .withFilteringBehavior(null)
            .build();

        assertThat(generator).isNotNull();
    }

    @BothImplementationsTest
    void testSuccessfulFilteringWithCustomRetries(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
            {
                "items": {
                    "count": 10,
                    "item": {
                        "id": {"gen": "uuid"},
                        "number": {
                            "gen": "number",
                            "min": 1,
                            "max": 10,
                            "filter": [1, 2, 3, 4, 5]
                        }
                    }
                }
            }
            """);

        Generation generation = createGenerator(memoryOptimized)
            .withMaxFilteringRetries(200) // Higher retry count
            .withFilteringBehavior(FilteringBehavior.RETURN_NULL)
            .fromJsonNode(dslNode)
            .generate();

        List<JsonNode> items = generation.streamJsonNodes("items").toList();

        assertThat(items)
            .hasSize(10)
            .extracting(item -> item.get("number"))
            .as("All numbers should be non-null and between 6-10")
            .allSatisfy(number -> {
                assertThat(number).isNotNull();
                int value = number.intValue();
                assertThat(value).isBetween(6, 10);
            });
    }

    private void generateAndConsumeWithThrowBehavior(JsonNode dslNode, boolean memoryOptimized) throws IOException {
        Generation generation = createGenerator(memoryOptimized)
            .withFilteringBehavior(FilteringBehavior.THROW_EXCEPTION)
            .fromJsonNode(dslNode)
            .generate();

        // Force evaluation by consuming the stream
        generation.streamJsonNodes("items").forEach(item -> {
        });
    }

    private void generateAndConsumeCollection(JsonNode dslNode, boolean memoryOptimized, String collectionName) throws IOException {
        Generation generation = createGenerator(memoryOptimized)
            .withFilteringBehavior(FilteringBehavior.THROW_EXCEPTION)
            .fromJsonNode(dslNode)
            .generate();

        // Force evaluation by consuming the stream
        generation.streamJsonNodes(collectionName).forEach(item -> {
        });
    }
}
