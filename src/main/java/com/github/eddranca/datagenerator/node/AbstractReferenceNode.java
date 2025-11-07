package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.util.JsonNodeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Abstract base class for all reference nodes.
 * Provides common functionality for filters and sequential behavior.
 */
public abstract class AbstractReferenceNode implements DslNode, Sequential, ReferenceResolver {
    protected final List<FilterNode> filters;
    protected final boolean sequential;

    protected AbstractReferenceNode(boolean sequential) {
        this(new ArrayList<>(), sequential);
    }

    protected AbstractReferenceNode(List<FilterNode> filters, boolean sequential) {
        this.filters = new ArrayList<>(filters);
        this.sequential = sequential;
    }

    public List<FilterNode> getFilters() {
        return filters;
    }

    public boolean isSequential() {
        return sequential;
    }

    /**
     * Returns a string representation of this reference for debugging and error messages.
     */
    public abstract String getReferenceString();

    /**
     * Returns the name of the collection being referenced.
     * Returns empty Optional for references that don't reference a collection (e.g., PickReferenceNode, SelfReferenceNode).
     */
    public abstract Optional<String> getCollectionName();

    protected JsonNode extractNestedField(JsonNode node, String fieldPath) {
        return JsonNodeUtils.extractNestedField(node, fieldPath);
    }
}
