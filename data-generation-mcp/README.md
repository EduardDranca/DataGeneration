# DataGeneration MCP Server

MCP (Model Context Protocol) server that enables LLMs to create DSL files and generate test data using the DataGeneration library.

## Tools

| Tool | Description |
|------|-------------|
| `create_dsl_file` | Create a new empty DSL JSON file |
| `read_dsl_file` | Read the current contents of a DSL file |
| `add_collection` | Add a collection to an existing DSL file |
| `remove_collection` | Remove a collection from a DSL file |
| `save_dsl_file` | Save an in-memory DSL file to disk (in-memory mode only) |
| `list_documentation` | List available documentation files |
| `read_documentation` | Read a specific documentation file |
| `validate_dsl` | Validate a DSL file without generating |
| `preview_generation` | Generate a small inline preview (no file written) |
| `generate_sql_inserts` | Generate SQL INSERT statements from DSL |
| `generate_json` | Generate JSON data from DSL |

## Build

```bash
# Requires the data-generation library in local Maven repo
cd .. && mvn install -DskipTests && cd data-generation-mcp

# Build fat jar
mvn package
```

## Usage

```bash
java -jar target/data-generation-mcp-0.1.0.jar [options]
```

### Options

| Flag | Description |
|------|-------------|
| `--in-memory` | Run in memory mode (DSL files are managed in-memory; generated output always writes to filesystem) |
| `--docs-path <path>` | Custom path to documentation directory (default: `docs-site/docs`) |

## MCP Configuration

### Kiro / VS Code

Add to `.kiro/settings/mcp.json` or equivalent:

```json
{
  "mcpServers": {
    "data-generation": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/data-generation-mcp/target/data-generation-mcp-0.1.0.jar",
        "--docs-path",
        "/absolute/path/to/docs-site/docs"
      ],
      "disabled": false,
      "autoApprove": []
    }
  }
}
```

### In-Memory Mode

For environments where you want DSL file management to stay in-memory (generated output still writes to filesystem):

```json
{
  "mcpServers": {
    "data-generation": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/data-generation-mcp/target/data-generation-mcp-0.1.0.jar",
        "--in-memory",
        "--docs-path",
        "/absolute/path/to/docs-site/docs"
      ]
    }
  }
}
```
