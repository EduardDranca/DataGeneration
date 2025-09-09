package com.github.eddranca.datagenerator.exception;

/**
 * Exception thrown when serialization/deserialization operations fail.
 */
public class SerializationException extends DataGenerationException {
    private static final long serialVersionUID = 1L;

    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
