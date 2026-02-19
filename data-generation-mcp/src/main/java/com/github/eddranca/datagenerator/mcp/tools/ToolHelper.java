package com.github.eddranca.datagenerator.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public final class ToolHelper {

    private ToolHelper() {}

    public static Tool tool(String name, String description, String inputSchemaJson, McpJsonMapper jsonMapper) {
        return Tool.builder()
            .name(name)
            .description(description)
            .inputSchema(jsonMapper, inputSchemaJson)
            .build();
    }

    public static CallToolResult textResult(String text) {
        return CallToolResult.builder()
            .addTextContent(text)
            .build();
    }

    public static CallToolResult errorResult(String error) {
        return CallToolResult.builder()
            .addTextContent("Error: " + error)
            .isError(true)
            .build();
    }
}
