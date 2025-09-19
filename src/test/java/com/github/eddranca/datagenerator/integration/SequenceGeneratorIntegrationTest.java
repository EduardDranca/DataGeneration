package com.github.eddranca.datagenerator.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.IGeneration;
import com.github.eddranca.datagenerator.Generation;
import com.github.eddranca.datagenerator.ParameterizedGenerationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the sequence generator functionality.
 */
class SequenceGeneratorIntegrationTest extends ParameterizedGenerationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void testSequenceGeneratorBasicFunctionality() throws Exception {
        JsonNode dsl = mapper.readTree("""
                {
                    "items": {
                        "count": 5,
                        "item": {
                            "id": {"gen": "sequence", "start": 0, "increment": 2}
                        }
                    }
                }
                """);

        IGeneration generation = generateFromDsl(dsl, false);

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        assertThat(collections).containsKey("items");

        assertThat(collections.get("items"))
            .hasSize(5)
            .extracting(m -> m.get("id").intValue())
            .containsExactly(0, 2, 4, 6, 8);
    }

    @Test
    void testMultipleFieldsHaveIndependentSequences() throws Exception {
        JsonNode dsl = mapper.readTree("""
                {
                    "items": {
                        "count": 3,
                        "item": {
                            "evenNumbers": {"gen": "sequence", "start": 0, "increment": 2},
                            "oddNumbers": {"gen": "sequence", "start": 1, "increment": 2},
                            "negativeNumbers": {"gen": "sequence", "start": -5, "increment": -1}
                        }
                    }
                }
                """);

        IGeneration generation = generateFromDslWithSeed(dsl, 456L, false);

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        assertThat(collections).containsKey("items");
        assertThat(collections.get("items")).hasSize(3);

        List<JsonNode> items = collections.get("items");

        // Verify even numbers sequence: 0, 2, 4
        assertThat(items.get(0).get("evenNumbers").intValue()).isEqualTo(0);
        assertThat(items.get(1).get("evenNumbers").intValue()).isEqualTo(2);
        assertThat(items.get(2).get("evenNumbers").intValue()).isEqualTo(4);

        // Verify odd numbers sequence: 1, 3, 5
        assertThat(items.get(0).get("oddNumbers").intValue()).isEqualTo(1);
        assertThat(items.get(1).get("oddNumbers").intValue()).isEqualTo(3);
        assertThat(items.get(2).get("oddNumbers").intValue()).isEqualTo(5);

        // Verify negative numbers sequence: -5, -6, -7
        assertThat(items.get(0).get("negativeNumbers").intValue()).isEqualTo(-5);
        assertThat(items.get(1).get("negativeNumbers").intValue()).isEqualTo(-6);
        assertThat(items.get(2).get("negativeNumbers").intValue()).isEqualTo(-7);
    }

    @Test
    void testSequenceGeneratorWithDefaultValues() throws Exception {
        JsonNode dsl = mapper.readTree("""
                {
                    "items": {
                        "count": 4,
                        "item": {
                            "id": {"gen": "sequence"}
                        }
                    }
                }
                """);

        IGeneration generation = generateFromDslWithSeed(dsl, 789L, false);;

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        assertThat(collections).containsKey("items");
        assertThat(collections.get("items")).hasSize(4);

        // Verify default sequence values: 0, 1, 2, 3 using extracting
        assertThat(collections.get("items"))
            .extracting(item -> item.get("id").intValue())
            .containsExactly(0, 1, 2, 3);
    }

    @Test
    void testSequenceGeneratorWithNegativeIncrement() throws Exception {
        JsonNode dsl = mapper.readTree("""
                {
                    "items": {
                        "count": 5,
                        "item": {
                            "id": {"gen": "sequence", "start": 10, "increment": -3}
                        }
                    }
                }
                """);

        IGeneration generation = generateFromDslWithSeed(dsl, 999L, false);;

        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        assertThat(collections).containsKey("items");
        assertThat(collections.get("items")).hasSize(5);

        // Verify sequence values: 10, 7, 4, 1, -2
        List<JsonNode> items = collections.get("items");
        assertThat(items.get(0).get("id").intValue()).isEqualTo(10);
        assertThat(items.get(1).get("id").intValue()).isEqualTo(7);
        assertThat(items.get(2).get("id").intValue()).isEqualTo(4);
        assertThat(items.get(3).get("id").intValue()).isEqualTo(1);
        assertThat(items.get(4).get("id").intValue()).isEqualTo(-2);
    }
}
