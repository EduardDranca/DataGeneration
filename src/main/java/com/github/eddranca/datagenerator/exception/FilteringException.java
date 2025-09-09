package com.github.eddranca.datagenerator.exception;

/**
 * Exception thrown when filtering results in no available values and the
 * filtering behavior is set to THROW_EXCEPTION.
 */
public class FilteringException extends DataGenerationException {
    private static final long serialVersionUID = 1L;

    public FilteringException(String message) {
        super(message);
    }
}
