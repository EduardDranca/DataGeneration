package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.exception.FilteringException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChoiceGeneratorTest {
    private ObjectMapper mapper;
    private ChoiceGenerator generator;
    private Random random;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        random = new Random(42);
        generator = new ChoiceGenerator(random);
    }

    @Test
    void testGenerateWithValidOptions() {
        ObjectNode options = mapper.createObjectNode();
        ArrayNode optionsArray = options.putArray("options");
        optionsArray.add("option1");
        optionsArray.add("option2");
        optionsArray.add("option3");

        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isIn("option1", "option2", "option3");
    }

    @Test
    void testGenerateWithDifferentDataTypes() {
        ObjectNode options = mapper.createObjectNode();
        ArrayNode optionsArray = options.putArray("options");
        optionsArray.add("string_option");
        optionsArray.add(42);
        optionsArray.add(true);
        optionsArray.add(mapper.nullNode());

        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(options.get("options"))
            .as("Result should match one of the provided options")
            .contains(result);
    }

    @Test
    void testSupportsFiltering() {
        assertThat(generator.supportsFiltering()).isTrue();
    }

    @Test
    void testGenerateAtPath() {
        ObjectNode options = mapper.createObjectNode();
        ArrayNode optionsArray = options.putArray("options");
        optionsArray.add("option1");
        optionsArray.add("option2");
        optionsArray.add("option3");

        // For choice generators, generateAtPath should behave the same as generate
        JsonNode result = generator.generateAtPath(options, "someField");

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isIn("option1", "option2", "option3");
    }

    @Test
    void testGetFieldSuppliers() {
        assertThat(generator.getFieldSuppliers(null)).isEmpty();
    }

    @Test
    void testDeterministicBehaviorWithSeed() {
        // Test that the same seed produces the same results
        Random random1 = new Random(123);
        Random random2 = new Random(123);

        ChoiceGenerator generator1 = new ChoiceGenerator(random1);
        ChoiceGenerator generator2 = new ChoiceGenerator(random2);

        ObjectNode options = mapper.createObjectNode();
        ArrayNode optionsArray = options.putArray("options");
        optionsArray.add("option1");
        optionsArray.add("option2");
        optionsArray.add("option3");

        JsonNode result1 = generator1.generate(options);
        JsonNode result2 = generator2.generate(options);

        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void testGenerateWithSingleOption() {
        ObjectNode options = mapper.createObjectNode();
        ArrayNode optionsArray = options.putArray("options");
        optionsArray.add("only_option");

        JsonNode result = generator.generate(options);

        assertThat(result).isNotNull();
        assertThat(result.asText()).isEqualTo("only_option");
    }

    @Test
    void testGenerateWithFilterNoFiltering() {
        ObjectNode options = mapper.createObjectNode();
        ArrayNode optionsArray = options.putArray("options");
        optionsArray.add("option1");
        optionsArray.add("option2");
        optionsArray.add("option3");

        JsonNode result = generator.generateWithFilter(options, null);

        assertThat(result).isNotNull();
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isIn("option1", "option2", "option3");
    }

    @Test
    void testGenerateWithFilterSomeFiltered() {
        ObjectNode options = mapper.createObjectNode();
        ArrayNode optionsArray = options.putArray("options");
        optionsArray.add("option1");
        optionsArray.add("option2");
        optionsArray.add("option3");

        List<JsonNode> filterValues = Arrays.asList(
            mapper.valueToTree("option1"),
            mapper.valueToTree("option2")
        );

        JsonNode result = generator.generateWithFilter(options, filterValues);

        assertThat(result).isNotNull();
        assertThat(result.asText()).isEqualTo("option3");
    }

    @Test
    void testGenerateWithFilterAllFiltered() {
        ObjectNode options = mapper.createObjectNode();
        ArrayNode optionsArray = options.putArray("options");
        optionsArray.add("option1");
        optionsArray.add("option2");
        optionsArray.add("option3");

        List<JsonNode> filterValues = Arrays.asList(
            mapper.valueToTree("option1"),
            mapper.valueToTree("option2"),
            mapper.valueToTree("option3")
        );

        assertThatThrownBy(() -> generator.generateWithFilter(options, filterValues))
            .isInstanceOf(FilteringException.class)
            .hasMessage("All choice options were filtered out");
    }

    @Test
    void testGenerateWithWeights() {
        ObjectNode options = mapper.createObjectNode();
        ArrayNode optionsArray = options.putArray("options");
        optionsArray.add("option1");
        optionsArray.add("option2");
        optionsArray.add("option3");

        ArrayNode weightsArray = options.putArray("weights");
        weightsArray.add(1.0);
        weightsArray.add(2.0);
        weightsArray.add(3.0);

        // Test multiple generations to verify weighted distribution
        int iterations = 1000;

        Map<String, Long> counts = IntStream.range(0, iterations)
            .mapToObj(i -> generator.generate(options).asText())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        assertThat(counts.get("option3"))
            .isGreaterThan(counts.get("option1"));
        assertThat(counts.get("option2"))
            .isGreaterThan(counts.get("option1"));
    }

    @Test
    void testGenerateWithWeightsAndFiltering() {
        ObjectNode options = mapper.createObjectNode();
        ArrayNode optionsArray = options.putArray("options");
        optionsArray.add("option1");
        optionsArray.add("option2");
        optionsArray.add("option3");

        ArrayNode weightsArray = options.putArray("weights");
        weightsArray.add(1.0);
        weightsArray.add(2.0);
        weightsArray.add(3.0);

        List<JsonNode> filterValues = Collections.singletonList(
            mapper.valueToTree("option1")
        );

        // Test multiple generations to verify weighted distribution among remaining options
        int iterations = 1000;

        Map<String, Long> counts = IntStream.range(0, iterations)
            .mapToObj(i -> generator.generateWithFilter(options, filterValues).asText())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // option1 should never appear (it's filtered)
        assertThat(counts).doesNotContainKey("option1");
        // option3 should appear more frequently than option2 (higher weight)
        assertThat(counts.get("option3")).isGreaterThan(counts.get("option2"));
    }
}
