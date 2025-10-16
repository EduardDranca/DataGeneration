package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a logical AND condition that combines multiple conditions.
 * All conditions must be true for the AND condition to be true.
 */
public class AndCondition implements Condition {
    private final List<Condition> conditions;

    public AndCondition(List<Condition> conditions) {
        if (conditions == null || conditions.size() < 2) {
            throw new IllegalArgumentException("AND condition requires at least 2 conditions");
        }
        this.conditions = conditions;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    @Override
    public boolean matches(JsonNode item) {
        return conditions.stream().allMatch(condition -> condition.matches(item));
    }

    @Override
    public String toConditionString() {
        return conditions.stream()
                .map(Condition::toConditionString)
                .collect(Collectors.joining(" and ", "(", ")"));
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
