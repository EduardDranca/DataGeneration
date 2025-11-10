package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.builder.DslTreeBuilder;
import com.github.eddranca.datagenerator.exception.DslValidationException;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.node.RootNode;
import com.github.eddranca.datagenerator.validation.DslTreeBuildResult;
import com.github.eddranca.datagenerator.visitor.AbstractGenerationContext;
import com.github.eddranca.datagenerator.visitor.DataGenerationVisitor;
import com.github.eddranca.datagenerator.visitor.EagerGenerationContext;
import com.github.eddranca.datagenerator.visitor.LazyGenerationContext;
import com.github.eddranca.datagenerator.visitor.PathDependencyAnalyzer;
import net.datafaker.Faker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Main entry point for generating test data from a JSON DSL specification.
 * <p>
 * This class provides a fluent builder API for configuring and executing data generation.
 * It supports both eager (all data in memory) and lazy (streaming) generation modes.
 * <p>
 * <b>Thread Safety:</b> This class is NOT thread-safe. Each instance should be used by a single thread.
 * The internal Random instance and generator registry maintain mutable state that is not synchronized.
 * If concurrent data generation is needed, create separate DslDataGenerator instances per thread.
 * <p>
 * Example usage:
 * <pre>{@code
 * Generation result = DslDataGenerator.builder()
 *     .withSeed(12345L)
 *     .withMemoryOptimization()
 *     .generateFromFile("dsl.json");
 * }</pre>
 */
public class DslDataGenerator {
    private final ObjectMapper mapper;
    private final GeneratorRegistry generatorRegistry;
    private final Random random;
    private final int maxFilteringRetries;
    private final FilteringBehavior filteringBehavior;
    private final boolean memoryOptimizationEnabled;

    private DslDataGenerator(Builder builder) {
        this.random = new Random(builder.seed);
        this.mapper = new ObjectMapper();
        this.maxFilteringRetries = builder.maxFilteringRetries;
        this.filteringBehavior = builder.filteringBehavior;
        this.memoryOptimizationEnabled = builder.memoryOptimizationEnabled;
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

        // Generate data using the appropriate visitor context
        AbstractGenerationContext<?> context;

        if (memoryOptimizationEnabled) {
            // For lazy generation, analyze dependencies first
            PathDependencyAnalyzer analyzer = new PathDependencyAnalyzer();
            Map<String, Set<String>> referencedPaths = analyzer.analyzeRoot(rootNode);

            LazyGenerationContext lazyContext = new LazyGenerationContext(generatorRegistry, random,
                maxFilteringRetries, filteringBehavior);
            lazyContext.setReferencedPaths(referencedPaths);
            context = lazyContext;
        } else {
            context = new EagerGenerationContext(generatorRegistry, random, maxFilteringRetries, filteringBehavior);
        }

        DataGenerationVisitor<?> visitor = new DataGenerationVisitor<>(context);

        rootNode.accept(visitor);
        return getGeneration(context);
    }

    private Generation getGeneration(AbstractGenerationContext<?> context) {
        if (memoryOptimizationEnabled) {
            LazyGenerationContext lazyContext = (LazyGenerationContext) context;
            return new LazyGeneration(lazyContext.getNamedCollections());
        } else {
            EagerGenerationContext eagerContext = (EagerGenerationContext) context;
            return new EagerGeneration(eagerContext.getNamedCollections());
        }
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
        private boolean memoryOptimizationEnabled = false;

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
         * Enables memory optimization using lazy field materialization.
         * Only referenced fields are initially generated; other fields are created on-demand during streaming.
         * This reduces memory usage when generating large datasets.
         *
         * <p><strong>Consistency behavior:</strong> Streaming the same collection multiple times
         * will yield different results. Processing order affects generated data.
         * Not suitable for parallel processing.
         *
         * <p>Use when memory usage is a concern and you need to stream data once.
         * For consistent results, always use {@link #withSeed(long)} and process sequentially.
         *
         * @return this builder for method chaining
         * @see LazyGeneration
         */
        public Builder withMemoryOptimization() {
            this.memoryOptimizationEnabled = true;
            return this;
        }

        /**
         * Creates a Generation.Builder for fluent file-based generation.
         *
         * @param file the DSL file to process
         * @return a Generation.Builder for further configuration
         */
        public AbstractGeneration.Builder fromFile(File file) {
            return new AbstractGeneration.Builder(build(), file);
        }

        /**
         * Creates a Generation.Builder for fluent file-based generation.
         *
         * @param filePath the path to the DSL file
         * @return a Generation.Builder for further configuration
         */
        public AbstractGeneration.Builder fromFile(String filePath) {
            return fromFile(new File(filePath));
        }

        /**
         * Creates a Generation.Builder for fluent file-based generation.
         *
         * @param path the path to the DSL file
         * @return a Generation.Builder for further configuration
         */
        public AbstractGeneration.Builder fromFile(Path path) {
            return fromFile(path.toFile());
        }

        /**
         * Creates a Generation.Builder for fluent JSON string-based generation.
         *
         * @param jsonString the DSL JSON as a string
         * @return a Generation.Builder for further configuration
         */
        public AbstractGeneration.Builder fromJsonString(String jsonString) {
            return new AbstractGeneration.Builder(build(), jsonString);
        }

        /**
         * Creates a Generation.Builder for fluent JsonNode-based generation.
         *
         * @param jsonNode the DSL as a JsonNode
         * @return a Generation.Builder for further configuration
         */
        public AbstractGeneration.Builder fromJsonNode(JsonNode jsonNode) {
            return new AbstractGeneration.Builder(build(), jsonNode);
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
