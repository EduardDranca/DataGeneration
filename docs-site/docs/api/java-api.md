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

### Methods

- `List<Map<String, Object>> getCollection(String name)` - Get collection (eager mode)
- `Stream<Map<String, Object>> streamCollection(String name)` - Stream collection
- `String toJson()` - Export as JSON
- `String toJson(boolean pretty)` - Export as formatted JSON
- `String toSqlInserts()` - Export as SQL INSERT statements

### Example

```java
// Access collections
List<Map<String, Object>> users = generation.getCollection("users");

// Stream for memory efficiency
generation.streamCollection("orders").forEach(order -> {
    System.out.println(order);
});

// Export
String json = generation.toJson(true);
String sql = generation.toSqlInserts();
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
