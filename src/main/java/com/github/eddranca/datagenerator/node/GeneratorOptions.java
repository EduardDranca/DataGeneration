package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents generator options that may contain both static values and runtime-computed references.
 * <p>
 * This class enables dynamic, context-dependent data generation by allowing generator options
 * to reference other field values in the same item. For example:
 * <pre>
 * {
 *   "startAge": {"gen": "number", "min": 22, "max": 35},
 *   "retirementAge": {
 *     "gen": "number",
 *     "min": {"ref": "this.startAge"},  // Runtime reference
 *     "max": 65                          // Static value
 *   }
 * }
 * </pre>
 * <p>
 * The class separates static options (known at parse time) from runtime options
 * (resolved during generation), allowing efficient handling of both types.
 *
 * @see OptionReferenceNode
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
