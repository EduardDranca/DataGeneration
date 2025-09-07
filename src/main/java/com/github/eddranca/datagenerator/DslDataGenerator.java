package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.builder.DslTreeBuilder;
import com.github.eddranca.datagenerator.exception.DslValidationException;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.node.RootNode;
import com.github.eddranca.datagenerator.validation.DslTreeBuildResult;
import com.github.eddranca.datagenerator.visitor.DataGenerationVisitor;
import com.github.eddranca.datagenerator.visitor.GenerationContext;
import net.datafaker.Faker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DslDataGenerator {
    private final ObjectMapper mapper;
    private final GeneratorRegistry generatorRegistry;
    private final Random random;
    private final int maxFilteringRetries;
    private final FilteringBehavior filteringBehavior;

    private DslDataGenerator(Builder builder) {
        this.random = new Random(builder.seed);
        this.mapper = new ObjectMapper();
        this.maxFilteringRetries = builder.maxFilteringRetries;
        this.filteringBehavior = builder.filteringBehavior;
        this.generatorRegistry = builder.generatorRegistry != null ? builder.generatorRegistry
                : GeneratorRegistry.withDefaultGenerators(new Faker(random));

        // Add custom generators if any
        if (builder.customGenerators != null) {
            for (Map.Entry<String, Generator> entry : builder.customGenerators.entrySet()) {
                this.generatorRegistry.register(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Creates a new fluent builder for DslDataGenerator.
     *
     * @return a new Builder instance
     */
    public static Builder create() {
        return new Builder();
    }

    /**
     * Internal generation method used by the fluent API for files.
     */
    Generation generateInternal(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File not found: " + file.getPath());
        }

        JsonNode root = mapper.readTree(file);
        return generateFromJsonNode(root);
    }

    /**
     * Internal generation method used by the fluent API for JSON strings.
     */
    Generation generateInternal(String jsonString) throws IOException {
        JsonNode root = mapper.readTree(jsonString);
        return generateFromJsonNode(root);
    }

    /**
     * Internal generation method used by the fluent API for JsonNodes.
     */
    Generation generateInternal(JsonNode jsonNode) {
        return generateFromJsonNode(jsonNode);
    }

    /**
     * Common generation logic for all input types using the new visitor
     * architecture.
     */
    private Generation generateFromJsonNode(JsonNode root) {
        // Build and validate the DSL tree
        DslTreeBuilder treeBuilder = new DslTreeBuilder(generatorRegistry);
        DslTreeBuildResult buildResult = treeBuilder.build(root);

        // Handle validation errors by throwing exception
        if (buildResult.hasErrors()) {
            throw new DslValidationException(buildResult.getErrors());
        }

        // Handle seed from the tree
        RootNode rootNode = buildResult.getTree();
        if (rootNode.getSeed() != null) {
            this.random.setSeed(rootNode.getSeed());
        }

        // Generate data using the visitor
        GenerationContext context = new GenerationContext(generatorRegistry, random, maxFilteringRetries,
                filteringBehavior);
        DataGenerationVisitor visitor = new DataGenerationVisitor(context);

        rootNode.accept(visitor);

        // Convert the result to the expected format for Generation
        return new Generation(context.getNamedCollections());
    }

    /**
     * Creates a streaming generation for memory-efficient processing of large
     * datasets.
     *
     * @param jsonNode the DSL definition
     * @return StreamingGeneration instance for streaming SQL generation
     */
    public StreamingGeneration createStreamingGeneration(JsonNode jsonNode) {
        // Build and validate the DSL tree
        DslTreeBuilder treeBuilder = new DslTreeBuilder(generatorRegistry);
        DslTreeBuildResult buildResult = treeBuilder.build(jsonNode);

        // Handle validation errors by throwing exception
        if (buildResult.hasErrors()) {
            throw new DslValidationException(buildResult.getErrors());
        }

        // Handle seed from the tree
        RootNode rootNode = buildResult.getTree();
        if (rootNode.getSeed() != null) {
            this.random.setSeed(rootNode.getSeed());
        }

        // Create streaming generation context
        GenerationContext context = new GenerationContext(generatorRegistry, random, maxFilteringRetries,
                filteringBehavior);

        return new StreamingGeneration(context, rootNode.getCollections());
    }

    /**
     * Creates a streaming generation from a JSON string.
     */
    public StreamingGeneration createStreamingGeneration(String jsonString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonString);
        return createStreamingGeneration(jsonNode);
    }

    /**
     * Creates a streaming generation from a file.
     */
    public StreamingGeneration createStreamingGeneration(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(file);
        return createStreamingGeneration(jsonNode);
    }

    /**
     * Fluent builder for DslDataGenerator with a readable, chainable API.
     */
    public static class Builder {
        private long seed = System.currentTimeMillis();
        private GeneratorRegistry generatorRegistry;
        private Map<String, Generator> customGenerators;
        private int maxFilteringRetries = 100;
        private FilteringBehavior filteringBehavior = FilteringBehavior.RETURN_NULL;

        private Builder() {
        }

        /**
         * Sets the random seed for deterministic data generation.
         *
         * @param seed the seed value
         * @return this builder for method chaining
         */
        public Builder withSeed(long seed) {
            this.seed = seed;
            return this;
        }

        /**
         * Sets a custom generator registry.
         *
         * @param registry the generator registry to use
         * @return this builder for method chaining
         */
        public Builder withGeneratorRegistry(GeneratorRegistry registry) {
            this.generatorRegistry = registry;
            return this;
        }

        /**
         * Adds a custom generator for a specific type.
         *
         * @param name      the generator name (e.g., "custom.myType")
         * @param generator the generator implementation
         * @return this builder for method chaining
         */
        public Builder withCustomGenerator(String name, Generator generator) {
            if (this.customGenerators == null) {
                this.customGenerators = new HashMap<>();
            }
            this.customGenerators.put(name, generator);
            return this;
        }

        /**
         * Sets the maximum number of retries when filtering results in no valid values.
         * This applies to generators that don't support native filtering and use retry
         * logic.
         * Default is 100 retries.
         *
         * @param maxRetries the maximum number of retries (must be positive)
         * @return this builder for method chaining
         * @throws IllegalArgumentException if maxRetries is not positive
         */
        public Builder withMaxFilteringRetries(int maxRetries) {
            if (maxRetries <= 0) {
                throw new IllegalArgumentException("Max filtering retries must be positive, got: " + maxRetries);
            }
            this.maxFilteringRetries = maxRetries;
            return this;
        }

        /**
         * Sets the behavior when all possible values are filtered out and no valid
         * value can be generated.
         * The default is RETURN_NULL.
         *
         * @param behavior the filtering behavior to use
         * @return this builder for method chaining
         */
        public Builder withFilteringBehavior(FilteringBehavior behavior) {
            this.filteringBehavior = behavior != null ? behavior : FilteringBehavior.RETURN_NULL;
            return this;
        }

        /**
         * Creates a Generation.Builder for fluent file-based generation.
         *
         * @param file the DSL file to process
         * @return a Generation.Builder for further configuration
         */
        public Generation.Builder fromFile(File file) {
            return new Generation.Builder(build(), file);
        }

        /**
         * Creates a Generation.Builder for fluent file-based generation.
         *
         * @param filePath the path to the DSL file
         * @return a Generation.Builder for further configuration
         */
        public Generation.Builder fromFile(String filePath) {
            return fromFile(new File(filePath));
        }

        /**
         * Creates a Generation.Builder for fluent file-based generation.
         *
         * @param path the path to the DSL file
         * @return a Generation.Builder for further configuration
         */
        public Generation.Builder fromFile(Path path) {
            return fromFile(path.toFile());
        }

        /**
         * Creates a Generation.Builder for fluent JSON string-based generation.
         *
         * @param jsonString the DSL JSON as a string
         * @return a Generation.Builder for further configuration
         */
        public Generation.Builder fromJsonString(String jsonString) {
            return new Generation.Builder(build(), jsonString);
        }

        /**
         * Creates a Generation.Builder for fluent JsonNode-based generation.
         *
         * @param jsonNode the DSL as a JsonNode
         * @return a Generation.Builder for further configuration
         */
        public Generation.Builder fromJsonNode(JsonNode jsonNode) {
            return new Generation.Builder(build(), jsonNode);
        }

        /**
         * Creates a streaming generation for memory-efficient processing of large
         * datasets.
         *
         * @param jsonNode the DSL definition
         * @return StreamingGeneration instance for streaming SQL generation
         */
        public StreamingGeneration createStreamingGeneration(JsonNode jsonNode) {
            return build().createStreamingGeneration(jsonNode);
        }

        /**
         * Creates a streaming generation from a JSON string.
         */
        public StreamingGeneration createStreamingGeneration(String jsonString) throws IOException {
            return build().createStreamingGeneration(jsonString);
        }

        /**
         * Creates a streaming generation from a file.
         */
        public StreamingGeneration createStreamingGeneration(File file) throws IOException {
            return build().createStreamingGeneration(file);
        }

        /**
         * Builds the DslDataGenerator instance.
         *
         * @return a configured DslDataGenerator
         */
        public DslDataGenerator build() {
            return new DslDataGenerator(this);
        }
    }

}
