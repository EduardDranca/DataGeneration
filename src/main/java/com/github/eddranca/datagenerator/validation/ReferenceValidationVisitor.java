package com.github.eddranca.datagenerator.validation;

import com.github.eddranca.datagenerator.ValidationError;
import com.github.eddranca.datagenerator.node.ArrayFieldNode;
import com.github.eddranca.datagenerator.node.ArrayFieldReferenceNode;
import com.github.eddranca.datagenerator.node.ChoiceFieldNode;
import com.github.eddranca.datagenerator.node.CollectionNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.DslNodeVisitor;
import com.github.eddranca.datagenerator.node.FilterNode;
import com.github.eddranca.datagenerator.node.GeneratedFieldNode;
import com.github.eddranca.datagenerator.node.IndexedReferenceNode;
import com.github.eddranca.datagenerator.node.ItemNode;
import com.github.eddranca.datagenerator.node.LiteralFieldNode;
import com.github.eddranca.datagenerator.node.ObjectFieldNode;
import com.github.eddranca.datagenerator.node.PickReferenceNode;
import com.github.eddranca.datagenerator.node.ReferenceSpreadFieldNode;
import com.github.eddranca.datagenerator.node.RootNode;
import com.github.eddranca.datagenerator.node.SelfReferenceNode;
import com.github.eddranca.datagenerator.node.SimpleReferenceNode;
import com.github.eddranca.datagenerator.node.SpreadFieldNode;
import com.github.eddranca.datagenerator.node.TagReferenceNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.eddranca.datagenerator.builder.KeyWords.THIS_PREFIX;

/**
 * Validation visitor that traverses the DSL node tree and validates all references,
 * particularly self-references (this.*) in their proper context.
 */
public class ReferenceValidationVisitor implements DslNodeVisitor<Void> {

    private final List<ValidationError> errors;
    private String currentCollection;
    private Map<String, DslNode> currentItemFields;

    public ReferenceValidationVisitor() {
        this.errors = new ArrayList<>();
    }

    public List<ValidationError> getErrors() {
        return new ArrayList<>(errors);
    }

    @Override
    public Void visitRoot(RootNode node) {
        for (Map.Entry<String, CollectionNode> entry : node.getCollections().entrySet()) {
            currentCollection = entry.getKey();
            entry.getValue().accept(this);
        }
        return null;
    }

    @Override
    public Void visitCollection(CollectionNode node) {
        if (node.getItem() != null) {
            node.getItem().accept(this);
        }
        return null;
    }

    @Override
    public Void visitItem(ItemNode node) {
        // Store current item fields for self-reference validation
        Map<String, DslNode> previousItemFields = currentItemFields;
        currentItemFields = new HashMap<>(node.getFields());

        // Visit all fields
        for (DslNode field : node.getFields().values()) {
            field.accept(this);
        }

        // Restore previous context
        currentItemFields = previousItemFields;
        return null;
    }

    @Override
    public Void visitGeneratedField(GeneratedFieldNode node) {
        // Generated fields don't have references to validate
        return null;
    }


    @Override
    public Void visitTagReference(TagReferenceNode node) {
        // Tag references don't need additional validation beyond what was done during parsing
        // Visit filters if present
        for (FilterNode filter : node.getFilters()) {
            filter.accept(this);
        }
        return null;
    }

    @Override
    public Void visitIndexedReference(IndexedReferenceNode node) {
        // Indexed references don't need additional validation beyond what was done during parsing
        // Visit filters if present
        for (FilterNode filter : node.getFilters()) {
            filter.accept(this);
        }
        return null;
    }

    @Override
    public Void visitArrayFieldReference(ArrayFieldReferenceNode node) {
        // Array field references don't need additional validation beyond what was done during parsing
        // Visit filters if present
        for (FilterNode filter : node.getFilters()) {
            filter.accept(this);
        }
        return null;
    }

