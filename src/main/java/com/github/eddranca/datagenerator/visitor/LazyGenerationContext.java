package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.FilteringBehavior;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.node.CollectionNode;

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
public class LazyGenerationContext extends AbstractGenerationContext<LazyItemProxy> {
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

    @Override
    public void registerCollection(String name, List<LazyItemProxy> collection) {
        List<LazyItemProxy> existing = lazyNamedCollections.get(name);
        if (existing == null) {
            lazyNamedCollections.put(name, new ArrayList<>(collection));
        } else {
            // Merge collections with the same name
            existing.addAll(collection);
        }
    }

    @Override
    public void registerReferenceCollection(String name, List<LazyItemProxy> collection) {
        lazyReferenceCollections.put(name, new ArrayList<>(collection));
    }

    @Override
    public void registerTaggedCollection(String tag, List<LazyItemProxy> collection) {
        List<LazyItemProxy> existing = lazyTaggedCollections.get(tag);
        if (existing == null) {
            lazyTaggedCollections.put(tag, new ArrayList<>(collection));
        } else {
            // Merge collections with the same tag
            existing.addAll(collection);
        }
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

        // Check lazy reference collections first (highest priority)
        List<LazyItemProxy> lazyCollection = lazyReferenceCollections.get(name);
        if (lazyCollection != null) {
            List<JsonNode> materialized = materializeLazyCollection(lazyCollection);
            materializedCollectionCache.put(name, materialized);
            return materialized;
        }

        // Check lazy named collections
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

        // Check lazy tagged collections
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
    public Map<String, List<LazyItemProxy>> getNamedCollections() {
        return lazyNamedCollections;
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

    @Override
    public Set<String> getReferencedPaths(String collection) {
        if (referencedPaths == null) {
            return Set.of();
        }
        return referencedPaths.getOrDefault(collection, Set.of());
    }

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

    @Override
    public JsonNode createAndRegisterCollection(CollectionNode node, DataGenerationVisitor visitor) {
        Set<String> referencedPaths = getReferencedPaths(node.getCollectionName());

        // Generate all items with only referenced fields materialized
        List<LazyItemProxy> lazyCollection = createLazyItemList(node, referencedPaths, visitor);

        // Register the lazy collection
        registerCollection(node.getCollectionName(), lazyCollection);

        if (!node.getName().equals(node.getCollectionName())) {
            registerReferenceCollection(node.getName(), lazyCollection);
        }

        for (String tag : node.getTags()) {
            registerTaggedCollection(tag, lazyCollection);
        }

        return mapper.createArrayNode();
    }

    @Override
    public void registerPickFromCollection(String alias, int index, String collectionName) {
        // Look up the collection from our own storage
        List<LazyItemProxy> lazyCollection = lazyNamedCollections.get(collectionName);
        if (lazyCollection == null) {
            lazyCollection = lazyReferenceCollections.get(collectionName);
        }

        if (lazyCollection != null && index < lazyCollection.size()) {
            LazyItemProxy lazyItem = lazyCollection.get(index);
            // Materialize the specific item for the pick
            JsonNode pickedItem = lazyItem.getMaterializedCopy();
            registerPick(alias, pickedItem);
        }
    }

    /**
     * Creates a list of LazyItemProxy objects for a collection.
     */
    private List<LazyItemProxy> createLazyItemList(CollectionNode node, Set<String> referencedPaths, DataGenerationVisitor visitor) {
        List<LazyItemProxy> items = new ArrayList<>();
        int count = node.getCount();

        for (int i = 0; i < count; i++) {
            LazyItemProxy item = new LazyItemProxy(
                node.getCollectionName(),
                node.getItem().getFields(),
                referencedPaths,
                visitor
            );
            items.add(item);
        }

        return items;
    }
}
