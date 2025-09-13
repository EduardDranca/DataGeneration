package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.node.AbstractReferenceNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.FilterNode;
import com.github.eddranca.datagenerator.node.ReferenceFieldNode;
import com.github.eddranca.datagenerator.node.ReferenceSpreadFieldNode;

import java.util.ArrayList;
import java.util.List;

import static com.github.eddranca.datagenerator.builder.KeyWords.*;

/**
 * Builder for reference field nodes (references, reference spreads).
 * Creates specialized reference nodes based on reference patterns.
 */
public class ReferenceFieldNodeBuilder {
    private final NodeBuilderContext context;
    private final FieldNodeBuilder fieldBuilder;
    private final ReferenceParser referenceParser;

    public ReferenceFieldNodeBuilder(NodeBuilderContext context, FieldNodeBuilder fieldBuilder) {
        this.context = context;
        this.fieldBuilder = fieldBuilder;
        this.referenceParser = new ReferenceParser(context.getValidationContext(), context);
    }

    public DslNode buildReferenceBasedField(String fieldName, JsonNode fieldDef) {
        if (fieldName.startsWith(ELLIPSIS)) {
            return buildReferenceSpreadField(fieldName, fieldDef);
        }
        return buildReferenceField(fieldName, fieldDef);
    }

    private DslNode buildReferenceField(String fieldName, JsonNode fieldDef) {
        String reference = fieldDef.get(REFERENCE).asText();

        // Parse sequential flag
        boolean sequential = fieldDef.path(SEQUENTIAL).asBoolean(false);

        // Build filters if present
        List<FilterNode> filters = buildReferenceFilters(fieldName, fieldDef);
        if (filters == null) {
            return null; // Error occurred during filter building
        }

        // Parse reference and create specialized node
        AbstractReferenceNode referenceNode = referenceParser.parseReference(fieldName, reference, filters, sequential);

        // If parsing failed, fall back to the old ReferenceFieldNode for backward
        // compatibility
        if (referenceNode == null) {
            return new ReferenceFieldNode(reference, sequential);
        }

        return referenceNode;
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
                    } else {
                        return null; // Error occurred during filter building
                    }
                }
            } else {
                context.addError("Reference field '" + fieldName + "' filter must be an array");
                return null;
            }
        }

        return filters;
    }

    private DslNode buildReferenceSpreadField(String fieldName, JsonNode fieldDef) {
        String reference = fieldDef.get(REFERENCE).asText();

        // Parse sequential flag
        boolean sequential = fieldDef.path(SEQUENTIAL).asBoolean(false);

        // Extract fields to spread
        List<String> fields = extractSpreadFields(fieldName, fieldDef);
        if (fields == null) {
            return null;
        }

        // Build filters
        List<FilterNode> filters = buildSpreadFieldFilters(fieldName, fieldDef);
        if (filters == null) {
            return null;
        }

        // Validate reference using parser (but we still use ReferenceSpreadFieldNode
        // for now)
        AbstractReferenceNode parsedRef = referenceParser.parseReference(fieldName, reference, new ArrayList<>(),
                sequential);
        if (parsedRef == null) {
            return null; // Validation failed
        }

        return new ReferenceSpreadFieldNode(reference, fields, filters, sequential);
    }

    private List<String> extractSpreadFields(String fieldName, JsonNode fieldDef) {
        // Fields array is optional - if not provided, all fields from referenced item
        // will be used
        List<String> fields = new ArrayList<>();
        if (fieldDef.has(FIELDS)) {
            JsonNode fieldsNode = fieldDef.get(FIELDS);
            if (!fieldsNode.isArray()) {
                context.addError("Reference spread field '" + fieldName + "' fields must be an array");
                return null;
            }

            for (JsonNode fieldNode : fieldsNode) {
                fields.add(fieldNode.asText());
            }

            if (fields.isEmpty()) {
                context.addError("Reference spread field '" + fieldName
                        + "' must have at least one field when fields array is provided");
                return null;
            }
        }
        // If fields is empty, it means use all available fields from the referenced
        // item
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
                context.addError("Reference spread field '" + fieldName + "' filter must be an array");
            }
        }
        return filters;
    }
}