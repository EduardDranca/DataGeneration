package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceGeneratorTest {

    private FinanceGenerator generator;
    private ObjectMapper mapper;
    private JsonNode options;

    @BeforeEach
    void setUp() {
        Faker faker = new Faker();
        generator = new FinanceGenerator(faker);
        mapper = new ObjectMapper();
        options = mapper.createObjectNode();
    }

    @Test
    void testGenerate() {
        JsonNode result = generator.generate(options);

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
        Faker faker1 = new Faker(new java.util.Random(123L));
        Faker faker2 = new Faker(new java.util.Random(123L));

        FinanceGenerator gen1 = new FinanceGenerator(faker1);
        FinanceGenerator gen2 = new FinanceGenerator(faker2);

        JsonNode result1 = gen1.generate(options);
        JsonNode result2 = gen2.generate(options);

        assertThat(result1).isEqualTo(result2);
    }
}
