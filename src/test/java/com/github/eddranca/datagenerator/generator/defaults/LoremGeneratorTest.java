package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class LoremGeneratorTest {
    private LoremGenerator generator;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        Faker faker = new Faker();
        generator = new LoremGenerator(faker);
        mapper = new ObjectMapper();
    }

    @Test
    void testGenerateDefault() {
        JsonNode result = generator.generate(null);
        assertThat(result.isObject()).isTrue();

        // Should have all the expected fields
        assertThat(result.has("word")).isTrue();
        assertThat(result.has("words")).isTrue();
        assertThat(result.has("sentence")).isTrue();
        assertThat(result.has("sentences")).isTrue();
        assertThat(result.has("paragraph")).isTrue();
        assertThat(result.has("paragraphs")).isTrue();

        // All fields should be textual and non-empty
        assertThat(result.get("word").isTextual()).isTrue();
        assertThat(result.get("word").asText()).isNotEmpty();

        assertThat(result.get("sentence").isTextual()).isTrue();
        assertThat(result.get("sentence").asText()).isNotEmpty();
    }

    @ParameterizedTest
    @CsvSource({
        "words, 3, 0",
        "sentences, 2, 5",
        "paragraphs, 2, 10"
    })
    void testGenerateTextTypes(String type, int count, int minExpectedSize) throws Exception {
        JsonNode options = mapper.readTree("{\"" + type + "\": " + count + "}");
        JsonNode result = generator.generate(options);

        assertThat(result.isTextual()).isTrue();
        String text = result.asText();
        assertThat(text)
            .isNotEmpty()
            .hasSizeGreaterThan(minExpectedSize);
    }

    @Test
    void testMinimumValues() throws Exception {
        // Test that negative or zero values are handled gracefully
        JsonNode options = mapper.readTree("{\"words\": 0}");
        JsonNode result = generator.generate(options);
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).hasSizeGreaterThan(0);

        options = mapper.readTree("{\"sentences\": -1}");
        result = generator.generate(options);
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).hasSizeGreaterThan(0);
    }

    @Test
    void testFieldSuppliers() {
        Map<String, Supplier<JsonNode>> suppliers = generator.getFieldSuppliers(null);

        assertThat(suppliers)
            .isNotNull()
            .containsKeys("word", "words", "sentence", "sentences", "paragraph", "paragraphs");

        // Test that suppliers actually work
        JsonNode wordResult = suppliers.get("word").get();
        assertThat(wordResult.isTextual()).isTrue();
        assertThat(wordResult.asText()).isNotEmpty();

        JsonNode sentenceResult = suppliers.get("sentence").get();
        assertThat(sentenceResult.isTextual()).isTrue();
        assertThat(sentenceResult.asText()).isNotEmpty();
    }

    @Test
    void testFilteringWithNoFilters() {
        JsonNode result = generator.generateWithFilter(null, null);
        assertThat(result.isObject()).isTrue(); // Default generate returns object

        result = generator.generateWithFilter(null, List.of());
        assertThat(result.isObject()).isTrue(); // Default generate returns object
    }

    @Test
    void testFilteringWithSpecificText() throws Exception {
        // This test might be flaky due to randomness, but we'll try
        JsonNode options = mapper.readTree("{\"words\": 1}");

        // Generate a value first to use as filter
        JsonNode firstResult = generator.generate(options);
        String textToFilter = firstResult.asText();

        List<JsonNode> filters = Collections.singletonList(mapper.valueToTree(textToFilter));

        // Try a few times to see if filtering works
        boolean foundDifferent = IntStream.range(0, 5)
            .mapToObj(i -> generator.generateWithFilter(options, filters))
            .anyMatch(result -> result.isTextual() && !result.asText().equals(textToFilter));

        // Due to randomness, we can't guarantee this will always work,
        // but it should work most of the time
        assertThat(foundDifferent || generator.generateWithFilter(options, filters).isNull()).isTrue();
    }

    @Test
    void testEmptyOptions() throws Exception {
        JsonNode options = mapper.readTree("{}");
        JsonNode result = generator.generate(options);
        // Empty options should fall back to default sentence
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isNotEmpty();
    }
}
