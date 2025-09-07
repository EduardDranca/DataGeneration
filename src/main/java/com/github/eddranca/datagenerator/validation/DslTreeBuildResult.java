package com.github.eddranca.datagenerator.validation;

import com.github.eddranca.datagenerator.ValidationError;
import com.github.eddranca.datagenerator.node.RootNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of building a DSL tree, containing both the tree and any validation errors.
 */
public class DslTreeBuildResult {
    private final RootNode tree;
    private final List<ValidationError> errors;

    public DslTreeBuildResult(RootNode tree, List<ValidationError> errors) {
        this.tree = tree;
        this.errors = new ArrayList<>(errors);
    }

    public RootNode getTree() {
        return tree;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
