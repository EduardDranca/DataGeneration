package com.github.eddranca.datagenerator.builder;

import com.github.eddranca.datagenerator.ValidationError;
import com.github.eddranca.datagenerator.expression.ExpressionFunctionRegistry;
import com.github.eddranca.datagenerator.generator.GeneratorOptionSpec;
import com.github.eddranca.datagenerator.validation.ValidationContext;

import java.util.List;

/**
 * Shared context for node builders containing validation context and error collection.
 */
class NodeBuilderContext {
    private final ValidationContext validationContext;
    private final List<ValidationError> errors;
    private final ExpressionFunctionRegistry expressionFunctionRegistry;

    public NodeBuilderContext(ValidationContext validationContext, List<ValidationError> errors,
                              ExpressionFunctionRegistry expressionFunctionRegistry) {
        this.validationContext = validationContext;
        this.errors = errors;
        this.expressionFunctionRegistry = expressionFunctionRegistry;
    }

    public void addError(String message) {
        String path = validationContext.getCurrentCollection() != null ?
            validationContext.getCurrentCollection() : "root";
        errors.add(new ValidationError(path, message));
    }

    public boolean isGeneratorRegistered(String generatorName) {
        return validationContext.isGeneratorRegistered(generatorName);
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

    public GeneratorOptionSpec getGeneratorOptionSpec(String generatorName) {
        return validationContext.getGeneratorOptionSpec(generatorName);
    }

    public ExpressionFunctionRegistry getExpressionFunctionRegistry() {
        return expressionFunctionRegistry;
    }
}
