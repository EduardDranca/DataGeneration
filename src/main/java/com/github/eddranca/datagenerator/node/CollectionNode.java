package com.github.eddranca.datagenerator.node;

import java.util.HashMap;
import java.util.Map;

/**
 * Node representing a collection definition in the DSL.
 * Contains count, item definition, and pick aliases.
 */
public class CollectionNode implements DslNode {
    private final String name;
    private final int count;
    private final ItemNode item;
    private final Map<String, Integer> picks; // alias -> index
    private final String collectionName; // custom name if different from key

    public CollectionNode(String name, int count, ItemNode item,
                          Map<String, Integer> picks, String collectionName) {
        this.name = name;
        this.count = count;
        this.item = item;
        this.picks = new HashMap<>(picks);
        this.collectionName = collectionName;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }


    public ItemNode getItem() {
        return item;
    }

    public Map<String, Integer> getPicks() {
        return picks;
    }

    public String getCollectionName() {
        return collectionName != null ? collectionName : name;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitCollection(this);
    }
}
