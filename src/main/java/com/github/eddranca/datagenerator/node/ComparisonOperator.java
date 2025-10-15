package com.github.eddranca.datagenerator.node;

/**
 * Comparison operators for conditional references.
 */
public enum ComparisonOperator {
    EQUALS("="),
    NOT_EQUALS("!=");

    private final String symbol;

    ComparisonOperator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
