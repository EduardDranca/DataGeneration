package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.FilteringBehavior;
import com.github.eddranca.datagenerator.exception.FilteringException;
import com.github.eddranca.datagenerator.generator.FilteringGeneratorAdapter;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.node.Sequential;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Abstract base class for generation contexts that provides shared functionality
 * for both eager and lazy generation modes.
 * <p>
 * This class contains all the core utilities that are common to both generation
 * strategies:
 * - Generator registry and random number generation
 * - Sequential tracking for deterministic generation
 * - Filtering utilities with configurable behavior
 * - Generator integration with filtering support
 * <p>
 * Subclasses implement the specific collection management strategies for their
 * respective generation modes.
 */
public abstract class AbstractGenerationContext {
    protected final GeneratorRegistry generatorRegistry;
    protected final Random random;
    protected final ObjectMapper mapper;
    protected final Map<Sequential, Integer> sequentialCounters;
    protected final int maxFilteringRetries;
    protected final FilteringBehavior filteringBehavior;

    protected AbstractGenerationContext(GeneratorRegistry generatorRegistry, Random random,
                                        int maxFilteringRetries, FilteringBehavior filteringBehavior) {
        this.generatorRegistry = generatorRegistry;
        this.random = random;
        this.mapper = new ObjectMapper();
        this.sequentialCounters = new IdentityHashMap<>();
        this.maxFilteringRetries = maxFilteringRetries;
        this.filteringBehavior = filteringBehavior;
    }

    protected AbstractGenerationContext(GeneratorRegistry generatorRegistry, Random random) {
        this(generatorRegistry, random, 100, FilteringBehavior.RETURN_NULL);
    }

    // Getters for shared resources
    public GeneratorRegistry getGeneratorRegistry() {
        return generatorRegistry;
    }

