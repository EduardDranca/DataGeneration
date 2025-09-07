package com.github.eddranca.datagenerator.node;

/**
 * Node representing a filter condition for reference fields.
 * Contains the expression that determines what values to filter out.
 */
public class FilterNode implements DslNode {
    private final DslNode filterExpression;

    public FilterNode(DslNode filterExpression) {
        this.filterExpression = filterExpression;
    }

    public DslNode getFilterExpression() {
        return filterExpression;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitFilter(this);
    }
}
