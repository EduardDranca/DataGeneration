package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class NameGeneratorTest {

    private NameGenerator generator;
    private ObjectMapper mapper;
    private JsonNode options;

    @BeforeEach
    void setUp() {
        Faker faker = new Faker();
        generator = new NameGenerator(faker);
        mapper = new ObjectMapper();
        options = mapper.createObjectNode();
    }

    @Test
    void testGenerate() {
        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isObject()).isTrue();

        assertThat(result.has("firstName")).isTrue();
        assertThat(result.has("lastName")).isTrue();
        assertThat(result.has("fullName")).isTrue();
        assertThat(result.has("prefix")).isTrue();
        assertThat(result.has("suffix")).isTrue();
        assertThat(result.has("title")).isTrue();

        assertThat(result.get("firstName").asText()).isNotEmpty();
        assertThat(result.get("lastName").asText()).isNotEmpty();
        assertThat(result.get("fullName").asText()).isNotEmpty();
    }

    @Test
    void testGetFieldSuppliers() {
        Map<String, Supplier<JsonNode>> suppliers = generator.getFieldSuppliers(options);

        assertThat(suppliers)
            .isNotNull()
            .containsKeys("firstName", "lastName", "fullName", "prefix", "suffix", "title");

        // Test each supplier
        assertThat(suppliers.get("firstName").get().asText()).isNotNull();
        assertThat(suppliers.get("lastName").get().asText()).isNotNull();
        assertThat(suppliers.get("fullName").get().asText()).isNotNull();
        assertThat(suppliers.get("prefix").get().asText()).isNotNull();
        assertThat(suppliers.get("suffix").get().asText()).isNotNull();
        assertThat(suppliers.get("title").get().asText()).isNotNull();
    }

    @Test
    void testSeedConsistency() {
        Faker faker1 = new Faker(new Random(123L));
        Faker faker2 = new Faker(new Random(123L));

        NameGenerator gen1 = new NameGenerator(faker1);
        NameGenerator gen2 = new NameGenerator(faker2);

        JsonNode result1 = gen1.generate(options);
        JsonNode result2 = gen2.generate(options);

        assertThat(result1).isEqualTo(result2);
    }
}
