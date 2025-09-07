package com.github.eddranca.datagenerator.node;

/**
 * Field node that generates arrays of values.
 * Supports both fixed size and variable size arrays.
 */
public class ArrayFieldNode implements DslNode {
    private final Integer size;
    private final Integer minSize;
    private final Integer maxSize;
    private final DslNode itemNode;

    /**
     * Creates an array field with fixed size.
     */
    public ArrayFieldNode(int size, DslNode itemNode) {
        this.size = size;
        this.minSize = null;
        this.maxSize = null;
        this.itemNode = itemNode;
    }

    /**
     * Creates an array field with variable size.
     */
    public ArrayFieldNode(int minSize, int maxSize, DslNode itemNode) {
        this.size = null;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.itemNode = itemNode;
    }

    public boolean hasFixedSize() {
        return size != null;
    }

    public int getSize() {
        return size != null ? size : 0;
    }

    public int getMinSize() {
        return minSize != null ? minSize : 0;
    }

    public int getMaxSize() {
        return maxSize != null ? maxSize : 0;
    }

    public DslNode getItemNode() {
        return itemNode;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitArrayField(this);
    }
}
