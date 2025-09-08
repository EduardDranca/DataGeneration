package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class UuidGeneratorTest {

    private UuidGenerator generator;
    private ObjectMapper mapper;
    private JsonNode options;

    @BeforeEach
    void setUp() {
        Faker faker = new Faker();
        generator = new UuidGenerator(faker);
        mapper = new ObjectMapper();
        options = mapper.createObjectNode();
    }

    @Test
    void testGenerateValidUuid() {
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText())
            .isNotNull()
            .isNotEmpty()
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void testGenerateMultipleUuids() {
        long count = IntStream.range(0, 100)
            .mapToObj(i -> generator.generate(options).asText())
            .distinct()
            .count();
        assertThat(count).isEqualTo(100);
    }

    @Test
    void testGenerateWithDifferentOptions() throws Exception {
        // UUID generator should ignore options and always generate valid UUIDs
        JsonNode customOptions = mapper.readTree("{\"someOption\": \"someValue\"}");

        JsonNode result = generator.generate(customOptions);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText())
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void testSeedConsistency() {
        // Create two generators with the same seed
        Faker faker1 = new Faker(new java.util.Random(123L));
        Faker faker2 = new Faker(new java.util.Random(123L));

        UuidGenerator gen1 = new UuidGenerator(faker1);
        UuidGenerator gen2 = new UuidGenerator(faker2);

        JsonNode result1 = gen1.generate(options);
        JsonNode result2 = gen2.generate(options);

        assertThat(result1.asText()).isEqualTo(result2.asText());
    }

    @Test
    void testNullOptions() {
        // Generator should handle null options gracefully
        JsonNode result = generator.generate(null);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
