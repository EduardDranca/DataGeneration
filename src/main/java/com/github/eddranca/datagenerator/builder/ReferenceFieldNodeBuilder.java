package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.node.AbstractReferenceNode;
import com.github.eddranca.datagenerator.node.ArrayFieldReferenceNode;
import com.github.eddranca.datagenerator.node.ComparisonCondition;
import com.github.eddranca.datagenerator.node.ComparisonOperator;
import com.github.eddranca.datagenerator.node.Condition;
import com.github.eddranca.datagenerator.node.ConditionalReferenceNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.FilterNode;
import com.github.eddranca.datagenerator.node.IndexedReferenceNode;
import com.github.eddranca.datagenerator.node.PickReferenceNode;
import com.github.eddranca.datagenerator.node.ReferenceSpreadFieldNode;
import com.github.eddranca.datagenerator.node.SelfReferenceNode;
import com.github.eddranca.datagenerator.node.SimpleReferenceNode;

import java.util.ArrayList;
import java.util.List;

import static com.github.eddranca.datagenerator.builder.KeyWords.ELLIPSIS;
import static com.github.eddranca.datagenerator.builder.KeyWords.FIELDS;
import static com.github.eddranca.datagenerator.builder.KeyWords.FILTER;
import static com.github.eddranca.datagenerator.builder.KeyWords.REFERENCE;
import static com.github.eddranca.datagenerator.builder.KeyWords.SEQUENTIAL;
import static com.github.eddranca.datagenerator.builder.KeyWords.THIS_PREFIX;

/**
 * Specialized builder for reference field nodes.
 * Handles all reference types and patterns.
 */
class ReferenceFieldNodeBuilder {
    private final NodeBuilderContext context;
    private final FieldBuilder fieldBuilder;

    public ReferenceFieldNodeBuilder(NodeBuilderContext context, FieldBuilder fieldBuilder) {
        this.context = context;
        this.fieldBuilder = fieldBuilder;
    }

    public DslNode buildReferenceBasedField(String fieldName, JsonNode fieldDef) {
        if (fieldName.startsWith(ELLIPSIS)) {
            return buildReferenceSpreadField(fieldName, fieldDef);
        }
        return buildReferenceField(fieldName, fieldDef);
    }

    private DslNode buildReferenceField(String fieldName, JsonNode fieldDef) {
        String reference = fieldDef.get(REFERENCE).asText();
        boolean sequential = fieldDef.path(SEQUENTIAL).asBoolean(false);

        List<FilterNode> filters = buildReferenceFilters(fieldName, fieldDef);

        AbstractReferenceNode referenceNode = parseReference(fieldName, reference, filters, sequential);
        if (referenceNode != null) {
            return referenceNode;
        }

        return new SimpleReferenceNode(reference, null, filters, sequential);
    }

    private DslNode buildReferenceSpreadField(String fieldName, JsonNode fieldDef) {
        String reference = fieldDef.get(REFERENCE).asText();
        boolean sequential = fieldDef.path(SEQUENTIAL).asBoolean(false);

        List<String> fields = extractSpreadFields(fieldName, fieldDef);
        List<FilterNode> filters = buildSpreadFieldFilters(fieldName, fieldDef);

        AbstractReferenceNode referenceNode = parseReference(fieldName, reference, filters, sequential);
        if (referenceNode != null) {
            return new ReferenceSpreadFieldNode(referenceNode, fields);
        }

        SimpleReferenceNode fallbackNode = new SimpleReferenceNode(reference, null, filters, sequential);
        return new ReferenceSpreadFieldNode(fallbackNode, fields);
    }

    private List<FilterNode> buildReferenceFilters(String fieldName, JsonNode fieldDef) {
        List<FilterNode> filters = new ArrayList<>();

        if (fieldDef.has(FILTER)) {
            JsonNode filtersNode = fieldDef.get(FILTER);
            if (filtersNode.isArray()) {
                for (JsonNode filterNode : filtersNode) {
                    DslNode filterExpression = fieldBuilder.buildField(fieldName + "[filter]", filterNode);
                    if (filterExpression != null) {
                        filters.add(new FilterNode(filterExpression));
                    }
                }
            } else {
                addReferenceFieldError(fieldName, "filter must be an array");
            }
        }

        return filters;
    }

    private List<String> extractSpreadFields(String fieldName, JsonNode fieldDef) {
        List<String> fields = new ArrayList<>();
        if (fieldDef.has(FIELDS)) {
            JsonNode fieldsNode = fieldDef.get(FIELDS);
            if (!fieldsNode.isArray()) {
                addReferenceSpreadFieldError(fieldName, "fields must be an array");
                return fields;
            }

            for (JsonNode fieldNode : fieldsNode) {
                String fieldText = fieldNode.asText();
                if (fieldText != null && !fieldText.trim().isEmpty()) {
                    fields.add(fieldText);
                }
            }

            if (fields.isEmpty()) {
                addReferenceSpreadFieldError(fieldName, "must have at least one valid field when fields array is provided");
            }
        }
        return fields;
    }

