package com.github.eddranca.datagenerator.mcp.storage;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStorage implements StorageMode {

    private final Map<String, String> files = new ConcurrentHashMap<>();

    @Override
    public void write(String path, String content) {
        files.put(path, content);
    }

    @Override
    public String read(String path) throws IOException {
        String content = files.get(path);
        if (content == null) {
            throw new IOException("File not found in memory: " + path);
        }
        return content;
    }

    @Override
    public boolean exists(String path) {
        return files.containsKey(path);
    }

    @Override
    public void append(String path, String content) throws IOException {
        String existing = files.get(path);
        if (existing == null) {
            throw new IOException("File not found in memory: " + path);
        }
        files.put(path, existing + content);
    }

    @Override
    public boolean isInMemory() {
        return true;
    }

    public Map<String, String> getAllFiles() {
        return Map.copyOf(files);
    }
}
