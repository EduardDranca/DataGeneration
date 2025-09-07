package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class AddressGeneratorTest {

    private AddressGenerator generator;
    private ObjectMapper mapper;
    private JsonNode options;

    @BeforeEach
    void setUp() {
        Faker faker = new Faker();
        generator = new AddressGenerator(faker);
        mapper = new ObjectMapper();
        options = mapper.createObjectNode();
    }

    @Test
    void testGenerate() {
        JsonNode result = generator.generate(options);

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
    void testGetFieldSuppliers() {
        Map<String, Supplier<JsonNode>> suppliers = generator.getFieldSuppliers(options);

        assertThat(suppliers)
            .isNotNull()
            .containsKeys("streetAddress", "city", "state", "zipCode", "country", "countryCode", "fullAddress");

        // Test each supplier produces non-empty values
        assertThat(suppliers.get("streetAddress").get().asText()).isNotEmpty();
        assertThat(suppliers.get("city").get().asText()).isNotEmpty();
        assertThat(suppliers.get("country").get().asText()).isNotEmpty();
    }

    @Test
    void testSeedConsistency() {
        Faker faker1 = new Faker(new java.util.Random(123L));
        Faker faker2 = new Faker(new java.util.Random(123L));

        AddressGenerator gen1 = new AddressGenerator(faker1);
        AddressGenerator gen2 = new AddressGenerator(faker2);

        JsonNode result1 = gen1.generate(options);
        JsonNode result2 = gen2.generate(options);

        assertThat(result1).isEqualTo(result2);
    }
}
