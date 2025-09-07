package com.github.eddranca.datagenerator.node;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root node representing the entire DSL document.
 * Contains all collection definitions in order.
 */
public class RootNode implements DslNode {
    private final Map<String, CollectionNode> collections;
    private final Long seed;

    public RootNode(Long seed) {
        this.collections = new LinkedHashMap<>();
        this.seed = seed;
    }

    public void addCollection(String name, CollectionNode collection) {
        collections.put(name, collection);
    }

    public Map<String, CollectionNode> getCollections() {
        return collections;
    }

    public Long getSeed() {
        return seed;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitRoot(this);
    }
}
