package com.github.eddranca.datagenerator.validation;

import com.github.eddranca.datagenerator.generator.GeneratorOptionSpec;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;

import java.util.HashSet;
import java.util.Set;

/**
 * Context for tracking validation state during DSL tree building.
 * Keeps track of declared collections, and available generators.
 */
public class ValidationContext {
    private final Set<String> declaredCollections;
    private final Set<String> declaredPicks;
    private final Set<String> registeredGenerators;
    private final GeneratorRegistry generatorRegistry;
    private String currentCollection; // for context in error messages

    public ValidationContext(GeneratorRegistry generatorRegistry) {
        this.declaredCollections = new HashSet<>();
        this.declaredPicks = new HashSet<>();
        this.registeredGenerators = new HashSet<>(generatorRegistry.getRegisteredGeneratorNames());
        this.generatorRegistry = generatorRegistry;
    }

    public void declareCollection(String name) {
        declaredCollections.add(name);
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

    public boolean hasCollection(String name) {
        return declaredCollections.contains(name);
    }

    public boolean isCollectionDeclared(String name) {
        return declaredCollections.contains(name);
    }

    public boolean isPickDeclared(String name) {
        return declaredPicks.contains(name);
    }

    public boolean isGeneratorRegistered(String name) {
        return registeredGenerators.contains(name);
    }

    public GeneratorOptionSpec getGeneratorOptionSpec(String name) {
        return generatorRegistry.getOptionSpec(name);
    }
}
