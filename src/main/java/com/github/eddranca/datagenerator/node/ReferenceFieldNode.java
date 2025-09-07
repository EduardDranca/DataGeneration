package com.github.eddranca.datagenerator.node;

import java.util.ArrayList;
import java.util.List;

/**
 * Field node that references values from other collections.
 * Supports various reference patterns like "collection[*].field", "byTag[tag]", etc.
 */
public class ReferenceFieldNode implements DslNode {
    private final String reference;
    private final List<FilterNode> filters;
    private final boolean sequential;

    public ReferenceFieldNode(String reference) {
        this(reference, new ArrayList<>(), false);
    }

    public ReferenceFieldNode(String reference, List<FilterNode> filters) {
        this(reference, filters, false);
    }

    public ReferenceFieldNode(String reference, boolean sequential) {
        this(reference, new ArrayList<>(), sequential);
    }

    public ReferenceFieldNode(String reference, List<FilterNode> filters, boolean sequential) {
        this.reference = reference;
        this.filters = new ArrayList<>(filters);
        this.sequential = sequential;
    }

    public String getReference() {
        return reference;
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

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitReferenceField(this);
    }
}
