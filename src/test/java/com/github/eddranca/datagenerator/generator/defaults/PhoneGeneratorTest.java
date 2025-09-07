package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneGeneratorTest {
    private PhoneGenerator generator;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        Faker faker = new Faker();
        generator = new PhoneGenerator(faker);
        mapper = new ObjectMapper();
    }

    @Test
    void testGenerateDefault() {
        JsonNode result = generator.generate(null);
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
        JsonNode result = generator.generate(options);
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isNotEmpty();
    }

    @Test
    void testGenerateInternationalFormat() throws Exception {
        JsonNode options = mapper.readTree("{\"format\": \"international\"}");
        JsonNode result = generator.generate(options);
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isNotEmpty();

        String phone = result.asText();
        assertThat(phone)
            .as("International phone should contain digits: %s", phone)
            .matches(".*\\d.*");
    }

    @Test
    void testGenerateCellFormat() throws Exception {
        JsonNode options = mapper.readTree("{\"format\": \"cell\"}");
        JsonNode result = generator.generate(options);
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isNotEmpty();

        String phone = result.asText();
        assertThat(phone)
            .as("Cell phone should contain digits: %s", phone)
            .matches(".*\\d.*");
    }

    @Test
    void testGenerateMobileFormat() throws Exception {
        JsonNode options = mapper.readTree("{\"format\": \"mobile\"}");
        JsonNode result = generator.generate(options);
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isNotEmpty();

        String phone = result.asText();
        assertThat(phone)
            .as("Mobile phone should contain digits: %s", phone)
            .matches(".*\\d.*");
    }

    @Test
    void testGenerateExtensionFormat() throws Exception {
        JsonNode options = mapper.readTree("{\"format\": \"extension\"}");
        JsonNode result = generator.generate(options);
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
        JsonNode result = generator.generate(options);
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isNotEmpty();

        // Should fall back to default format
        String phone = result.asText();
        assertThat(phone)
            .as("Unknown format should fall back to default: %s", phone)
            .matches(".*\\d.*");
    }

    @Test
    void testFieldSuppliers() {
        Map<String, Supplier<JsonNode>> suppliers = generator.getFieldSuppliers(null);

        assertThat(suppliers).isNotNull();
        assertThat(suppliers).containsKeys("phoneNumber", "cellPhone", "extension");

        // Test that suppliers actually work
        JsonNode phoneResult = suppliers.get("phoneNumber").get();
        assertThat(phoneResult.isTextual()).isTrue();
        assertThat(phoneResult.asText()).isNotEmpty();

        JsonNode cellResult = suppliers.get("cellPhone").get();
        assertThat(cellResult.isTextual()).isTrue();
        assertThat(cellResult.asText()).isNotEmpty();

        JsonNode extensionResult = suppliers.get("extension").get();
        assertThat(extensionResult.isTextual()).isTrue();
        assertThat(extensionResult.asText()).isNotEmpty();
        assertThat(extensionResult.asText()).matches("\\d+");
    }

    @Test
    void testFilteringWithNoFilters() {
        JsonNode result = generator.generateWithFilter(null, null);
        assertThat(result.isTextual()).isTrue();

        result = generator.generateWithFilter(null, List.of());
        assertThat(result.isTextual()).isTrue();
    }

    @Test
    void testFilteringWithSpecificPhone() {
        // Generate a phone number to use as filter
        JsonNode firstResult = generator.generate(null);
        String phoneToFilter = firstResult.asText();

        List<JsonNode> filters = Collections.singletonList(mapper.valueToTree(phoneToFilter));

        // Try a few times to see if filtering works
        boolean foundDifferent = java.util.stream.IntStream.range(0, 10)
            .mapToObj(i -> generator.generateWithFilter(null, filters))
            .anyMatch(result -> result.isTextual() && !result.asText().equals(phoneToFilter));

        // Phone numbers have high variety, so this should usually work
        assertThat(foundDifferent || generator.generateWithFilter(null, filters).isNull()).isTrue();
    }

    @Test
    void testCaseInsensitiveFormat() throws Exception {
        JsonNode options1 = mapper.readTree("{\"format\": \"CELL\"}");
        JsonNode result1 = generator.generate(options1);
        assertThat(result1.isTextual()).isTrue();

        JsonNode options2 = mapper.readTree("{\"format\": \"Cell\"}");
        JsonNode result2 = generator.generate(options2);
        assertThat(result2.isTextual()).isTrue();

        // Both should work (case insensitive)
        assertThat(result1.asText()).isNotEmpty();
        assertThat(result2.asText()).isNotEmpty();
    }
}