    public Random getRandom() {
        return random;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    // Abstract methods that subclasses must implement
    public abstract void registerCollection(String name, List<JsonNode> collection);

    public abstract void registerReferenceCollection(String name, List<JsonNode> collection);

    public abstract void registerTaggedCollection(String tag, List<JsonNode> collection);

    public abstract void registerPick(String name, JsonNode value);

    public abstract List<JsonNode> getCollection(String name);

    public abstract List<JsonNode> getTaggedCollection(String tag);

    public abstract JsonNode getNamedPick(String name);

    public abstract Map<String, List<JsonNode>> getNamedCollections();

    public abstract boolean isMemoryOptimizationEnabled();

    /**
     * Gets a random or sequential element from a collection.
     * <p>
     * This is a CORE utility method that typed reference nodes should use.
     */
    public JsonNode getElementFromCollection(List<JsonNode> collection, Sequential node, boolean sequential) {
        if (collection.isEmpty()) {
            return mapper.nullNode();
        }

        int index = sequential && node != null ? getNextSequentialIndex(node, collection.size())
            : random.nextInt(collection.size());
        return collection.get(index);
    }

    /**
     * Applies filtering to a collection based on filter values.
     * If fieldName is provided, filters based on that field's value.
     * Otherwise, filters based on the entire object.
     * <p>
     * This is a CORE utility method that typed reference nodes should use.
     */
    public List<JsonNode> applyFiltering(List<JsonNode> collection, String fieldName, List<JsonNode> filterValues) {
        if (collection.isEmpty() || filterValues == null || filterValues.isEmpty()) {
            return new ArrayList<>(collection);
        }

        return collection.stream()
            .filter(item -> {
                JsonNode valueToCheck = fieldName.isEmpty() ? item : item.path(fieldName);
                return filterValues.stream().noneMatch(valueToCheck::equals);
            }).toList();
    }

    /**
     * Applies filtering to a collection based on field values, but keeps the
     * original objects.
     * This is used for field extraction cases where we want to filter based on
     * field values but return the original objects so field extraction can happen
     * later.
     * <p>
     * This is a CORE utility method that typed reference nodes should use.
     */
    public List<JsonNode> applyFilteringOnField(List<JsonNode> collection, String fieldName,
                                                List<JsonNode> filterValues) {
        if (collection.isEmpty() || filterValues == null || filterValues.isEmpty()) {
            return new ArrayList<>(collection);
        }

        return collection.stream()
            .filter(item -> {
                JsonNode fieldValue = item.path(fieldName);
                return !fieldValue.isMissingNode() &&
                    filterValues.stream().noneMatch(fieldValue::equals);
            }).toList();
    }

    /**
     * Handles filtering failure according to the configured filtering behavior.
     * <p>
     * This is a CORE utility method that typed reference nodes should use.
     */
    public JsonNode handleFilteringFailure(String message) {
        if (filteringBehavior == FilteringBehavior.THROW_EXCEPTION) {
            throw new FilteringException(message);
        }
        return mapper.nullNode();
    }

    /**
     * Handles reference resolution failure according to the configured filtering behavior.
     * This provides consistent error handling for both filtering and reference failures.
     * <p>
     * This is a CORE utility method that typed reference nodes should use.
     */
    public JsonNode handleReferenceFailure(String message) {
        if (filteringBehavior == FilteringBehavior.THROW_EXCEPTION) {
            throw new FilteringException("Reference resolution failed: " + message);
        }
        return mapper.nullNode();
    }

    /**
     * Gets the next sequential index for a reference field node.
     * Each reference field node maintains its own counter for round-robin access.
     * <p>
     * This is a CORE utility method that typed reference nodes should use.
     *
     * @param node           the reference field node
     * @param collectionSize the size of the collection being referenced
     * @return the next sequential index (with automatic wrap-around)
     */
    public int getNextSequentialIndex(Sequential node, int collectionSize) {
        if (collectionSize <= 0) {
            return 0;
        }

        int current = sequentialCounters.getOrDefault(node, 0);
        int index = current % collectionSize;
        sequentialCounters.put(node, current + 1);
        return index;
    }

    /**
     * Generates a value using a generator with optional filtering.
     * Uses the FilteringGeneratorAdapter to handle both native and retry-based
     * filtering.
     *
     * @param generator    the generator to use
     * @param options      the generation options
     * @param path         optional path for field extraction (null for full object)
     * @param filterValues values to exclude (null if no filtering)
     * @return generated value that doesn't match any filter values
     */
    public JsonNode generateWithFilter(Generator generator, JsonNode options, String path,
                                       List<JsonNode> filterValues) {
        FilteringGeneratorAdapter adapter = new FilteringGeneratorAdapter(generator, maxFilteringRetries);
        try {
            if (path != null) {
                return adapter.generateAtPathWithFilter(options, path, filterValues);
            } else {
                return adapter.generateWithFilter(options, filterValues);
            }
        } catch (FilteringException e) {
            return handleFilteringFailure(e.getMessage());
        }
    }

    // Default implementations for lazy collection methods (overridden in LazyGenerationContext)
    public void registerLazyCollection(String name, LazyItemCollection collection) {
        throw new UnsupportedOperationException("Lazy collection registration is only supported in LazyGenerationContext");
    }

    public void registerLazyReferenceCollection(String name, LazyItemCollection collection) {
        throw new UnsupportedOperationException("Lazy collection registration is only supported in LazyGenerationContext");
    }

    public void registerLazyTaggedCollection(String tag, LazyItemCollection collection) {
        throw new UnsupportedOperationException("Lazy collection registration is only supported in LazyGenerationContext");
    }

    public Map<String, List<LazyItemProxy>> getLazyNamedCollections() {
        throw new UnsupportedOperationException("Lazy collections are only available in LazyGenerationContext");
    }

    public Set<String> getReferencedPaths(String collection) {
        return Set.of(); // Default to empty set for eager context
    }

    public void setReferencedPaths(String collection, Set<String> paths) {
        // No-op for eager context
    }

    public void enableMemoryOptimization(Map<String, Set<String>> referencedPaths) {
        // No-op for eager context
    }
}
