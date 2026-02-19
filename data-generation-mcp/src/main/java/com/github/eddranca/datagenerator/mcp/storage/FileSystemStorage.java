package com.github.eddranca.datagenerator.mcp.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileSystemStorage implements StorageMode {

    @Override
    public void write(String path, String content) throws IOException {
        Path filePath = Path.of(path);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content);
    }

    @Override
    public String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    @Override
    public boolean exists(String path) {
        return Files.exists(Path.of(path));
    }

    @Override
    public void append(String path, String content) throws IOException {
        Files.writeString(Path.of(path), content, StandardOpenOption.APPEND);
    }

    @Override
    public boolean isInMemory() {
        return false;
    }
}
