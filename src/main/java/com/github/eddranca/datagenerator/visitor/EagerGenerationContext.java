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

/**
 * Generation context for eager (non-memory-optimized) data generation.
 * <p>
 * This implementation stores all generated data in memory immediately and
 * provides direct access to collections. It's suitable for smaller datasets
 * where memory usage is not a concern.
 */
public class EagerGenerationContext extends AbstractGenerationContext<JsonNode> {
    private final Map<String, List<JsonNode>> namedCollections; // Final collections for output
    private final Map<String, List<JsonNode>> referenceCollections; // Collections available for references (includes DSL keys)
    private final Map<String, List<JsonNode>> taggedCollections;
    private final Map<String, JsonNode> namedPicks;

    public EagerGenerationContext(GeneratorRegistry generatorRegistry, Random random,
                                  int maxFilteringRetries, FilteringBehavior filteringBehavior) {
        super(generatorRegistry, random, maxFilteringRetries, filteringBehavior);
        this.namedCollections = new HashMap<>();
        this.referenceCollections = new HashMap<>();
        this.taggedCollections = new HashMap<>();
        this.namedPicks = new HashMap<>();
    }

    public EagerGenerationContext(GeneratorRegistry generatorRegistry, Random random) {
        this(generatorRegistry, random, 100, FilteringBehavior.RETURN_NULL);
    }

    @Override
    public void registerCollection(String name, List<JsonNode> collection) {
        List<JsonNode> existing = namedCollections.get(name);
        if (existing == null) {
            namedCollections.put(name, new ArrayList<>(collection));
        } else {
            // Merge collections with the same name
            existing.addAll(collection);
        }
    }

    @Override
    public void registerReferenceCollection(String name, List<JsonNode> collection) {
        referenceCollections.put(name, new ArrayList<>(collection));
    }

    @Override
    public void registerTaggedCollection(String tag, List<JsonNode> collection) {
        List<JsonNode> existing = taggedCollections.get(tag);
        if (existing == null) {
            taggedCollections.put(tag, new ArrayList<>(collection));
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
        List<JsonNode> collection = referenceCollections.get(name);
        if (collection != null) {
            return collection;
        }
        collection = namedCollections.get(name);
        return collection != null ? collection : List.of();
    }

    @Override
    public List<JsonNode> getTaggedCollection(String tag) {
        List<JsonNode> collection = taggedCollections.get(tag);
        return collection != null ? collection : List.of();
    }

    @Override
    public JsonNode getNamedPick(String name) {
        return namedPicks.get(name);
    }

    @Override
    public Map<String, List<JsonNode>> getNamedCollections() {
        return new HashMap<>(namedCollections);
    }

    @Override
    public boolean isMemoryOptimizationEnabled() {
        return false;
    }

    @Override
    public JsonNode createAndRegisterCollection(CollectionNode node, DataGenerationVisitor<JsonNode> visitor) {
        // Standard eager generation
        List<JsonNode> items = new ArrayList<>();

        for (int i = 0; i < node.getCount(); i++) {
            JsonNode item = node.getItem().accept(visitor);
            items.add(item);
        }

        // Register the collection
        registerCollection(node.getCollectionName(), items);

        if (!node.getName().equals(node.getCollectionName())) {
            registerReferenceCollection(node.getName(), items);
        }

        for (String tag : node.getTags()) {
            registerTaggedCollection(tag, items);
        }

        return mapper.valueToTree(items);
    }

    @Override
    public void registerPickFromCollection(String alias, int index, String collectionName) {
        List<JsonNode> items = getCollection(collectionName);
        if (index < items.size()) {
            registerPick(alias, items.get(index));
        }
    }
}
