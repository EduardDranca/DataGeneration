package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Represents a runtime-computed option value that references another field.
 * <p>
 * Supports two types of references:
 * <ul>
 *   <li><b>Simple references</b>: Directly use the referenced value as the option value</li>
 *   <li><b>Mapped references</b>: Map the referenced value to a different value using a lookup table</li>
 * </ul>
 * <p>
 * Examples:
 * <pre>
 * // Simple reference - use startAge directly as min value
 * {"ref": "this.startAge"}
 *
 * // Mapped reference - map category to different price ranges
 * {"ref": "this.category", "map": {"budget": 10, "premium": 100, "luxury": 1000}}
 * </pre>
 * <p>
 * This enables dynamic option values that adapt based on other field values,
 * allowing for more realistic and contextually appropriate data generation.
 *
 * @see GeneratorOptions
 */
public class OptionReferenceNode {
    private final AbstractReferenceNode reference;
    private final Map<String, JsonNode> valueMap;

    /**
     * Creates a simple option reference without mapping.
     */
    public OptionReferenceNode(AbstractReferenceNode reference) {
        this(reference, null);
    }

    /**
     * Creates an option reference with conditional value mapping.
     */
    public OptionReferenceNode(AbstractReferenceNode reference, Map<String, JsonNode> valueMap) {
        this.reference = reference;
        this.valueMap = valueMap;
    }

    public AbstractReferenceNode getReference() {
        return reference;
    }

    public Map<String, JsonNode> getValueMap() {
        return valueMap;
    }

    public boolean hasMapping() {
        return valueMap != null && !valueMap.isEmpty();
    }
}
