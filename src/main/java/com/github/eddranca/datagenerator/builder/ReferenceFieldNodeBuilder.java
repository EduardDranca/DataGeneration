package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.node.AbstractReferenceNode;
import com.github.eddranca.datagenerator.node.ArrayFieldReferenceNode;
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
        // filters is never null now, always returns empty list on error

        AbstractReferenceNode referenceNode = parseReference(fieldName, reference, filters, sequential);
        if (referenceNode != null) {
            // Return the typed reference node directly
            return referenceNode;
        }

        // If parsing fails, create a SimpleReferenceNode as fallback
        // This handles cases where validation fails but the reference might still work at runtime
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

        // Fallback to SimpleReferenceNode for invalid cases
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
                    // Continue processing other filters even if one fails
                }
            } else {
                addReferenceFieldError(fieldName, "filter must be an array");
                // Return empty list instead of null
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
                return fields; // Return empty list instead of null
            }

            for (JsonNode fieldNode : fieldsNode) {
                String fieldText = fieldNode.asText();
                if (fieldText != null && !fieldText.trim().isEmpty()) {
                    fields.add(fieldText);
                }
            }

            if (fields.isEmpty()) {
                addReferenceSpreadFieldError(fieldName, "must have at least one valid field when fields array is provided");
                // Return empty list - this will spread all fields from the reference
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
                    // Continue processing other filters even if one fails
                }
            } else {
                addReferenceSpreadFieldError(fieldName, "filter must be an array");
                // Return empty list instead of null
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
        } else if (reference.contains("[")) {
            return parseIndexedReference(fieldName, reference, filters, sequential);
        } else if (reference.contains(".")) {
            return parseDotNotationReference(fieldName, reference, filters, sequential);
        } else {
            return parseSimpleReference(fieldName, reference, filters, sequential);
        }
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

    private void addReferenceFieldError(String fieldName, String message) {
        context.addError("Reference field '" + fieldName + "' " + message);
    }

    private void addReferenceSpreadFieldError(String fieldName, String message) {
        context.addError("Reference spread field '" + fieldName + "' " + message);
    }
}
