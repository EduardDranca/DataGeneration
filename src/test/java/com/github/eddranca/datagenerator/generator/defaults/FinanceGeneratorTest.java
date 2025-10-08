package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceGeneratorTest {

    private final FinanceGenerator generator = new FinanceGenerator();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Faker faker = new Faker();
    private JsonNode options;

    @BeforeEach
    void setUp() {
        options = mapper.createObjectNode();
    }

    @Test
    void testGenerate() {
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));

        assertThat(result).isNotNull();
        assertThat(result.isObject()).isTrue();

        assertThat(result.has("iban")).isTrue();
        assertThat(result.has("bic")).isTrue();
        assertThat(result.has("creditCard")).isTrue();

        assertThat(result.get("iban").asText()).isNotEmpty();
        assertThat(result.get("bic").asText()).isNotEmpty();
        assertThat(result.get("creditCard").asText()).isNotEmpty();
    }

    @Test
    void testSeedConsistency() {
        Faker faker1 = new Faker(new Random(123L));
        Faker faker2 = new Faker(new Random(123L));

        JsonNode result1 = generator.generate(new GeneratorContext(faker1, options, mapper));
        JsonNode result2 = generator.generate(new GeneratorContext(faker2, options, mapper));

        assertThat(result1).isEqualTo(result2);
    }
}
