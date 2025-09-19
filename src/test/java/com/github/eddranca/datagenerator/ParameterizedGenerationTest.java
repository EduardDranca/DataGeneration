package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for parameterized tests that run with both normal and memory-optimized implementations.
 */
public abstract class ParameterizedGenerationTest {
    protected final ObjectMapper mapper = new ObjectMapper();

    /**
     * Provides test parameters for both normal and memory-optimized implementations.
     */
    public static Stream<Arguments> generationImplementations() {
        return Stream.of(
            Arguments.of("Normal", false),
            Arguments.of("Memory-Optimized", true)
        );
    }

    /**
     * Creates a generation builder with the specified memory optimization setting.
     */
    protected DslDataGenerator.Builder createGenerator(boolean memoryOptimized) {
        DslDataGenerator.Builder builder = DslDataGenerator.create().withSeed(123L);
        if (memoryOptimized) {
            builder = builder.withMemoryOptimization();
        }
        return builder;
    }

    /**
     * Generates data from DSL string with the specified memory optimization setting.
     */
    protected IGeneration generateFromDsl(String dsl, boolean memoryOptimized) throws IOException {
        return createGenerator(memoryOptimized)
            .fromJsonString(dsl)
            .generate();
    }

    /**
     * Generates data from DSL JsonNode with the specified memory optimization setting.
     */
    protected IGeneration generateFromDsl(JsonNode dslNode, boolean memoryOptimized) throws IOException {
        return createGenerator(memoryOptimized)
            .fromJsonNode(dslNode)
            .generate();
    }

    /**
     * Generates data from DSL JsonNode with a custom seed and memory optimization setting.
     */
    protected IGeneration generateFromDslWithSeed(JsonNode dslNode, long seed, boolean memoryOptimized) throws IOException {
        DslDataGenerator.Builder builder = DslDataGenerator.create().withSeed(seed);
        if (memoryOptimized) {
            builder = builder.withMemoryOptimization();
        }
        return builder.fromJsonNode(dslNode).generate();
    }

    /**
     * Generates data from DSL string with a custom seed and memory optimization setting.
     */
    protected IGeneration generateFromDslWithSeed(String dsl, long seed, boolean memoryOptimized) throws IOException {
        DslDataGenerator.Builder builder = DslDataGenerator.create().withSeed(seed);
        if (memoryOptimized) {
            builder = builder.withMemoryOptimization();
        }
        return builder.fromJsonString(dsl).generate();
    }

    /**
     * Utility method to convert streaming API to old format for easier test migration.
     * Collects all JSON node streams into a Map<String, List<JsonNode>> format.
     * This helps tests that were written for the old API to work with minimal changes.
     */
    protected Map<String, List<JsonNode>> collectAllJsonNodes(IGeneration generation) {
        Map<String, Stream<JsonNode>> streams = generation.asJsonNodes();
        Map<String, List<JsonNode>> collections = new HashMap<>();

        for (Map.Entry<String, Stream<JsonNode>> entry : streams.entrySet()) {
            collections.put(entry.getKey(), entry.getValue().toList());
        }

        return collections;
    }

    /**
     * Utility method to create a JsonNode that mimics the old asJsonNode() behavior.
     * This creates an ObjectNode with collection names as keys and arrays as values.
     */
    protected JsonNode createLegacyJsonNode(IGeneration generation) throws IOException {
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();
        for (Map.Entry<String, List<JsonNode>> entry : collections.entrySet()) {
            ArrayNode arrayNode = mapper.createArrayNode();
            for (JsonNode item : entry.getValue()) {
                arrayNode.add(item);
            }
            root.set(entry.getKey(), arrayNode);
        }

        return root;
    }

    /**
     * Utility method to create a JSON string that mimics the old asJson() behavior.
     */
    protected String createLegacyJsonString(IGeneration generation) throws IOException {
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(collections);
    }

    /**
     * Utility method to convert SQL streams to the old Map<String, String> format.
     * This collects all SQL streams and joins them with newlines.
     */
    protected Map<String, String> collectAllSqlInserts(IGeneration generation) {
        Map<String, Stream<String>> sqlStreams = generation.asSqlInserts();
        Map<String, String> sqlMap = new HashMap<>();

        for (Map.Entry<String, Stream<String>> entry : sqlStreams.entrySet()) {
            String joinedSql = entry.getValue().collect(Collectors.joining("\n"));
            sqlMap.put(entry.getKey(), joinedSql);
        }

        return sqlMap;
    }

    /**
     * Static utility methods for backward compatibility in non-parameterized tests.
     * These can be used in tests that haven't been converted to parameterized testing yet.
     */
    public static class LegacyApiHelper {
        private static final ObjectMapper mapper = new ObjectMapper();

        static {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }

        /**
         * Mimics the old asJson() method behavior.
         */
        public static String asJson(IGeneration generation) throws IOException {
            Map<String, List<JsonNode>> collections = new HashMap<>();
            Map<String, Stream<JsonNode>> streams = generation.asJsonNodes();

            for (Map.Entry<String, Stream<JsonNode>> entry : streams.entrySet()) {
                collections.put(entry.getKey(), entry.getValue().toList());
            }

            return mapper.writeValueAsString(collections);
        }

        /**
         * Mimics the old asJsonNode() method behavior.
         */
        public static JsonNode asJsonNode(IGeneration generation) throws IOException {
            Map<String, List<JsonNode>> collections = new HashMap<>();
            Map<String, Stream<JsonNode>> streams = generation.asJsonNodes();

            for (Map.Entry<String, Stream<JsonNode>> entry : streams.entrySet()) {
                collections.put(entry.getKey(), entry.getValue().toList());
            }

            ObjectNode root = mapper.createObjectNode();
            for (Map.Entry<String, List<JsonNode>> entry : collections.entrySet()) {
                ArrayNode arrayNode = mapper.createArrayNode();
                for (JsonNode item : entry.getValue()) {
                    arrayNode.add(item);
                }
                root.set(entry.getKey(), arrayNode);
            }

            return root;
        }



        /**
         * Mimics the old asSqlInserts() method behavior.
         */
        public static Map<String, String> asSqlInserts(IGeneration generation) {
            Map<String, Stream<String>> sqlStreams = generation.asSqlInserts();
            Map<String, String> sqlMap = new HashMap<>();

            for (Map.Entry<String, Stream<String>> entry : sqlStreams.entrySet()) {
                String joinedSql = entry.getValue().collect(Collectors.joining("\n"));
                sqlMap.put(entry.getKey(), joinedSql);
            }

            return sqlMap;
        }
    }

    /**
     * Annotation for parameterized tests that run with both implementations.
     * Use this instead of @Test for tests that should run with both normal and memory-optimized modes.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @ParameterizedTest(name = "{0} Implementation")
    @MethodSource("generationImplementations")
    protected @interface BothImplementations {
    }
}
