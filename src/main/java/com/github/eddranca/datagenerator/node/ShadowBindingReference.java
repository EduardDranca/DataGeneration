package com.github.eddranca.datagenerator.node;

/**
 * Represents a reference to a shadow binding value in a condition.
 * Used in conditional references like: products[regionId=$user.regionId].id
 * 
 * The binding name is the variable (e.g., "$user") and the field path
 * is the field to access from that binding (e.g., "regionId").
 */
public class ShadowBindingReference {
    private final String bindingName;
    private final String fieldPath;

    public ShadowBindingReference(String bindingName, String fieldPath) {
        this.bindingName = bindingName;
        this.fieldPath = fieldPath;
    }

    /**
     * Parses a shadow binding reference string like "$user.regionId".
     * 
     * @param reference the reference string starting with $
     * @return the parsed ShadowBindingReference
     * @throws IllegalArgumentException if the format is invalid
     */
    public static ShadowBindingReference parse(String reference) {
        if (!reference.startsWith("$")) {
            throw new IllegalArgumentException("Shadow binding reference must start with $: " + reference);
        }

        int dotIndex = reference.indexOf('.');
        if (dotIndex == -1) {
            throw new IllegalArgumentException("Shadow binding reference must include field path: " + reference);
        }

        String bindingName = reference.substring(0, dotIndex);
        String fieldPath = reference.substring(dotIndex + 1);

        if (bindingName.length() <= 1) {
            throw new IllegalArgumentException("Shadow binding name cannot be empty: " + reference);
        }

        if (fieldPath.isEmpty()) {
            throw new IllegalArgumentException("Shadow binding field path cannot be empty: " + reference);
        }

        return new ShadowBindingReference(bindingName, fieldPath);
    }

    public String getBindingName() {
        return bindingName;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    @Override
    public String toString() {
        return bindingName + "." + fieldPath;
    }
}
