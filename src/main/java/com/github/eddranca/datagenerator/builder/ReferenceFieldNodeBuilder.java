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
import java.util.Optional;

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
        return parseReference(fieldName, reference, filters, sequential)
            .orElseGet(() -> new SimpleReferenceNode(reference, null, filters, sequential));
    }

    private DslNode buildReferenceSpreadField(String fieldName, JsonNode fieldDef) {
        String reference = fieldDef.get(REFERENCE).asText();
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

    private Optional<AbstractReferenceNode> parseReference(String fieldName, String reference,
                                                           List<FilterNode> filters, boolean sequential) {
        if (reference == null || reference.trim().isEmpty()) {
            addReferenceFieldError(fieldName, "has empty reference");
            return Optional.empty();
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
            addReferenceFieldError(fieldName, "references undeclared collection: " + collectionName);
            return Optional.empty();
        }

        if (field.isEmpty()) {
            addReferenceFieldError(fieldName, "has empty field name in array reference: " + reference);
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
            addReferenceFieldError(fieldName, "references undeclared collection: " + collectionName);
            return Optional.empty();
        }

        if (!indexPart.equals("*") && !indexPart.matches("\\d+")) {
            addReferenceFieldError(fieldName, "has invalid index format: " + indexPart);
            return Optional.empty();
        }

        try {
            return Optional.of(new IndexedReferenceNode(collectionName, indexPart, fieldPart, filters, sequential));
        } catch (IllegalArgumentException e) {
            addReferenceFieldError(fieldName, "has invalid numeric index: " + indexPart);
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

        addReferenceFieldError(fieldName, "references field within collection: " + reference + " without index");
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

        addReferenceFieldError(fieldName, "references undeclared collection or pick: " + reference);
        return Optional.empty();
    }

    private void addReferenceFieldError(String fieldName, String message) {
        context.addError("Reference field '" + fieldName + "' " + message);
    }

    private void addReferenceSpreadFieldError(String fieldName, String message) {
        context.addError("Reference spread field '" + fieldName + "' " + message);
    }
}
