# CSV Generator

Reads data from CSV files for data generation.

## Basic Usage

```json
{
  "data": {"gen": "csv", "file": "data/users.csv"}
}
```

**Output:** Returns an object with CSV column headers as keys

## Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `file` | string | - | Path to CSV file (required) |
| `sequential` | boolean | true | Read sequentially or randomly |

## CSV Format

- First row must contain headers
- Each row becomes an object with header names as keys
- All values are returned as strings

## Examples

### Basic CSV Reading

**CSV file (data/users.csv):**
```csv
name,email,age
John Doe,john@example.com,30
Jane Smith,jane@example.com,25
Bob Johnson,bob@example.com,35
```

**DSL:**
```json
{
  "users": {
    "count": 3,
    "item": {
      "id": {"gen": "uuid"},
      "userData": {"gen": "csv", "file": "data/users.csv"}
    }
  }
}
```

**Output:**
```json
{
  "users": [
    {
      "id": "550e8400-...",
      "userData": {
        "name": "John Doe",
        "email": "john@example.com",
        "age": "30"
      }
    }
  ]
}
```

### Sequential Reading

```json
{
  "records": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "data": {"gen": "csv", "file": "data/products.csv", "sequential": true}
    }
  }
}
```

Reads CSV rows in order: row 1, row 2, row 3, etc.

### Random Reading

```json
{
  "records": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "data": {"gen": "csv", "file": "data/products.csv", "sequential": false}
    }
  }
}
```

Reads random rows from the CSV file.

## Common Patterns

### Product Catalog

**CSV file (data/products.csv):**
```csv
sku,name,category,price
PROD001,Widget,Electronics,29.99
PROD002,Gadget,Electronics,49.99
PROD003,Tool,Hardware,19.99
```

**DSL:**
```json
{
  "products": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "productData": {"gen": "csv", "file": "data/products.csv"},
      "stock": {"gen": "number", "min": 0, "max": 100}
    }
  }
}
```

### User Seed Data

**CSV file (data/seed-users.csv):**
```csv
firstName,lastName,department
John,Doe,Engineering
Jane,Smith,Marketing
Bob,Johnson,Sales
```

**DSL:**
```json
{
  "users": {
    "count": 3,
    "item": {
      "id": {"gen": "uuid"},
      "userData": {"gen": "csv", "file": "data/seed-users.csv", "sequential": true},
      "email": {"gen": "internet.emailAddress"},
      "createdAt": {"gen": "date"}
    }
  }
}
```

### Test Data Import

```json
{
  "testCases": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "testData": {"gen": "csv", "file": "data/test-inputs.csv", "sequential": true},
      "executedAt": {"gen": "date", "format": "iso_datetime"}
    }
  }
}
```

## Best Practices

1. **Use Relative Paths**: Paths are relative to where you run the generator
2. **Sequential for Ordered Data**: Use sequential for test data that must be in order
3. **Random for Variety**: Use random for more varied datasets
4. **All Strings**: Remember CSV values are strings - convert if needed
5. **Cache Friendly**: CSV files are cached automatically

## Limitations

- All CSV values are returned as strings
- CSV file must exist and be readable
- First row must be headers
- Large CSV files are loaded into memory

## Next Steps

- [Choice Generator](./choice.md) - For selecting from options
- [Sequence Generator](./sequence.md) - For sequential IDs
