package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BookGeneratorTest {

    private BookGenerator generator;
    private JsonNode options;

    @BeforeEach
    void setUp() {
        Faker faker = new Faker();
        generator = new BookGenerator(faker);
        options = mock(ObjectNode.class);
    }

    @Test
    void testGenerate() {
        JsonNode result = generator.generate(options);

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

        BookGenerator gen1 = new BookGenerator(faker1);
        BookGenerator gen2 = new BookGenerator(faker2);

        JsonNode result1 = gen1.generate(options);
        JsonNode result2 = gen2.generate(options);

        assertThat(result1).isEqualTo(result2);
    }
}