    private List<FilterNode> buildSpreadFieldFilters(String fieldName, JsonNode fieldDef) {
        List<FilterNode> filters = new ArrayList<>();
        if (fieldDef.has(FILTER)) {
            JsonNode filtersNode = fieldDef.get(FILTER);
            if (filtersNode.isArray()) {
                for (JsonNode filterNode : filtersNode) {
                    DslNode filterExpression = fieldBuilder.buildField(FILTER, filterNode);
                    if (filterExpression != null) {
                        filters.add(new FilterNode(filterExpression));
                    }
                }
            } else {
                addReferenceSpreadFieldError(fieldName, "filter must be an array");
            }
        }
        return filters;
    }

    private AbstractReferenceNode parseReference(String fieldName, String reference,
                                                 List<FilterNode> filters, boolean sequential) {
        if (reference == null || reference.trim().isEmpty()) {
            addReferenceFieldError(fieldName, "has empty reference");
            return null;
        }

        reference = reference.trim();

        if (reference.startsWith(THIS_PREFIX)) {
            return parseSelfReference(fieldName, reference, filters, sequential);
        } else if (reference.contains("[*].")) {
            return parseArrayFieldReference(fieldName, reference, filters, sequential);
        } else if (reference.contains("[") && containsConditionalOperator(reference)) {
            return parseConditionalReference(fieldName, reference, filters, sequential);
        } else if (reference.contains("[")) {
            return parseIndexedReference(fieldName, reference, filters, sequential);
        } else if (reference.contains(".")) {
            return parseDotNotationReference(fieldName, reference, filters, sequential);
        } else {
            return parseSimpleReference(fieldName, reference, filters, sequential);
        }
    }
    
    private boolean containsConditionalOperator(String reference) {
        int bracketStart = reference.indexOf("[");
        int bracketEnd = reference.indexOf("]");
        if (bracketStart == -1) {
            return false;
        }
        // If there's a bracket but no closing bracket, treat as conditional to get better error message
        if (bracketEnd == -1) {
            return true;
        }
        if (bracketEnd < bracketStart) {
            return false;
        }
        String bracketContent = reference.substring(bracketStart + 1, bracketEnd);
        // Check for comparison operators (not just numeric index or *)
        return bracketContent.contains("=") || bracketContent.contains("<") || 
               bracketContent.contains(">") || bracketContent.contains(" and ") || 
               bracketContent.contains(" or ");
    }


    private SelfReferenceNode parseSelfReference(String fieldName, String reference,
                                                 List<FilterNode> filters, boolean sequential) {
        String localField = reference.substring(THIS_PREFIX.length());

        if (localField.isEmpty()) {
            addReferenceFieldError(fieldName, "has invalid self-reference: " + reference);
            return null;
        }

        return new SelfReferenceNode(localField, filters, sequential);
    }

    private ArrayFieldReferenceNode parseArrayFieldReference(String fieldName, String reference,
                                                             List<FilterNode> filters, boolean sequential) {
        String collectionName = reference.substring(0, reference.indexOf("[*]."));
        String field = reference.substring(reference.indexOf("[*].") + 4);

        if (!context.isCollectionDeclared(collectionName)) {
            addReferenceFieldError(fieldName, "references undeclared collection: " + collectionName);
            return null;
        }

        if (field.isEmpty()) {
            addReferenceFieldError(fieldName, "has empty field name in array reference: " + reference);
            return null;
        }

        return new ArrayFieldReferenceNode(collectionName, field, filters, sequential);
    }

    private IndexedReferenceNode parseIndexedReference(String fieldName, String reference,
                                                       List<FilterNode> filters, boolean sequential) {
        String collectionName = reference.substring(0, reference.indexOf("["));
        String indexPart = reference.substring(reference.indexOf("[") + 1, reference.indexOf("]"));
        String fieldPart = "";

        if (reference.contains("].")) {
            fieldPart = reference.substring(reference.indexOf("].") + 2);
        }

        if (!context.isCollectionDeclared(collectionName)) {
            addReferenceFieldError(fieldName, "references undeclared collection: " + collectionName);
            return null;
        }

        if (!indexPart.equals("*") && !indexPart.matches("\\d+")) {
            addReferenceFieldError(fieldName, "has invalid index format: " + indexPart);
            return null;
        }

        try {
            return new IndexedReferenceNode(collectionName, indexPart, fieldPart, filters, sequential);
        } catch (IllegalArgumentException e) {
            addReferenceFieldError(fieldName, "has invalid numeric index: " + indexPart);
            return null;
        }
    }

    private AbstractReferenceNode parseDotNotationReference(String fieldName, String reference,
                                                            List<FilterNode> filters, boolean sequential) {
        String baseName = reference.substring(0, reference.indexOf("."));
        String field = reference.substring(reference.indexOf(".") + 1);

        if (context.isPickDeclared(baseName)) {
            return new PickReferenceNode(baseName, field, filters, sequential);
        }

        if (context.isCollectionDeclared(baseName)) {
            return new SimpleReferenceNode(baseName, field, filters, sequential);
        }

        addReferenceFieldError(fieldName, "references field within collection: " + reference + " without index");
        return null;
    }

