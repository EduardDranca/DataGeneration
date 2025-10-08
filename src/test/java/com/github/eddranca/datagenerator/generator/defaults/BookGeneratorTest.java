package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BookGeneratorTest {

    private BookGenerator generator;
    private JsonNode options;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Faker faker = new Faker();

    @BeforeEach
    void setUp() {
        generator = new BookGenerator();
        options = mock(ObjectNode.class);
    }

    @Test
    void testGenerate() {
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));

        assertThat(result).isNotNull();
        assertThat(result.isObject()).isTrue();

        assertThat(result.has("title")).isTrue();
        assertThat(result.has("author")).isTrue();
        assertThat(result.has("publisher")).isTrue();
        assertThat(result.has("genre")).isTrue();

        assertThat(result.get("title").asText()).isNotEmpty();
        assertThat(result.get("author").asText()).isNotEmpty();
    }

    @Test
    void testSeedConsistency() {
        Faker faker1 = new Faker(new Random(123L));
        Faker faker2 = new Faker(new Random(123L));

        BookGenerator gen1 = new BookGenerator();
        BookGenerator gen2 = new BookGenerator();

        Object result1 = gen1.generate(new GeneratorContext(faker1, options, mapper));
        Object result2 = gen2.generate(new GeneratorContext(faker2, options, mapper));

        assertThat(result1).isEqualTo(result2);
    }
}
