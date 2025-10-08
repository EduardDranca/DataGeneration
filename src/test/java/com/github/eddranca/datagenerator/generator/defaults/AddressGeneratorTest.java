package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class AddressGeneratorTest {

    private AddressGenerator generator;
    private ObjectMapper mapper;
    private Faker faker;
    private JsonNode options;

    @BeforeEach
    void setUp() {
        faker = new Faker();
        generator = new AddressGenerator();
        mapper = new ObjectMapper();
        options = mapper.createObjectNode();
    }

    @Test
    void testGenerate() {
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));

        assertThat(result).isNotNull();
        assertThat(result.isObject()).isTrue();

        assertThat(result.has("streetAddress")).isTrue();
        assertThat(result.has("city")).isTrue();
        assertThat(result.has("state")).isTrue();
        assertThat(result.has("zipCode")).isTrue();
        assertThat(result.has("country")).isTrue();
        assertThat(result.has("countryCode")).isTrue();
        assertThat(result.has("fullAddress")).isTrue();

        assertThat(result.get("streetAddress").asText()).isNotEmpty();
        assertThat(result.get("city").asText()).isNotEmpty();
        assertThat(result.get("country").asText()).isNotEmpty();
    }

    @Test
    void testSeedConsistency() {
        Faker faker1 = new Faker(new Random(123L));
        Faker faker2 = new Faker(new Random(123L));

        AddressGenerator gen1 = new AddressGenerator();
        AddressGenerator gen2 = new AddressGenerator();

        JsonNode result1 = gen1.generate(new GeneratorContext(faker1, options, mapper));
        JsonNode result2 = gen2.generate(new GeneratorContext(faker2, options, mapper));

        assertThat(result1).isEqualTo(result2);
    }
}
