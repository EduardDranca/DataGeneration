package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Set;

/**
 * Represents a condition for filtering items in conditional references.
 * Conditions are internal filtering logic created during reference parsing.
 */
public interface Condition {
    /**
     * Tests whether the given item matches this condition.
     *
     * @param item the item to test
     * @return true if the item matches the condition, false otherwise
     */
    boolean matches(JsonNode item);

    /**
     * Returns a string representation of this condition for debugging and error messages.
     */
    String toConditionString();

    /**
     * Returns the set of field paths that this condition depends on.
     * 
     * @return set of field paths (e.g., "category", "address.country")
     */
    Set<String> getReferencedPaths();

    /**
     * Checks if this condition contains any shadow binding references.
     * 
     * @return true if the condition has shadow binding references
     */
    default boolean hasShadowBindingReferences() {
        return false;
    }

    /**
     * Resolves shadow binding references in this condition using the provided bindings.
     * Returns a new condition with resolved values, or this condition if no resolution needed.
     * 
     * @param shadowBindings map of binding names to their resolved values
     * @return a condition with resolved shadow binding references
     */
    default Condition resolveShadowBindings(Map<String, JsonNode> shadowBindings) {
        return this;
    }
}
