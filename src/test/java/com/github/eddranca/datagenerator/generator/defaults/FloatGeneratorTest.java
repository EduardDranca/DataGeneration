package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class FloatGeneratorTest {

    private final FloatGenerator generator = new FloatGenerator();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Faker faker = new Faker();

    @Test
    void testGenerateDefault() {
        JsonNode options = mapper.createObjectNode();
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();

        double value = result.asDouble();
        // Default range is Integer.MIN_VALUE to Integer.MAX_VALUE
        assertThat(value).isBetween((double) Integer.MIN_VALUE, (double) Integer.MAX_VALUE);
    }

    @Test
    void testGenerateWithRange() throws Exception {
        JsonNode options = mapper.readTree("{\"min\": 1, \"max\": 3}");
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();

        double value = result.asDouble();
        // Range should be between 1 and 3 (inclusive)
        assertThat(value).isBetween(1.0, 3.0);
    }

    @Test
    void testGenerateWithDecimals() throws Exception {
        JsonNode options = mapper.readTree("{\"min\": 1, \"max\": 2, \"decimals\": 2}");
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));

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
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));

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
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));

        assertThat(result).isNotNull();
        assertThat(result.isNumber()).isTrue();
        assertThat(result.asDouble()).isBetween(1.0, 2.0);
    }

    @Test
    void testGenerateWithInvalidDecimals() throws Exception {
        // Test negative decimals (should default to 0)
        JsonNode options1 = mapper.readTree("{\"min\": 1, \"max\": 2, \"decimals\": -1}");
        JsonNode result1 = generator.generate(new GeneratorContext(faker, options1, mapper));
        assertThat(result1).isNotNull();
        assertThat(result1.isNumber()).isTrue();

        // Test very high decimals (should be capped at 10)
        JsonNode options2 = mapper.readTree("{\"min\": 1, \"max\": 2, \"decimals\": 15}");
        JsonNode result2 = generator.generate(new GeneratorContext(faker, options2, mapper));
        assertThat(result2).isNotNull();
        assertThat(result2.isNumber()).isTrue();
    }

    @Test
    void testSeedConsistency() throws Exception {
        JsonNode options = mapper.readTree("{\"min\": 1, \"max\": 10}");

        Faker faker1 = new Faker(new Random(123L));
        Faker faker2 = new Faker(new Random(123L));

        JsonNode result1 = generator.generate(new GeneratorContext(faker1, options, mapper));
        JsonNode result2 = generator.generate(new GeneratorContext(faker2, options, mapper));

        assertThat(result1.asDouble()).isEqualTo(result2.asDouble());
    }
}
