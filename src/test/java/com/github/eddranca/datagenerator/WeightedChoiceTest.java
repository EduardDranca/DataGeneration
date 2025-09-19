package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeightedChoiceTest extends ParameterizedGenerationTest {

    @BothImplementations
    void testBasicWeightedChoice(String implementationName, boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                    "items": {
                        "count": 1000,
                        "item": {
                            "type": {
                                "gen": "choice",
                                "options": ["rare", "common", "uncommon"],
                                "weights": [0.1, 0.8, 0.1]
                            }
                        }
                    }
                }
                """;

        IGeneration generation = createGenerator(memoryOptimized)
            .withSeed(1993L)
            .fromJsonString(dsl)
            .generate();

        List<JsonNode> items = generation.streamJsonNodes("items").toList();

        assertThat(items).hasSize(1000);

        // Count occurrences
        long rareCount = items.stream()
            .mapToLong(item -> "rare".equals(item.get("type").asText()) ? 1 : 0)
            .sum();
        long commonCount = items.stream()
            .mapToLong(item -> "common".equals(item.get("type").asText()) ? 1 : 0)
            .sum();
        long uncommonCount = items.stream()
            .mapToLong(item -> "uncommon".equals(item.get("type").asText()) ? 1 : 0)
            .sum();

        // With weights [0.1, 0.8, 0.1], we expect roughly:
        // rare: 10%, common: 80%, uncommon: 10%
        // Allow some variance due to randomness
        assertThat(rareCount >= 50 && rareCount <= 150).as("Rare count should be around 100, got: " + rareCount)
            .isTrue();
        assertThat(commonCount >= 700 && commonCount <= 900)
            .as("Common count should be around 800, got: " + commonCount).isTrue();
        assertThat(uncommonCount >= 50 && uncommonCount <= 150)
            .as("Uncommon count should be around 100, got: " + uncommonCount).isTrue();
    }

    @BothImplementations
    void testWeightedChoiceWithDifferentWeights(String implementationName, boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                    "products": {
                        "count": 1000,
                        "item": {
                            "category": {
                                "gen": "choice",
                                "options": ["electronics", "books", "clothing", "home"],
                                "weights": [5.0, 2.0, 1.0, 2.0]
                            }
                        }
                    }
                }
                """;

        IGeneration generation = createGenerator(memoryOptimized)
            .withSeed(456L)
            .fromJsonString(dsl)
            .generate();

        List<JsonNode> products = generation.streamJsonNodes("products").toList();

        assertThat(products).hasSize(1000);

        // Count occurrences
        long electronicsCount = products.stream()
            .mapToLong(product -> "electronics".equals(product.get("category").asText()) ? 1 : 0)
            .sum();
        long booksCount = products.stream()
            .mapToLong(product -> "books".equals(product.get("category").asText()) ? 1 : 0)
            .sum();
        long clothingCount = products.stream()
            .mapToLong(product -> "clothing".equals(product.get("category").asText()) ? 1 : 0)
            .sum();
        long homeCount = products.stream()
            .mapToLong(product -> "home".equals(product.get("category").asText()) ? 1 : 0)
            .sum();

        // With weights [5.0, 2.0, 1.0, 2.0] (total = 10.0), we expect:
        // electronics: 50%, books: 20%, clothing: 10%, home: 20%
        assertThat(electronicsCount >= 400 && electronicsCount <= 600)
            .as("Electronics count should be around 500, got: " + electronicsCount).isTrue();
        assertThat(booksCount >= 100 && booksCount <= 300).as("Books count should be around 200, got: " + booksCount)
            .isTrue();
        assertThat(clothingCount >= 50 && clothingCount <= 150)
            .as("Clothing count should be around 100, got: " + clothingCount).isTrue();
        assertThat(homeCount >= 100 && homeCount <= 300).as("Home count should be around 200, got: " + homeCount)
            .isTrue();
    }

    @BothImplementations
    void testWeightedChoiceWithComplexOptions(String implementationName, boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                    "users": {
                        "count": 500,
                        "item": {
                            "id": {"gen": "uuid"},
                            "role": {
                                "gen": "choice",
                                "options": [
                                    {"gen": "choice", "options": ["admin", "super_admin"]},
                                    {"gen": "choice", "options": ["user", "member"]},
                                    "guest"
                                ],
                                "weights": [0.05, 0.9, 0.05]
                            }
                        }
                    }
                }
                """;

        IGeneration generation = createGenerator(memoryOptimized)
            .withSeed(789L)
            .fromJsonString(dsl)
            .generate();

        List<JsonNode> users = generation.streamJsonNodes("users").toList();

        assertThat(users).hasSize(500);

        // Extract roles and verify distribution using AssertJ
        List<String> roles = users.stream()
            .map(user -> user.get("role").asText())
            .toList();

        // Count each role type for distribution verification
        long adminTypeCount = roles.stream().filter(role -> "admin".equals(role) || "super_admin".equals(role)).count();
        long userTypeCount = roles.stream().filter(role -> "user".equals(role) || "member".equals(role)).count();
        long guestCount = roles.stream().filter("guest"::equals).count();

        // Verify the roles contain expected values
        assertThat(roles)
            .contains("admin", "super_admin", "user", "member", "guest")
            .hasSize(500);

        // With weights [0.05, 0.9, 0.05], we expect roughly:
        // admin types: 5%, user types: 90%, guest: 5%
        assertThat(adminTypeCount).as("Admin type count should be around 25, got: " + adminTypeCount)
            .isGreaterThan(10)
            .isLessThan(50);
        assertThat(userTypeCount >= 400 && userTypeCount <= 480)
            .as("User type count should be around 450, got: " + userTypeCount).isTrue();
        assertThat(guestCount >= 10 && guestCount <= 50).as("Guest count should be around 25, got: " + guestCount)
            .isTrue();
    }

    @BothImplementations
    void testUnweightedChoiceStillWorks(String implementationName, boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                    "items": {
                        "count": 300,
                        "item": {
                            "color": {
                                "gen": "choice",
                                "options": ["red", "green", "blue"]
                            }
                        }
                    }
                }
                """;

        IGeneration generation = createGenerator(memoryOptimized)
            .withSeed(999L)
            .fromJsonString(dsl)
            .generate();

        List<JsonNode> items = generation.streamJsonNodes("items").toList();

        assertThat(items).hasSize(300);

        // Count occurrences - should be roughly equal
        long redCount = items.stream()
            .mapToLong(item -> "red".equals(item.get("color").asText()) ? 1 : 0)
            .sum();
        long greenCount = items.stream()
            .mapToLong(item -> "green".equals(item.get("color").asText()) ? 1 : 0)
            .sum();
        long blueCount = items.stream()
            .mapToLong(item -> "blue".equals(item.get("color").asText()) ? 1 : 0)
            .sum();

        // Each should be roughly 33% (100 items), allow variance
        assertThat(redCount >= 70 && redCount <= 130).as("Red count should be around 100, got: " + redCount).isTrue();
        assertThat(greenCount >= 70 && greenCount <= 130).as("Green count should be around 100, got: " + greenCount)
            .isTrue();
        assertThat(blueCount >= 70 && blueCount <= 130).as("Blue count should be around 100, got: " + blueCount)
            .isTrue();
    }

    @BothImplementations
    void testWeightedChoiceWithDecimalWeights(String implementationName, boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                    "items": {
                        "count": 1000,
                        "item": {
                            "priority": {
                                "gen": "choice",
                                "options": ["high", "medium", "low"],
                                "weights": [0.15, 0.35, 0.50]
                            }
                        }
                    }
                }
                """;

        IGeneration generation = createGenerator(memoryOptimized)
            .withSeed(111L)
            .fromJsonString(dsl)
            .generate();

        List<JsonNode> items = generation.streamJsonNodes("items").toList();

        assertThat(items).hasSize(1000);

        // Count occurrences
        long highCount = items.stream()
            .mapToLong(item -> "high".equals(item.get("priority").asText()) ? 1 : 0)
            .sum();
        long mediumCount = items.stream()
            .mapToLong(item -> "medium".equals(item.get("priority").asText()) ? 1 : 0)
            .sum();
        long lowCount = items.stream()
            .mapToLong(item -> "low".equals(item.get("priority").asText()) ? 1 : 0)
            .sum();

        // With weights [0.15, 0.35, 0.50], we expect:
        // high: 15%, medium: 35%, low: 50%
        assertThat(highCount >= 100 && highCount <= 200).as("High count should be around 150, got: " + highCount)
            .isTrue();
        assertThat(mediumCount >= 250 && mediumCount <= 450)
            .as("Medium count should be around 350, got: " + mediumCount).isTrue();
        assertThat(lowCount >= 400 && lowCount <= 600).as("Low count should be around 500, got: " + lowCount).isTrue();
    }
}
