package com.github.eddranca.datagenerator.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.mcp.storage.FileSystemStorage;
import com.github.eddranca.datagenerator.exception.DslValidationException;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.github.eddranca.datagenerator.mcp.tools.ToolHelper.*;

public class DslFileTools {

    private final FileSystemStorage storage;
    private final ObjectMapper mapper;
    private final McpJsonMapper jsonMapper;

    public DslFileTools(FileSystemStorage storage, ObjectMapper mapper, McpJsonMapper jsonMapper) {
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
                        "description": "Absolute path for the DSL JSON file to create"
                    }
                },
                "required": ["filePath"]
            }
            """;

        return SyncToolSpecification.builder()
            .tool(tool("create_dsl_file", "Create a new empty DSL JSON file for data generation", schema, jsonMapper))
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
                        "description": "Absolute path to the DSL JSON file to read"
                    }
                },
                "required": ["filePath"]
            }
            """;

        return SyncToolSpecification.builder()
            .tool(tool("read_dsl_file", "Read the current contents of a DSL JSON file", schema, jsonMapper))
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
                        "description": "Absolute path to the DSL JSON file"
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

        return SyncToolSpecification.builder()
            .tool(tool("add_collection", "Add a collection to an existing DSL JSON file", schema, jsonMapper))
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
                        "description": "Absolute path to the DSL JSON file"
                    },
                    "collectionName": {
                        "type": "string",
                        "description": "Name of the collection to remove"
                    }
                },
                "required": ["filePath", "collectionName"]
            }
            """;

        return SyncToolSpecification.builder()
            .tool(tool("remove_collection", "Remove a collection from a DSL JSON file", schema, jsonMapper))
            .callHandler((exchange, request) -> handleRemoveCollection(request.arguments()))
            .build();
    }

    public SyncToolSpecification updateCollection() {
        String schema = """
            {
                "type": "object",
                "properties": {
                    "filePath": {
                        "type": "string",
                        "description": "Absolute path to the DSL JSON file"
                    },
                    "collectionName": {
                        "type": "string",
                        "description": "Name of the collection to update"
                    },
                    "collectionJson": {
                        "type": "string",
                        "description": "Updated JSON definition of the collection (the value object with count, item, etc.)"
                    }
                },
                "required": ["filePath", "collectionName", "collectionJson"]
            }
            """;

        return SyncToolSpecification.builder()
            .tool(tool("update_collection", "Update an existing collection in a DSL JSON file", schema, jsonMapper))
            .callHandler((exchange, request) -> handleUpdateCollection(request.arguments()))
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

            return textResult("Created DSL file: " + filePath);
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

            String message = "Added collection '" + collectionName + "' to " + filePath;
            return textResult(message + validateAfterMutation(updated));
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

            String message = "Removed collection '" + collectionName + "' from " + filePath;
            return textResult(message + validateAfterMutation(updated));
        } catch (Exception e) {
            return errorResult("Failed to remove collection: " + e.getMessage());
        }
    }

    private CallToolResult handleUpdateCollection(Map<String, Object> args) {
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

            if (!root.has(collectionName)) {
                return errorResult("Collection '" + collectionName + "' not found in DSL file");
            }

            JsonNode collectionNode = mapper.readTree(collectionJson);
            root.set(collectionName, collectionNode);

            String updated = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            storage.write(filePath, updated);

            String message = "Updated collection '" + collectionName + "' in " + filePath;
            return textResult(message + validateAfterMutation(updated));
        } catch (Exception e) {
            return errorResult("Failed to update collection: " + e.getMessage());
        }
    }

    private String validateAfterMutation(String dslContent) {
        try {
            DslDataGenerator.create().fromJsonString(dslContent).generate();
            return "";
        } catch (DslValidationException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n\nWARNING: The DSL file now has validation errors that should be addressed:");
            for (var error : e.getValidationErrors()) {
                sb.append("\n  - ").append(error.toString());
            }
            return sb.toString();
        } catch (Exception e) {
            return "\n\nWARNING: The DSL file now has a validation error: " + e.getMessage();
        }
    }

}