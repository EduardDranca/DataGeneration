package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.node.AbstractReferenceNode;
import com.github.eddranca.datagenerator.node.ArrayFieldReferenceNode;
import com.github.eddranca.datagenerator.node.Condition;
import com.github.eddranca.datagenerator.node.ConditionalReferenceNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.FilterNode;
import com.github.eddranca.datagenerator.node.IndexedReferenceNode;
import com.github.eddranca.datagenerator.node.PickReferenceNode;
import com.github.eddranca.datagenerator.node.ReferenceSpreadFieldNode;
import com.github.eddranca.datagenerator.node.SelfReferenceNode;
import com.github.eddranca.datagenerator.node.ShadowBindingFieldNode;
import com.github.eddranca.datagenerator.node.SimpleReferenceNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.eddranca.datagenerator.builder.KeyWords.ELLIPSIS;
import static com.github.eddranca.datagenerator.builder.KeyWords.FIELDS;
import static com.github.eddranca.datagenerator.builder.KeyWords.FILTER;
import static com.github.eddranca.datagenerator.builder.KeyWords.REF;
import static com.github.eddranca.datagenerator.builder.KeyWords.SEQUENTIAL;
import static com.github.eddranca.datagenerator.builder.KeyWords.THIS_PREFIX;

/**
 * Specialized builder for reference field nodes.
 * Handles all reference types and patterns.
 */
class ReferenceFieldNodeBuilder {
    private static final String ERROR_UNDECLARED_COLLECTION = "references undeclared collection: ";
    private static final String ERROR_INVALID_RANGE_FORMAT = "has invalid range format '";
    private static final String ERROR_EMPTY_REFERENCE = "has empty reference";
    private static final String ERROR_FILTER_MUST_BE_ARRAY = "filter must be an array";
    private static final String ERROR_EMPTY_FIELD_IN_ARRAY_REF = "has empty field name in array reference: ";
    private static final String ERROR_FIELD_WITHOUT_INDEX = "references field within collection: ";
    private static final String ERROR_UNDECLARED_COLLECTION_OR_PICK = "references undeclared collection or pick: ";
    
    private final NodeBuilderContext context;
    private final FieldBuilder fieldBuilder;

    public ReferenceFieldNodeBuilder(NodeBuilderContext context, FieldBuilder fieldBuilder) {
        this.context = context;
        this.fieldBuilder = fieldBuilder;
    }

    /**
     * Builds a reference node from a reference string.
     * Used by OptionReferenceParser to build runtime option references.
     */
    public AbstractReferenceNode buildReferenceNode(String fieldName, String reference) {
        return parseReference(fieldName, reference, new ArrayList<>(), false).orElse(null);
    }

    public DslNode buildReferenceBasedField(String fieldName, JsonNode fieldDef) {
        if (fieldName.startsWith(ELLIPSIS)) {
            return buildReferenceSpreadField(fieldName, fieldDef);
        }
        return buildReferenceField(fieldName, fieldDef);
    }

    private DslNode buildReferenceField(String fieldName, JsonNode fieldDef) {
        String reference = fieldDef.get(REF).asText();
        boolean sequential = fieldDef.path(SEQUENTIAL).asBoolean(false);

        List<FilterNode> filters = buildReferenceFilters(fieldName, fieldDef);

        return parseReference(fieldName, reference, filters, sequential)
                .orElseGet(() -> new SimpleReferenceNode(reference, null, filters, sequential));
    }

