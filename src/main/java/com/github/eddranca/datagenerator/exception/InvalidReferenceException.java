package com.github.eddranca.datagenerator.exception;

/**
 * Exception thrown when an invalid reference pattern is encountered during generation.
 * This indicates a bug in the validation logic.
 */
public class InvalidReferenceException extends RuntimeException {

    public InvalidReferenceException(String reference) {
        super("Invalid reference pattern: '" + reference + "'");
    }

    public InvalidReferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
