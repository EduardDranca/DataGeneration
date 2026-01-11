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
- `.withCustomGenerator(String name, Generator generator)` - Add custom generator
- `.withFilteringBehavior(FilteringBehavior behavior)` - Configure filtering behavior
- `.withMaxFilteringRetries(int maxRetries)` - Set max retries when filtering

#### Input Methods

- `.fromJsonString(String json)` - Load DSL from JSON string
- `.fromFile(String path)` - Load DSL from file path
- `.fromFile(File file)` - Load DSL from File
- `.fromFile(Path path)` - Load DSL from Path
- `.fromJsonNode(JsonNode node)` - Load DSL from JsonNode

#### Generation

- `.generate()` - Generate data and return Generation object

### Example

```java
Generation generation = DslDataGenerator.create()
    .withSeed(123L)
    .withMemoryOptimization()
    .fromFile("dsl.json")
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

- `Stream<String> streamSqlInsertsWithProjection(String name, SqlProjection projection)` - Stream with field projection
- `Map<String, Stream<String>> asSqlInsertsWithProjections(Map<String, SqlProjection> projections)` - All collections with projections

### Builder Convenience Methods

The builder also provides convenience methods that combine generation and output:

- `.generateAsSql()` - Generate and return SQL streams for all collections
- `.generateAsJson()` - Generate and return JsonNode streams for all collections
- `.streamSqlInsertsFromSchema(String name, String createTableSql)` - Generate with auto-parsed schema

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

result.streamSqlInsertsWithProjection("users", projection).forEach(System.out::println);
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

// Use the builder's convenience method for schema-based generation
DslDataGenerator.create()
    .withSeed(123L)
    .fromJsonString(dsl)
    .streamSqlInsertsFromSchema("users", createTableSql)
    .forEach(System.out::println);
```

## Custom Generators

Implement the `Generator` interface:

```java
public interface Generator {
    JsonNode generate(GeneratorContext context);
}
```

### GeneratorContext

Provides access to options and utilities (it's a Java record):

```java
public class MyGenerator implements Generator {
    @Override
    public JsonNode generate(GeneratorContext context) {
        // Get options (no default parameter - check for null)
        String format = context.getStringOption("format");
        int min = context.getIntOption("min", 0);  // Has default overload
        
        // Access Faker (record accessor, not getter)
        Faker faker = context.faker();
        
        // Access ObjectMapper for creating JsonNodes
        ObjectMapper mapper = context.mapper();
        
        return mapper.valueToTree(faker.name().fullName());
    }
}
```

### Registration

```java
DslDataGenerator.create()
    .withCustomGenerator("myGenerator", new MyGenerator())
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
