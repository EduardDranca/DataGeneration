package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.node.CollectionNode;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A lazy collection that generates items on-demand during streaming.
 * This allows for memory-efficient generation where items are only created
 * when they're actually needed (during JSON serialization or SQL generation).
 */
public class LazyCollection implements List<JsonNode> {
    private final CollectionNode collectionNode;
    private final DataGenerationVisitor visitor;
    private final String collectionName;
    private final Set<String> referencedPaths;
    private final List<JsonNode> materializedItems;
    private boolean fullyMaterialized = false;

    public LazyCollection(CollectionNode collectionNode, DataGenerationVisitor visitor, 
                         String collectionName, Set<String> referencedPaths) {
        this.collectionNode = collectionNode;
        this.visitor = visitor;
        this.collectionName = collectionName;
        this.referencedPaths = referencedPaths != null ? referencedPaths : Set.of();
        this.materializedItems = new ArrayList<>();
    }

    @Override
    public int size() {
        return collectionNode.getCount();
    }

    @Override
    public boolean isEmpty() {
        return collectionNode.getCount() == 0;
    }

    @Override
    public JsonNode get(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }

        // Ensure we have materialized up to this index
        materializeUpTo(index);
        return materializedItems.get(index);
    }

    /**
     * Materializes items up to the specified index (inclusive).
     */
    private void materializeUpTo(int index) {
        while (materializedItems.size() <= index) {
            JsonNode item = generateItem();
            materializedItems.add(item);
        }
    }

    /**
     * Generates a single item using the visitor.
     * This creates a LazyItemProxy that only materializes referenced fields initially.
     */
    private JsonNode generateItem() {
        // System.out.println("LazyCollection: Generating item for collection: " + collectionName + 
        //                   " with referenced paths: " + referencedPaths);
        
        // Create a LazyItemProxy that only generates referenced fields initially
        LazyItemProxy lazyItem = new LazyItemProxy(
            collectionName, 
            collectionNode.getItem().getFields(), 
            referencedPaths, 
            visitor
        );
        
        return lazyItem;
    }

    /**
     * Returns a stream that generates items lazily without caching.
     * This is the key method for memory-efficient streaming.
     */
    @Override
    public Stream<JsonNode> stream() {
        return StreamSupport.stream(
            Spliterators.spliterator(streamingIterator(), size(), Spliterator.ORDERED | Spliterator.SIZED),
            false
        );
    }

    @Override
    public Iterator<JsonNode> iterator() {
        return new LazyIterator();
    }

    /**
     * Returns a streaming iterator that doesn't cache items.
     * This is used by stream() for true memory efficiency.
     */
    private Iterator<JsonNode> streamingIterator() {
        return new StreamingIterator();
    }

    /**
     * Iterator that generates items on-demand and caches them for indexed access.
     */
    private class LazyIterator implements Iterator<JsonNode> {
        private int currentIndex = 0;

        @Override
        public boolean hasNext() {
            return currentIndex < size();
        }

        @Override
        public JsonNode next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return get(currentIndex++);
        }
    }

    /**
     * Iterator that generates items on-demand WITHOUT caching them.
     * This is used for streaming operations to maintain memory efficiency.
     */
    private class StreamingIterator implements Iterator<JsonNode> {
        private int currentIndex = 0;

        @Override
        public boolean hasNext() {
            return currentIndex < size();
        }

        @Override
        public JsonNode next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            currentIndex++;
            // Generate item directly without caching
            return generateItem();
        }
    }

    /**
     * Forces full materialization of all items.
     * This should only be called when absolutely necessary (e.g., for JSON serialization).
     */
    public void materializeAll() {
        if (!fullyMaterialized) {
            materializeUpTo(size() - 1);
            fullyMaterialized = true;
        }
    }

    /**
     * Gets the referenced paths for this collection.
     */
    public Set<String> getReferencedPaths() {
        return referencedPaths;
    }

    /**
     * Checks if this collection has any referenced paths.
     */
    public boolean hasReferencedPaths() {
        return !referencedPaths.isEmpty();
    }

    // Unsupported operations for a read-only lazy collection
    @Override
    public boolean add(JsonNode jsonNode) {
        throw new UnsupportedOperationException("LazyCollection is read-only");
    }

    @Override
    public void add(int index, JsonNode element) {
        throw new UnsupportedOperationException("LazyCollection is read-only");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("LazyCollection is read-only");
    }

    @Override
    public JsonNode remove(int index) {
        throw new UnsupportedOperationException("LazyCollection is read-only");
    }

    @Override
    public boolean addAll(Collection<? extends JsonNode> c) {
        throw new UnsupportedOperationException("LazyCollection is read-only");
    }

    @Override
    public boolean addAll(int index, Collection<? extends JsonNode> c) {
        throw new UnsupportedOperationException("LazyCollection is read-only");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("LazyCollection is read-only");
    }

    @Override
    public JsonNode set(int index, JsonNode element) {
        throw new UnsupportedOperationException("LazyCollection is read-only");
    }

    @Override
    public boolean contains(Object o) {
        // This would require full materialization, which we want to avoid
        throw new UnsupportedOperationException("Contains check would require full materialization");
    }

    @Override
    public Object[] toArray() {
        materializeAll();
        return materializedItems.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        materializeAll();
        return materializedItems.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("ContainsAll check would require full materialization");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("LazyCollection is read-only");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("LazyCollection is read-only");
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException("IndexOf would require full materialization");
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException("LastIndexOf would require full materialization");
    }

    @Override
    public ListIterator<JsonNode> listIterator() {
        throw new UnsupportedOperationException("ListIterator not supported for lazy collections");
    }

    @Override
    public ListIterator<JsonNode> listIterator(int index) {
        throw new UnsupportedOperationException("ListIterator not supported for lazy collections");
    }

    @Override
    public List<JsonNode> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("SubList not supported for lazy collections");
    }
}