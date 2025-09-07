package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.eddranca.datagenerator.FilteringBehavior;
import com.github.eddranca.datagenerator.FilteringException;
import com.github.eddranca.datagenerator.exception.InvalidReferenceException;
import com.github.eddranca.datagenerator.generator.FilteringGeneratorAdapter;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.node.ReferenceFieldNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Context for data generation that maintains state during visitor traversal.
 * Tracks generated collections, tagged collections, and provides access to
 * generators.
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
    private final Map<FilteredCollectionKey, List<JsonNode>> filteredCollectionCache;
    private final Map<ReferenceFieldNode, Integer> sequentialCounters;
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
        this.filteredCollectionCache = new HashMap<>();
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
        return namedCollections.get(name);
    }

    public List<JsonNode> getTaggedCollection(String tag) {
        return taggedCollections.get(tag);
    }

    public Map<String, List<JsonNode>> getNamedCollections() {
        return new HashMap<>(namedCollections);
    }

    public Map<String, List<JsonNode>> getTaggedCollections() {
        return new HashMap<>(taggedCollections);
    }

    /**
     * Resolves a reference with full control over filtering and sequential behavior.
     */
    public JsonNode resolveReferenceWithFiltering(String reference, JsonNode currentItem, List<JsonNode> filterValues,
            ReferenceFieldNode node, boolean sequential) {
        // Get or create filtered collection if filtering is needed
        List<JsonNode> filteredCollection = null;
        if (filterValues != null && !filterValues.isEmpty()) {
            filteredCollection = getOrCreateFilteredCollection(reference, currentItem, filterValues);
            if (filteredCollection != null && filteredCollection.isEmpty()) {
                return handleFilteringFailure(
                        new FilteringException("Reference '" + reference + "' has no valid values after filtering"));
            }
        }

        if (reference.startsWith("byTag[")) {
            return resolveTagReference(reference, currentItem, filteredCollection, node, sequential);
        } else if (reference.startsWith("this.")) {
            return resolveThisReference(reference, currentItem);
        } else if (reference.contains("[*].")) {
            return resolveArrayFieldReference(reference, filteredCollection, node, sequential);
        } else if (reference.contains("[")) {
            return resolveIndexedReference(reference, filteredCollection, node, sequential);
        } else if (namedPicks.containsKey(reference) || (reference.contains(".")
                && namedPicks.containsKey(reference.substring(0, reference.indexOf('.'))))) {
            return resolvePickReference(reference);
        } else {
            // Invalid reference pattern - this should have been caught during validation
            throw new InvalidReferenceException(reference);
        }
    }

    private JsonNode getReferencedElementFromCollection(String reference, ReferenceFieldNode node, boolean sequential,
            List<JsonNode> filteredCollection) {
        List<JsonNode> collection = filteredCollection != null ? filteredCollection : getCollection(reference);
        if (collection == null || collection.isEmpty()) {
            return mapper.nullNode();
        }

        int index = sequential && node != null ? getNextSequentialIndex(node, collection.size())
                : random.nextInt(collection.size());
        return collection.get(index);
    }

    /**
     * Gets or creates a filtered collection for the given reference and filter
     * values.
     * Uses caching to avoid recomputing the same filtered collection multiple
     * times.
     */
    public List<JsonNode> getOrCreateFilteredCollection(String reference, JsonNode currentItem,
            List<JsonNode> filterValues) {
        FilteredCollectionKey key = new FilteredCollectionKey(reference, currentItem, filterValues);

        return filteredCollectionCache.computeIfAbsent(key, k -> {
            List<JsonNode> sourceCollection;
            String fieldName = "";

            if (reference.startsWith("byTag[")) {
                int start = reference.indexOf('[') + 1;
                int end = reference.indexOf(']');
                String tagExpr = reference.substring(start, end);

                if (reference.length() > end + 1 && reference.charAt(end + 1) == '.') {
                    fieldName = reference.substring(end + 2);
                }

                String tag;
                if (tagExpr.startsWith("this.")) {
                    String localField = tagExpr.substring(5);
                    JsonNode val = currentItem.path(localField);
                    if (val == null || val.isNull())
                        return new ArrayList<>();
                    tag = val.asText();
                } else {
                    tag = tagExpr;
                }

                sourceCollection = getTaggedCollection(tag);
            } else if (reference.contains("[*].")) {
                String base = reference.substring(0, reference.indexOf("[*]."));
                String field = reference.substring(reference.indexOf("[*].") + 4);
                sourceCollection = getCollection(base);

                // Filter based on the field values, but keep the original objects
                if (sourceCollection != null) {
                    return applyFilteringOnField(sourceCollection, field, filterValues);
                }
            } else {
                // Simple collection reference (no special syntax)
                sourceCollection = getCollection(reference);
            }

            if (sourceCollection == null) {
                return new ArrayList<>();
            }

            return applyFiltering(sourceCollection, fieldName, filterValues);
        });
    }

    /**
     * Clears the filtered collection cache. Should be called when generation
     * context is reset
     * or when we want to ensure fresh filtering results.
     */
    public void clearFilteredCollectionCache() {
        filteredCollectionCache.clear();
    }

    /**
     * Handles filtering failure according to the configured filtering behavior.
     *
     * @param exception the FilteringException that occurred
     * @return null JsonNode if behavior is RETURN_NULL
     * @throws FilteringException if behavior is THROW_EXCEPTION
     */
    private JsonNode handleFilteringFailure(FilteringException exception) {
        if (filteringBehavior == FilteringBehavior.THROW_EXCEPTION) {
            throw exception;
        }
        return mapper.nullNode();
    }

    /**
     * Gets the next sequential index for a reference field node.
     * Each reference field node maintains its own counter for round-robin access.
     *
     * @param node           the reference field node
     * @param collectionSize the size of the collection being referenced
     * @return the next sequential index (with automatic wrap-around)
     */
    public int getNextSequentialIndex(ReferenceFieldNode node, int collectionSize) {
        if (collectionSize <= 0) {
            return 0;
        }

        int current = sequentialCounters.getOrDefault(node, 0);
        int index = current % collectionSize;
        sequentialCounters.put(node, current + 1);
        return index;
    }

    private JsonNode resolveTagReference(String reference, JsonNode currentItem, List<JsonNode> preFilteredCollection,
            ReferenceFieldNode node, boolean sequential) {
        int start = reference.indexOf('[') + 1;
        int end = reference.indexOf(']');
        String tagExpr = reference.substring(start, end);

        String fieldNameAfter = "";
        if (reference.length() > end + 1 && reference.charAt(end + 1) == '.') {
            fieldNameAfter = reference.substring(end + 2);
        }

        List<JsonNode> collection;
        if (preFilteredCollection != null) {
            collection = preFilteredCollection;
        } else {
            String tag;
            if (tagExpr.startsWith("this.")) {
                String localField = tagExpr.substring(5);
                JsonNode val = currentItem.path(localField);
                if (val == null || val.isNull())
                    return mapper.nullNode();
                tag = val.asText();
            } else {
                tag = tagExpr;
            }

            collection = getTaggedCollection(tag);
        }

        if (collection == null || collection.isEmpty()) {
            return mapper.nullNode();
        }

        // Pick item (sequential or random) and extract field if needed
        int index = sequential && node != null ? getNextSequentialIndex(node, collection.size())
                : random.nextInt(collection.size());
        JsonNode picked = collection.get(index);
        return fieldNameAfter.isEmpty() ? picked : picked.path(fieldNameAfter);
    }

    private JsonNode resolveThisReference(String reference, JsonNode currentItem) {
        return currentItem.path(reference.substring(5));
    }

    private JsonNode pickFieldFromCollection(List<JsonNode> collection, String field, ReferenceFieldNode node,
            boolean sequential) {
        if (collection == null || collection.isEmpty()) {
            return mapper.nullNode();
        }

        // Pick item (sequential or random) and extract field
        int index = sequential && node != null ? getNextSequentialIndex(node, collection.size())
                : random.nextInt(collection.size());
        JsonNode item = collection.get(index);
        JsonNode fieldValue = item.path(field);

        return fieldValue.isMissingNode() ? mapper.nullNode() : fieldValue;
    }

    private JsonNode resolveArrayFieldReference(String reference, List<JsonNode> preFilteredCollection,
            ReferenceFieldNode node, boolean sequential) {
        String base = reference.substring(0, reference.indexOf("[*]."));
        String field = reference.substring(reference.indexOf("[*].") + 4);

        List<JsonNode> collection = preFilteredCollection != null ? preFilteredCollection : getCollection(base);
        return pickFieldFromCollection(collection, field, node, sequential);
    }

    private JsonNode resolveIndexedReference(String reference, List<JsonNode> preFilteredCollection,
            ReferenceFieldNode node, boolean sequential) {
        String base = reference.substring(0, reference.indexOf("["));
        String inner = reference.substring(reference.indexOf("[") + 1, reference.indexOf("]"));

        if (inner.matches("\\d+")) {
            // Numeric index
            int index = Integer.parseInt(inner);
            List<JsonNode> collection = getCollection(base);
            if (collection == null || index >= collection.size()) {
                return mapper.nullNode();
            }

            JsonNode item = collection.get(index);

            // Check if there's a field access after the index
            if (reference.contains("].")) {
                String field = reference.substring(reference.indexOf("].") + 2);
                return item.path(field);
            } else {
                return item;
            }
        } else if ("*".equals(inner)) {
            // Wildcard - return random item from collection
            return getReferencedElementFromCollection(base, node, sequential, preFilteredCollection);
        } else {
            // Invalid indexed reference pattern - this should have been caught during
            // validation
            throw new InvalidReferenceException(reference);
        }
    }

    private JsonNode resolvePickReference(String reference) {
        if (namedPicks.containsKey(reference)) {
            return namedPicks.get(reference);
        } else {
            String base = reference.substring(0, reference.indexOf('.'));
            String field = reference.substring(reference.indexOf('.') + 1);
            JsonNode pick = namedPicks.get(base);
            if (pick != null && pick.has(field)) {
                return pick.path(field);
            }
        }
        return mapper.nullNode();
    }

    /**
     * Applies filtering to a collection based on filter values.
     * If fieldName is provided, filters based on that field's value.
     * Otherwise, filters based on the entire object.
     */
    private List<JsonNode> applyFiltering(List<JsonNode> collection, String fieldName, List<JsonNode> filterValues) {
        List<JsonNode> filtered = new ArrayList<>();

        for (JsonNode item : collection) {
            JsonNode valueToCheck = fieldName.isEmpty() ? item : item.path(fieldName);

            boolean shouldFilter = false;
            for (JsonNode filterValue : filterValues) {
                if (valueToCheck.equals(filterValue)) {
                    shouldFilter = true;
                    break;
                }
            }

            if (!shouldFilter) {
                filtered.add(item);
            }
        }

        return filtered;
    }

    /**
     * Applies filtering to a collection based on field values, but keeps the
     * original objects.
     * This is used for field extraction cases where we want to filter based on
     * field values
     * but return the original objects so field extraction can happen later.
     */
    private List<JsonNode> applyFilteringOnField(List<JsonNode> collection, String fieldName,
            List<JsonNode> filterValues) {
        List<JsonNode> filtered = new ArrayList<>();

        for (JsonNode item : collection) {
            JsonNode fieldValue = item.path(fieldName);

            if (!fieldValue.isMissingNode()) {
                boolean shouldFilter = false;
                for (JsonNode filterValue : filterValues) {
                    if (fieldValue.equals(filterValue)) {
                        shouldFilter = true;
                        break;
                    }
                }

                if (!shouldFilter) {
                    filtered.add(item);
                }
            }
        }

        return filtered;
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
            return handleFilteringFailure(e);
        }
    }

    /**
     * Key class for caching filtered collections.
     * Combines reference string, current item context, and filter values to create
     * a unique cache key.
     * Uses pre-computed hash code for better performance in HashMap operations.
     */
    private static class FilteredCollectionKey {
        private final String reference;
        private final JsonNode currentItem;
        private final List<JsonNode> filterValues;
        private final int hashCode;

        public FilteredCollectionKey(String reference, JsonNode currentItem, List<JsonNode> filterValues) {
            this.reference = reference;
            this.currentItem = currentItem;
            this.filterValues = filterValues;
            this.hashCode = Objects.hash(reference, currentItem, filterValues);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;

            FilteredCollectionKey that = (FilteredCollectionKey) obj;
            return Objects.equals(reference, that.reference) &&
                    Objects.equals(currentItem, that.currentItem) &&
                    Objects.equals(filterValues, that.filterValues);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
