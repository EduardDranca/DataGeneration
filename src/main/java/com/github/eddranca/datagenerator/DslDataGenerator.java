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
import com.github.eddranca.datagenerator.visitor.LazyItemProxy;
import net.datafaker.Faker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
    IGeneration generateInternal(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File not found: " + file.getPath());
        }

        JsonNode root = mapper.readTree(file);
        return generateFromJsonNode(root);
    }

    /**
     * Internal generation method used by the fluent API for JSON strings.
     */
    IGeneration generateInternal(String jsonString) throws IOException {
        JsonNode root = mapper.readTree(jsonString);
        return generateFromJsonNode(root);
    }

    /**
     * Internal generation method used by the fluent API for JsonNodes.
     */
    IGeneration generateInternal(JsonNode jsonNode) {
        return generateFromJsonNode(jsonNode);
    }

    /**
     * Common generation logic for all input types using the new visitor
     * architecture.
     */
    private IGeneration generateFromJsonNode(JsonNode root) {
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

        // Enable memory optimization if requested
        if (memoryOptimizationEnabled) {
            // Initialize with empty referenced paths - they will be populated during analysis
            context.enableMemoryOptimization(new HashMap<>());
        }

        DataGenerationVisitor visitor = new DataGenerationVisitor(context);

        rootNode.accept(visitor);

        // Return the appropriate Generation implementation based on memory optimization
        if (memoryOptimizationEnabled) {
            // For memory optimization, use the lazy collections directly
            Map<String, List<LazyItemProxy>> lazyCollections = new HashMap<>();

            for (Map.Entry<String, List<LazyItemProxy>> entry : context.getLazyNamedCollections().entrySet()) {
                lazyCollections.put(entry.getKey(), entry.getValue());
            }

            // Perform a dry run of the first item in each collection
            // to trigger any reference filtering exceptions early
            performDryRunValidation(lazyCollections);

            return new LazyGeneration(lazyCollections);
        } else {
            // Normal generation
            return new Generation(context.getNamedCollections());
        }
    }



    /**
     * Performs a dry run validation by attempting to generate the first item
     * from each lazy collection. This helps catch reference filtering issues
     * that can't be detected in the static validation phase.
     */
    // TODO: Maybe we can rethink this approach to avoid generating items here
    private void performDryRunValidation(Map<String, List<com.github.eddranca.datagenerator.visitor.LazyItemProxy>> lazyCollections) {
        for (Map.Entry<String, List<com.github.eddranca.datagenerator.visitor.LazyItemProxy>> entry : lazyCollections.entrySet()) {
            List<com.github.eddranca.datagenerator.visitor.LazyItemProxy> collection = entry.getValue();
            if (collection.size() > 0) {
                try {
                    // Try to get the first item - this will trigger any filtering exceptions
                    com.github.eddranca.datagenerator.visitor.LazyItemProxy firstItem = collection.get(0);
                    // Try to materialize the first item to trigger any reference resolution
                    firstItem.getMaterializedCopy();
                } catch (Exception e) {
                    // If getting the first item fails, it's likely a filtering issue
                    // Re-throw the exception to fail fast during .generate()
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    } else {
                        throw new RuntimeException("Dry run validation failed: " + e.getMessage(), e);
                    }
                }
            }
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
         * Enables memory optimization for lazy field materialization.
         * This can significantly reduce memory usage when generating large datasets
         * where only some fields are referenced by other collections.
         *
         * @return this builder for method chaining
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
         * Builds the DslDataGenerator instance.
         *
         * @return a configured DslDataGenerator
         */
        public DslDataGenerator build() {
            return new DslDataGenerator(this);
        }
    }

}
