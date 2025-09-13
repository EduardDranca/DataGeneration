package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import net.datafaker.Faker;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class LoremGenerator implements Generator {
    private static final String WORD = "word";
    private static final String WORDS = "words";
    private static final String SENTENCE = "sentence";
    private static final String SENTENCES = "sentences";
    private static final String PARAGRAPH = "paragraph";
    private static final String PARAGRAPHS = "paragraphs";
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
                    WORD, faker.lorem().word(),
                    WORDS, String.join(" ", faker.lorem().words(5)),
                    SENTENCE, faker.lorem().sentence(),
                    SENTENCES, String.join(" ", faker.lorem().sentences(3)),
                    PARAGRAPH, faker.lorem().paragraph(),
                    PARAGRAPHS, String.join("\n\n", faker.lorem().paragraphs(2))
                )
            );
        }

        if (options.has(WORDS)) {
            int wordCount = options.get(WORDS).asInt(5);
            List<String> words = faker.lorem().words(Math.max(1, wordCount));
            return mapper.valueToTree(String.join(" ", words));
        }

        if (options.has(SENTENCES)) {
            int sentenceCount = options.get(SENTENCES).asInt(1);
            List<String> sentences = faker.lorem().sentences(Math.max(1, sentenceCount));
            return mapper.valueToTree(String.join(" ", sentences));
        }

        if (options.has(PARAGRAPHS)) {
            int paragraphCount = options.get(PARAGRAPHS).asInt(1);
            List<String> paragraphs = faker.lorem().paragraphs(Math.max(1, paragraphCount));
            return mapper.valueToTree(String.join("\n\n", paragraphs));
        }

        // Default fallback
        return mapper.valueToTree(faker.lorem().sentence());
    }

    @Override
    public Map<String, Supplier<JsonNode>> getFieldSuppliers(JsonNode options) {
        return Map.of(
            WORD, () -> mapper.valueToTree(faker.lorem().word()),
            WORDS, () -> mapper.valueToTree(String.join(" ", faker.lorem().words(5))),
            SENTENCE, () -> mapper.valueToTree(faker.lorem().sentence()),
            SENTENCES, () -> mapper.valueToTree(String.join(" ", faker.lorem().sentences(3))),
            PARAGRAPH, () -> mapper.valueToTree(faker.lorem().paragraph()),
            PARAGRAPHS, () -> mapper.valueToTree(String.join("\n\n", faker.lorem().paragraphs(2)))
        );
    }

    @Override
    public JsonNode generateWithFilter(JsonNode options, List<JsonNode> filterValues) {
        return generate(options);
    }
}
