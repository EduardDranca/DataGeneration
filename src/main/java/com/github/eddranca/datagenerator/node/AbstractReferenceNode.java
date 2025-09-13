package com.github.eddranca.datagenerator.node;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all reference nodes.
 * Provides common functionality for filters and sequential behavior.
 */
public abstract class AbstractReferenceNode implements DslNode, SequentialTrackable, ReferenceResolver {
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

    public void addFilter(FilterNode filter) {
        filters.add(filter);
    }

    /**
     * Returns a string representation of this reference for debugging and error messages.
     */
    public abstract String getReferenceString();
}