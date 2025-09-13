package com.github.eddranca.datagenerator.builder;

import com.github.eddranca.datagenerator.node.AbstractReferenceNode;
import com.github.eddranca.datagenerator.node.ArrayFieldReferenceNode;
import com.github.eddranca.datagenerator.node.FilterNode;
import com.github.eddranca.datagenerator.node.IndexedReferenceNode;
import com.github.eddranca.datagenerator.node.PickReferenceNode;
import com.github.eddranca.datagenerator.node.SelfReferenceNode;
import com.github.eddranca.datagenerator.node.SimpleReferenceNode;
import com.github.eddranca.datagenerator.node.TagReferenceNode;
import com.github.eddranca.datagenerator.validation.ValidationContext;

import java.util.List;

/**
 * Parser that analyzes reference strings and creates appropriate specialized reference nodes.
 * Handles validation of reference patterns and creates type-safe node representations.
 */
public class ReferenceParser {
    private final ValidationContext context;
    private final NodeBuilderContext builderContext;

    public ReferenceParser(ValidationContext context, NodeBuilderContext builderContext) {
        this.context = context;
        this.builderContext = builderContext;
    }

    /**
     * Parses a reference string and creates the appropriate specialized reference node.
     */
    public AbstractReferenceNode parseReference(String fieldName, String reference, 
                                               List<FilterNode> filters, boolean sequential) {
        // Validate reference is not empty
        if (reference == null || reference.trim().isEmpty()) {
            builderContext.addError("Reference field '" + fieldName + "' has empty reference");
            return null;
        }

        reference = reference.trim();

        // Parse based on reference pattern
        if (reference.startsWith("byTag[")) {
            return parseTagReference(fieldName, reference, filters, sequential);
        } else if (reference.startsWith("this.")) {
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

    private TagReferenceNode parseTagReference(String fieldName, String reference, 
                                              List<FilterNode> filters, boolean sequential) {
        int start = reference.indexOf('[') + 1;
        int end = reference.indexOf(']');
        
        if (start >= end) {
            builderContext.addError("Reference field '" + fieldName + "' has malformed byTag reference: " + reference);
            return null;
        }

        String tagExpr = reference.substring(start, end);
        String fieldNamePart = "";
        
        if (reference.length() > end + 1 && reference.charAt(end + 1) == '.') {
            fieldNamePart = reference.substring(end + 2);
        }

        // Validate tag expression
        if (!tagExpr.startsWith("this.") && !context.isTagDeclared(tagExpr)) {
            builderContext.addError("Reference field '" + fieldName + "' references undeclared tag: " + tagExpr);
            return null;
        }

        return new TagReferenceNode(tagExpr, fieldNamePart, filters, sequential);
    }

    private SelfReferenceNode parseSelfReference(String fieldName, String reference, 
                                                List<FilterNode> filters, boolean sequential) {
        String localField = reference.substring(5); // Remove "this."
        
        if (localField.isEmpty()) {
            builderContext.addError("Reference field '" + fieldName + "' has invalid self-reference: " + reference);
            return null;
        }

        return new SelfReferenceNode(localField, filters, sequential);
    }

    private ArrayFieldReferenceNode parseArrayFieldReference(String fieldName, String reference, 
                                                            List<FilterNode> filters, boolean sequential) {
        String collectionName = reference.substring(0, reference.indexOf("[*]."));
        String field = reference.substring(reference.indexOf("[*].") + 4);

        // Validate collection exists
        if (!context.isCollectionDeclared(collectionName)) {
            builderContext.addError("Reference field '" + fieldName + "' references undeclared collection: " + collectionName);
            return null;
        }

        if (field.isEmpty()) {
            builderContext.addError("Reference field '" + fieldName + "' has empty field name in array reference: " + reference);
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

        // Validate collection exists
        if (!context.isCollectionDeclared(collectionName)) {
            builderContext.addError("Reference field '" + fieldName + "' references undeclared collection: " + collectionName);
            return null;
        }

        // Validate index format
        if (!indexPart.equals("*") && !indexPart.matches("\\d+")) {
            builderContext.addError("Reference field '" + fieldName + "' has invalid index format: " + indexPart);
            return null;
        }

        try {
            return new IndexedReferenceNode(collectionName, indexPart, fieldPart, filters, sequential);
        } catch (IllegalArgumentException e) {
            builderContext.addError("Reference field '" + fieldName + "' has invalid numeric index: " + indexPart);
            return null;
        }
    }

    private AbstractReferenceNode parseDotNotationReference(String fieldName, String reference, 
                                                           List<FilterNode> filters, boolean sequential) {
        String baseName = reference.substring(0, reference.indexOf("."));
        String field = reference.substring(reference.indexOf(".") + 1);

        // Check if it's a pick reference
        if (context.isPickDeclared(baseName)) {
            return new PickReferenceNode(baseName, field, filters, sequential);
        }

        // Check if it's a simple collection reference
        if (context.isCollectionDeclared(baseName)) {
            return new SimpleReferenceNode(baseName, field, filters, sequential);
        }

        builderContext.addError("Reference field '" + fieldName + "' references field within collection: " + 
                               reference + " without index");
        return null;
    }

    private AbstractReferenceNode parseSimpleReference(String fieldName, String reference, 
                                                      List<FilterNode> filters, boolean sequential) {
        // Check if it's a pick reference
        if (context.isPickDeclared(reference)) {
            return new PickReferenceNode(reference, null, filters, sequential);
        }

        // Check if it's a collection reference
        if (context.isCollectionDeclared(reference)) {
            return new SimpleReferenceNode(reference, null, filters, sequential);
        }

        builderContext.addError("Reference field '" + fieldName + "' references undeclared collection or pick: " + reference);
        return null;
    }
}