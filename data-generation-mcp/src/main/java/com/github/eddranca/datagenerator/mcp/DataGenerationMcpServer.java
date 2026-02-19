package com.github.eddranca.datagenerator.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.mcp.storage.FileSystemStorage;
import com.github.eddranca.datagenerator.mcp.storage.InMemoryStorage;
import com.github.eddranca.datagenerator.mcp.storage.StorageMode;
import com.github.eddranca.datagenerator.mcp.tools.DocumentationTools;
import com.github.eddranca.datagenerator.mcp.tools.DslFileTools;
import com.github.eddranca.datagenerator.mcp.tools.GenerationTools;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DataGenerationMcpServer {

    private static final String SERVER_NAME = "data-generation";
    private static final String SERVER_VERSION = "0.1.0";

    public static void main(String[] args) {
        boolean inMemory = false;
        String docsPath = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--in-memory" -> inMemory = true;
                case "--docs-path" -> {
                    if (i + 1 < args.length) {
                        docsPath = args[++i];
                    }
                }
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(objectMapper);
        StorageMode storage = inMemory ? new InMemoryStorage() : new FileSystemStorage();

        Path docsRoot = docsPath != null
            ? Path.of(docsPath)
            : Path.of("docs-site", "docs");

        DslFileTools dslFileTools = new DslFileTools(storage, objectMapper, jsonMapper);
        DocumentationTools documentationTools = new DocumentationTools(docsRoot, jsonMapper);
        GenerationTools generationTools = new GenerationTools(storage, objectMapper, jsonMapper);

        List<SyncToolSpecification> tools = new ArrayList<>();
        tools.add(dslFileTools.createDslFile());
        tools.add(dslFileTools.readDslFile());
        tools.add(dslFileTools.addCollection());
        tools.add(dslFileTools.removeCollection());
        if (storage.isInMemory()) {
            tools.add(dslFileTools.saveDslFile());
        }
        tools.add(documentationTools.listDocumentation());
        tools.add(documentationTools.readDocumentation());
        tools.add(generationTools.validateDsl());
        tools.add(generationTools.previewGeneration());
        tools.add(generationTools.generateSqlInserts());
        tools.add(generationTools.generateJson());

        StdioServerTransportProvider transport = new StdioServerTransportProvider(jsonMapper);

        McpSyncServer server = McpServer.sync(transport)
            .serverInfo(SERVER_NAME, SERVER_VERSION)
            .capabilities(ServerCapabilities.builder()
                .tools(true)
                .build())
            .tools(tools.toArray(SyncToolSpecification[]::new))
            .build();

        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
    }
}
