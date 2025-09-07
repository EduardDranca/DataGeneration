package com.github.eddranca.datagenerator;

/**
 * Exception thrown when filtering results in no available values and the
 * filtering behavior is set to THROW_EXCEPTION.
 */
public class FilteringException extends RuntimeException {

    public FilteringException(String message) {
        super(message);
    }
}
