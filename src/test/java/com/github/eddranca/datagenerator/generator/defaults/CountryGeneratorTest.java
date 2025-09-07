package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CountryGeneratorTest {

    private CountryGenerator generator;
    private ObjectMapper mapper;
    private JsonNode options;

    @BeforeEach
    void setUp() {
        Faker faker = new Faker();
        generator = new CountryGenerator(faker);
        mapper = new ObjectMapper();
        options = mapper.createObjectNode();
    }

    @Test
    void testGenerate() {
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isObject()).isTrue();

        assertThat(result.has("name")).isTrue();
        assertThat(result.has("countryCode")).isTrue();
        assertThat(result.has("capital")).isTrue();
        assertThat(result.has("currency")).isTrue();
        assertThat(result.has("currencyCode")).isTrue();

        assertThat(result.get("name").asText()).isNotEmpty();
        assertThat(result.get("countryCode").asText()).isNotEmpty();
    }

    @Test
    void testSeedConsistency() {
        Faker faker1 = new Faker(new java.util.Random(123L));
        Faker faker2 = new Faker(new java.util.Random(123L));

        CountryGenerator gen1 = new CountryGenerator(faker1);
        CountryGenerator gen2 = new CountryGenerator(faker2);

        JsonNode result1 = gen1.generate(options);
        JsonNode result2 = gen2.generate(options);

        assertThat(result1).isEqualTo(result2);
    }
}
