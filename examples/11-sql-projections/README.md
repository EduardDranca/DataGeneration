# SQL Projections and Schema-Based Generation

This example demonstrates the three SQL generation features:

1. **Field Projections** - Select which fields to include in SQL INSERTs
2. **Data Type Specifications** - Specify SQL data types for proper formatting
3. **Automatic Schema Parsing** - Parse CREATE TABLE scripts and auto-map types

## Use Cases

### 1. Excluding Helper Fields

When generating data, you might use temporary fields for references or calculations that shouldn't appear in the final SQL:

```java
// DSL with helper field
{
  "products": {
    "count": 10,
    "item": {
      "id": {"gen": "sequence"},
      "name": {"gen": "company.name"},
      "category_id": {"ref": "categories[*].id"},
      "_tempCategoryName": {"ref": "categories[*].name"}  // Helper field
    }
  }
}

// Exclude helper field from SQL
SqlProjection projection = SqlProjection.builder()
    .includeFields(Set.of("id", "name", "category_id"))
    .build();
```

### 2. Type-Aware Formatting

Different databases have different boolean representations. Specify types to ensure correct formatting:

```java
SqlProjection projection = SqlProjection.builder()
    .withFieldType("active", "TINYINT")  // Formats boolean as 0/1
    .withFieldType("created_at", "TIMESTAMP")
    .withFieldType("price", "DECIMAL(10,2)")
    .build();
```

### 3. Automatic Schema Parsing

The most powerful feature - provide CREATE TABLE scripts and the library handles everything:

```java
String createTableSql = """
    CREATE TABLE users (
        id BIGINT PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        email VARCHAR(255),
        active TINYINT,
        created_at TIMESTAMP
    )
    """;

// Automatically extracts column types and formats values correctly
Stream<String> inserts = DslDataGenerator.create()
    .withSeed(12345L)
    .fromJsonString(dsl)
    .streamSqlInsertsFromSchema("users", createTableSql);
```

## Running the Example

```bash
mvn exec:java -Dexec.mainClass="com.github.eddranca.datagenerator.examples.SqlProjectionsExample"
```

## Key Benefits

- **Cleaner DSL**: Use helper fields without polluting SQL output
- **Database Compatibility**: Format values correctly for your database
- **Less Boilerplate**: Parse schemas once, generate correctly formatted SQL automatically
- **Type Safety**: Ensure booleans, dates, and numbers are formatted correctly
