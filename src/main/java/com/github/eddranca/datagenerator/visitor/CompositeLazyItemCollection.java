package com.github.eddranca.datagenerator.visitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Stream;

/**
 * A composite lazy collection that combines multiple LazyItemCollection instances.
 * This is used for multi-step collections where multiple collection definitions
 * contribute to the same final collection name.
 */
public class CompositeLazyItemCollection implements List<LazyItemProxy> {
    private final List<LazyItemCollection> collections;
    private final String collectionName;

    public CompositeLazyItemCollection(String collectionName) {
        this.collections = new ArrayList<>();
        this.collectionName = collectionName;
    }

    public void addCollection(LazyItemCollection collection) {
        collections.add(collection);
    }

    @Override
    public int size() {
        return collections.stream().mapToInt(List::size).sum();
    }

    @Override
    public boolean isEmpty() {
        return collections.stream().allMatch(List::isEmpty);
    }

    @Override
    public LazyItemProxy get(int index) {
        int currentIndex = 0;
        for (LazyItemCollection collection : collections) {
            int collectionSize = collection.size();
            if (index < currentIndex + collectionSize) {
                return collection.get(index - currentIndex);
            }
            currentIndex += collectionSize;
        }
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
    }

    @Override
    public Stream<LazyItemProxy> stream() {
        return collections.stream().flatMap(List::stream);
    }

    // Unsupported operations for lazy collections
    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException("Contains not supported for lazy collections");
    }

    @Override
    public Iterator<LazyItemProxy> iterator() {
        return stream().iterator();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("ToArray not supported for lazy collections");
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("ToArray not supported for lazy collections");
    }

    @Override
    public boolean add(LazyItemProxy lazyItemProxy) {
        throw new UnsupportedOperationException("Add not supported for lazy collections");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Remove not supported for lazy collections");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("ContainsAll not supported for lazy collections");
    }

    @Override
    public boolean addAll(Collection<? extends LazyItemProxy> c) {
        throw new UnsupportedOperationException("AddAll not supported for lazy collections");
    }

    @Override
    public boolean addAll(int index, Collection<? extends LazyItemProxy> c) {
        throw new UnsupportedOperationException("AddAll not supported for lazy collections");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("RemoveAll not supported for lazy collections");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("RetainAll not supported for lazy collections");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Clear not supported for lazy collections");
    }

    @Override
    public LazyItemProxy set(int index, LazyItemProxy element) {
        throw new UnsupportedOperationException("Set not supported for lazy collections");
    }

    @Override
    public void add(int index, LazyItemProxy element) {
        throw new UnsupportedOperationException("Add not supported for lazy collections");
    }

    @Override
    public LazyItemProxy remove(int index) {
        throw new UnsupportedOperationException("Remove not supported for lazy collections");
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException("IndexOf not supported for lazy collections");
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException("LastIndexOf not supported for lazy collections");
    }

    @Override
    public ListIterator<LazyItemProxy> listIterator() {
        throw new UnsupportedOperationException("ListIterator not supported for lazy collections");
    }

    @Override
    public ListIterator<LazyItemProxy> listIterator(int index) {
        throw new UnsupportedOperationException("ListIterator not supported for lazy collections");
    }

    @Override
    public List<LazyItemProxy> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("SubList not supported for lazy collections");
    }

    @Override
    public String toString() {
        return String.format("CompositeLazyItemCollection{name=%s, collections=%d, totalSize=%d}",
            collectionName, collections.size(), size());
    }
}
