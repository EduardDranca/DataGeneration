package com.github.eddranca.datagenerator.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.mcp.storage.StorageMode;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.github.eddranca.datagenerator.mcp.tools.ToolHelper.*;

public class DslFileTools {

    private final StorageMode storage;
    private final ObjectMapper mapper;
    private final McpJsonMapper jsonMapper;

    public DslFileTools(StorageMode storage, ObjectMapper mapper, McpJsonMapper jsonMapper) {
        this.storage = storage;
        this.mapper = mapper;
        this.jsonMapper = jsonMapper;
    }

    public SyncToolSpecification createDslFile() {
        String schema = """
            {
                "type": "object",
                "properties": {
                    "filePath": {
                        "type": "string",
                        "description": "Full path for the DSL JSON file to create"
                    }
                },
                "required": ["filePath"]
            }
            """;

        String description = storage.isInMemory()
            ? "Create a new empty DSL JSON file in memory for data generation. The file path is used as an identifier but no file is written to disk."
            : "Create a new empty DSL JSON file for data generation";

        return SyncToolSpecification.builder()
            .tool(tool("create_dsl_file", description, schema, jsonMapper))
            .callHandler((exchange, request) -> handleCreateDslFile(request.arguments()))
            .build();
    }

    public SyncToolSpecification readDslFile() {
        String schema = """
            {
                "type": "object",
                "properties": {
                    "filePath": {
                        "type": "string",
                        "description": "Full path to the DSL JSON file to read"
                    }
                },
                "required": ["filePath"]
            }
            """;

        String description = storage.isInMemory()
            ? "Read the current contents of a DSL JSON file from memory. This is the only way to inspect the DSL since the file only exists in server memory."
            : "Read the current contents of a DSL JSON file";

        return SyncToolSpecification.builder()
            .tool(tool("read_dsl_file", description, schema, jsonMapper))
            .callHandler((exchange, request) -> handleReadDslFile(request.arguments()))
            .build();
    }

    public SyncToolSpecification addCollection() {
        String schema = """
            {
                "type": "object",
                "properties": {
                    "filePath": {
                        "type": "string",
                        "description": "Full path to the DSL JSON file"
                    },
                    "collectionName": {
                        "type": "string",
                        "description": "Name of the collection to add"
                    },
                    "collectionJson": {
                        "type": "string",
                        "description": "JSON definition of the collection (the value object with count, item, etc.)"
                    }
                },
                "required": ["filePath", "collectionName", "collectionJson"]
            }
            """;

        String description = storage.isInMemory()
            ? "Add a collection to an existing DSL JSON file in memory. The DSL file only exists in server memory."
            : "Add a collection to an existing DSL JSON file";

        return SyncToolSpecification.builder()
            .tool(tool("add_collection", description, schema, jsonMapper))
            .callHandler((exchange, request) -> handleAddCollection(request.arguments()))
            .build();
    }

    public SyncToolSpecification removeCollection() {
        String schema = """
            {
                "type": "object",
                "properties": {
                    "filePath": {
                        "type": "string",
                        "description": "Full path to the DSL JSON file"
                    },
                    "collectionName": {
                        "type": "string",
                        "description": "Name of the collection to remove"
                    }
                },
                "required": ["filePath", "collectionName"]
            }
            """;

        String description = storage.isInMemory()
            ? "Remove a collection from a DSL JSON file in memory. The DSL file only exists in server memory."
            : "Remove a collection from a DSL JSON file";

        return SyncToolSpecification.builder()
            .tool(tool("remove_collection", description, schema, jsonMapper))
            .callHandler((exchange, request) -> handleRemoveCollection(request.arguments()))
            .build();
    }

    public SyncToolSpecification saveDslFile() {
        String schema = """
            {
                "type": "object",
                "properties": {
                    "filePath": {
                        "type": "string",
                        "description": "The in-memory DSL file identifier to save"
                    },
                    "outputPath": {
                        "type": "string",
                        "description": "Full path where the DSL JSON file should be saved on disk"
                    }
                },
                "required": ["filePath", "outputPath"]
            }
            """;

        return SyncToolSpecification.builder()
            .tool(tool("save_dsl_file", "Save an in-memory DSL JSON file to disk", schema, jsonMapper))
            .callHandler((exchange, request) -> handleSaveDslFile(request.arguments()))
            .build();
    }

