package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import net.datafaker.Faker;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class LoremGenerator implements Generator {
    private final Faker faker;
    private final ObjectMapper mapper;

    public LoremGenerator(Faker faker) {
        this.faker = faker;
        this.mapper = new ObjectMapper();
    }

    @Override
    public JsonNode generate(JsonNode options) {
        if (options == null) {
            // Default: return all available fields
            return mapper.valueToTree(
                Map.of(
                    "word", faker.lorem().word(),
                    "words", String.join(" ", faker.lorem().words(5)),
                    "sentence", faker.lorem().sentence(),
                    "sentences", String.join(" ", faker.lorem().sentences(3)),
                    "paragraph", faker.lorem().paragraph(),
                    "paragraphs", String.join("\n\n", faker.lorem().paragraphs(2))
                )
            );
        }

        if (options.has("words")) {
            int wordCount = options.get("words").asInt(5);
            List<String> words = faker.lorem().words(Math.max(1, wordCount));
            return mapper.valueToTree(String.join(" ", words));
        }

        if (options.has("sentences")) {
            int sentenceCount = options.get("sentences").asInt(1);
            List<String> sentences = faker.lorem().sentences(Math.max(1, sentenceCount));
            return mapper.valueToTree(String.join(" ", sentences));
        }

        if (options.has("paragraphs")) {
            int paragraphCount = options.get("paragraphs").asInt(1);
            List<String> paragraphs = faker.lorem().paragraphs(Math.max(1, paragraphCount));
            return mapper.valueToTree(String.join("\n\n", paragraphs));
        }

        // Default fallback
        return mapper.valueToTree(faker.lorem().sentence());
    }

    @Override
    public Map<String, Supplier<JsonNode>> getFieldSuppliers(JsonNode options) {
        return Map.of(
                "word", () -> mapper.valueToTree(faker.lorem().word()),
                "words", () -> mapper.valueToTree(String.join(" ", faker.lorem().words(5))),
                "sentence", () -> mapper.valueToTree(faker.lorem().sentence()),
                "sentences", () -> mapper.valueToTree(String.join(" ", faker.lorem().sentences(3))),
                "paragraph", () -> mapper.valueToTree(faker.lorem().paragraph()),
                "paragraphs", () -> mapper.valueToTree(String.join("\n\n", faker.lorem().paragraphs(2)))
        );
    }

    @Override
    public JsonNode generateWithFilter(JsonNode options, List<JsonNode> filterValues) {
        return generate(options);
    }
}
