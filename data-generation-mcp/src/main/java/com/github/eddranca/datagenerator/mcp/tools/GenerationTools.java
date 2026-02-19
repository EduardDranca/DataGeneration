package com.github.eddranca.datagenerator.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.Generation;
import com.github.eddranca.datagenerator.mcp.storage.StorageMode;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.github.eddranca.datagenerator.mcp.tools.ToolHelper.*;

public class GenerationTools {

    private final StorageMode storage;
    private final ObjectMapper mapper;
    private final McpJsonMapper jsonMapper;

    public GenerationTools(StorageMode storage, ObjectMapper mapper, McpJsonMapper jsonMapper) {
        this.storage = storage;
        this.mapper = mapper;
        this.jsonMapper = jsonMapper;
    }

    public SyncToolSpecification validateDsl() {
        String schema = """
            {
                "type": "object",
                "properties": {
                    "filePath": {
                        "type": "string",
                        "description": "Full path to the DSL JSON file to validate"
                    }
                },
                "required": ["filePath"]
            }
            """;

        return SyncToolSpecification.builder()
            .tool(tool("validate_dsl", "Validate a DSL JSON file without generating data", schema, jsonMapper))
            .callHandler((exchange, request) -> handleValidateDsl(request.arguments()))
            .build();
    }

    public SyncToolSpecification previewGeneration() {
        String schema = """
            {
                "type": "object",
                "properties": {
                    "dslFilePath": {
                        "type": "string",
                        "description": "Full path to the DSL JSON file"
                    },
                    "itemsPerCollection": {
                        "type": "integer",
                        "description": "Number of items to generate per collection for preview (default: 3, max: 10)"
                    },
                    "format": {
                        "type": "string",
                        "enum": ["json", "sql"],
                        "description": "Output format for preview (default: json)"
                    }
                },
                "required": ["dslFilePath"]
            }
            """;

        return SyncToolSpecification.builder()
            .tool(tool("preview_generation", "Generate a small preview of data from a DSL file and return it inline (no file written)", schema, jsonMapper))
            .callHandler((exchange, request) -> handlePreviewGeneration(request.arguments()))
            .build();
    }

    public SyncToolSpecification generateSqlInserts() {
        String schema = """
            {
                "type": "object",
                "properties": {
                    "dslFilePath": {
                        "type": "string",
                        "description": "Full path to the DSL JSON file"
                    },
                    "outputFilePath": {
                        "type": "string",
                        "description": "Full path for the output SQL file"
                    },
                    "memoryOptimized": {
                        "type": "boolean",
                        "description": "Use memory-optimized generation (default: false)"
                    },
                    "seed": {
                        "type": "integer",
                        "description": "Seed for reproducible generation (optional)"
                    }
                },
                "required": ["dslFilePath", "outputFilePath"]
            }
            """;

        return SyncToolSpecification.builder()
            .tool(tool("generate_sql_inserts", "Generate SQL INSERT statements from a DSL file and write to output file", schema, jsonMapper))
            .callHandler((exchange, request) -> handleGenerateSqlInserts(request.arguments()))
            .build();
    }

    public SyncToolSpecification generateJson() {
        String schema = """
            {
                "type": "object",
                "properties": {
                    "dslFilePath": {
                        "type": "string",
                        "description": "Full path to the DSL JSON file"
                    },
                    "outputFilePath": {
                        "type": "string",
                        "description": "Full path for the output JSON file"
                    },
                    "memoryOptimized": {
                        "type": "boolean",
                        "description": "Use memory-optimized generation (default: false)"
                    },
                    "seed": {
                        "type": "integer",
                        "description": "Seed for reproducible generation (optional)"
                    }
                },
                "required": ["dslFilePath", "outputFilePath"]
            }
            """;

        return SyncToolSpecification.builder()
            .tool(tool("generate_json", "Generate JSON data from a DSL file and write to output file", schema, jsonMapper))
            .callHandler((exchange, request) -> handleGenerateJson(request.arguments()))
            .build();
    }

    private CallToolResult handleValidateDsl(Map<String, Object> args) {
        try {
            String filePath = (String) args.get("filePath");
            if (filePath == null || filePath.isBlank()) {
                return errorResult("filePath is required");
            }
            if (!storage.exists(filePath)) {
                return errorResult("DSL file not found: " + filePath);
            }

            String dslContent = storage.read(filePath);
            DslDataGenerator.create().fromJsonString(dslContent).generate();
            return textResult("DSL file is valid: " + filePath);
        } catch (Exception e) {
            return errorResult("Validation failed: " + e.getMessage());
        }
    }

