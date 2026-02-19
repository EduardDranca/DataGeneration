package com.github.eddranca.datagenerator.mcp.storage;

import java.io.IOException;

/**
 * Abstraction for file storage - supports both file system and in-memory modes.
 */
public interface StorageMode {

    /**
     * Write content to the given path.
     */
    void write(String path, String content) throws IOException;

    /**
     * Read content from the given path.
     */
    String read(String path) throws IOException;

    /**
     * Check if a file exists at the given path.
     */
    boolean exists(String path);

    /**
     * Append content to an existing file at the given path.
     */
    void append(String path, String content) throws IOException;

    /**
     * Returns true if this is in-memory mode.
     */
    boolean isInMemory();
}
