package com.github.eddranca.datagenerator.validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Context for tracking validation state during DSL tree building.
 * Keeps track of declared collections, tags, and available generators.
 */
public class ValidationContext {
    private final Set<String> declaredCollections;
    private final Set<String> declaredTags;
    private final Set<String> declaredPicks;
    private final Set<String> registeredGenerators;
    private final Map<String, String> tagToCollectionMapping; // tag -> final collection name
    private String currentCollection; // for context in error messages

    public ValidationContext(Set<String> registeredGenerators) {
        this.declaredCollections = new HashSet<>();
        this.declaredTags = new HashSet<>();
        this.declaredPicks = new HashSet<>();
        this.registeredGenerators = new HashSet<>(registeredGenerators);
        this.tagToCollectionMapping = new HashMap<>();
    }

    public void declareCollection(String name) {
        declaredCollections.add(name);
    }

    public void declareTag(String tag) {
        declaredTags.add(tag);
    }

    /**
     * Declares a tag for a specific collection with validation.
     * Tags can only be redeclared by collections with the same final name.
     *
     * @param tag                 the tag name
     * @param finalCollectionName the final collection name (after considering custom names)
     * @return true if the tag was successfully declared, false if there's a conflict
     */
    public boolean declareTagForCollection(String tag, String finalCollectionName) {
        if (tagToCollectionMapping.containsKey(tag)) {
            String existingCollection = tagToCollectionMapping.get(tag);
            if (!existingCollection.equals(finalCollectionName)) {
                return false; // Tag conflict - different collection names
            }
        } else {
            tagToCollectionMapping.put(tag, finalCollectionName);
        }
        declaredTags.add(tag);
        return true;
    }

    /**
     * Gets the collection name that declared a specific tag.
     *
     * @param tag the tag name
     * @return the collection name that declared the tag, or null if not found
     */
    public String getTagCollection(String tag) {
        return tagToCollectionMapping.get(tag);
    }

    public void declarePick(String name) {
        declaredPicks.add(name);
    }

    public String getCurrentCollection() {
        return currentCollection;
    }

    public void setCurrentCollection(String collection) {
        this.currentCollection = collection;
    }

    public boolean isCollectionDeclared(String name) {
        return declaredCollections.contains(name);
    }

    public boolean isTagDeclared(String tag) {
        return declaredTags.contains(tag);
    }

    public boolean isPickDeclared(String name) {
        return declaredPicks.contains(name);
    }

    public boolean isGeneratorRegistered(String name) {
        return registeredGenerators.contains(name);
    }
}
