package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyGeneratorTest {

    private final CompanyGenerator generator = new CompanyGenerator();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Faker faker = new Faker();
    private final JsonNode options = mapper.createObjectNode();

    @Test
    void testGenerate() {
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));

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

        Object result1 = generator.generate(new GeneratorContext(faker1, options, mapper));
        Object result2 = generator.generate(new GeneratorContext(faker2, options, mapper));

        assertThat(result1).isEqualTo(result2);
    }
}
