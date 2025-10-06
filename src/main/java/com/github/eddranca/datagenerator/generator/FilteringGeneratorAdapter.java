package com.github.eddranca.datagenerator.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.exception.FilteringException;

import java.util.List;
import java.util.function.Supplier;

/**
 * Adapter that adds filtering support to any Generator using retry logic.
 * This allows generators that don't support native filtering to work with filter values.
 * Always throws FilteringException when filtering fails after max retries.
 */
public class FilteringGeneratorAdapter implements Generator {
    private final Generator delegate;
    private final int maxFilteringRetries;

    public FilteringGeneratorAdapter(Generator delegate, int maxFilteringRetries) {
        this.delegate = delegate;
        this.maxFilteringRetries = maxFilteringRetries;
    }

    @Override
    public JsonNode generate(GeneratorContext context) {
        return delegate.generate(context);
    }

    @Override
    public JsonNode generateWithFilter(GeneratorContext context, List<JsonNode> filterValues) {
        if (delegate.supportsFiltering()) {
            // Delegate supports filtering natively
            return delegate.generateWithFilter(context, filterValues);
        }

        // Apply retry logic for generators that don't support filtering
        return generateWithRetryFiltering(
            () -> delegate.generate(context),
            filterValues,
            "Generator filtering"
        );
    }

    @Override
    public JsonNode generateAtPath(GeneratorContext context, String path) {
        return delegate.generateAtPath(context, path);
    }

    @Override
    public JsonNode generateAtPathWithFilter(GeneratorContext context, String path, List<JsonNode> filterValues) {
        if (delegate.supportsFiltering()) {
            // Delegate supports filtering natively
            return delegate.generateAtPathWithFilter(context, path, filterValues);
        }

        // Apply retry logic for path generation with filtering
        return generateWithRetryFiltering(
            () -> delegate.generateAtPath(context, path),
            filterValues,
            "Generator path filtering"
        );
    }

    @Override
    public boolean supportsFiltering() {
        return true; // Adapter always supports filtering through retry logic
    }

    /**
     * Generates a value using retry logic with filtering.
     * This method encapsulates the common pattern of generating values and retrying
     * when the generated value matches any of the filter values.
     *
     * @param generator    the supplier that generates values
     * @param filterValues values to exclude (null or empty if no filtering)
     * @param errorContext context string for error messages
     * @return generated value that doesn't match any filter values
     */
    private JsonNode generateWithRetryFiltering(
        Supplier<JsonNode> generator,
        List<JsonNode> filterValues,
        String errorContext
    ) {
        if (filterValues == null || filterValues.isEmpty()) {
            return generator.get();
        }

        for (int attempt = 0; attempt < maxFilteringRetries; attempt++) {
            JsonNode generated = generator.get();

            if (!isValueFiltered(generated, filterValues)) {
                return generated;
            }
        }

        throw new FilteringException(errorContext + " failed to generate a valid value after " +
            maxFilteringRetries + " retries");
    }

    /**
     * Checks if a value should be filtered out based on the filter values.
     *
     * @param value        the value to check
     * @param filterValues the list of values to filter out
     * @return true if the value should be filtered out, false otherwise
     */
    private boolean isValueFiltered(JsonNode value, List<JsonNode> filterValues) {
        if (filterValues == null || filterValues.isEmpty()) {
            return false;
        }

        for (JsonNode filterValue : filterValues) {
            if (value != null && value.equals(filterValue)) {
                return true;
            }
        }
        return false;
    }
}
