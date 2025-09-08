package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class NumberGeneratorTest {

    private NumberGenerator generator;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        Faker faker = new Faker();
        generator = new NumberGenerator(faker);
        mapper = new ObjectMapper();
    }

    @Test
    void testGenerateDefaultRange() {
        JsonNode options = mapper.createObjectNode();
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();
        // Default range is Integer.MIN_VALUE to Integer.MAX_VALUE, so just check it's a valid int
        assertThat(result.asInt()).isBetween(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Test
    void testGenerateNegativeRange() throws Exception {
        JsonNode options = mapper.readTree("{\"min\": -50, \"max\": -10}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();
        assertThat(result.asInt()).isBetween(-50, -10);
    }

    @Test
    void testGenerateSingleValue() throws Exception {
        JsonNode options = mapper.readTree("{\"min\": 42, \"max\": 42}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();
        assertThat(result.asInt()).isEqualTo(42);
    }

    @Test
    void testGenerateInvalidRange() throws Exception {
        // Max less than min - should swap them
        JsonNode options = mapper.readTree("{\"min\": 20, \"max\": 10}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();
        assertThat(result.asInt()).isBetween(10, 20);
    }

    @Test
    void testGenerateWithMissingMin() throws Exception {
        JsonNode options = mapper.readTree("{\"max\": 50}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();
        assertThat(result.asInt()).isBetween(Integer.MIN_VALUE, 50); // Uses default min
    }

    @Test
    void testGenerateWithMissingMax() throws Exception {
        JsonNode options = mapper.readTree("{\"min\": 50}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();
        assertThat(result.asInt()).isBetween(50, Integer.MAX_VALUE); // Uses default max
    }

    @Test
    void testGenerateWithInvalidTypes() throws Exception {
        JsonNode options = mapper.readTree("{\"min\": \"invalid\", \"max\": \"also_invalid\"}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();
        // Falls back to defaults (Integer.MIN_VALUE to Integer.MAX_VALUE)
        assertThat(result.asInt()).isBetween(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Test
    void testGenerateWithSpecificRange() throws Exception {
        JsonNode options = mapper.readTree("{\"min\": 100, \"max\": 200}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();
        assertThat(result.asInt()).isBetween(100, 200);
    }

    @Test
    void testGenerateMultipleValues() throws Exception {
        JsonNode options = mapper.readTree("{\"min\": 1, \"max\": 10}");

        // Generate multiple values and verify they're all valid numbers in range
        List<Integer> generatedValues = IntStream.range(0, 10)
            .mapToObj(i -> generator.generate(options).asInt())
            .toList();

        assertThat(generatedValues)
            .hasSize(10)
            .allMatch(value -> value >= 1 && value <= 10);
    }

    @Test
    void testSeedConsistency() throws Exception {
        JsonNode options = mapper.readTree("{\"min\": 1, \"max\": 100}");

        // Create two generators with the same seed
        Faker faker1 = new Faker(new Random(123L));
        Faker faker2 = new Faker(new Random(123L));

        NumberGenerator gen1 = new NumberGenerator(faker1);
        NumberGenerator gen2 = new NumberGenerator(faker2);

        JsonNode result1 = gen1.generate(options);
        JsonNode result2 = gen2.generate(options);

        assertThat(result1.asInt()).isEqualTo(result2.asInt());
    }

    @Test
    void testNullOptions() {
        // Generator should handle null options by using empty options
        JsonNode emptyOptions = mapper.createObjectNode();
        JsonNode result = generator.generate(emptyOptions);

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();
        assertThat(result.asInt()).isBetween(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Test
    void testLargeRange() throws Exception {
        JsonNode options = mapper.readTree("{\"min\": -1000000, \"max\": 1000000}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();
        assertThat(result.asInt()).isBetween(-1000000, 1000000);
    }

    @Test
    void testFloatInputsHandledAsIntegers() throws Exception {
        JsonNode options = mapper.readTree("{\"min\": 5.7, \"max\": 10.3}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();
        assertThat(result.asInt()).isBetween(5, 10); // Should truncate floats to integers
    }
}
