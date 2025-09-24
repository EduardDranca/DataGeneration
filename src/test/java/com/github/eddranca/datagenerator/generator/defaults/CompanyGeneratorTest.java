package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyGeneratorTest {

    private CompanyGenerator generator;
    private ObjectMapper mapper;
    private JsonNode options;

    @BeforeEach
    void setUp() {
        Faker faker = new Faker();
        generator = new CompanyGenerator(faker);
        mapper = new ObjectMapper();
        options = mapper.createObjectNode();
    }

    @Test
    void testGenerate() {
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isObject()).isTrue();

        assertThat(result.has("name")).isTrue();
        assertThat(result.has("industry")).isTrue();
        assertThat(result.has("profession")).isTrue();
        assertThat(result.has("buzzword")).isTrue();

        assertThat(result.get("name").asText()).isNotEmpty();
        assertThat(result.get("industry").asText()).isNotEmpty();
    }

    @Test
    void testSeedConsistency() {
        Faker faker1 = new Faker(new Random(123L));
        Faker faker2 = new Faker(new Random(123L));

        CompanyGenerator gen1 = new CompanyGenerator(faker1);
        CompanyGenerator gen2 = new CompanyGenerator(faker2);

        JsonNode result1 = gen1.generate(options);
        JsonNode result2 = gen2.generate(options);

        assertThat(result1).isEqualTo(result2);
    }
}
