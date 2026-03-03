package com.github.eddranca.datagenerator.expression;

import java.util.List;

/**
 * Interface for expression functions that transform string values.
 * Functions are invoked within {@code expr} fields in the DSL.
 * <p>
 * Built-in functions: {@code lowercase}, {@code uppercase}, {@code trim}, {@code substring}.
 * Custom functions can be registered via the builder API.
 * <p>
 * Example DSL usage:
 * <pre>
 * {"expr": "lowercase(${this.firstName}.${this.lastName}@example.com)"}
 * {"expr": "substring(${this.id}, 0, 8)"}
 * </pre>
 */
@FunctionalInterface
public interface ExpressionFunction {

    /**
     * Applies this function to the given string value with optional arguments.
     *
     * @param value the string value to transform (first argument in the function call)
     * @param args  additional arguments (e.g., start/end indices for substring)
     * @return the transformed string
     */
    String apply(String value, List<String> args);
}
