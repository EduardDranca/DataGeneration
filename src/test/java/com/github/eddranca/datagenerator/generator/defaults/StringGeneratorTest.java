package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class StringGeneratorTest {

    private StringGenerator generator;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        Faker faker = new Faker();
        generator = new StringGenerator(faker);
        mapper = new ObjectMapper();
    }

    @Test
    void testGenerateDefaultLength() {
        JsonNode options = mapper.createObjectNode();
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        // Default is random between 1 and 20, so just check it's in range
        assertThat(result.asText())
            .hasSizeBetween(1, 20)
            .matches("[a-zA-Z0-9]+");
    }

    @Test
    void testGenerateCustomLength() throws Exception {
        JsonNode options = mapper.readTree("{\"length\": 5}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText())
            .hasSize(5)
            .matches("[a-zA-Z0-9]+");
    }

    @Test
    void testGenerateLongString() throws Exception {
        JsonNode options = mapper.readTree("{\"length\": 100}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();

        String value = result.asText();
        assertThat(value)
            .hasSize(100)
            .matches("[a-zA-Z0-9]+");
    }

    @Test
    void testGenerateZeroLength() throws Exception {
        JsonNode options = mapper.readTree("{\"length\": 0}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isEmpty();
    }

    @Test
    void testGenerateNegativeLength() throws Exception {
        JsonNode options = mapper.readTree("{\"length\": -5}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).matches("[a-zA-Z0-9]*");
    }

    @Test
    void testGenerateWithInvalidLengthType() throws Exception {
        JsonNode options = mapper.readTree("{\"length\": \"invalid\"}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).matches("[a-zA-Z0-9]+");
    }

    @Test
    void testGenerateWithMinMaxLength() throws Exception {
        JsonNode options = mapper.readTree("{\"minLength\": 5, \"maxLength\": 10}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText())
            .hasSizeBetween(5, 10)
            .matches("[a-zA-Z0-9]+");
    }

    @Test
    void testGenerateWithCustomAllowedChars() throws Exception {
        JsonNode options = mapper.readTree("{\"length\": 10, \"allowedChars\": \"ABC\"}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText())
            .hasSize(10)
            .matches("[ABC]+");
    }

    @Test
    void testCharacterSet() throws Exception {
        JsonNode options = mapper.readTree("{\"length\": 1000}");
        JsonNode result = generator.generate(options);

        String value = result.asText();
        assertThat(value.chars())
            .as("String should contain letters")
            .anyMatch(Character::isLetter);
        assertThat(value.chars())
            .as("String should contain digits")
            .anyMatch(Character::isDigit);
        assertThat(value.chars())
            .as("String should contain uppercase letters")
            .anyMatch(Character::isUpperCase);
        assertThat(value.chars())
            .as("String should contain lowercase letters")
            .anyMatch(Character::isLowerCase);
    }

    @Test
    void testVeryLargeLength() throws Exception {
        JsonNode options = mapper.readTree("{\"length\": 10000}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText())
            .hasSize(10000)
            .matches("[a-zA-Z0-9]+");
    }

    @Test
    void testSeedConsistency() throws Exception {
        JsonNode options = mapper.readTree("{\"length\": 15}");

        // Create two generators with the same seed
        Faker faker1 = new Faker(new Random(123L));
        Faker faker2 = new Faker(new Random(123L));

        StringGenerator gen1 = new StringGenerator(faker1);
        StringGenerator gen2 = new StringGenerator(faker2);

        JsonNode result1 = gen1.generate(options);
        JsonNode result2 = gen2.generate(options);

        assertThat(result1.asText()).isEqualTo(result2.asText());
    }

    @Test
    void testGenerateWithRegex() throws Exception {
        JsonNode options = mapper.readTree("{\"regex\": \"[a-z]{5}[0-9]{5}\"}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();

        String value = result.asText();
        assertThat(value)
            .as("Generated string should match regex [a-z]{5}[0-9]{5}")
            .matches("[a-z]{5}[0-9]{5}");
    }
}
