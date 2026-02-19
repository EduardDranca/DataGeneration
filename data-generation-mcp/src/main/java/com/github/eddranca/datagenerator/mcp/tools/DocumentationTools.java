package com.github.eddranca.datagenerator.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.eddranca.datagenerator.mcp.tools.ToolHelper.*;

public class DocumentationTools {

    private final Path docsRoot;
    private final McpJsonMapper jsonMapper;

    public DocumentationTools(Path docsRoot, McpJsonMapper jsonMapper) {
        this.docsRoot = docsRoot;
        this.jsonMapper = jsonMapper;
    }

    public SyncToolSpecification listDocumentation() {
        String schema = """
            {
                "type": "object",
                "properties": {}
            }
            """;

        return SyncToolSpecification.builder()
            .tool(tool("list_documentation", "List all available documentation files with relative paths", schema, jsonMapper))
            .callHandler((exchange, request) -> handleListDocumentation())
            .build();
    }

    public SyncToolSpecification readDocumentation() {
        String schema = """
            {
                "type": "object",
                "properties": {
                    "relativePath": {
                        "type": "string",
                        "description": "Relative path to the documentation file, e.g. 'generators/uuid.md'"
                    }
                },
                "required": ["relativePath"]
            }
            """;

        return SyncToolSpecification.builder()
            .tool(tool("read_documentation", "Read a specific documentation file by relative path", schema, jsonMapper))
            .callHandler((exchange, request) -> handleReadDocumentation(request.arguments()))
            .build();
    }

    private CallToolResult handleListDocumentation() {
        try {
            if (!Files.exists(docsRoot)) {
                return errorResult("Documentation directory not found: " + docsRoot);
            }

            String listing;
            try (Stream<Path> paths = Files.walk(docsRoot)) {
                listing = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .map(p -> docsRoot.relativize(p).toString())
                    .sorted()
                    .collect(Collectors.joining("\n"));
            }

            if (listing.isEmpty()) {
                return textResult("No documentation files found.");
            }
            return textResult(listing);
        } catch (IOException e) {
            return errorResult("Failed to list documentation: " + e.getMessage());
        }
    }

    private CallToolResult handleReadDocumentation(Map<String, Object> args) {
        try {
            String relativePath = (String) args.get("relativePath");
            if (relativePath == null || relativePath.isBlank()) {
                return errorResult("relativePath is required");
            }

            Path filePath = docsRoot.resolve(relativePath).normalize();
            if (!filePath.startsWith(docsRoot)) {
                return errorResult("Path traversal not allowed");
            }
            if (!Files.exists(filePath)) {
                return errorResult("Documentation file not found: " + relativePath);
            }

            return textResult(Files.readString(filePath));
        } catch (IOException e) {
            return errorResult("Failed to read documentation: " + e.getMessage());
        }
    }
}
