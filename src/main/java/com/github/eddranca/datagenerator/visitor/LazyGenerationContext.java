package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.FilteringBehavior;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Generation context for lazy (memory-optimized) data generation.
 * <p>
 * This implementation uses lazy evaluation and caching to minimize memory usage.
 * Collections are stored as lazy proxies and materialized on-demand, with
 * caching to ensure consistency across multiple accesses.
 */
public class LazyGenerationContext extends AbstractGenerationContext {
    // Lazy collection support for memory optimization
    private final Map<String, List<LazyItemProxy>> lazyNamedCollections;
    private final Map<String, List<LazyItemProxy>> lazyReferenceCollections;
    private final Map<String, List<LazyItemProxy>> lazyTaggedCollections;
    private final Map<String, JsonNode> namedPicks;

    // Cache for materialized lazy collections to ensure consistency
    private final Map<String, List<JsonNode>> materializedCollectionCache;

    // Memory optimization fields
    private Map<String, Set<String>> referencedPaths;

    public LazyGenerationContext(GeneratorRegistry generatorRegistry, Random random,
                                 int maxFilteringRetries, FilteringBehavior filteringBehavior) {
        super(generatorRegistry, random, maxFilteringRetries, filteringBehavior);
        this.lazyNamedCollections = new HashMap<>();
        this.lazyReferenceCollections = new HashMap<>();
        this.lazyTaggedCollections = new HashMap<>();
        this.namedPicks = new HashMap<>();
        this.materializedCollectionCache = new HashMap<>();
    }

    public LazyGenerationContext(GeneratorRegistry generatorRegistry, Random random) {
        this(generatorRegistry, random, 100, FilteringBehavior.RETURN_NULL);
    }

    // Lazy collection registration methods for memory optimization
    @Override
    public void registerLazyCollection(String name, LazyItemCollection collection) {
        List<LazyItemProxy> existing = lazyNamedCollections.get(name);
        if (existing == null) {
            lazyNamedCollections.put(name, collection);
        } else {
            // Merge collections with the same name
            CompositeLazyItemCollection composite;
            if (existing instanceof CompositeLazyItemCollection existingComposite) {
                composite = existingComposite;
            } else {
                composite = new CompositeLazyItemCollection(name);
                composite.addCollection((LazyItemCollection) existing);
                lazyNamedCollections.put(name, composite);
            }
            composite.addCollection(collection);
        }
    }

    @Override
    public void registerLazyReferenceCollection(String name, LazyItemCollection collection) {
        lazyReferenceCollections.put(name, collection);
    }

    @Override
    public void registerLazyTaggedCollection(String tag, LazyItemCollection collection) {
        lazyTaggedCollections.put(tag, collection);
    }

    /**
     * Gets the lazy collections for memory-optimized generation.
     */
    @Override
    public Map<String, List<LazyItemProxy>> getLazyNamedCollections() {
        return new HashMap<>(lazyNamedCollections);
    }

    @Override
    public void registerCollection(String name, List<JsonNode> collection) {
        // For lazy context, we don't support direct eager collection registration
        // This should not be called in lazy mode, but we'll provide a fallback
        throw new UnsupportedOperationException("Lazy generation context does not support eager collection registration. Use registerLazyCollection instead.");
    }

    @Override
    public void registerReferenceCollection(String name, List<JsonNode> collection) {
        // For lazy context, we don't support direct eager collection registration
        throw new UnsupportedOperationException("Lazy generation context does not support eager collection registration. Use registerLazyReferenceCollection instead.");
    }

    @Override
    public void registerTaggedCollection(String tag, List<JsonNode> collection) {
        // For lazy context, we don't support direct eager collection registration
        throw new UnsupportedOperationException("Lazy generation context does not support eager collection registration. Use registerLazyTaggedCollection instead.");
    }

    @Override
    public void registerPick(String name, JsonNode value) {
        namedPicks.put(name, value);
    }

    @Override
    public List<JsonNode> getCollection(String name) {
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

        return List.of();
    }

    @Override
    public List<JsonNode> getTaggedCollection(String tag) {
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

        return List.of();
    }

    @Override
    public JsonNode getNamedPick(String name) {
        return namedPicks.get(name);
    }

    @Override
    public Map<String, List<JsonNode>> getNamedCollections() {
        // For lazy context, we need to materialize all collections
        Map<String, List<JsonNode>> result = new HashMap<>();
        for (Map.Entry<String, List<LazyItemProxy>> entry : lazyNamedCollections.entrySet()) {
            result.put(entry.getKey(), materializeLazyCollection(entry.getValue()));
        }
        return result;
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

    /**
     * Enables memory optimization with the given referenced paths.
     */
    @Override
    public void enableMemoryOptimization(Map<String, Set<String>> referencedPaths) {
        this.referencedPaths = new HashMap<>(referencedPaths);
    }

    /**
     * Gets the referenced paths for a collection.
     */
    @Override
    public Set<String> getReferencedPaths(String collection) {
        if (referencedPaths == null) {
            return Set.of();
        }
        return referencedPaths.getOrDefault(collection, Set.of());
    }

    /**
     * Sets the referenced paths for a collection.
     */
    @Override
    public void setReferencedPaths(String collection, Set<String> paths) {
        if (referencedPaths == null) {
            referencedPaths = new HashMap<>();
        }
        referencedPaths.put(collection, paths);
    }

    @Override
    public boolean isMemoryOptimizationEnabled() {
        return true;
    }
}
