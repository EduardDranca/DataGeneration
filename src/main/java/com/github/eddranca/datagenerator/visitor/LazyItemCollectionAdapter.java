package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.stream.Stream;

/**
 * Adapter that wraps a LazyItemCollection to provide List<JsonNode> interface
 * by materializing LazyItemProxy objects on demand.
 */
public class LazyItemCollectionAdapter implements List<JsonNode> {
    private final LazyItemCollection lazyCollection;

    public LazyItemCollectionAdapter(LazyItemCollection lazyCollection) {
        this.lazyCollection = lazyCollection;
    }

    @Override
    public int size() {
        return lazyCollection.size();
    }

    @Override
    public boolean isEmpty() {
        return lazyCollection.isEmpty();
    }

    @Override
    public JsonNode get(int index) {
        LazyItemProxy lazyItem = lazyCollection.get(index);
        return lazyItem.getMaterializedCopy();
    }

    @Override
    public Stream<JsonNode> stream() {
        return lazyCollection.stream()
            .map(LazyItemProxy::getMaterializedCopy);
    }

    @Override
    public Iterator<JsonNode> iterator() {
        return new Iterator<JsonNode>() {
            private final Iterator<LazyItemProxy> lazyIterator = lazyCollection.iterator();

            @Override
            public boolean hasNext() {
                return lazyIterator.hasNext();
            }

            @Override
            public JsonNode next() {
                return lazyIterator.next().getMaterializedCopy();
            }
        };
    }

    // Unsupported operations for read-only adapter
    @Override
    public boolean add(JsonNode jsonNode) {
        throw new UnsupportedOperationException("LazyItemCollectionAdapter is read-only");
    }

    @Override
    public void add(int index, JsonNode element) {
        throw new UnsupportedOperationException("LazyItemCollectionAdapter is read-only");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("LazyItemCollectionAdapter is read-only");
    }

    @Override
    public JsonNode remove(int index) {
        throw new UnsupportedOperationException("LazyItemCollectionAdapter is read-only");
    }

    @Override
    public boolean addAll(Collection<? extends JsonNode> c) {
        throw new UnsupportedOperationException("LazyItemCollectionAdapter is read-only");
    }

    @Override
    public boolean addAll(int index, Collection<? extends JsonNode> c) {
        throw new UnsupportedOperationException("LazyItemCollectionAdapter is read-only");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("LazyItemCollectionAdapter is read-only");
    }

    @Override
    public JsonNode set(int index, JsonNode element) {
        throw new UnsupportedOperationException("LazyItemCollectionAdapter is read-only");
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException("Contains check would require full materialization");
    }

    @Override
    public Object[] toArray() {
        return stream().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return stream().toArray(size -> a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("ContainsAll check would require full materialization");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("LazyItemCollectionAdapter is read-only");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("LazyItemCollectionAdapter is read-only");
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