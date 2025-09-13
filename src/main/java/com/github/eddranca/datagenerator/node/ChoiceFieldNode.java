package com.github.eddranca.datagenerator.node;

import java.util.ArrayList;
import java.util.List;

/**
 * Field node that randomly selects from a list of options.
 * Each option can be any type of field (generated, reference, literal, etc.).
 * Supports weighted selection where each option can have a weight that influences
 * its probability of being selected.
 * Supports filtering to exclude specific values from selection.
 */
public class ChoiceFieldNode implements DslNode {
    private final List<DslNode> options;
    private final List<Double> weights;
    private final double totalWeight;
    private final List<FilterNode> filters;

    /**
     * Creates a ChoiceFieldNode with equal weights for all options.
     */
    public ChoiceFieldNode(List<DslNode> options) {
        this(options, null, new ArrayList<>());
    }

    /**
     * Creates a ChoiceFieldNode with specified weights and filters.
     *
     * @param options the list of field options
     * @param weights the weights for each option (must match options size, null for equal weights)
     * @param filters the filters to apply
     */
    public ChoiceFieldNode(List<DslNode> options, List<Double> weights, List<FilterNode> filters) {
        if (weights != null && options.size() != weights.size()) {
            throw new IllegalArgumentException("Options and weights lists must have the same size");
        }

        this.options = new ArrayList<>(options);
        this.weights = weights != null ? new ArrayList<>(weights) : null;
        this.filters = new ArrayList<>(filters);

        // Calculate total weight and validate weights
        if (weights != null) {
            double total = 0.0;
            for (Double weight : weights) {
                if (weight == null || weight <= 0) {
                    throw new IllegalArgumentException("All weights must be positive numbers");
                }
                total += weight;
            }
            this.totalWeight = total;
        } else {
            this.totalWeight = options.size();
        }
    }

    /**
     * Creates a ChoiceFieldNode with filters (static factory method to avoid type erasure issues).
     *
     * @param options the list of field options
     * @param filters the filters to apply
     * @return a new ChoiceFieldNode with filters
     */
    public static ChoiceFieldNode withFilters(List<DslNode> options, List<FilterNode> filters) {
        return new ChoiceFieldNode(options, null, filters);
    }

    /**
     * Creates a ChoiceFieldNode with weights and filters (static factory method).
     *
     * @param options the list of field options
     * @param weights the weights for each option (must match options size)
     * @param filters the filters to apply
     * @return a new ChoiceFieldNode with weights and filters
     */
    public static ChoiceFieldNode withWeightsAndFilters(List<DslNode> options, List<Double> weights, List<FilterNode> filters) {
        return new ChoiceFieldNode(options, weights, filters);
    }

    public List<DslNode> getOptions() {
        return options;
    }

    public List<Double> getWeights() {
        return weights;
    }

    public boolean hasWeights() {
        return weights != null;
    }

    public List<FilterNode> getFilters() {
        return filters;
    }

    public boolean hasFilters() {
        return !filters.isEmpty();
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitChoiceField(this);
    }
}