    @Override
    public Void visitSelfReference(SelfReferenceNode node) {
        // Validate self-reference
        validateSelfReference(THIS_PREFIX + node.getFieldName());

        // Visit filters if present
        for (FilterNode filter : node.getFilters()) {
            filter.accept(this);
        }
        return null;
    }

    @Override
    public Void visitSimpleReference(SimpleReferenceNode node) {
        // Simple references don't need additional validation beyond what was done during parsing
        // Visit filters if present
        for (FilterNode filter : node.getFilters()) {
            filter.accept(this);
        }
        return null;
    }

    @Override
    public Void visitPickReference(PickReferenceNode node) {
        // Pick references don't need additional validation beyond what was done during parsing
        // Visit filters if present
        for (FilterNode filter : node.getFilters()) {
            filter.accept(this);
        }
        return null;
    }

    @Override
    public Void visitChoiceField(ChoiceFieldNode node) {
        // Choice fields don't have references to validate
        return null;
    }

    @Override
    public Void visitObjectField(ObjectFieldNode node) {
        // Visit all fields in the object
        for (DslNode field : node.getFields().values()) {
            field.accept(this);
        }
        return null;
    }

    @Override
    public Void visitSpreadField(SpreadFieldNode node) {
        // Spread fields don't have references to validate
        return null;
    }

    @Override
    public Void visitReferenceSpreadField(ReferenceSpreadFieldNode node) {
        // Validate the reference in the spread field
        String reference = node.getReferenceNode().getReferenceString();

        // Validate self-references
        if (reference.startsWith(THIS_PREFIX)) {
            String fieldName = reference.substring(THIS_PREFIX.length());
            if (currentItemFields == null || !currentItemFields.containsKey(fieldName)) {
                addSelfReferenceError(THIS_PREFIX + fieldName, "refers to non-existent field in current item");
            }
        }

        // Validate filters if present
        for (FilterNode filter : node.getFilters()) {
            filter.accept(this);
        }

        return null;
    }

    @Override
    public Void visitLiteralField(LiteralFieldNode node) {
        // Literal fields don't have references to validate
        return null;
    }

    @Override
    public Void visitArrayField(ArrayFieldNode node) {
        // Visit the item node to validate any references within array items
        if (node.getItemNode() != null) {
            node.getItemNode().accept(this);
        }
        return null;
    }

    @Override
    public Void visitFilter(FilterNode node) {
        // Filter expressions are now handled by their specific typed reference nodes
        node.getFilterExpression().accept(this);
        return null;
    }

    private void validateSelfReference(String reference) {
        if (currentItemFields == null) {
            addSelfReferenceError(reference, "found outside of item context");
            return;
        }

        // Extract the field path from "this.fieldName" or "this.field.subfield"
        String fieldPath = reference.substring(THIS_PREFIX.length()); // Remove THIS_PREFIX

        if (fieldPath.isEmpty()) {
            addSelfReferenceError(reference, "is invalid - missing field name after '" + THIS_PREFIX + "'");
            return;
        }

        if (!validateFieldPath(fieldPath, currentItemFields)) {
            addSelfReferenceError(reference, "references non-existent field: " + fieldPath);
        }
    }

    private boolean validateFieldPath(String fieldPath, Map<String, DslNode> itemFields) {
        String[] pathParts = fieldPath.split("\\.", 2);
        String currentFieldName = pathParts[0];

        // Check if the field exists in the current item
        DslNode field = itemFields.get(currentFieldName);
        if (field == null) {
            return false;
        }

        // If there are more path parts, validate recursively for object fields
        if (pathParts.length > 1) {
            String remainingPath = pathParts[1];

            // Only object fields can have sub-fields
            if (field instanceof ObjectFieldNode objectField) {
                return validateFieldPath(remainingPath, objectField.getFields());
            }
            // Non-object fields cannot have sub-fields
            return false;
        }

        return true;
    }

    private void addError(String message) {
        String path = currentCollection != null ? currentCollection : "root";
        errors.add(new ValidationError(path, message));
    }

    private void addSelfReferenceError(String reference, String message) {
        addError("Self reference '" + reference + "' " + message);
    }
}
