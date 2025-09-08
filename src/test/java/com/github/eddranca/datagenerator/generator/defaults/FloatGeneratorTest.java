package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FloatGeneratorTest {

    private FloatGenerator generator;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        Faker faker = new Faker();
        generator = new FloatGenerator(faker);
        mapper = new ObjectMapper();
    }

    @Test
    void testGenerateDefault() {
        JsonNode options = mapper.createObjectNode();
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();

        double value = result.asDouble();
        // Default range is Integer.MIN_VALUE to Integer.MAX_VALUE
        assertThat(value).isBetween((double) Integer.MIN_VALUE, (double) Integer.MAX_VALUE);
    }

    @Test
    void testGenerateWithRange() throws Exception {
        JsonNode options = mapper.readTree("{\"min\": 1, \"max\": 3}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();

        double value = result.asDouble();
        // Range should be between 1 and 3 (inclusive)
        assertThat(value).isBetween(1.0, 3.0);
    }

    @Test
    void testGenerateWithDecimals() throws Exception {
        JsonNode options = mapper.readTree("{\"min\": 1, \"max\": 2, \"decimals\": 2}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();

        double value = result.asDouble();
        assertThat(value).isBetween(1.0, 2.0);

        // Check decimal places (approximately)
        String valueStr = String.valueOf(value);
        int decimalIndex = valueStr.indexOf('.');
        if (decimalIndex != -1) {
            int decimalPlaces = valueStr.length() - decimalIndex - 1;
            assertThat(decimalPlaces).isLessThanOrEqualTo(2);
        }
    }

    @Test
    void testGenerateWithZeroDecimals() throws Exception {
        JsonNode options = mapper.readTree("{\"min\": 1, \"max\": 10, \"decimals\": 0}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();

        double value = result.asDouble();
        assertThat(value)
            .isBetween(1.0, 10.0)
            .isEqualTo(Math.floor(value));
    }

    @Test
    void testGenerateWithHighDecimals() throws Exception {
        JsonNode options = mapper.readTree("{\"min\": 1, \"max\": 2, \"decimals\": 5}");
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();
        assertThat(result.asDouble()).isBetween(1.0, 2.0);
    }

    @Test
    void testGenerateWithInvalidDecimals() throws Exception {
        // Test negative decimals (should default to 0)
        JsonNode options1 = mapper.readTree("{\"min\": 1, \"max\": 2, \"decimals\": -1}");
        JsonNode result1 = generator.generate(options1);
        assertThat(result1).isNotNull();
        assertThat(result1.isNumber()).isTrue();

        // Test very high decimals (should be capped at 10)
        JsonNode options2 = mapper.readTree("{\"min\": 1, \"max\": 2, \"decimals\": 15}");
        JsonNode result2 = generator.generate(options2);
        assertThat(result2).isNotNull();
        assertThat(result2.isNumber()).isTrue();
    }

    @Test
    void testSeedConsistency() throws Exception {
        JsonNode options = mapper.readTree("{\"min\": 1, \"max\": 10}");

        Faker faker1 = new Faker(new java.util.Random(123L));
        Faker faker2 = new Faker(new java.util.Random(123L));

        FloatGenerator gen1 = new FloatGenerator(faker1);
        FloatGenerator gen2 = new FloatGenerator(faker2);

        JsonNode result1 = gen1.generate(options);
        JsonNode result2 = gen2.generate(options);

        assertThat(result1.asDouble()).isEqualTo(result2.asDouble());
    }
}
