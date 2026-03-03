package com.github.eddranca.datagenerator.expression;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registry for expression functions used in {@code expr} fields.
 * Provides built-in functions and supports registration of custom functions.
 */
public class ExpressionFunctionRegistry {
    private final Map<String, ExpressionFunction> functions = new HashMap<>();

    public ExpressionFunctionRegistry() {
        registerDefaults();
    }

    private void registerDefaults() {
        functions.put("lowercase", (value, args) -> value.toLowerCase(Locale.ROOT));
        functions.put("uppercase", (value, args) -> value.toUpperCase(Locale.ROOT));
        functions.put("trim", (value, args) -> value.trim());
        functions.put("substring", (value, args) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("substring requires at least a start index");
            }
            int start = Integer.parseInt(args.get(0).trim());
            int end = args.size() > 1
                ? Integer.parseInt(args.get(1).trim())
                : value.length();
            start = Math.max(0, Math.min(start, value.length()));
            end = Math.max(start, Math.min(end, value.length()));
            return value.substring(start, end);
        });
    }

    /**
     * Registers a custom expression function.
     *
     * @param name     the function name as used in expr strings
     * @param function the function implementation
     */
    public void register(String name, ExpressionFunction function) {
        functions.put(name, function);
    }

    /**
     * Gets a function by name.
     *
     * @param name the function name
     * @return the function, or null if not registered
     */
    public ExpressionFunction get(String name) {
        return functions.get(name);
    }

    /**
     * Checks if a function is registered.
     */
    public boolean has(String name) {
        return functions.containsKey(name);
    }
}