    private CallToolResult handleCreateDslFile(Map<String, Object> args) {
        try {
            String filePath = (String) args.get("filePath");
            if (filePath == null || filePath.isBlank()) {
                return errorResult("filePath is required");
            }

            String content = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(mapper.createObjectNode());
            storage.write(filePath, content);

            String mode = storage.isInMemory() ? " (in-memory)" : "";
            return textResult("Created DSL file: " + filePath + mode);
        } catch (Exception e) {
            return errorResult("Failed to create DSL file: " + e.getMessage());
        }
    }

    private CallToolResult handleReadDslFile(Map<String, Object> args) {
        try {
            String filePath = (String) args.get("filePath");
            if (filePath == null || filePath.isBlank()) {
                return errorResult("filePath is required");
            }
            if (!storage.exists(filePath)) {
                return errorResult("DSL file not found: " + filePath);
            }

            return textResult(storage.read(filePath));
        } catch (Exception e) {
            return errorResult("Failed to read DSL file: " + e.getMessage());
        }
    }

    private CallToolResult handleAddCollection(Map<String, Object> args) {
        try {
            String filePath = (String) args.get("filePath");
            String collectionName = (String) args.get("collectionName");
            String collectionJson = (String) args.get("collectionJson");

            if (filePath == null || filePath.isBlank()) {
                return errorResult("filePath is required");
            }
            if (collectionName == null || collectionName.isBlank()) {
                return errorResult("collectionName is required");
            }
            if (collectionJson == null || collectionJson.isBlank()) {
                return errorResult("collectionJson is required");
            }

            if (!storage.exists(filePath)) {
                return errorResult("DSL file not found: " + filePath);
            }

            String existing = storage.read(filePath);
            ObjectNode root = (ObjectNode) mapper.readTree(existing);
            JsonNode collectionNode = mapper.readTree(collectionJson);
            root.set(collectionName, collectionNode);

            String updated = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            storage.write(filePath, updated);

            return textResult("Added collection '" + collectionName + "' to " + filePath);
        } catch (Exception e) {
            return errorResult("Failed to add collection: " + e.getMessage());
        }
    }

    private CallToolResult handleRemoveCollection(Map<String, Object> args) {
        try {
            String filePath = (String) args.get("filePath");
            String collectionName = (String) args.get("collectionName");

            if (filePath == null || filePath.isBlank()) {
                return errorResult("filePath is required");
            }
            if (collectionName == null || collectionName.isBlank()) {
                return errorResult("collectionName is required");
            }
            if (!storage.exists(filePath)) {
                return errorResult("DSL file not found: " + filePath);
            }

            String existing = storage.read(filePath);
            ObjectNode root = (ObjectNode) mapper.readTree(existing);

            if (!root.has(collectionName)) {
                return errorResult("Collection '" + collectionName + "' not found in DSL file");
            }

            root.remove(collectionName);
            String updated = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            storage.write(filePath, updated);

            return textResult("Removed collection '" + collectionName + "' from " + filePath);
        } catch (Exception e) {
            return errorResult("Failed to remove collection: " + e.getMessage());
        }
    }


    private CallToolResult handleSaveDslFile(Map<String, Object> args) {
        try {
            String filePath = (String) args.get("filePath");
            String outputPath = (String) args.get("outputPath");

            if (filePath == null || filePath.isBlank()) {
                return errorResult("filePath is required");
            }
            if (outputPath == null || outputPath.isBlank()) {
                return errorResult("outputPath is required");
            }
            if (!storage.exists(filePath)) {
                return errorResult("DSL file not found in memory: " + filePath);
            }

            String content = storage.read(filePath);
            Path path = Path.of(outputPath);
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content);

            return textResult("Saved DSL file to: " + outputPath);
        } catch (Exception e) {
            return errorResult("Failed to save DSL file: " + e.getMessage());
        }
    }

}
