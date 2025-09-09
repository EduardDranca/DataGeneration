package com.github.eddranca.datagenerator.exception;

/**
 * Base runtime exception for data processing issues during generation.
 * These are typically programming errors or unexpected data conditions.
 */
public class DataGenerationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DataGenerationException(String message) {
        super(message);
    }

    public DataGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
