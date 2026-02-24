package com.github.eddranca.datagenerator.mcp.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemStorage {

    public void write(String path, String content) throws IOException {
        Path filePath = Path.of(path);
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.writeString(filePath, content);
    }

    public String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    public boolean exists(String path) {
        return Files.exists(Path.of(path));
    }
}