    private AbstractReferenceNode parseSimpleReference(String fieldName, String reference,
                                                       List<FilterNode> filters, boolean sequential) {
        if (context.isPickDeclared(reference)) {
            return new PickReferenceNode(reference, null, filters, sequential);
        }

        if (context.isCollectionDeclared(reference)) {
            return new SimpleReferenceNode(reference, null, filters, sequential);
        }

        addReferenceFieldError(fieldName, "references undeclared collection or pick: " + reference);
        return null;
    }

    private ConditionalReferenceNode parseConditionalReference(String fieldName, String reference,
                                                               List<FilterNode> filters, boolean sequential) {
        int bracketStart = reference.indexOf("[");
        int bracketEnd = reference.indexOf("]");
        
        if (bracketEnd == -1) {
            addReferenceFieldError(fieldName, "has unclosed bracket in conditional reference: " + reference);
            return null;
        }
        
        if (bracketEnd < bracketStart) {
            addReferenceFieldError(fieldName, "has invalid bracket order in conditional reference: " + reference);
            return null;
        }
        
        String collectionName = reference.substring(0, bracketStart);
        String conditionStr = reference.substring(bracketStart + 1, bracketEnd);
        String fieldPart = "";
        
        if (conditionStr.trim().isEmpty()) {
            addReferenceFieldError(fieldName, "has empty condition in brackets: " + reference);
            return null;
        }
        
        if (reference.length() > bracketEnd + 1 && reference.charAt(bracketEnd + 1) == '.') {
            fieldPart = reference.substring(bracketEnd + 2);
        }
        
        if (!context.isCollectionDeclared(collectionName)) {
            addReferenceFieldError(fieldName, "references undeclared collection: " + collectionName);
            return null;
        }
        
        List<Condition> conditions = parseConditions(fieldName, conditionStr);
        if (conditions == null || conditions.isEmpty()) {
            return null;
        }
        
        return new ConditionalReferenceNode(collectionName, fieldPart, conditions, filters, sequential);
    }

    private List<Condition> parseConditions(String fieldName, String conditionStr) {
        // Check for uppercase logical operators (common mistake)
        if (conditionStr.contains(" AND ") || conditionStr.contains(" OR ")) {
            addReferenceFieldError(fieldName, "has invalid condition format: " + conditionStr + 
                    " (logical operators must be lowercase: 'and', 'or')");
            return null;
        }
        
        // Check for logical operators (and/or)
        if (conditionStr.contains(" and ")) {
            return parseLogicalCondition(fieldName, conditionStr, " and ", true);
        } else if (conditionStr.contains(" or ")) {
            return parseLogicalCondition(fieldName, conditionStr, " or ", false);
        }
        
        // Single comparison condition
        Condition condition = parseComparisonCondition(fieldName, conditionStr);
        if (condition == null) {
            return null;
        }
        
        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition);
        return conditions;
    }
    
    private List<Condition> parseLogicalCondition(String fieldName, String conditionStr, String operator, boolean isAnd) {
        String[] parts = conditionStr.split(operator);
        List<Condition> subConditions = new ArrayList<>();
        
        for (String part : parts) {
            Condition condition = parseComparisonCondition(fieldName, part.trim());
            if (condition == null) {
                return null;
            }
            subConditions.add(condition);
        }
        
        if (subConditions.size() < 2) {
            addReferenceFieldError(fieldName, "logical operator requires at least 2 conditions");
            return null;
        }
        
        List<Condition> result = new ArrayList<>();
        if (isAnd) {
            result.add(new com.github.eddranca.datagenerator.node.AndCondition(subConditions));
        } else {
            result.add(new com.github.eddranca.datagenerator.node.OrCondition(subConditions));
        }
        return result;
    }
    
    private Condition parseComparisonCondition(String fieldName, String conditionStr) {
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
            addReferenceFieldError(fieldName, "has invalid condition format: " + conditionStr);
            return null;
        }
        
        if (parts.length != 2) {
            addReferenceFieldError(fieldName, "has invalid condition format: " + conditionStr);
            return null;
        }
        
        String field = parts[0].trim();
        String valueStr = parts[1].trim();
        
        if (field.isEmpty()) {
            addReferenceFieldError(fieldName, "has empty field name in condition");
            return null;
        }
        
        Object value = parseConditionValue(valueStr);
        return new ComparisonCondition(field, operator, value);
    }

    /**
     * Parses a condition value from string to appropriate type.
     * Supports: 'string', true, false, null, numbers
     */
    private Object parseConditionValue(String valueStr) {
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
            // Not a number, treat as unquoted string (for backward compatibility)
        }
        
        // Default: unquoted string value (for backward compatibility)
        return valueStr;
    }

    private void addReferenceFieldError(String fieldName, String message) {
        context.addError("Reference field '" + fieldName + "' " + message);
    }

    private void addReferenceSpreadFieldError(String fieldName, String message) {
        context.addError("Reference spread field '" + fieldName + "' " + message);
    }
}
