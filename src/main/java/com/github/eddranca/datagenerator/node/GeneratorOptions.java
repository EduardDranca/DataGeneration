package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents generator options that may contain both static values and runtime-computed references.
 * This allows generator options to depend on other field values in the same item.
 */
public class GeneratorOptions {
    private final JsonNode staticOptions;
    private final Map<String, OptionReferenceNode> runtimeOptions;

    public GeneratorOptions(JsonNode staticOptions) {
        this(staticOptions, new HashMap<>());
    }

    public GeneratorOptions(JsonNode staticOptions, Map<String, OptionReferenceNode> runtimeOptions) {
        this.staticOptions = staticOptions;
        this.runtimeOptions = runtimeOptions;
    }

    public JsonNode getStaticOptions() {
        return staticOptions;
    }

    public Map<String, OptionReferenceNode> getRuntimeOptions() {
        return runtimeOptions;
    }

    public boolean hasRuntimeOptions() {
        return !runtimeOptions.isEmpty();
    }

    /**
     * Checks if a specific option key has a runtime reference.
     */
    public boolean hasRuntimeOption(String key) {
        return runtimeOptions.containsKey(key);
    }

    /**
     * Gets the runtime option reference for a specific key.
     */
    public OptionReferenceNode getRuntimeOption(String key) {
        return runtimeOptions.get(key);
    }
}
