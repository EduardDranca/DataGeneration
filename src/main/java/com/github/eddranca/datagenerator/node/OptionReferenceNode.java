package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Represents a runtime-computed option value that references another field.
 * Supports simple references and conditional mapping based on referenced values.
 * <p>
 * Examples:
 * <pre>
 * {"ref": "this.startAge"}                                    // Simple reference
 * {"ref": "this.category", "map": {"budget": 10, "premium": 100}}  // Mapped reference
 * </pre>
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
