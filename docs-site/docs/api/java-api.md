# Java API Reference

Complete reference for the DataGeneration Java API.

## DslDataGenerator

The main entry point for creating data generators.

### Builder Methods

```java
DslDataGenerator.Builder builder = DslDataGenerator.create();
```

#### Configuration Methods

- `.withSeed(long seed)` - Set seed for reproducible generation
- `.withMemoryOptimization()` - Enable lazy generation mode
- `.registerGenerator(String name, Generator generator)` - Add custom generator
- `.withFilteringBehavior(FilteringBehavior behavior)` - Configure filtering behavior

#### Input Methods

- `.fromJsonString(String json)` - Load DSL from JSON string
- `.fromJsonFile(String path)` - Load DSL from file
- `.fromJsonFile(Path path)` - Load DSL from Path

#### Generation

- `.generate()` - Generate data and return Generation object

### Example

```java
Generation generation = DslDataGenerator.create()
    .withSeed(123L)
    .withMemoryOptimization()
    .fromJsonFile("dsl.json")
    .generate();
```

## Generation

Interface for accessing generated data.

### Collection Access Methods

- `boolean hasCollection(String name)` - Check if collection exists
- `int getCollectionSize(String name)` - Get collection item count
- `Set<String> getCollectionNames()` - Get all collection names

### Streaming Methods

- `Stream<JsonNode> streamJsonNodes(String name)` - Stream collection as JsonNode
- `Map<String, Stream<JsonNode>> asJsonNodes()` - All collections as JsonNode streams
- `Stream<String> streamSqlInserts(String name)` - Stream SQL INSERT statements
- `Map<String, Stream<String>> asSqlInserts()` - All collections as SQL streams

### SQL Projection Methods

- `Stream<String> streamSqlInserts(String name, SqlProjection projection)` - Stream with field projection
- `Stream<String> streamSqlInsertsFromSchema(String name, String createTableSql)` - Stream with auto-parsed schema

### Example

```java
Generation result = DslDataGenerator.create()
    .withSeed(123L)
    .fromJsonString(dsl)
    .generate();

// Collection metadata
boolean hasUsers = result.hasCollection("users");
int userCount = result.getCollectionSize("users");
Set<String> names = result.getCollectionNames();

// Stream as JsonNode
result.streamJsonNodes("users").forEach(user -> {
    System.out.println(user.get("name").asText());
});

// Stream SQL inserts
result.streamSqlInserts("users").forEach(sql -> {
    database.execute(sql);
});

// All collections as streams
Map<String, Stream<JsonNode>> allData = result.asJsonNodes();
Map<String, Stream<String>> allSql = result.asSqlInserts();
```

## SQL Projections

Control which fields appear in SQL output and how they're formatted.

### Field Projection

Select specific fields to include:

```java
SqlProjection projection = SqlProjection.builder()
    .includeFields(Set.of("id", "name", "email"))  // Only these fields
    .build();

result.streamSqlInserts("users", projection).forEach(System.out::println);
```

### Type Specifications

Specify SQL data types for proper formatting:

```java
SqlProjection projection = SqlProjection.builder()
    .withFieldType("active", "TINYINT")      // Boolean as 0/1
    .withFieldType("created_at", "TIMESTAMP")
    .withFieldType("price", "DECIMAL(10,2)")
    .build();
```

### Schema-Based Generation

Parse CREATE TABLE scripts for automatic type mapping:

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
result.streamSqlInsertsFromSchema("users", createTableSql)
    .forEach(System.out::println);
```

## Custom Generators

Implement the `Generator` interface:

```java
public interface Generator {
    Object generate(GeneratorContext context);
}
```

### GeneratorContext

Provides access to options and utilities:

```java
public class MyGenerator implements Generator {
    @Override
    public Object generate(GeneratorContext context) {
        // Get options
        int min = context.getIntOption("min", 0);
        String format = context.getStringOption("format", "default");
        
        // Access Faker
        Faker faker = context.getFaker();
        
        // Your logic
        return faker.name().fullName();
    }
}
```

### Registration

```java
DslDataGenerator.create()
    .registerGenerator("myGenerator", new MyGenerator())
    .fromJsonString(dsl)
    .generate();
```

## Exceptions

- `DataGenerationException` - Base exception for generation errors
- `DslValidationException` - DSL validation errors
- `FilteringException` - All values filtered out
- `SerializationException` - JSON/SQL serialization errors

## Next Steps

- [Custom Generators Guide](../guides/how-to/custom-generators.md)
- [Memory Optimization](../guides/how-to/memory-optimization.md)
- [Generators Overview](../generators/overview.md)
