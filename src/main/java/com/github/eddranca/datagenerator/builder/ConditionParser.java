package com.github.eddranca.datagenerator.builder;

import com.github.eddranca.datagenerator.node.ComparisonCondition;
import com.github.eddranca.datagenerator.node.ComparisonOperator;
import com.github.eddranca.datagenerator.node.Condition;
import com.github.eddranca.datagenerator.node.LogicalCondition;
import com.github.eddranca.datagenerator.node.ShadowBindingReference;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Parser for conditional reference conditions.
 * Handles parsing of comparison and logical conditions.
 */
class ConditionParser {
    private final Consumer<String> errorHandler;

    public ConditionParser(Consumer<String> errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Parses a condition string into a Condition object.
     *
     * @param conditionStr the condition string to parse
     * @return the parsed Condition, or null if parsing failed
     */
    public Condition parse(String conditionStr) {
        // Check for uppercase logical operators (common mistake)
        if (containsLogicalOperatorOutsideQuotes(conditionStr, " AND ") ||
            containsLogicalOperatorOutsideQuotes(conditionStr, " OR ")) {
            errorHandler.accept("has invalid condition format: " + conditionStr +
                " (logical operators must be lowercase: 'and', 'or')");
            return null;
        }

        // Check for logical operators (and/or) outside of quoted strings
        if (containsLogicalOperatorOutsideQuotes(conditionStr, " and ")) {
            return parseLogicalCondition(conditionStr, " and ", LogicalCondition.Type.AND);
        } else if (containsLogicalOperatorOutsideQuotes(conditionStr, " or ")) {
            return parseLogicalCondition(conditionStr, " or ", LogicalCondition.Type.OR);
        }

        // Single comparison condition
        return parseComparisonCondition(conditionStr);
    }

    /**
     * Checks if a logical operator exists outside of quoted strings.
     * This prevents treating "value='something and something'" as a logical AND.
     */
    private boolean containsLogicalOperatorOutsideQuotes(String str, String operator) {
        boolean inQuotes = false;
        int operatorLength = operator.length();

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (c == '\'') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && i + operatorLength <= str.length() && str.substring(i, i + operatorLength).equals(operator)) {
                return true;
            }
        }

        return false;
    }

    private Condition parseLogicalCondition(String conditionStr, String operator, LogicalCondition.Type type) {
        List<String> parts = splitOutsideQuotes(conditionStr, operator);
        List<Condition> subConditions = new ArrayList<>();

        for (String part : parts) {
            Condition condition = parseComparisonCondition(part.trim());
            if (condition == null) {
                return null;
            }
            subConditions.add(condition);
        }

        if (subConditions.size() < 2) {
            errorHandler.accept("logical operator requires at least 2 conditions");
            return null;
        }

        return new LogicalCondition(type, subConditions);
    }

    /**
     * Splits a string by a delimiter, but only when the delimiter is outside of quoted strings.
     */
    private List<String> splitOutsideQuotes(String str, String delimiter) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int delimiterLength = delimiter.length();
        int length = str.length();

        int i = 0;
        while (i < length) {
            char c = str.charAt(i);

            if (c == '\'') {
                inQuotes = !inQuotes;
                current.append(c);
                i++;
            } else if (!inQuotes && i + delimiterLength <= length &&
                str.substring(i, i + delimiterLength).equals(delimiter)) {
                parts.add(current.toString());
                current = new StringBuilder();
                i += delimiterLength; // Skip the delimiter
            } else {
                current.append(c);
                i++;
            }
        }

        parts.add(current.toString());
        return parts;
    }

    private Condition parseComparisonCondition(String conditionStr) {
        // Try operators in order of length (longest first to avoid partial matches)
        String[] operatorStrings = {"<=", ">=", "!=", "<", ">", "="};
        ComparisonOperator[] operators = {
            ComparisonOperator.LESS_THAN_OR_EQUAL,
            ComparisonOperator.GREATER_THAN_OR_EQUAL,
            ComparisonOperator.NOT_EQUALS,
            ComparisonOperator.LESS_THAN,
            ComparisonOperator.GREATER_THAN,
            ComparisonOperator.EQUALS
        };

        for (int i = 0; i < operatorStrings.length; i++) {
            String opStr = operatorStrings[i];
            int opIndex = findOperatorOutsideQuotes(conditionStr, opStr);

            if (opIndex != -1) {
                String field = conditionStr.substring(0, opIndex).trim();
                String valueStr = conditionStr.substring(opIndex + opStr.length()).trim();

                if (field.isEmpty()) {
                    errorHandler.accept("has empty field name in condition");
                    return null;
                }

                Object value = parseValue(valueStr);
                return new ComparisonCondition(field, operators[i], value);
            }
        }

        errorHandler.accept("has invalid condition format: " + conditionStr);
        return null;
    }

    /**
     * Finds the index of an operator outside of quoted strings.
     * Returns -1 if not found.
     */
    private int findOperatorOutsideQuotes(String str, String operator) {
        boolean inQuotes = false;
        int operatorLength = operator.length();

        for (int i = 0; i <= str.length() - operatorLength; i++) {
            char c = str.charAt(i);

            if (c == '\'') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && str.substring(i, i + operatorLength).equals(operator)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Parses a condition value from string to appropriate type.
     * Supports: 'string', true, false, null, numbers, $binding.field references
     */
    private Object parseValue(String valueStr) {
        if (valueStr.isEmpty()) {
            return "";
        }

        // Check for shadow binding reference ($name.field)
        if (valueStr.startsWith("$") && valueStr.contains(".")) {
            return ShadowBindingReference.parse(valueStr);
        }

        // Check for quoted string (single quotes)
        if (valueStr.startsWith("'") && valueStr.endsWith("'") && valueStr.length() >= 2) {
            return valueStr.substring(1, valueStr.length() - 1);
        }

        // Check for null
        if ("null".equalsIgnoreCase(valueStr)) {
            return null;
        }

        // Check for boolean
        if ("true".equalsIgnoreCase(valueStr)) {
            return true;
        }
        if ("false".equalsIgnoreCase(valueStr)) {
            return false;
        }

        // Check for number
        try {
            if (valueStr.contains(".")) {
                return Double.parseDouble(valueStr);
            } else {
                return Integer.parseInt(valueStr);
            }
        } catch (NumberFormatException e) {
            // ignore exception
        }

        return valueStr;
    }
}
