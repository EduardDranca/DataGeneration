package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class LoremGeneratorTest {
    private final LoremGenerator generator = new LoremGenerator();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Faker faker = new Faker();

    @Test
    void testGenerateDefault() {
        JsonNode result = generator.generate(new GeneratorContext(faker, null, mapper));
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
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));

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
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).hasSizeGreaterThan(0);

        options = mapper.readTree("{\"sentences\": -1}");
        result = generator.generate(new GeneratorContext(faker, options, mapper));
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).hasSizeGreaterThan(0);
    }

    @Test
    void testFilteringWithNoFilters() {
        JsonNode result = generator.generateWithFilter(new GeneratorContext(faker, null, mapper), null);
        assertThat(result.isObject()).isTrue(); // Default generate returns object

        result = generator.generateWithFilter(new GeneratorContext(faker, null, mapper), List.of());
        assertThat(result.isObject()).isTrue(); // Default generate returns object
    }

    @Test
    void testFilteringWithSpecificText() throws Exception {
        // This test might be flaky due to randomness, but we'll try
        JsonNode options = mapper.readTree("{\"words\": 1}");

        // Generate a value first to use as filter
        JsonNode firstResult = generator.generate(new GeneratorContext(faker, options, mapper));
        String textToFilter = firstResult.asText();

        List<JsonNode> filters = Collections.singletonList(mapper.valueToTree(textToFilter));

        // Try a few times to see if filtering works
        boolean foundDifferent = IntStream.range(0, 5)
            .mapToObj(i -> generator.generateWithFilter(new GeneratorContext(faker, options, mapper), filters))
            .anyMatch(result -> result.isTextual() && !result.asText().equals(textToFilter));

        // Due to randomness, we can't guarantee this will always work,
        // but it should work most of the time
        assertThat(foundDifferent || generator.generateWithFilter(new GeneratorContext(faker, options, mapper), filters).isNull()).isTrue();
    }

    @Test
    void testEmptyOptions() throws Exception {
        JsonNode options = mapper.readTree("{}");
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));
        // Empty options should fall back to default sentence
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isNotEmpty();
    }
}
