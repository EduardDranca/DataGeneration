package com.github.eddranca.datagenerator.visitor;

import com.github.eddranca.datagenerator.node.CollectionNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;


/**
 * A lazy collection that generates LazyItemProxy objects on-demand.
 *
 * <p>This collection provides two modes of operation:
 * <ul>
 *   <li><strong>Indexed access</strong> - Items are cached for repeated access via {@link #get(int)}</li>
 *   <li><strong>Streaming access</strong> - Items are generated fresh each time via {@link #stream()}</li>
 * </ul>
 *
 * <p>The streaming mode is key for memory efficiency as it doesn't cache items,
 * allowing processing of large datasets without memory accumulation.
 *
 * <p>This class is an internal implementation detail of the memory optimization feature
 * and should not be used directly by client code.
 */
public class LazyItemCollection implements List<LazyItemProxy> {
    private final CollectionNode collectionNode;
    private final DataGenerationVisitor visitor;
    private final String collectionName;
    private final Set<String> referencedPaths;
    private final List<LazyItemProxy> materializedItems;

    public LazyItemCollection(CollectionNode collectionNode, DataGenerationVisitor visitor,
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
    public LazyItemProxy get(int index) {
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
            LazyItemProxy item = generateItem();
            materializedItems.add(item);
        }
    }

    /**
     * Generates a single LazyItemProxy.
     */
    private LazyItemProxy generateItem() {
        return new LazyItemProxy(
            collectionName,
            collectionNode.getItem().getFields(),
            referencedPaths,
            visitor
        );
    }

    /**
     * Returns a stream that uses cached items to ensure consistency with indexed access.
     * This ensures that reference resolution and output use the same data.
     */
    @Override
    public Stream<LazyItemProxy> stream() {
        // Materialize all items first to ensure consistency
        materializeUpTo(size() - 1);
        return materializedItems.stream();
    }

    @Override
    public Iterator<LazyItemProxy> iterator() {
        return new LazyIterator();
    }

    /**
     * Iterator that generates items on-demand and caches them for indexed access.
     */
    private class LazyIterator implements Iterator<LazyItemProxy> {
        private int currentIndex = 0;

        @Override
        public boolean hasNext() {
            return currentIndex < size();
        }

        @Override
        public LazyItemProxy next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return get(currentIndex++);
        }
    }

    /**
     * Forces full materialization of all items.
     * This should only be called when absolutely necessary.
     */
    public void materializeAll() {
        materializeUpTo(size() - 1);
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
    public boolean add(LazyItemProxy lazyItemProxy) {
        throw new UnsupportedOperationException("LazyItemCollection is read-only");
    }

    @Override
    public void add(int index, LazyItemProxy element) {
        throw new UnsupportedOperationException("LazyItemCollection is read-only");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("LazyItemCollection is read-only");
    }

    @Override
    public LazyItemProxy remove(int index) {
        throw new UnsupportedOperationException("LazyItemCollection is read-only");
    }

    @Override
    public boolean addAll(Collection<? extends LazyItemProxy> c) {
        throw new UnsupportedOperationException("LazyItemCollection is read-only");
    }

    @Override
    public boolean addAll(int index, Collection<? extends LazyItemProxy> c) {
        throw new UnsupportedOperationException("LazyItemCollection is read-only");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("LazyItemCollection is read-only");
    }

    @Override
    public LazyItemProxy set(int index, LazyItemProxy element) {
        throw new UnsupportedOperationException("LazyItemCollection is read-only");
    }

    @Override
    public boolean contains(Object o) {
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
        throw new UnsupportedOperationException("LazyItemCollection is read-only");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("LazyItemCollection is read-only");
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
}
