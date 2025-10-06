package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneGeneratorTest {
    private final PhoneGenerator generator = new PhoneGenerator();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Faker faker = new Faker();

    @Test
    void testGenerateDefault() {
        JsonNode result = generator.generate(new GeneratorContext(faker, null, mapper));
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isNotEmpty();

        // Phone numbers should contain digits
        String phone = result.asText();
        assertThat(phone)
            .as("Phone number should contain digits: %s", phone)
            .matches(".*\\d.*");
    }

    @Test
    void testGenerateWithDefaultFormat() throws Exception {
        JsonNode options = mapper.readTree("{}");
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"international", "cell", "mobile"})
    void testGeneratePhoneFormats(String format) throws Exception {
        JsonNode options = mapper.readTree("{\"format\": \"" + format + "\"}");
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));

        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isNotEmpty();

        String phone = result.asText();
        assertThat(phone)
            .as("%s phone should contain digits: %s", format, phone)
            .matches(".*\\d.*");
    }

    @Test
    void testGenerateExtensionFormat() throws Exception {
        JsonNode options = mapper.readTree("{\"format\": \"extension\"}");
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isNotEmpty();

        // Extensions are typically shorter and numeric
        String extension = result.asText();
        assertThat(extension)
            .as("Extension should be numeric: %s", extension)
            .matches("\\d+");
    }

    @Test
    void testUnknownFormat() throws Exception {
        JsonNode options = mapper.readTree("{\"format\": \"unknown\"}");
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isNotEmpty();

        // Should fall back to default format
        String phone = result.asText();
        assertThat(phone)
            .as("Unknown format should fall back to default: %s", phone)
            .matches(".*\\d.*");
    }

    @Test
    void testFilteringWithNoFilters() {
        JsonNode result = generator.generateWithFilter(new GeneratorContext(faker, null, mapper), null);
        assertThat(result.isTextual()).isTrue();

        result = generator.generateWithFilter(new GeneratorContext(faker, null, mapper), List.of());
        assertThat(result.isTextual()).isTrue();
    }

    @Test
    void testFilteringWithSpecificPhone() {
        // Generate a phone number to use as filter
        JsonNode firstResult = generator.generate(new GeneratorContext(faker, null, mapper));
        String phoneToFilter = firstResult.asText();

        List<JsonNode> filters = Collections.singletonList(mapper.valueToTree(phoneToFilter));

        // Try a few times to see if filtering works
        boolean foundDifferent = IntStream.range(0, 10)
            .mapToObj(i -> generator.generateWithFilter(new GeneratorContext(faker, null, mapper), filters))
            .anyMatch(result -> result.isTextual() && !result.asText().equals(phoneToFilter));

        // Phone numbers have high variety, so this should usually work
        assertThat(foundDifferent || generator.generateWithFilter(new GeneratorContext(faker, null, mapper),  filters).isNull()).isTrue();
    }

    @Test
    void testCaseInsensitiveFormat() throws Exception {
        JsonNode options1 = mapper.readTree("{\"format\": \"CELL\"}");
        JsonNode result1 = generator.generate(new GeneratorContext(faker, options1, mapper));
        assertThat(result1.isTextual()).isTrue();

        JsonNode options2 = mapper.readTree("{\"format\": \"Cell\"}");
        JsonNode result2 = generator.generate(new GeneratorContext(faker, options2, mapper));
        assertThat(result2.isTextual()).isTrue();

        // Both should work (case insensitive)
        assertThat(result1.asText()).isNotEmpty();
        assertThat(result2.asText()).isNotEmpty();
    }
}
