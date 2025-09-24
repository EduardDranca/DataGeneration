package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class BooleanGeneratorTest {
    private BooleanGenerator generator;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        Faker faker = new Faker();
        generator = new BooleanGenerator(faker);
        mapper = new ObjectMapper();
    }

    @Test
    void testGenerateDefault() {
        JsonNode result = generator.generate(null);
        assertThat(result.isBoolean()).isTrue();
    }

    @Test
    void testGenerateWithEmptyOptions() throws Exception {
        JsonNode options = mapper.readTree("{}");
        JsonNode result = generator.generate(options);
        assertThat(result.isBoolean()).isTrue();
    }

    @Test
    void testGenerateWithProbability() throws Exception {
        // Test with 100% probability (always true)
        JsonNode options = mapper.readTree("{\"probability\": 1.0}");
        JsonNode result = generator.generate(options);
        assertThat(result.asBoolean()).isTrue();

        // Test with 0% probability (always false)
        options = mapper.readTree("{\"probability\": 0.0}");
        result = generator.generate(options);
        assertThat(result.asBoolean()).isFalse();
    }

    @Test
    void testProbabilityDistribution() throws Exception {
        JsonNode options = mapper.readTree("{\"probability\": 0.8}");

        int totalTests = 1000;

        long trueCount = IntStream.range(0, totalTests)
            .mapToObj(i -> generator.generate(options))
            .mapToInt(result -> result.asBoolean() ? 1 : 0)
            .sum();

        double actualProbability = (double) trueCount / totalTests;
        // Allow 5% tolerance for randomness
        assertThat(actualProbability)
            .as("Expected probability around 0.8, got %f", actualProbability)
            .isBetween(0.75, 0.85);
    }

    @Test
    void testProbabilityBounds() throws Exception {
        // Test values outside bounds are clamped
        JsonNode options = mapper.readTree("{\"probability\": 1.5}");
        JsonNode result = generator.generate(options);
        assertThat(result.asBoolean()).isTrue(); // Should be clamped to 1.0

        options = mapper.readTree("{\"probability\": -0.5}");
        result = generator.generate(options);
        assertThat(result.asBoolean()).isFalse(); // Should be clamped to 0.0
    }

    @Test
    void testFilteringSupported() {
        assertThat(generator.supportsFiltering()).isTrue();
    }

    @Test
    void testFilteringWithNoFilters() {
        JsonNode result = generator.generateWithFilter(null, null);
        assertThat(result.isBoolean()).isTrue();

        result = generator.generateWithFilter(null, List.of());
        assertThat(result.isBoolean()).isTrue();
    }

    @Test
    void testFilteringTrue() {
        List<JsonNode> filters = Collections.singletonList(mapper.valueToTree(true));
        JsonNode result = generator.generateWithFilter(null, filters);
        assertThat(result.asBoolean()).isFalse(); // Should return false when true is filtered
    }

    @Test
    void testFilteringFalse() {
        List<JsonNode> filters = Collections.singletonList(mapper.valueToTree(false));
        JsonNode result = generator.generateWithFilter(null, filters);
        assertThat(result.asBoolean()).isTrue(); // Should return true when false is filtered
    }

    @Test
    void testFilteringBoth() {
        List<JsonNode> filters = Arrays.asList(
            mapper.valueToTree(true),
            mapper.valueToTree(false)
        );
        JsonNode result = generator.generateWithFilter(null, filters);
        assertThat(result.isNull()).isTrue(); // Should return null when both are filtered
    }

    @Test
    void testFilteringNonBooleanValues() {
        List<JsonNode> filters = Arrays.asList(
            mapper.valueToTree("not a boolean"),
            mapper.valueToTree(42)
        );
        JsonNode result = generator.generateWithFilter(null, filters);
        assertThat(result.isBoolean()).isTrue(); // Should ignore non-boolean filters
    }
}
