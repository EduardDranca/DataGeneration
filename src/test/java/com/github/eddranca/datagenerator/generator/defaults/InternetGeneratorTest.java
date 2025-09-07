package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InternetGeneratorTest {

    private InternetGenerator generator;
    private ObjectMapper mapper;
    private JsonNode options;

    @BeforeEach
    void setUp() {
        Faker faker = new Faker();
        generator = new InternetGenerator(faker);
        mapper = new ObjectMapper();
        options = mapper.createObjectNode();
    }

    @Test
    void testGenerate() {
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isObject()).isTrue();

        assertThat(result.has("emailAddress")).isTrue();
        assertThat(result.has("domainName")).isTrue();
        assertThat(result.has("url")).isTrue();

        String email = result.get("emailAddress").asText();
        assertThat(email).contains("@", ".");

        String url = result.get("url").asText();
        assertThat(url.contains(".") || url.startsWith("http")).isTrue();
    }

    @Test
    void testSeedConsistency() {
        Faker faker1 = new Faker(new java.util.Random(123L));
        Faker faker2 = new Faker(new java.util.Random(123L));

        InternetGenerator gen1 = new InternetGenerator(faker1);
        InternetGenerator gen2 = new InternetGenerator(faker2);

        JsonNode result1 = gen1.generate(options);
        JsonNode result2 = gen2.generate(options);

        assertThat(result1).isEqualTo(result2);
    }
}
