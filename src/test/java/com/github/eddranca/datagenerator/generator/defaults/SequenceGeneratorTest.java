package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SequenceGeneratorTest {

    private SequenceGenerator generator;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        generator = new SequenceGenerator();
        mapper = new ObjectMapper();
    }

    @Test
    void testGenerateDefaultSequence() {
        JsonNode options = mapper.createObjectNode();
        JsonNode result1 = generator.generate(options);
        JsonNode result2 = generator.generate(options);
        JsonNode result3 = generator.generate(options);

        assertThat(result1).isNotNull();
        assertThat(result1.isInt()).isTrue();
        assertThat(result1.asInt()).isEqualTo(0);

        assertThat(result2).isNotNull();
        assertThat(result2.isInt()).isTrue();
        assertThat(result2.asInt()).isEqualTo(1);

        assertThat(result3).isNotNull();
        assertThat(result3.isInt()).isTrue();
        assertThat(result3.asInt()).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({
        "5, 3, 5, 8, 11",
        "-10, 2, -10, -8, -6",
        "0, 1, 0, 1, 2"
    })
    void testGenerateSequences(int start, int increment, int expected1, int expected2, int expected3) throws Exception {
        JsonNode options = mapper.readTree("{\"start\": " + start + ", \"increment\": " + increment + "}");
        
        JsonNode result1 = generator.generate(options);
        JsonNode result2 = generator.generate(options);
        JsonNode result3 = generator.generate(options);

        assertThat(result1).isNotNull();
        assertThat(result1.isInt()).isTrue();
        assertThat(result1.asInt()).isEqualTo(expected1);

        assertThat(result2).isNotNull();
        assertThat(result2.isInt()).isTrue();
        assertThat(result2.asInt()).isEqualTo(expected2);

        assertThat(result3).isNotNull();
        assertThat(result3.isInt()).isTrue();
        assertThat(result3.asInt()).isEqualTo(expected3);
    }

    @Test
    void testGenerateZeroIncrement() throws Exception {
        JsonNode options = mapper.readTree("{\"start\": 5, \"increment\": 0}");
        JsonNode result1 = generator.generate(options);
        JsonNode result2 = generator.generate(options);

        assertThat(result1).isNotNull();
        assertThat(result1.isInt()).isTrue();
        assertThat(result1.asInt()).isEqualTo(5);

        assertThat(result2).isNotNull();
        assertThat(result2.isInt()).isTrue();
        assertThat(result2.asInt()).isEqualTo(5);
    }

    @Test
    void testGenerateNegativeIncrement() throws Exception {
        JsonNode options = mapper.readTree("{\"start\": 10, \"increment\": -2}");
        JsonNode result1 = generator.generate(options);
        JsonNode result2 = generator.generate(options);
        JsonNode result3 = generator.generate(options);

        assertThat(result1).isNotNull();
        assertThat(result1.isInt()).isTrue();
        assertThat(result1.asInt()).isEqualTo(10);

        assertThat(result2).isNotNull();
        assertThat(result2.isInt()).isTrue();
        assertThat(result2.asInt()).isEqualTo(8);

        assertThat(result3).isNotNull();
        assertThat(result3.isInt()).isTrue();
        assertThat(result3.asInt()).isEqualTo(6);
    }

    @Test
    void testMultipleFieldsHaveIndependentSequences() throws Exception {
        // Each field should have its own sequence counter
        JsonNode options1 = mapper.readTree("{\"start\": 0, \"increment\": 2}");
        JsonNode options2 = mapper.readTree("{\"start\": 1, \"increment\": 3}");

        // Generate values for first field
        JsonNode result1a = generator.generate(options1);
        JsonNode result1b = generator.generate(options1);

        // Generate values for second field
        JsonNode result2a = generator.generate(options2);
        JsonNode result2b = generator.generate(options2);

        // First field sequence: 0, 2, 4, ...
        assertThat(result1a.asInt()).isEqualTo(0);
        assertThat(result1b.asInt()).isEqualTo(2);

        // Second field sequence: 1, 4, 7, ...
        assertThat(result2a.asInt()).isEqualTo(1);
        assertThat(result2b.asInt()).isEqualTo(4);
    }

    @Test
    void testSequenceWithMissingStart() throws Exception {
        JsonNode options = mapper.readTree("{\"increment\": 5}");
        JsonNode result1 = generator.generate(options);
        JsonNode result2 = generator.generate(options);

        assertThat(result1).isNotNull();
        assertThat(result1.isInt()).isTrue();
        assertThat(result1.asInt()).isEqualTo(0); // Default start is 0

        assertThat(result2).isNotNull();
        assertThat(result2.isInt()).isTrue();
        assertThat(result2.asInt()).isEqualTo(5);
    }

    @Test
    void testSequenceWithMissingIncrement() throws Exception {
        JsonNode options = mapper.readTree("{\"start\": 3}");
        JsonNode result1 = generator.generate(options);
        JsonNode result2 = generator.generate(options);

        assertThat(result1).isNotNull();
        assertThat(result1.isInt()).isTrue();
        assertThat(result1.asInt()).isEqualTo(3);

        assertThat(result2).isNotNull();
        assertThat(result2.isInt()).isTrue();
        assertThat(result2.asInt()).isEqualTo(4); // Default increment is 1
    }
}
