package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class UuidGeneratorTest {

    private final UuidGenerator generator = new UuidGenerator();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Faker faker = new Faker();
    private final JsonNode options = mapper.createObjectNode();

    @Test
    void testGenerateValidUuid() {
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));

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
            .mapToObj(i -> generator.generate(new GeneratorContext(faker, options, mapper)).asText())
            .distinct()
            .count();
        assertThat(count).isEqualTo(100);
    }

    @Test
    void testGenerateWithDifferentOptions() throws Exception {
        // UUID generator should ignore options and always generate valid UUIDs
        JsonNode customOptions = mapper.readTree("{\"someOption\": \"someValue\"}");

        JsonNode result = generator.generate(new GeneratorContext(faker, customOptions, mapper));

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText())
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void testSeedConsistency() {
        // Create two generators with the same seed
        Faker faker1 = new Faker(new Random(123L));
        Faker faker2 = new Faker(new Random(123L));

        JsonNode result1 = generator.generate(new GeneratorContext(faker1, options, mapper));
        JsonNode result2 = generator.generate(new GeneratorContext(faker2, options, mapper));

        assertThat(result1.asText()).isEqualTo(result2.asText());
    }

    @Test
    void testNullOptions() {
        // Generator should handle null options gracefully
        JsonNode result = generator.generate(new GeneratorContext(faker, null, mapper));

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
