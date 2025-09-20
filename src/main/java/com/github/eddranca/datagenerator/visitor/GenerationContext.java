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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


/**
 * Context for data generation that maintains state during visitor traversal.
 * Tracks generated collections, tagged collections, and provides access to
 * generators.
 * <p>
 * ARCHITECTURE:
 * This class provides core utilities for the typed reference system:
 * 1. Collection management (registration, retrieval, caching)
 * 2. Sequential tracking for deterministic generation
 * 3. Filtering utilities with configurable behavior
 * 4. Generator integration with filtering support
 * <p>
 * All reference nodes use these core utility methods:
 * - getElementFromCollection() for element selection
 * - applyFiltering() for collection filtering
 * - handleFilteringFailure() for error handling
 * - getNextSequentialIndex() for sequential tracking
 * <p>
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

    // Lazy collection support for memory optimization
    private final Map<String, List<LazyItemProxy>> lazyNamedCollections;
    private final Map<String, List<LazyItemProxy>> lazyReferenceCollections;
    private final Map<String, List<LazyItemProxy>> lazyTaggedCollections;
    private final Map<Sequential, Integer> sequentialCounters;
    
    // Cache for materialized lazy collections to ensure consistency
    private final Map<String, List<JsonNode>> materializedCollectionCache;
    private final int maxFilteringRetries;
    private final FilteringBehavior filteringBehavior;

    // Memory optimization fields
    private Map<String, Set<String>> referencedPaths;
    private boolean memoryOptimizationEnabled = false;

    public GenerationContext(GeneratorRegistry generatorRegistry, Random random, int maxFilteringRetries,
                             FilteringBehavior filteringBehavior) {
        this.generatorRegistry = generatorRegistry;
        this.random = random;
        this.mapper = new ObjectMapper();
        this.namedCollections = new HashMap<>();
        this.referenceCollections = new HashMap<>();
        this.taggedCollections = new HashMap<>();
        this.namedPicks = new HashMap<>();
        this.lazyNamedCollections = new HashMap<>();
        this.lazyReferenceCollections = new HashMap<>();
        this.lazyTaggedCollections = new HashMap<>();
        this.sequentialCounters = new IdentityHashMap<>();
        this.materializedCollectionCache = new HashMap<>();
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

    // Lazy collection registration methods for memory optimization
    public void registerLazyCollection(String name, LazyItemCollection collection) {
        List<LazyItemProxy> existing = lazyNamedCollections.get(name);
        if (existing == null) {
            lazyNamedCollections.put(name, collection);
        } else {
            // Merge collections with the same name
            CompositeLazyItemCollection composite;
            if (existing instanceof CompositeLazyItemCollection) {
                composite = (CompositeLazyItemCollection) existing;
            } else {
                composite = new CompositeLazyItemCollection(name);
                composite.addCollection((LazyItemCollection) existing);
                lazyNamedCollections.put(name, composite);
            }
            composite.addCollection(collection);
        }
    }

    public void registerLazyReferenceCollection(String name, LazyItemCollection collection) {
        lazyReferenceCollections.put(name, collection);
    }

    public void registerLazyTaggedCollection(String tag, LazyItemCollection collection) {
        lazyTaggedCollections.put(tag, collection);
    }

    public List<JsonNode> getCollection(String name) {
        // First check lazy collections if memory optimization is enabled
        if (memoryOptimizationEnabled) {
            // Check cache first to ensure consistency
            List<JsonNode> cached = materializedCollectionCache.get(name);
            if (cached != null) {
                return cached;
            }
            
            List<LazyItemProxy> lazyCollection = lazyReferenceCollections.get(name);
            if (lazyCollection != null) {
                List<JsonNode> materialized = materializeLazyCollection(lazyCollection);
                materializedCollectionCache.put(name, materialized);
                return materialized;
            }
            lazyCollection = lazyNamedCollections.get(name);
            if (lazyCollection != null) {
                List<JsonNode> materialized = materializeLazyCollection(lazyCollection);
                materializedCollectionCache.put(name, materialized);
                return materialized;
            }
        }

        // Fall back to eager collections
        List<JsonNode> collection = referenceCollections.get(name);
        if (collection != null) {
            return collection;
        }
        collection = namedCollections.get(name);
        return collection != null ? collection : List.of();
    }

    public List<JsonNode> getTaggedCollection(String tag) {
        // First check lazy collections if memory optimization is enabled
        if (memoryOptimizationEnabled) {
            // Check cache first to ensure consistency
            String cacheKey = "tag:" + tag;
            List<JsonNode> cached = materializedCollectionCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
            
            List<LazyItemProxy> lazyCollection = lazyTaggedCollections.get(tag);
            if (lazyCollection != null) {
                List<JsonNode> materialized = materializeLazyCollection(lazyCollection);
                materializedCollectionCache.put(cacheKey, materialized);
                return materialized;
            }
        }

        // Fall back to eager collections
        List<JsonNode> collection = taggedCollections.get(tag);
        return collection != null ? collection : List.of();
    }

    /**
     * Helper method to materialize a lazy collection into a regular List<JsonNode>.
     * This is used when lazy collections need to be accessed for reference resolution.
     */
    private List<JsonNode> materializeLazyCollection(List<LazyItemProxy> lazyCollection) {
        List<JsonNode> materializedList = new ArrayList<>();
        for (LazyItemProxy lazyItem : lazyCollection) {
            materializedList.add(lazyItem.getMaterializedCopy());
        }
        return materializedList;
    }

    public Map<String, List<JsonNode>> getNamedCollections() {
        return new HashMap<>(namedCollections);
    }

    /**
     * Gets the lazy collections for memory-optimized generation.
     * This method should only be called when memory optimization is enabled.
     */
    public Map<String, List<LazyItemProxy>> getLazyNamedCollections() {
        if (!memoryOptimizationEnabled) {
            throw new IllegalStateException("Lazy collections are only available when memory optimization is enabled");
        }
        return new HashMap<>(lazyNamedCollections);
    }

    public JsonNode getNamedPick(String name) {
        return namedPicks.get(name);
    }

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

    /**
     * Enables memory optimization with the given referenced paths.
     */
    public void enableMemoryOptimization(Map<String, Set<String>> referencedPaths) {
        this.referencedPaths = new HashMap<>(referencedPaths);
        this.memoryOptimizationEnabled = true;
    }

    /**
     * Returns true if memory optimization is enabled.
     */
    public boolean isMemoryOptimizationEnabled() {
        return memoryOptimizationEnabled;
    }

    /**
     * Gets the referenced paths for a collection.
     */
    public Set<String> getReferencedPaths(String collection) {
        if (!memoryOptimizationEnabled || referencedPaths == null) {
            return Set.of();
        }
        return referencedPaths.getOrDefault(collection, Set.of());
    }

    /**
     * Sets the referenced paths for a collection.
     */
    public void setReferencedPaths(String collection, Set<String> paths) {
        if (referencedPaths == null) {
            referencedPaths = new HashMap<>();
        }
        referencedPaths.put(collection, paths);
    }
}
