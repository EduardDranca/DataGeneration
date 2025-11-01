package com.github.eddranca.datagenerator.node;

/**
 * Comparison operators for conditional references.
 */
public enum ComparisonOperator {
    EQUALS("="),
    NOT_EQUALS("!="),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL(">=");

    private final String symbol;

    ComparisonOperator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public static ComparisonOperator fromSymbol(String symbol) {
        for (ComparisonOperator op : values()) {
            if (op.symbol.equals(symbol)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown comparison operator: " + symbol);
    }

    @Override
    public String toString() {
        return symbol;
    }
}
