package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class InternetGeneratorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final InternetGenerator generator = new InternetGenerator();
    private final JsonNode options = mapper.createObjectNode();
    private final Faker faker = new Faker();

    @Test
    void testGenerate() {
        JsonNode result = generator.generate(new GeneratorContext(faker, options, mapper));

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
        Faker faker1 = new Faker(new Random(123L));
        Faker faker2 = new Faker(new Random(123L));

        JsonNode result1 = generator.generate(new GeneratorContext(faker1, options, mapper));
        JsonNode result2 = generator.generate(new GeneratorContext(faker2, options, mapper));

        assertThat(result1).isEqualTo(result2);
    }
}
