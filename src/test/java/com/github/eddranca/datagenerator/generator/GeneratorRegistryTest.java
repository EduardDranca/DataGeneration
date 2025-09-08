package com.github.eddranca.datagenerator.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratorRegistryTest {

    private GeneratorRegistry registry;
    private Faker faker;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        faker = new Faker();
        registry = GeneratorRegistry.withDefaultGenerators(faker);
        mapper = new ObjectMapper();
    }

    @Test
    void testDefaultGeneratorsRegistered() {
        // Test that all default generators are registered
        assertThat(registry.get("uuid")).isNotNull();
        assertThat(registry.get("string")).isNotNull();
        assertThat(registry.get("number")).isNotNull();
        assertThat(registry.get("float")).isNotNull();
        assertThat(registry.get("name")).isNotNull();
        assertThat(registry.get("company")).isNotNull();
        assertThat(registry.get("address")).isNotNull();
        assertThat(registry.get("internet")).isNotNull();
        assertThat(registry.get("country")).isNotNull();
        assertThat(registry.get("book")).isNotNull();
        assertThat(registry.get("finance")).isNotNull();
        assertThat(registry.get("sequence")).isNotNull();
    }

    @Test
    void testGetGenerator() {
        Generator uuidGenerator = registry.get("uuid");
        assertThat(uuidGenerator).isNotNull();

        JsonNode options = mapper.createObjectNode();
        JsonNode result = uuidGenerator.generate(options);
        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void testGetSequenceGenerator() throws Exception {
        Generator sequenceGenerator = registry.get("sequence");
        assertThat(sequenceGenerator).isNotNull();

        JsonNode options = mapper.readTree("{\"start\": 0, \"increment\": 2}");
        JsonNode result1 = sequenceGenerator.generate(options);
        JsonNode result2 = sequenceGenerator.generate(options);
        JsonNode result3 = sequenceGenerator.generate(options);

        assertThat(result1).isNotNull();
        assertThat(result1.isInt()).isTrue();
        assertThat(result1.asInt()).isEqualTo(0);

        assertThat(result2).isNotNull();
        assertThat(result2.isInt()).isTrue();
        assertThat(result2.asInt()).isEqualTo(2);

        assertThat(result3).isNotNull();
        assertThat(result3.isInt()).isTrue();
        assertThat(result3.asInt()).isEqualTo(4);
    }

    @Test
    void testGetNonExistentGenerator() {
        Generator nonExistent = registry.get("nonexistent");
        assertThat(nonExistent).isNull();
    }

    @Test
    void testRegisterCustomGenerator() {
        Generator customGenerator = options -> mapper.valueToTree("CUSTOM_VALUE");

        registry.register("custom", customGenerator);

        Generator retrieved = registry.get("custom");
        assertThat(retrieved).isNotNull();

        JsonNode options = mapper.createObjectNode();
        JsonNode result = retrieved.generate(options);
        assertThat(result.asText()).isEqualTo("CUSTOM_VALUE");
    }

    @Test
    void testOverrideExistingGenerator() {
        Generator customUuidGenerator = options -> mapper.valueToTree("CUSTOM_UUID");

        // Override existing uuid generator
        registry.register("uuid", customUuidGenerator);

        Generator retrieved = registry.get("uuid");
        JsonNode options = mapper.createObjectNode();
        JsonNode result = retrieved.generate(options);
        assertThat(result.asText()).isEqualTo("CUSTOM_UUID");
    }
}
