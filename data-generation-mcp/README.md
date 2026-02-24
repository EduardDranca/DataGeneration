# DataGeneration MCP Server

MCP (Model Context Protocol) server that enables LLMs to create DSL files and generate test data using the DataGeneration library.

## Tools

| Tool | Description |
|------|-------------|
| `create_dsl_file` | Create a new empty DSL JSON file |
| `read_dsl_file` | Read the current contents of a DSL file |
| `add_collection` | Add a collection to an existing DSL file |
| `update_collection` | Update an existing collection in a DSL file |
| `remove_collection` | Remove a collection from a DSL file |
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

## Path Requirements

All file paths must be absolute paths, not relative to the working directory. For example:
- `/Users/username/project/test-data/dsl.json` (macOS/Linux)
- `C:\Users\username\project\test-data\dsl.json` (Windows)

Generated output (SQL INSERT statements, JSON data) is always written to the filesystem using the provided absolute paths.