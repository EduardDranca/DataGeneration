package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Field node that generates values using a registered generator.
 * Supports dot notation for accessing specific fields (e.g., "name.firstName").
 * Supports filtering to exclude specific values from generation.
 */
public class GeneratedFieldNode implements DslNode {
    private final String generatorName;
    private final GeneratorOptions options;
    private final String path; // for dot notation like "name.firstName"
    private final List<FilterNode> filters;

    public GeneratedFieldNode(String generatorName, GeneratorOptions options, String path, List<FilterNode> filters) {
        this.generatorName = generatorName;
        this.options = options;
        this.path = path;
        this.filters = new ArrayList<>(filters);
    }

    public String getGeneratorName() {
        return generatorName;
    }

    public GeneratorOptions getOptions() {
        return options;
    }

    public String getPath() {
        return path;
    }

    public boolean hasPath() {
        return path != null && !path.isEmpty();
    }

    public List<FilterNode> getFilters() {
        return filters;
    }

    public boolean hasFilters() {
        return !filters.isEmpty();
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitGeneratedField(this);
    }
}
