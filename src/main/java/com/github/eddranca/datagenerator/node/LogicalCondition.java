package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a logical condition that combines multiple conditions.
 * Supports AND and OR operators.
 */
public class LogicalCondition implements Condition {
    
    /**
     * Type of logical operator.
     */
    public enum Type {
        AND("and"),
        OR("or");
        
        private final String operator;
        
        Type(String operator) {
            this.operator = operator;
        }
        
        public String getOperator() {
            return operator;
        }
    }
    
    private final Type type;
    private final List<Condition> conditions;

    public LogicalCondition(Type type, List<Condition> conditions) {
        if (conditions == null || conditions.size() < 2) {
            throw new IllegalArgumentException(type + " condition requires at least 2 conditions");
        }
        this.type = type;
        this.conditions = conditions;
    }

    public Type getType() {
        return type;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    @Override
    public boolean matches(JsonNode item) {
        return switch (type) {
            case AND -> conditions.stream().allMatch(condition -> condition.matches(item));
            case OR -> conditions.stream().anyMatch(condition -> condition.matches(item));
        };
    }

    @Override
    public String toConditionString() {
        return conditions.stream()
                .map(Condition::toConditionString)
                .collect(Collectors.joining(" " + type.getOperator() + " ", "(", ")"));
    }

    @Override
    public Set<String> getReferencedPaths() {
        Set<String> paths = new HashSet<>();
        for (Condition condition : conditions) {
            paths.addAll(condition.getReferencedPaths());
        }
        return paths;
    }
}