    private CallToolResult handlePreviewGeneration(Map<String, Object> args) {
        try {
            String dslFilePath = (String) args.get("dslFilePath");
            if (dslFilePath == null || dslFilePath.isBlank()) {
                return errorResult("dslFilePath is required");
            }
            if (!storage.exists(dslFilePath)) {
                return errorResult("DSL file not found: " + dslFilePath);
            }

            int itemsPerCollection = 3;
            if (args.get("itemsPerCollection") != null) {
                itemsPerCollection = Math.min(((Number) args.get("itemsPerCollection")).intValue(), 10);
            }

            String format = "json";
            if (args.get("format") != null) {
                format = (String) args.get("format");
            }

            String dslContent = storage.read(dslFilePath);
            String previewDsl = overrideCounts(dslContent, itemsPerCollection);
            Generation generation = buildGeneration(previewDsl, false, null);

            if ("sql".equals(format)) {
                StringBuilder sql = new StringBuilder();
                for (String collectionName : generation.getCollectionNames()) {
                    generation.streamSqlInserts(collectionName)
                        .forEach(insert -> sql.append(insert).append("\n"));
                    sql.append("\n");
                }
                return textResult(sql.toString());
            } else {
                var rootNode = mapper.createObjectNode();
                for (String collectionName : generation.getCollectionNames()) {
                    var arrayNode = mapper.createArrayNode();
                    generation.streamJsonNodes(collectionName).forEach(arrayNode::add);
                    rootNode.set(collectionName, arrayNode);
                }
                return textResult(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode));
            }
        } catch (Exception e) {
            return errorResult("Preview failed: " + e.getMessage());
        }
    }

    private CallToolResult handleGenerateSqlInserts(Map<String, Object> args) {
        try {
            String dslFilePath = (String) args.get("dslFilePath");
            String outputFilePath = (String) args.get("outputFilePath");
            boolean memoryOptimized = Boolean.TRUE.equals(args.get("memoryOptimized"));
            Long seed = args.get("seed") != null ? ((Number) args.get("seed")).longValue() : null;

            if (dslFilePath == null || dslFilePath.isBlank()) {
                return errorResult("dslFilePath is required");
            }
            if (outputFilePath == null || outputFilePath.isBlank()) {
                return errorResult("outputFilePath is required");
            }
            if (!storage.exists(dslFilePath)) {
                return errorResult("DSL file not found: " + dslFilePath);
            }

            String dslContent = storage.read(dslFilePath);
            Generation generation = buildGeneration(dslContent, memoryOptimized, seed);

            StringBuilder sql = new StringBuilder();
            for (String collectionName : generation.getCollectionNames()) {
                generation.streamSqlInserts(collectionName)
                    .forEach(insert -> sql.append(insert).append("\n"));
                sql.append("\n");
            }

            writeToFile(outputFilePath, sql.toString());

            return textResult("Generated SQL inserts to: " + outputFilePath
                + "\nCollections: " + String.join(", ", generation.getCollectionNames()));
        } catch (Exception e) {
            return errorResult("SQL generation failed: " + e.getMessage());
        }
    }

    private CallToolResult handleGenerateJson(Map<String, Object> args) {
        try {
            String dslFilePath = (String) args.get("dslFilePath");
            String outputFilePath = (String) args.get("outputFilePath");
            boolean memoryOptimized = Boolean.TRUE.equals(args.get("memoryOptimized"));
            Long seed = args.get("seed") != null ? ((Number) args.get("seed")).longValue() : null;

            if (dslFilePath == null || dslFilePath.isBlank()) {
                return errorResult("dslFilePath is required");
            }
            if (outputFilePath == null || outputFilePath.isBlank()) {
                return errorResult("outputFilePath is required");
            }
            if (!storage.exists(dslFilePath)) {
                return errorResult("DSL file not found: " + dslFilePath);
            }

            String dslContent = storage.read(dslFilePath);
            Generation generation = buildGeneration(dslContent, memoryOptimized, seed);

            var rootNode = mapper.createObjectNode();
            for (String collectionName : generation.getCollectionNames()) {
                var arrayNode = mapper.createArrayNode();
                generation.streamJsonNodes(collectionName).forEach(arrayNode::add);
                rootNode.set(collectionName, arrayNode);
            }

            String jsonOutput = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            writeToFile(outputFilePath, jsonOutput);

            return textResult("Generated JSON to: " + outputFilePath
                + "\nCollections: " + String.join(", ", generation.getCollectionNames()));
        } catch (Exception e) {
            return errorResult("JSON generation failed: " + e.getMessage());
        }
    }

    private String overrideCounts(String dslContent, int count) throws Exception {
        ObjectNode root = (ObjectNode) mapper.readTree(dslContent);
        root.properties().forEach(entry -> {
            JsonNode collection = entry.getValue();
            if (collection.isObject() && collection.has("count")) {
                ((ObjectNode) collection).put("count", count);
            }
        });
        return mapper.writeValueAsString(root);
    }

    private Generation buildGeneration(String dslContent, boolean memoryOptimized, Long seed) throws Exception {
        var builder = DslDataGenerator.create();
        if (memoryOptimized) {
            builder = builder.withMemoryOptimization();
        }
        if (seed != null) {
            builder = builder.withSeed(seed);
        }
        return builder.fromJsonString(dslContent).generate();
    }

    private void writeToFile(String filePath, String content) throws Exception {
        Path path = Path.of(filePath);
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, content);
    }
}
