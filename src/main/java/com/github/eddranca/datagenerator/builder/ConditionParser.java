package com.github.eddranca.datagenerator.builder;

import com.github.eddranca.datagenerator.node.ComparisonCondition;
import com.github.eddranca.datagenerator.node.ComparisonOperator;
import com.github.eddranca.datagenerator.node.Condition;
import com.github.eddranca.datagenerator.node.LogicalCondition;

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
        if (conditionStr.contains(" AND ") || conditionStr.contains(" OR ")) {
            errorHandler.accept("has invalid condition format: " + conditionStr +
                    " (logical operators must be lowercase: 'and', 'or')");
            return null;
        }

        // Check for logical operators (and/or)
        if (conditionStr.contains(" and ")) {
            return parseLogicalCondition(conditionStr, " and ", LogicalCondition.Type.AND);
        } else if (conditionStr.contains(" or ")) {
            return parseLogicalCondition(conditionStr, " or ", LogicalCondition.Type.OR);
        }

        // Single comparison condition
        return parseComparisonCondition(conditionStr);
    }

    private Condition parseLogicalCondition(String conditionStr, String operator, LogicalCondition.Type type) {
        String[] parts = conditionStr.split(operator);
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

    private Condition parseComparisonCondition(String conditionStr) {
        ComparisonOperator operator;
        String[] parts;

        // Try operators in order of length (longest first to avoid partial matches)
        if (conditionStr.contains("<=")) {
            operator = ComparisonOperator.LESS_THAN_OR_EQUAL;
            parts = conditionStr.split("<=", 2);
        } else if (conditionStr.contains(">=")) {
            operator = ComparisonOperator.GREATER_THAN_OR_EQUAL;
            parts = conditionStr.split(">=", 2);
        } else if (conditionStr.contains("!=")) {
            operator = ComparisonOperator.NOT_EQUALS;
            parts = conditionStr.split("!=", 2);
        } else if (conditionStr.contains("<")) {
            operator = ComparisonOperator.LESS_THAN;
            parts = conditionStr.split("<", 2);
        } else if (conditionStr.contains(">")) {
            operator = ComparisonOperator.GREATER_THAN;
            parts = conditionStr.split(">", 2);
        } else if (conditionStr.contains("=")) {
            operator = ComparisonOperator.EQUALS;
            parts = conditionStr.split("=", 2);
        } else {
            errorHandler.accept("has invalid condition format: " + conditionStr);
            return null;
        }

        if (parts.length != 2) {
            errorHandler.accept("has invalid condition format: " + conditionStr);
            return null;
        }

        String field = parts[0].trim();
        String valueStr = parts[1].trim();

        if (field.isEmpty()) {
            errorHandler.accept("has empty field name in condition");
            return null;
        }

        Object value = parseValue(valueStr);
        return new ComparisonCondition(field, operator, value);
    }

    /**
     * Parses a condition value from string to appropriate type.
     * Supports: 'string', true, false, null, numbers
     */
    private Object parseValue(String valueStr) {
        if (valueStr.isEmpty()) {
            return "";
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
