package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.util.JsonNodeUtils;

import java.util.Set;

/**
 * Represents a comparison condition for conditional references.
 * Supports = and != operators.
 */
public class ComparisonCondition implements Condition {
    private final String fieldPath;
    private final ComparisonOperator operator;
    private final Object expectedValue;

    public ComparisonCondition(String fieldPath, ComparisonOperator operator, Object expectedValue) {
        this.fieldPath = fieldPath;
        this.operator = operator;
        this.expectedValue = expectedValue;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public ComparisonOperator getOperator() {
        return operator;
    }

    public Object getExpectedValue() {
        return expectedValue;
    }

    @Override
    public boolean matches(JsonNode item) {
        JsonNode actualNode = JsonNodeUtils.extractNestedField(item, fieldPath);
        
        return switch (operator) {
            case EQUALS -> matchesValue(actualNode, expectedValue);
            case NOT_EQUALS -> !matchesValue(actualNode, expectedValue);
            case LESS_THAN -> compareNumeric(actualNode, expectedValue) < 0;
            case LESS_THAN_OR_EQUAL -> compareNumeric(actualNode, expectedValue) <= 0;
            case GREATER_THAN -> compareNumeric(actualNode, expectedValue) > 0;
            case GREATER_THAN_OR_EQUAL -> compareNumeric(actualNode, expectedValue) >= 0;
        };
    }

    @Override
    public String toConditionString() {
        String valueStr;
        if (expectedValue == null) {
            valueStr = "null";
        } else if (expectedValue instanceof String str) {
            valueStr = "'" + str + "'";
        } else {
            valueStr = expectedValue.toString();
        }
        return fieldPath + operator.getSymbol() + valueStr;
    }

    @Override
    public Set<String> getReferencedPaths() {
        return Set.of(fieldPath);
    }

    private boolean matchesValue(JsonNode actualNode, Object expectedValue) {
        if (actualNode.isMissingNode() || actualNode.isNull()) {
            return expectedValue == null;
        }

        if (expectedValue == null) {
            return actualNode.isNull();
        }

        if (expectedValue instanceof String str) {
            return actualNode.isTextual() && actualNode.asText().equals(str);
        }

        if (expectedValue instanceof Boolean bool) {
            return actualNode.isBoolean() && actualNode.asBoolean() == bool;
        }

        if (expectedValue instanceof Integer intVal) {
            return actualNode.isIntegralNumber() && actualNode.asInt() == intVal;
        }

        if (expectedValue instanceof Long longVal) {
            return actualNode.isIntegralNumber() && actualNode.asLong() == longVal;
        }

        if (expectedValue instanceof Double doubleVal) {
            return actualNode.isNumber() && Math.abs(actualNode.asDouble() - doubleVal) < 0.0001;
        }

        if (expectedValue instanceof Float floatVal) {
            return actualNode.isNumber() && Math.abs(actualNode.asDouble() - floatVal) < 0.0001;
        }

        return actualNode.asText().equals(expectedValue.toString());
    }

    private int compareNumeric(JsonNode actualNode, Object expectedValue) {
        if (actualNode.isMissingNode() || actualNode.isNull()) {
            throw new IllegalArgumentException("Cannot perform numeric comparison on null or missing value");
        }

        if (!actualNode.isNumber()) {
            throw new IllegalArgumentException("Cannot perform numeric comparison on non-numeric value: " + actualNode);
        }

        double actualDouble = actualNode.asDouble();
        double expectedDouble;

        if (expectedValue instanceof Number num) {
            expectedDouble = num.doubleValue();
        } else {
            throw new IllegalArgumentException("Expected value must be numeric for comparison operators: " + expectedValue);
        }

        return Double.compare(actualDouble, expectedDouble);
    }
}
