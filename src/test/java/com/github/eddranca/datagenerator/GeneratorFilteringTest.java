package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.generator.Generator;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class GeneratorFilteringTest extends ParameterizedGenerationTest {

    @BothImplementations
    void testBasicGeneratorFiltering(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "reference_data": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 10},
                            "name": {"gen": "choice", "options": ["Alice", "Bob", "Charlie"]}
                        },
                        "pick": {
                            "first": 0
                        }
                    },
                    "filtered_items": {
                        "count": 20,
                        "item": {
                            "id": {"gen": "uuid"},
                            "filteredNumber": {
                                "gen": "number",
                                "min": 1,
                                "max": 10,
                                "filter": [{"ref": "first.id"}]
                            },
                            "filteredChoice": {
                                "gen": "choice",
                                "options": ["Alice", "Bob", "Charlie", "David", "Eve"],
                                "filter": [{"ref": "first.name"}, "David"]
                            }
                        }
                    }
                }
                """);

        IGeneration generation = generateFromDsl(dslNode, memoryOptimized);

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        List<JsonNode> referenceData = collections.get("reference_data");
        List<JsonNode> filteredItems = collections.get("filtered_items");

        assertThat(referenceData).hasSize(3);
        assertThat(filteredItems).hasSize(20);

        // Get the first reference item's values that should be filtered out
        int firstId = referenceData.get(0).get("id").intValue();
        String firstName = referenceData.get(0).get("name").asText();


        assertThat(filteredItems)
            .extracting(item -> item.get("filteredNumber").intValue())
            .as("Filtered numbers should not equal first reference ID: %d", firstId)
            .doesNotContain(firstId);

        assertThat(filteredItems)
            .extracting(item -> item.get("filteredChoice").asText())
            .as("Filtered choices should not equal first reference name or 'David'")
            .doesNotContain(firstName, "David");

        // Collect unique values to verify filtering worked
        Set<Integer> uniqueNumbers = filteredItems.stream()
            .map(item -> item.get("filteredNumber").intValue())
            .collect(Collectors.toSet());
        Set<String> uniqueChoices = filteredItems.stream()
            .map(item -> item.get("filteredChoice").asText())
            .collect(Collectors.toSet());


        // Verify we have variety in the results (not all the same value)
        assertThat(uniqueNumbers)
            .as("Should have variety in filtered numbers")
            .hasSizeGreaterThan(1);
        assertThat(uniqueChoices)
            .as("Should have variety in filtered choices")
            .hasSizeGreaterThan(1);
    }

    @BothImplementations
    void testGeneratorFilteringWithComplexOptions(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "categories": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "choice", "options": ["electronics", "books"]},
                            "priority": {"gen": "choice", "options": ["high", "low"]}
                        },
                        "pick": {
                            "excluded": 0
                        }
                    },
                    "products": {
                        "count": 15,
                        "item": {
                            "id": {"gen": "uuid"},
                            "category": {
                                "gen": "choice",
                                "options": ["electronics", "books", "clothing", "home"],
                                "filter": [{"ref": "excluded.name"}]
                            },
                            "price": {
                                "gen": "float",
                                "min": 10.0,
                                "max": 100.0,
                                "decimals": 2,
                                "filter": [50.0, 75.0]
                            }
                        }
                    }
                }
                """);

        IGeneration generation = generateFromDsl(dslNode, memoryOptimized);

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        List<JsonNode> categories = collections.get("categories");
        List<JsonNode> products = collections.get("products");

        assertThat(categories).hasSize(2);
        assertThat(products).hasSize(15);

        String excludedCategory = categories.get(0).get("name").asText();

        assertThat(products)
            .extracting(product -> product.get("category").asText())
            .as("Product categories should not equal excluded category: %s", excludedCategory)
            .doesNotContain(excludedCategory);

        assertThat(products)
            .extracting(product -> product.get("price").doubleValue())
            .as("Prices should not be exactly 50.0 or 75.0")
            .allSatisfy(price -> {
                assertThat(price).isNotCloseTo(50.0, offset(0.001));
                assertThat(price).isNotCloseTo(75.0, offset(0.001));
            });

        // Collect unique values
        Set<String> uniqueCategories = products.stream()
            .map(product -> product.get("category").asText())
            .collect(Collectors.toSet());


        assertThat(uniqueCategories)
            .as("Should have variety in product categories")
            .hasSizeGreaterThan(1)
            .as("Product categories should not contain excluded category")
            .doesNotContain(excludedCategory);
    }

    @BothImplementations
    void testGeneratorFilteringWithPathExtraction(boolean memoryOptimized) throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "reference_names": {
                        "count": 2,
                        "item": {
                            "firstName": {"gen": "choice", "options": ["John", "Jane"]},
                            "lastName": {"gen": "choice", "options": ["Doe", "Smith"]}
                        },
                        "pick": {
                            "excluded": 0
                        }
                    },
                    "users": {
                        "count": 10,
                        "item": {
                            "id": {"gen": "uuid"},
                            "firstName": {
                                "gen": "choice",
                                "options": ["John", "Jane", "Bob", "Alice"],
                                "filter": [{"ref": "excluded.firstName"}]
                            },
                            "lastName": {
                                "gen": "choice",
                                "options": ["Doe", "Smith", "Johnson", "Brown"],
                                "filter": [{"ref": "excluded.lastName"}]
                            }
                        }
                    }
                }
                """);

        IGeneration generation = generateFromDsl(dslNode, memoryOptimized);

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        List<JsonNode> referenceNames = collections.get("reference_names");
        List<JsonNode> users = collections.get("users");

        assertThat(referenceNames).hasSize(2);
        assertThat(users).hasSize(10);

        String excludedFirstName = referenceNames.get(0).get("firstName").asText();
        String excludedLastName = referenceNames.get(0).get("lastName").asText();

        assertThat(users)
            .extracting(user -> user.get("firstName").asText())
            .as("User first names should not equal excluded first name: %s", excludedFirstName)
            .doesNotContain(excludedFirstName);

        assertThat(users)
            .extracting(user -> user.get("lastName").asText())
            .as("User last names should not equal excluded last name: %s", excludedLastName)
            .doesNotContain(excludedLastName);

        // Verify we have variety
        Set<String> uniqueFirstNames = users.stream()
            .map(user -> user.get("firstName").asText())
            .collect(Collectors.toSet());
        Set<String> uniqueLastNames = users.stream()
            .map(user -> user.get("lastName").asText())
            .collect(Collectors.toSet());


        assertThat(uniqueFirstNames)
            .as("Should have variety in first names")
            .hasSizeGreaterThan(1);
        assertThat(uniqueLastNames)
            .as("Should have variety in last names")
            .hasSizeGreaterThan(1);
    }

    @BothImplementations
    void testGeneratorFilteringFallbackToNull(boolean memoryOptimized) throws IOException {
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

        IGeneration generation = createGenerator(memoryOptimized)
            .withFilteringBehavior(FilteringBehavior.RETURN_NULL)
            .fromJsonNode(dslNode)
            .generate();

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        List<JsonNode> items = collections.get("items");

        assertThat(items).hasSize(5);

        assertThat(items)
            .extracting(item -> item.get("restrictedChoice"))
            .as("Should be null when all options are filtered")
            .allSatisfy(choice -> assertThat(choice.isNull()).isTrue());


    }

    @BothImplementations
    void testCustomGeneratorWithNativeFiltering(boolean memoryOptimized) throws IOException {
        // Create a custom generator that supports native filtering
        Generator customFilteringGenerator = new Generator() {
            @Override
            public JsonNode generate(JsonNode options) {
                return mapper.valueToTree("default_value");
            }

            @Override
            public JsonNode generateWithFilter(JsonNode options, List<JsonNode> filterValues) {
                // Custom logic: if "filtered" is in filter values, return "custom_filtered"
                if (filterValues != null) {
                    for (JsonNode filter : filterValues) {
                        if ("filtered".equals(filter.asText())) {
                            return mapper.valueToTree("custom_filtered");
                        }
                    }
                }
                return mapper.valueToTree("custom_unfiltered");
            }

            @Override
            public boolean supportsFiltering() {
                return true;
            }
        };

        JsonNode dslNode = mapper.readTree("""
                {
                    "items": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "uuid"},
                            "customValue": {
                                "gen": "customFiltering",
                                "filter": ["filtered"]
                            }
                        }
                    }
                }
                """);

        IGeneration generation = createGenerator(memoryOptimized)
            .withCustomGenerator("customFiltering", customFilteringGenerator)
            .fromJsonNode(dslNode)
            .generate();

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        List<JsonNode> items = collections.get("items");

        assertThat(items).hasSize(3);

        assertThat(items)
            .extracting(item -> item.get("customValue").asText())
            .as("Custom generator should use native filtering logic")
            .allSatisfy(customValue -> assertThat(customValue).isEqualTo("custom_filtered"));


    }
}
