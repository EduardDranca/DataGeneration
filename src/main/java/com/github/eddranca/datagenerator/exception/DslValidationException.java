package com.github.eddranca.datagenerator.exception;

import com.github.eddranca.datagenerator.ValidationError;

import java.util.List;

/**
 * Exception thrown when DSL validation fails.
 * Contains all validation errors found during parsing.
 */
public class DslValidationException extends RuntimeException {
    private final List<ValidationError> validationErrors;

    public DslValidationException(List<ValidationError> validationErrors) {
        super(buildMessage(validationErrors));
        this.validationErrors = validationErrors;
    }

    private static String buildMessage(List<ValidationError> errors) {
        StringBuilder message = new StringBuilder("DSL validation failed with ");
        message.append(errors.size()).append(" error(s):\n");

        for (ValidationError error : errors) {
            message.append("  - ").append(error.toString()).append("\n");
        }

        return message.toString();
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }
}
