---
name: data-generation-mcp
description: Skill for using the DataGeneration MCP server to generate test data via declarative JSON DSL
---

# Overview

The DataGeneration MCP server enables LLMs to create DSL (Domain Specific Language) files and generate test data using the DataGeneration library. The typical workflow is:

1. **Understand the project requirements** - Analyze the project's schema, data models, and test data needs
2. **Read the documentation** - Use `list_documentation` and `read_documentation` to understand available generators and DSL syntax
3. **Create a DSL file** - Use `create_dsl_file` to start a new DSL definition
4. **Add collections** - Use `add_collection` to define data collections with generators
5. **Validate and preview** - Use `validate_dsl` and `preview_generation` to check the DSL
6. **Generate output** - Use `generate_sql_inserts` or `generate_json` to produce test data

## DSL Workflow

### Step 1: Read Documentation First

**CRITICAL: Do not hallucinate DSL syntax or generator options.**

Always start by understanding what generators and features you need, then read the relevant documentation:

1. Use `list_documentation` to see available documentation files
2. Use `read_documentation` to read about specific generators and features you plan to use:
   - `generators/overview.md` - Overview of all available generators
   - `generators/<generator-name>.md` - Specific generator documentation (e.g., `generators/number.md`, `generators/date.md`)
   - `dsl-reference/overview.md` - DSL structure and syntax
   - `dsl-reference/references.md` - Reference syntax for cross-collection references
   - `dsl-reference/filtering.md` - Filtering options
   - `dsl-reference/arrays.md` - Array/collection syntax

**Common hallucination risks:**
- Generator options that don't exist (e.g., `number.numberBetween`)
- Incorrect reference syntax
- Missing required fields in collection definitions
- Invalid filter expressions

Always verify generator options and DSL syntax in the documentation before creating DSL files.

### Step 2: Create a DSL File

Use `create_dsl_file` to create a new empty DSL JSON file:

- Provide an **absolute path** for `filePath` (e.g., `/Users/username/project/test-data/dsl.json`)
- The file will be created empty with `{}`
- Use `read_dsl_file` to verify the file was created

### Step 3: Add Collections

Use `add_collection` to add collections to the DSL file:

- Each collection represents a table/entity in the generated data
- Provide the `filePath` (absolute path), `collectionName`, and `collectionJson`
- The `collectionJson` should be the JSON object defining count, item, fields, etc.

Example collection:
```json
{
  "count": 10,
  "item": {
    "id": {"gen": "uuid"},
    "name": {"gen": "name.fullName"},
    "email": {"gen": "internet.emailAddress"}
  }
}
```

### Step 4: Update Collections as Needed

Use `update_collection` to modify existing collections:

- Provide the `filePath` (absolute path), `collectionName`, and updated `collectionJson`
- The entire collection will be replaced with the new definition
- Use `read_dsl_file` first to get the current state if needed

### Step 5: Remove Collections (if needed)

Use `remove_collection` to remove collections from the DSL file:

- Provide the `filePath` (absolute path) and `collectionName`
- The collection and all its data will be removed

## Tool Usage Guidelines

### Always Use Tools for DSL Interaction

**DO NOT** modify DSL files directly. **ALWAYS** use the provided tools:

- Use `create_dsl_file` to create new DSL files
- Use `read_dsl_file` to inspect DSL content
- Use `add_collection` to add collections
- Use `update_collection` to modify collections
- Use `remove_collection` to remove collections

This ensures the MCP server maintains proper state and can validate changes.

### Validation Before Generation

Always validate the DSL before generating output:

1. Use `validate_dsl` to check for DSL syntax errors
2. Use `preview_generation` to see a small inline preview (no file written)
3. Fix any errors before proceeding to full generation

### Generation Always Writes to Filesystem

Generated output (SQL INSERT statements, JSON data) is always written to the filesystem:

- Use `generate_sql_inserts` to generate SQL INSERT statements
- Use `generate_json` to generate JSON data
- Provide **absolute paths** for `outputPath` to specify where files should be written
- Generated files are never returned inline to avoid overwhelming context

## Path Requirements

**All file paths must be absolute paths, not relative to the working directory.**

Examples:
- macOS/Linux: `/Users/username/project/test-data/dsl.json`
- Windows: `C:\Users\username\project\test-data\dsl.json`

This applies to:
- DSL file paths (`filePath` parameter)
- Output paths for generated files (`outputPath` parameter)

## Common Patterns

### User-Order Relationship

Create users first, then orders that reference users:

```json
{
  "users": {
    "count": 10,
    "item": {"id": {"gen": "uuid"}}
  },
  "orders": {
    "count": 20,
    "item": {
      "userId": {"ref": "users[*].id"},
      "total": {"gen": "number", "min": 10, "max": 1000}
    }
  }
}
```

### Admin User with Pick

Create a specific admin user and reference it:

```json
{
  "users": {
    "count": 10,
    "item": {"id": {"gen": "uuid"}},
    "pick": {"admin": 0}
  },
  "logs": {
    "count": 50,
    "item": {
      "userId": {"ref": "users[*].id", "filter": [{"ref": "admin.id"}]}
    }
  }
}
```

### Hierarchical Data with Sequential References

Create departments and teams with sequential references:

```json
{
  "departments": {
    "count": 3,
    "item": {"id": {"gen": "uuid"}}
  },
  "teams": {
    "count": 9,
    "item": {
      "departmentId": {"ref": "departments[*].id", "sequential": true}
    }
  }
}
```

## Available Generators

### No Options Required
- `uuid` - Generate UUIDs
- `name` - Generate full names (use `name.firstName`, `name.lastName` for parts)
- `internet` - Generate email, username, etc. (use `internet.emailAddress`, `internet.userName`)
- `address` - Generate addresses (use `address.city`, `address.country`)
- `company` - Generate company names
- `country` - Generate country names
- `book` - Generate book titles
- `finance` - Generate credit card numbers, etc.

### With Options
- `phone` - `format` (default, international, cell, extension)
- `number` - `min`, `max`
- `float` - `min`, `max`, `decimals`
- `boolean` - `probability` (0.0-1.0)
- `string` - `length` or `minLength`/`maxLength`, `allowedChars`, `regex`
- `date` - `from`, `to` (ISO format), `format`
- `lorem` - `words`, `sentences`, `paragraphs`
- `sequence` - `start`, `increment`
- `choice` - `options` (required), `weights` (optional)
- `csv` - `file` (required), `sequential`

## Error Handling

- Use `validate_dsl` to catch DSL errors early
- Check collection existence with `read_dsl_file` before updating/removing
- Generated output always writes to filesystem - provide valid absolute paths
- All DSL operations work with files on disk

## Best Practices

1. **Start with documentation** - Always read docs before creating DSL
2. **Validate early** - Use `validate_dsl` and `preview_generation` frequently
3. **Use tools for all DSL operations** - Never modify DSL files directly
4. **Use absolute paths** - Always provide absolute paths for files
5. **Generate to filesystem** - Always provide output paths for generation
6. **Use sequential references** - For even distribution, use `"sequential": true`
7. **Use seeds for reproducibility** - Add `"seed": 12345` to collections
8. **Test with small counts** - Use `preview_generation` or small counts first