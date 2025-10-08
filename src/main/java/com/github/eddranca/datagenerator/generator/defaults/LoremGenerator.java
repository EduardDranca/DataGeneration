package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;

import java.util.List;
import java.util.Map;

public class LoremGenerator implements Generator {
    private static final String WORD = "word";
    private static final String WORDS = "words";
    private static final String SENTENCE = "sentence";
    private static final String SENTENCES = "sentences";
    private static final String PARAGRAPH = "paragraph";
    private static final String PARAGRAPHS = "paragraphs";

    @Override
    public JsonNode generate(GeneratorContext context) {
        Faker faker = context.faker();
        ObjectMapper mapper = context.mapper();
        JsonNode options = context.options();
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

        // Use syntactic sugar methods for option retrieval
        if (options.has(WORDS)) {
            int wordCount = context.getIntOption(WORDS, 5);
            List<String> words = faker.lorem().words(Math.max(1, wordCount));
            return mapper.valueToTree(String.join(" ", words));
        }

        if (options.has(SENTENCES)) {
            int sentenceCount = context.getIntOption(SENTENCES, 1);
            List<String> sentences = faker.lorem().sentences(Math.max(1, sentenceCount));
            return mapper.valueToTree(String.join(" ", sentences));
        }

        if (options.has(PARAGRAPHS)) {
            int paragraphCount = context.getIntOption(PARAGRAPHS, 1);
            List<String> paragraphs = faker.lorem().paragraphs(Math.max(1, paragraphCount));
            return mapper.valueToTree(String.join("\n\n", paragraphs));
        }

        // Default fallback
        return mapper.valueToTree(faker.lorem().sentence());
    }
}
