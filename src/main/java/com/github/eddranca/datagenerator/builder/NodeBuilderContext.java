package com.github.eddranca.datagenerator.builder;

import com.github.eddranca.datagenerator.ValidationError;
import com.github.eddranca.datagenerator.validation.ValidationContext;

import java.util.List;

/**
 * Shared context for node builders containing validation context and error collection.
 */
public class NodeBuilderContext {
    private final ValidationContext validationContext;
    private final List<ValidationError> errors;

    public NodeBuilderContext(ValidationContext validationContext, List<ValidationError> errors) {
        this.validationContext = validationContext;
        this.errors = errors;
    }

    public ValidationContext getValidationContext() {
        return validationContext;
    }

    public void addError(String message) {
        String path = validationContext.getCurrentCollection() != null ?
                validationContext.getCurrentCollection() : "root";
        errors.add(new ValidationError(path, message));
    }

    public boolean isGeneratorRegistered(String generatorName) {
        return validationContext.isGeneratorRegistered(generatorName);
    }

    public boolean isTagDeclared(String tag) {
        return validationContext.isTagDeclared(tag);
    }

    public boolean isCollectionDeclared(String collection) {
        return validationContext.isCollectionDeclared(collection);
    }

    public boolean isPickDeclared(String pick) {
        return validationContext.isPickDeclared(pick);
    }

    public void declarePick(String pick) {
        validationContext.declarePick(pick);
    }
}