package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.FilteringBehavior;
import com.github.eddranca.datagenerator.exception.FilteringException;
import com.github.eddranca.datagenerator.exception.InvalidReferenceException;
import com.github.eddranca.datagenerator.generator.FilteringGeneratorAdapter;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.node.SequentialTrackable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static com.github.eddranca.datagenerator.builder.KeyWords.THIS_PREFIX;

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
    private final Map<SequentialTrackable, Integer> sequentialCounters;
    private final int maxFilteringRetries;
    private final FilteringBehavior filteringBehavior;

    // Reference resolvers for different reference types
    private final ReferenceResolverRegistry referenceResolvers;

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
        this.referenceResolvers = initializeReferenceResolvers();
    }

    public GenerationContext(GeneratorRegistry generatorRegistry, Random random) {
        this(generatorRegistry, random, 100, FilteringBehavior.RETURN_NULL);
    }

    private ReferenceResolverRegistry initializeReferenceResolvers() {
        ReferenceResolverRegistry registry = new ReferenceResolverRegistry();
        registry.register(ref -> ref.startsWith("byTag["), this::resolveTagReference);
        registry.register(ref -> ref.startsWith(THIS_PREFIX), this::resolveThisReference);
        registry.register(ref -> ref.contains("[*]."), this::resolveArrayFieldReference);
        registry.register(ref -> ref.contains("["), this::resolveIndexedReference);
        registry.register(this::isPickReference, this::resolvePickReference);
        return registry;
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

    public Map<String, List<JsonNode>> getTaggedCollections() {
        return new HashMap<>(taggedCollections);
    }

    public JsonNode getNamedPick(String name) {
        return namedPicks.get(name);
    }

    /**
     * Gets a random or sequential element from a collection.
     */
    public JsonNode getElementFromCollection(List<JsonNode> collection, SequentialTrackable node, boolean sequential) {
        if (collection.isEmpty()) {
            return mapper.nullNode();
        }

        int index = sequential && node != null ?
                getNextSequentialIndex(node, collection.size()) :
                random.nextInt(collection.size());
        return collection.get(index);
    }

    /**
     * Applies filtering to a collection based on filter values.
     * If fieldName is provided, filters based on that field's value.
     * Otherwise, filters based on the entire object.
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
     * field values but return the original objects so field extraction can happen later.
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
     */
    public JsonNode handleFilteringFailure(String message) {
        if (filteringBehavior == FilteringBehavior.THROW_EXCEPTION) {
            throw new FilteringException(message);
        }
        return mapper.nullNode();
    }

    /**
     * Resolves a reference with full control over filtering and sequential behavior.
     */
    public JsonNode resolveReferenceWithFiltering(String reference, JsonNode currentItem, List<JsonNode> filterValues,
            SequentialTrackable node, boolean sequential) {
        // Prepare filtered collection if filtering is required
        CollectionContext collectionContext = new CollectionContext(currentItem, filterValues);
        if (filterValues != null && !filterValues.isEmpty()) {
            List<JsonNode> filteredCollection = getOrCreateFilteredCollection(reference, currentItem, filterValues);
            if (filteredCollection.isEmpty()) {
                return handleFilteringFailure("Reference '" + reference + "' has no valid values after filtering");
            }
            collectionContext.setFilteredCollection(filteredCollection);
        }

        // Resolve the reference using the appropriate resolver based on reference syntax
        return referenceResolvers.resolve(reference, collectionContext, node, sequential);
    }

    private boolean isPickReference(String reference) {
        return namedPicks.containsKey(reference) ||
               (reference.contains(".") && namedPicks.containsKey(reference.substring(0, reference.indexOf('.'))));
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

        return filteredCollectionCache.computeIfAbsent(key, k ->
            createFilteredCollection(reference, currentItem, filterValues));
    }

    private List<JsonNode> createFilteredCollection(String reference, JsonNode currentItem, List<JsonNode> filterValues) {
        // Use a strategy pattern approach to determine the correct collection creation method
        if (reference.startsWith("byTag[")) {
            return createTagBasedFilteredCollection(reference, currentItem, filterValues);
        } else if (reference.contains("[*].")) {
            return createArrayFieldFilteredCollection(reference, filterValues);
        } else {
            return createSimpleFilteredCollection(reference, filterValues);
        }
    }

    private List<JsonNode> createTagBasedFilteredCollection(String reference, JsonNode currentItem, List<JsonNode> filterValues) {
        int start = reference.indexOf('[') + 1;
        int end = reference.indexOf(']');
        String tagExpr = reference.substring(start, end);
        String fieldName = reference.length() > end + 1 && reference.charAt(end + 1) == '.' ?
                          reference.substring(end + 2) : "";

        String tag = resolveTagExpression(tagExpr, currentItem);
        if (tag == null) {
            return List.of();
        }

        List<JsonNode> sourceCollection = getTaggedCollection(tag);
        return applyFiltering(sourceCollection, fieldName, filterValues);
    }

    private String resolveTagExpression(String tagExpr, JsonNode currentItem) {
        if (tagExpr.startsWith(THIS_PREFIX)) {
            String localField = tagExpr.substring(THIS_PREFIX.length());
            JsonNode val = currentItem.path(localField);
            return (val == null || val.isNull()) ? null : val.asText();
        }
        return tagExpr;
    }

    private List<JsonNode> createArrayFieldFilteredCollection(String reference, List<JsonNode> filterValues) {
        String base = reference.substring(0, reference.indexOf("[*]."));
        String field = reference.substring(reference.indexOf("[*].") + 4);
        List<JsonNode> sourceCollection = getCollection(base);

        return applyFilteringOnField(sourceCollection, field, filterValues);
    }

    private List<JsonNode> createSimpleFilteredCollection(String reference, List<JsonNode> filterValues) {
        List<JsonNode> sourceCollection = getCollection(reference);
        return applyFiltering(sourceCollection, "", filterValues);
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
     * Gets the next sequential index for a reference field node.
     * Each reference field node maintains its own counter for round-robin access.
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

    private JsonNode resolveTagReference(String reference, CollectionContext context, SequentialTrackable node, boolean sequential) {
        // Extract tag expression and field name
        TagReferenceInfo tagInfo = parseTagReference(reference);

        // Get the collection to use
        List<JsonNode> collection = getTagCollection(context, tagInfo.tagExpression);
        if (collection.isEmpty()) {
            return mapper.nullNode();
        }

        // Select an element and extract field if needed
        JsonNode picked = getElementFromCollection(collection, node, sequential);
        return tagInfo.fieldName.isEmpty() ? picked : picked.path(tagInfo.fieldName);
    }

    /**
     * Parses a tag reference into its components.
     */
    private TagReferenceInfo parseTagReference(String reference) {
        int start = reference.indexOf('[') + 1;
        int end = reference.indexOf(']');
        String tagExpr = reference.substring(start, end);

        String fieldName = "";
        if (reference.length() > end + 1 && reference.charAt(end + 1) == '.') {
            fieldName = reference.substring(end + 2);
        }

        return new TagReferenceInfo(tagExpr, fieldName);
    }

    /**
     * Gets the appropriate collection for a tag reference.
     */
    private List<JsonNode> getTagCollection(CollectionContext context, String tagExpr) {
        if (context.hasFilteredCollection()) {
            return context.getFilteredCollection();
        }

        String tag = resolveTagValue(tagExpr, context.getCurrentItem());
        if (tag == null) {
            return List.of();
        }

        return getTaggedCollection(tag);
    }

    /**
     * Resolves a tag expression to its actual value.
     */
    private String resolveTagValue(String tagExpr, JsonNode currentItem) {
        if (!tagExpr.startsWith(THIS_PREFIX)) {
            return tagExpr;
        }

        String localField = tagExpr.substring(THIS_PREFIX.length());
        JsonNode val = currentItem.path(localField);
        return (val == null || val.isNull()) ? null : val.asText();
    }

    /**
     * Simple class to hold tag reference components.
     */
    private static class TagReferenceInfo {
        final String tagExpression;
        final String fieldName;

        TagReferenceInfo(String tagExpression, String fieldName) {
            this.tagExpression = tagExpression;
            this.fieldName = fieldName;
        }
    }

    private JsonNode resolveThisReference(String reference, CollectionContext context, SequentialTrackable node, boolean sequential) {
        return context.getCurrentItem().path(reference.substring(THIS_PREFIX.length()));
    }

    private JsonNode pickFieldFromCollection(List<JsonNode> collection, String field, SequentialTrackable node,
            boolean sequential) {
        JsonNode item = getElementFromCollection(collection, node, sequential);
        if (item.isNull()) {
            return mapper.nullNode();
        }

        JsonNode fieldValue = item.path(field);
        return fieldValue.isMissingNode() ? mapper.nullNode() : fieldValue;
    }

    private JsonNode resolveArrayFieldReference(String reference, CollectionContext context, SequentialTrackable node, boolean sequential) {
        String base = reference.substring(0, reference.indexOf("[*]."));
        String field = reference.substring(reference.indexOf("[*].") + 4);

        List<JsonNode> collection = context.hasFilteredCollection() ? context.getFilteredCollection() : getCollection(base);
        return pickFieldFromCollection(collection, field, node, sequential);
    }

    private JsonNode resolveIndexedReference(String reference, CollectionContext context, SequentialTrackable node, boolean sequential) {
        // Parse the reference into its components
        IndexedReferenceInfo refInfo = parseIndexedReference(reference);

        // Handle based on index type
        if (refInfo.isNumericIndex()) {
            return resolveNumericIndexReference(refInfo);
        } else if (refInfo.isWildcardIndex()) {
            return resolveWildcardIndexReference(refInfo, context, node, sequential);
        } else {
            // Invalid indexed reference pattern - this should have been caught during validation
            throw new InvalidReferenceException(reference);
        }
    }

    /**
     * Parses an indexed reference into its components.
     */
    private IndexedReferenceInfo parseIndexedReference(String reference) {
        String base = reference.substring(0, reference.indexOf("["));
        String inner = reference.substring(reference.indexOf("[") + 1, reference.indexOf("]"));

        String field = "";
        if (reference.contains("].")) {
            field = reference.substring(reference.indexOf("].") + 2);
        }

        return new IndexedReferenceInfo(base, inner, field);
    }

    /**
     * Resolves a reference with a specific numeric index.
     */
    private JsonNode resolveNumericIndexReference(IndexedReferenceInfo refInfo) {
        int index = Integer.parseInt(refInfo.index);
        List<JsonNode> collection = getCollection(refInfo.base);

        if (index >= collection.size()) {
            return mapper.nullNode();
        }

        JsonNode item = collection.get(index);
        return refInfo.field.isEmpty() ? item : item.path(refInfo.field);
    }

    /**
     * Resolves a reference with a wildcard index (random or sequential selection).
     */
    private JsonNode resolveWildcardIndexReference(IndexedReferenceInfo refInfo,
                                                CollectionContext context,
                                                SequentialTrackable node,
                                                boolean sequential) {
        List<JsonNode> collection = context.hasFilteredCollection() ?
                context.getFilteredCollection() : getCollection(refInfo.base);

        if (collection.isEmpty()) {
            return mapper.nullNode();
        }

        JsonNode item = getElementFromCollection(collection, node, sequential);
        return refInfo.field.isEmpty() ? item : item.path(refInfo.field);
    }

    /**
     * Simple class to hold indexed reference components.
     */
    private static class IndexedReferenceInfo {
        final String base;
        final String index;
        final String field;

        IndexedReferenceInfo(String base, String index, String field) {
            this.base = base;
            this.index = index;
            this.field = field;
        }

        boolean isNumericIndex() {
            return index.matches("\\d+");
        }

        boolean isWildcardIndex() {
            return "*".equals(index);
        }
    }

    private JsonNode resolvePickReference(String reference, CollectionContext context, SequentialTrackable node, boolean sequential) {
        if (namedPicks.containsKey(reference)) {
            return namedPicks.get(reference);
        } else if (reference.contains(".")) {
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
