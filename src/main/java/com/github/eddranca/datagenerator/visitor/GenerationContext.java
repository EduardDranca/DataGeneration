package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.FilteringBehavior;
import com.github.eddranca.datagenerator.exception.FilteringException;
import com.github.eddranca.datagenerator.generator.FilteringGeneratorAdapter;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.node.SequentialTrackable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;



/**
 * Context for data generation that maintains state during visitor traversal.
 * Tracks generated collections, tagged collections, and provides access to
 * generators.
 *
 * ARCHITECTURE:
 * This class provides core utilities for the typed reference system:
 * 1. Collection management (registration, retrieval, caching)
 * 2. Sequential tracking for deterministic generation
 * 3. Filtering utilities with configurable behavior
 * 4. Generator integration with filtering support
 *
 * All reference nodes use these core utility methods:
 * - getElementFromCollection() for element selection
 * - applyFiltering() for collection filtering
 * - handleFilteringFailure() for error handling
 * - getNextSequentialIndex() for sequential tracking
 *
 * The system now uses typed AbstractReferenceNode instances exclusively,
 * eliminating string-based reference resolution and improving type safety.
 */
public class GenerationContext {
    private final GeneratorRegistry generatorRegistry;
    private final Random random;
    private final ObjectMapper mapper;
    private final Map<String, List<JsonNode>> namedCollections; // Final collections for output
    private final Map<String, List<JsonNode>> referenceCollections; // Collections available for references (includes
                                                                    // DSL keys)
    private final Map<String, List<JsonNode>> taggedCollections;
    private final Map<String, JsonNode> namedPicks;
    private final Map<SequentialTrackable, Integer> sequentialCounters;
    private final int maxFilteringRetries;
    private final FilteringBehavior filteringBehavior;

    public GenerationContext(GeneratorRegistry generatorRegistry, Random random, int maxFilteringRetries,
            FilteringBehavior filteringBehavior) {
        this.generatorRegistry = generatorRegistry;
        this.random = random;
        this.mapper = new ObjectMapper();
        this.namedCollections = new HashMap<>();
        this.referenceCollections = new HashMap<>();
        this.taggedCollections = new HashMap<>();
        this.namedPicks = new HashMap<>();
        this.sequentialCounters = new IdentityHashMap<>();
        this.maxFilteringRetries = maxFilteringRetries;
        this.filteringBehavior = filteringBehavior;

    }

    public GenerationContext(GeneratorRegistry generatorRegistry, Random random) {
        this(generatorRegistry, random, 100, FilteringBehavior.RETURN_NULL);
    }

    public GeneratorRegistry getGeneratorRegistry() {
        return generatorRegistry;
    }

    public Random getRandom() {
        return random;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public void registerCollection(String name, List<JsonNode> collection) {
        List<JsonNode> existing = namedCollections.get(name);
        if (existing == null) {
            namedCollections.put(name, new ArrayList<>(collection));
        } else {
            // Merge collections with the same name
            existing.addAll(collection);
        }
    }

    public void registerPick(String name, JsonNode value) {
        namedPicks.put(name, value);
    }

    public void registerReferenceCollection(String name, List<JsonNode> collection) {
        referenceCollections.put(name, new ArrayList<>(collection));
    }

    public void registerTaggedCollection(String tag, List<JsonNode> collection) {
        List<JsonNode> existing = taggedCollections.get(tag);
        if (existing == null) {
            taggedCollections.put(tag, new ArrayList<>(collection));
        } else {
            // Merge collections with the same tag
            existing.addAll(collection);
        }
    }

    public List<JsonNode> getCollection(String name) {
        // First check reference collections (includes DSL keys), then named collections
        List<JsonNode> collection = referenceCollections.get(name);
        if (collection != null) {
            return collection;
        }
        collection = namedCollections.get(name);
        return collection != null ? collection : List.of();
    }

    public List<JsonNode> getTaggedCollection(String tag) {
        List<JsonNode> collection = taggedCollections.get(tag);
        return collection != null ? collection : List.of();
    }

    public Map<String, List<JsonNode>> getNamedCollections() {
        return new HashMap<>(namedCollections);
    }

    public JsonNode getNamedPick(String name) {
        return namedPicks.get(name);
    }

    /**
     * Gets a random or sequential element from a collection.
     *
     * This is a CORE utility method that typed reference nodes should use.
     */
    public JsonNode getElementFromCollection(List<JsonNode> collection, SequentialTrackable node, boolean sequential) {
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
     *
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
     *
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
     *
     * This is a CORE utility method that typed reference nodes should use.
     */
    public JsonNode handleFilteringFailure(String message) {
        if (filteringBehavior == FilteringBehavior.THROW_EXCEPTION) {
            throw new FilteringException(message);
        }
        return mapper.nullNode();
    }

    /**
     * Gets the next sequential index for a reference field node.
     * Each reference field node maintains its own counter for round-robin access.
     *
     * This is a CORE utility method that typed reference nodes should use.
     *
     * @param node           the reference field node
     * @param collectionSize the size of the collection being referenced
     * @return the next sequential index (with automatic wrap-around)
     */
    public int getNextSequentialIndex(SequentialTrackable node, int collectionSize) {
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
}
