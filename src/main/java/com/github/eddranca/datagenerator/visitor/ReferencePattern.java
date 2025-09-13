package com.github.eddranca.datagenerator.visitor;

/**
 * Functional interface for determining if a reference matches a specific pattern.
 */
@FunctionalInterface
public interface ReferencePattern {
    /**
     * Checks if the reference matches this pattern.
     *
     * @param reference the reference string to check
     * @return true if the reference matches this pattern
     */
    boolean matches(String reference);
}