    private DslNode buildReferenceSpreadField(String fieldName, JsonNode fieldDef) {
        String reference = fieldDef.get(REF).asText();
        boolean sequential = fieldDef.path(SEQUENTIAL).asBoolean(false);

        List<String> fields = extractSpreadFields(fieldName, fieldDef);
        List<FilterNode> filters = buildSpreadFieldFilters(fieldName, fieldDef);

        AbstractReferenceNode referenceNode = parseReference(fieldName, reference, filters, sequential)
                .orElseGet(() -> new SimpleReferenceNode(reference, null, filters, sequential));

        return new ReferenceSpreadFieldNode(referenceNode, fields);
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
                addReferenceFieldError(fieldName, ERROR_FILTER_MUST_BE_ARRAY);
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
                addReferenceSpreadFieldError(fieldName, ERROR_FILTER_MUST_BE_ARRAY);
            }
        }
        return filters;
    }

    private Optional<AbstractReferenceNode> parseReference(String fieldName, String reference,
                                                           List<FilterNode> filters, boolean sequential) {
        if (reference == null || reference.trim().isEmpty()) {
            addReferenceFieldError(fieldName, ERROR_EMPTY_REFERENCE);
            return Optional.empty();
        }

        reference = reference.trim();

        if (reference.startsWith("$")) {
            return parseShadowBindingFieldReference(fieldName, reference, filters, sequential);
        } else if (reference.startsWith(THIS_PREFIX)) {
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


    private Optional<AbstractReferenceNode> parseShadowBindingFieldReference(String fieldName, String reference,
                                                                              List<FilterNode> filters, boolean sequential) {
        int dotIndex = reference.indexOf('.');
        if (dotIndex == -1) {
            addReferenceFieldError(fieldName, "shadow binding reference must include field path: " + reference);
            return Optional.empty();
        }

        String bindingName = reference.substring(0, dotIndex);
        String fieldPath = reference.substring(dotIndex + 1);

        if (bindingName.length() <= 1) {
            addReferenceFieldError(fieldName, "shadow binding name cannot be empty: " + reference);
            return Optional.empty();
        }

        if (fieldPath.isEmpty()) {
            addReferenceFieldError(fieldName, "shadow binding field path cannot be empty: " + reference);
            return Optional.empty();
        }

        return Optional.of(new ShadowBindingFieldNode(bindingName, fieldPath, filters, sequential));
    }

    private Optional<AbstractReferenceNode> parseSelfReference(String fieldName, String reference,
                                                           List<FilterNode> filters, boolean sequential) {
        String localField = reference.substring(THIS_PREFIX.length());

        if (localField.isEmpty()) {
            addReferenceFieldError(fieldName, "has invalid self-reference: " + reference);
            return Optional.empty();
        }

        return Optional.of(new SelfReferenceNode(localField, filters, sequential));
    }

    private Optional<AbstractReferenceNode> parseArrayFieldReference(String fieldName, String reference,
                                                                       List<FilterNode> filters, boolean sequential) {
        String collectionName = reference.substring(0, reference.indexOf("[*]."));
        String field = reference.substring(reference.indexOf("[*].") + 4);

        if (!context.isCollectionDeclared(collectionName)) {
            addReferenceFieldError(fieldName, ERROR_UNDECLARED_COLLECTION + collectionName);
            return Optional.empty();
        }

        if (field.isEmpty()) {
            addReferenceFieldError(fieldName, ERROR_EMPTY_FIELD_IN_ARRAY_REF + reference);
            return Optional.empty();
        }

        return Optional.of(new ArrayFieldReferenceNode(collectionName, field, filters, sequential));
    }

    private Optional<AbstractReferenceNode> parseIndexedReference(String fieldName, String reference,
                                                                 List<FilterNode> filters, boolean sequential) {
        String collectionName = reference.substring(0, reference.indexOf("["));
        String indexPart = reference.substring(reference.indexOf("[") + 1, reference.indexOf("]"));
        String fieldPart = "";

        if (reference.contains("].")) {
            fieldPart = reference.substring(reference.indexOf("].") + 2);
        }

        if (!context.isCollectionDeclared(collectionName)) {
            addReferenceFieldError(fieldName, ERROR_UNDECLARED_COLLECTION + collectionName);
            return Optional.empty();
        }

        Optional<String> validationError = validateIndexFormat(indexPart);
        if (validationError.isPresent()) {
            addReferenceFieldError(fieldName, validationError.get());
            return Optional.empty();
        }

        try {
            return Optional.of(new IndexedReferenceNode(collectionName, indexPart, fieldPart, filters, sequential));
        } catch (IllegalArgumentException e) {
            addReferenceFieldError(fieldName, ERROR_INVALID_RANGE_FORMAT + indexPart + "': " + e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<AbstractReferenceNode> parseDotNotationReference(String fieldName, String reference,
                                                                      List<FilterNode> filters, boolean sequential) {
        String baseName = reference.substring(0, reference.indexOf("."));
        String field = reference.substring(reference.indexOf(".") + 1);

        if (context.isPickDeclared(baseName)) {
            return Optional.of(new PickReferenceNode(baseName, field, filters, sequential));
        }

        if (context.isCollectionDeclared(baseName)) {
            return Optional.of(new SimpleReferenceNode(baseName, field, filters, sequential));
        }

        addReferenceFieldError(fieldName, ERROR_FIELD_WITHOUT_INDEX + reference + " without index");
        return Optional.empty();
    }

    private Optional<AbstractReferenceNode> parseSimpleReference(String fieldName, String reference,
                                                                 List<FilterNode> filters, boolean sequential) {
        if (context.isPickDeclared(reference)) {
            return Optional.of(new PickReferenceNode(reference, null, filters, sequential));
        }

        if (context.isCollectionDeclared(reference)) {
            return Optional.of(new SimpleReferenceNode(reference, null, filters, sequential));
        }

        addReferenceFieldError(fieldName, ERROR_UNDECLARED_COLLECTION_OR_PICK + reference);
        return Optional.empty();
    }

    /**
     * Validates index format and returns error message if invalid, empty if valid.
     * Valid formats:
     * - "*" (wildcard)
     * - "10" (positive index)
     * - "-10" (negative index)
     * - "0:99" (range with both bounds)
     * - "10:" (range with open end)
     * - ":99" (range with open start)
     * - ":" (full range)
     * - "-10:-1" (range with negative bounds)
     */
    private Optional<String> validateIndexFormat(String indexPart) {
        if (indexPart == null || indexPart.isEmpty()) {
            return Optional.of("has empty index - use '*' for wildcard, a number for specific index, or 'start:end' for range");
        }

        // Wildcard is always valid
        if (indexPart.equals("*")) {
            return Optional.empty();
        }

        // Check for multiple colons (invalid)
        if (indexPart.chars().filter(ch -> ch == ':').count() > 1) {
            return Optional.of(ERROR_INVALID_RANGE_FORMAT + indexPart + "' - use 'start:end' with single colon (e.g., '0:99', '10:', ':99')");
        }

        // If it contains a colon, it's a range
        if (indexPart.contains(":")) {
            return validateRangeFormat(indexPart);
        }

        // Otherwise, it should be a simple numeric index
        return validateNumericIndex(indexPart);
    }

    private Optional<String> validateRangeFormat(String indexPart) {
        String[] parts = indexPart.split(":", -1);
        if (parts.length != 2) {
            return Optional.of(ERROR_INVALID_RANGE_FORMAT + indexPart + "' - use 'start:end' format (e.g., '0:99', '10:', ':99')");
        }

        String start = parts[0];
        String end = parts[1];

        // Both empty is valid (full range ":")
        if (start.isEmpty() && end.isEmpty()) {
            return Optional.empty();
        }

        // Validate start if present
        if (!start.isEmpty()) {
            try {
                Integer.parseInt(start);
            } catch (NumberFormatException e) {
                return Optional.of("has invalid range start '" + start + "' - must be a number (e.g., '0:99', '-10:-1')");
            }
        }

        // Validate end if present
        if (!end.isEmpty()) {
            try {
                Integer.parseInt(end);
            } catch (NumberFormatException e) {
                return Optional.of("has invalid range end '" + end + "' - must be a number (e.g., '0:99', '10:')");
            }
        }

        return Optional.empty();
    }

    private Optional<String> validateNumericIndex(String indexPart) {
        try {
            Integer.parseInt(indexPart);
            return Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.of("has invalid index format '" + indexPart + "' - use a number (e.g., '0', '-1'), '*' for wildcard, or 'start:end' for range (e.g., '0:99')");
        }
    }

    private Optional<AbstractReferenceNode> parseConditionalReference(String fieldName, String reference,
                                                               List<FilterNode> filters, boolean sequential) {
        int bracketStart = reference.indexOf("[");
        int bracketEnd = reference.indexOf("]");
        
        if (bracketEnd == -1) {
            addReferenceFieldError(fieldName, "has unclosed bracket in conditional reference: " + reference);
            return Optional.empty();
        }
        
        if (bracketEnd < bracketStart) {
            addReferenceFieldError(fieldName, "has invalid bracket order in conditional reference: " + reference);
            return Optional.empty();
        }
        
        String collectionName = reference.substring(0, bracketStart);
        String conditionStr = reference.substring(bracketStart + 1, bracketEnd);
        String fieldPart = "";
        
        if (conditionStr.trim().isEmpty()) {
            addReferenceFieldError(fieldName, "has empty condition in brackets: " + reference);
            return Optional.empty();
        }
        
        if (reference.length() > bracketEnd + 1 && reference.charAt(bracketEnd + 1) == '.') {
            fieldPart = reference.substring(bracketEnd + 2);
        }
        
        if (!context.isCollectionDeclared(collectionName)) {
            addReferenceFieldError(fieldName, ERROR_UNDECLARED_COLLECTION + collectionName);
            return Optional.empty();
        }
        
        ConditionParser parser = new ConditionParser(msg -> addReferenceFieldError(fieldName, msg));
        Condition condition = parser.parse(conditionStr);
        if (condition == null) {
            return Optional.empty();
        }
        
        return Optional.of(new ConditionalReferenceNode(collectionName, fieldPart, condition, filters, sequential));
    }

    private void addReferenceFieldError(String fieldName, String message) {
        context.addError("Reference field '" + fieldName + "' " + message);
    }

    private void addReferenceSpreadFieldError(String fieldName, String message) {
        context.addError("Reference spread field '" + fieldName + "' " + message);
    }
}
