package com.github.eddranca.datagenerator;

public class ValidationError {
    private final String path;
    private final String message;

    public ValidationError(String path, String message) {
        this.path = path;
        this.message = message;
    }

    @Override
    public String toString() {
        return "[" + path + "] " + message;
    }
}


