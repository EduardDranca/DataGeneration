package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.exception.DataGenerationException;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvGeneratorTest {

    private final CsvGenerator csvGenerator = new CsvGenerator();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File testCsv = new File("src/test/resources/test.csv");
    private final Faker faker = new Faker();

    @Test
    void testGenerateSequential() {
        ObjectNode options = objectMapper.createObjectNode();
        options.put("file", testCsv.getAbsolutePath());
        options.put("sequential", true);

        JsonNode result1 = csvGenerator.generate(new GeneratorContext(faker, options, objectMapper));
        assertThat(result1.get("header1").asText()).isEqualTo("value1");
        assertThat(result1.get("header2").asText()).isEqualTo("value2");

        JsonNode result2 = csvGenerator.generate(new GeneratorContext(faker, options, objectMapper));
        assertThat(result2.get("header1").asText()).isEqualTo("value3");
        assertThat(result2.get("header2").asText()).isEqualTo("value4");

        JsonNode result3 = csvGenerator.generate(new GeneratorContext(faker, options, objectMapper));
        assertThat(result3.get("header1").asText()).isEqualTo("value1");
        assertThat(result3.get("header2").asText()).isEqualTo("value2");
    }

    @Test
    void testGenerateRandom() {
        ObjectNode options = objectMapper.createObjectNode();
        options.put("file", testCsv.getAbsolutePath());
        options.put("sequential", false);

        // Generate multiple results and verify they all have required headers
        List<JsonNode> results = IntStream.range(0, 10)
            .mapToObj(i -> csvGenerator.generate(new GeneratorContext(faker, options, objectMapper)))
            .toList();

        assertThat(results)
            .hasSize(10)
            .allMatch(result -> result != null && result.has("header1") && result.has("header2"));
    }

    @Test
    void testFileNotFound() {
        ObjectNode options = objectMapper.createObjectNode();
        options.put("file", "non_existent_file.csv");
        options.put("sequential", true);

        assertThatThrownBy(() -> csvGenerator.generate(new GeneratorContext(faker, options, objectMapper)))
            .isInstanceOf(DataGenerationException.class);
    }
}
