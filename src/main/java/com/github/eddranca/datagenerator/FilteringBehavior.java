package com.github.eddranca.datagenerator;

/**
 * Defines how the generator should behave when all possible values are filtered out
 * and no valid value can be generated.
 */
public enum FilteringBehavior {
    /**
     * Return null when no valid value can be generated after filtering.
     */
    RETURN_NULL,

    /**
     * Throw an exception when no valid value can be generated after filtering.
     * Useful for strict validation scenarios where filtered-out values indicate
     * a configuration or data problem that should be caught early.
     */
    THROW_EXCEPTION
}
