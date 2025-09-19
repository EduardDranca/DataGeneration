package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.node.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A visitor that performs dry-run validation of filtering constraints without
 * actually generating data. This is used in lazy mode to ensure filtering
 * exceptions are thrown early, maintaining API compatibility.
 */
public class FilteringValidationVisitor implements DslNodeVisitor<Void> {
    private final GenerationContext context;

    public FilteringValidationVisitor(GenerationContext context) {
        this.context = context;
    }

    @Override
    public Void visitCollection(CollectionNode node) {
        // Validate the item template
        if (node.getItem() != null) {
            node.getItem().accept(this);
        }
        return null;
    }

    @Override
    public Void visitItem(ItemNode node) {
        // Validate all fields in the item
        for (DslNode fieldNode : node.getFields().values()) {
            fieldNode.accept(this);
        }
        return null;
    }

    @Override
    public Void visitObjectField(ObjectFieldNode node) {
        // Validate all fields in the object
        for (DslNode fieldNode : node.getFields().values()) {
            fieldNode.accept(this);
        }
        return null;
    }

    @Override
    public Void visitGeneratedField(GeneratedFieldNode node) {
        // This is where we need to validate filtering constraints
        if (node.hasFilters()) {
            validateGeneratedFieldFiltering(node);
        }
        return null;
    }

    @Override
    public Void visitChoiceField(ChoiceFieldNode node) {
        // Validate choice field filtering
        if (node.hasFilters()) {
            validateChoiceFieldFiltering(node);
        }
        return null;
    }

    @Override
    public Void visitLiteralField(LiteralFieldNode node) {
        // Literal fields don't need validation
        return null;
    }

    @Override
    public Void visitSpreadField(SpreadFieldNode node) {
        // Spread fields don't need validation at this stage
        return null;
    }

    @Override
    public Void visitReferenceSpreadField(ReferenceSpreadFieldNode node) {
        // Reference spread fields don't need validation at this stage
        return null;
    }

    @Override
    public Void visitTagReference(TagReferenceNode node) {
        // References don't need validation at this stage
        return null;
    }

    @Override
    public Void visitIndexedReference(IndexedReferenceNode node) {
        // References don't need validation at this stage
        return null;
    }

    @Override
    public Void visitArrayFieldReference(ArrayFieldReferenceNode node) {
        // References don't need validation at this stage
        return null;
    }

    @Override
    public Void visitSelfReference(SelfReferenceNode node) {
        // References don't need validation at this stage
        return null;
    }

    @Override
    public Void visitSimpleReference(SimpleReferenceNode node) {
        // References don't need validation at this stage
        return null;
    }

    @Override
    public Void visitPickReference(PickReferenceNode node) {
        // References don't need validation at this stage
        return null;
    }

    @Override
    public Void visitFilter(FilterNode node) {
        // Filters are handled by their parent nodes
        return null;
    }

    @Override
    public Void visitArrayField(ArrayFieldNode node) {
        // Validate the array element template
        if (node.getElement() != null) {
            node.getElement().accept(this);
        }
        return null;
    }

    @Override
    public Void visitRoot(RootNode node) {
        // Validate all collections
        for (CollectionNode collection : node.getCollections().values()) {
            collection.accept(this);
        }
        return null;
    }

    /**
     * Validates that a generated field with filters won't fail.
     * This performs a dry run to check if the generator can handle the filtering.
     */
    private void validateGeneratedFieldFiltering(GeneratedFieldNode node) {
        // For now, we'll skip validation of generated fields since it's complex
        // The main issue we're trying to solve is choice fields with all options filtered
        return;
    }

    /**
     * Validates that a choice field with filters won't fail.
     * This is the main case we need to handle - when all choice options are filtered out.
     */
    private void validateChoiceFieldFiltering(ChoiceFieldNode node) {
        List<JsonNode> filterValues = computeFilteredValues(node.getFilters());
        
        if (filterValues.isEmpty()) {
            return; // No filtering, so no issue
        }

        List<DslNode> optionsList = node.getOptions();
        if (optionsList == null || optionsList.isEmpty()) {
            return; // Invalid choice field, will fail elsewhere
        }

        // Check if all options would be filtered out
        boolean hasValidOption = false;
        for (DslNode optionNode : optionsList) {
            // For validation, we need to evaluate the option to see what value it would produce
            JsonNode optionValue = evaluateOptionForValidation(optionNode);
            if (optionValue != null && !filterValues.contains(optionValue)) {
                hasValidOption = true;
                break;
            }
        }

        if (!hasValidOption) {
            // Use the context's filtering failure handler to respect the configured behavior
            JsonNode result = context.handleFilteringFailure("All choice options were filtered out");
            // If handleFilteringFailure returns null (RETURN_NULL behavior), that's fine for validation
            // If it throws an exception (THROW_EXCEPTION behavior), that's what we want
        }
    }

    /**
     * Computes filtered values from filter nodes.
     * This is a simplified version that doesn't generate actual data.
     */
    private List<JsonNode> computeFilteredValues(List<FilterNode> filters) {
        List<JsonNode> filterValues = new ArrayList<>();
        for (FilterNode filter : filters) {
            // For validation purposes, we need to evaluate the filter
            // This is tricky because filters can reference other data
            // For now, we'll use a simplified approach
            JsonNode filterValue = evaluateFilterForValidation(filter);
            if (filterValue != null && !filterValue.isNull()) {
                filterValues.add(filterValue);
            }
        }
        return filterValues;
    }

    /**
     * Evaluates a filter node for validation purposes.
     * This is a simplified evaluation that doesn't generate full data.
     */
    private JsonNode evaluateFilterForValidation(FilterNode filter) {
        // For validation purposes, we'll use a simplified visitor
        // that can handle basic cases without full data generation
        try {
            return filter.accept(new DataGenerationVisitor(context));
        } catch (Exception e) {
            // If we can't evaluate the filter, we can't validate
            // This is acceptable for validation purposes
            return null;
        }
    }

    /**
     * Evaluates a choice option node for validation purposes.
     */
    private JsonNode evaluateOptionForValidation(DslNode optionNode) {
        try {
            return optionNode.accept(new DataGenerationVisitor(context));
        } catch (Exception e) {
            // If we can't evaluate the option, we can't validate
            return null;
        }
    }
}