# Memory Optimization

Handle large datasets efficiently with lazy generation mode.

## When to Use Memory Optimization

Enable memory optimization when:
- Generating a large number of items (e.g. > 100,000)
- Working with limited memory
- Streaming data to external systems
- Processing data in batches

## Enabling Memory Optimization

```java
Generation generation = DslDataGenerator.create()
    .withMemoryOptimization()  // Enable lazy mode
    .withSeed(123L)
    .fromJsonString(dsl)
    .generate();
```

## Streaming Collections

Process collections one item at a time:

```java
generation.streamJsonNodes("users").forEach(user -> {
    // Process each user as JsonNode
    System.out.println(user.get("name").asText());
    // Or save to database, write to file, etc.
});
```

## Eager vs Lazy Mode

### Eager Mode (Default)

```java
Generation generation = DslDataGenerator.create()
    .fromJsonString(dsl)
    .generate();

// Stream data (can be consumed once per call)
generation.streamJsonNodes("users").forEach(user -> process(user));

// Or get all streams at once
Map<String, Stream<JsonNode>> allData = generation.asJsonNodes();
```

**Pros:**
- Fast random access
- Can call streaming methods multiple times
- Simpler to use

**Cons:**
- High memory usage
- Slow for large datasets
- All data in memory

### Lazy Mode

```java
Generation generation = DslDataGenerator.create()
    .withMemoryOptimization()
    .fromJsonString(dsl)
    .generate();

// Data generated on-demand
generation.streamJsonNodes("users").forEach(user -> {
    process(user);
});
```

**Pros:**
- Low memory usage
- Handles huge datasets
- Efficient streaming

**Cons:**
- Streaming same collection multiple times yields different results
- No random access
- Slightly slower per-item

## Example: Large Dataset

```java
String dsl = """
    {
      "users": {
        "count": 1000000,
        "item": {
          "id": {"gen": "uuid"},
          "name": {"gen": "name.fullName"},
          "email": {"gen": "internet.emailAddress"}
        }
      }
    }
    """;

Generation generation = DslDataGenerator.create()
    .withMemoryOptimization()
    .fromJsonString(dsl)
    .generate();

// Process in batches
List<JsonNode> batch = new ArrayList<>();
generation.streamJsonNodes("users").forEach(user -> {
    batch.add(user);
    
    if (batch.size() >= 1000) {
        saveBatch(batch);
        batch.clear();
    }
});

// Save remaining
if (!batch.isEmpty()) {
    saveBatch(batch);
}
```

## Streaming to Database

```java
Generation generation = DslDataGenerator.create()
    .withMemoryOptimization()
    .fromJsonString(dsl)
    .generate();

try (Connection conn = dataSource.getConnection()) {
    String sql = "INSERT INTO users (id, name, email) VALUES (?, ?, ?)";
    PreparedStatement stmt = conn.prepareStatement(sql);
    
    generation.streamJsonNodes("users").forEach(user -> {
        try {
            stmt.setString(1, user.get("id").asText());
            stmt.setString(2, user.get("name").asText());
            stmt.setString(3, user.get("email").asText());
            stmt.addBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    });
    
    stmt.executeBatch();
}
```

## Streaming to File

```java
Generation generation = DslDataGenerator.create()
    .withMemoryOptimization()
    .fromJsonString(dsl)
    .generate();

try (BufferedWriter writer = Files.newBufferedWriter(Path.of("output.json"))) {
    writer.write("[\n");
    
    AtomicBoolean first = new AtomicBoolean(true);
    generation.streamJsonNodes("users").forEach(user -> {
        try {
            if (!first.get()) {
                writer.write(",\n");
            }
            first.set(false);
            
            writer.write("  " + user.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    });
    
    writer.write("\n]");
}
```

## Best Practices

1. **Use for Large Datasets**: Enable for > 100,000 items
2. **Stream Once**: Remember you can only stream each collection once
3. **Batch Processing**: Process in batches for efficiency
4. **Resource Management**: Use try-with-resources for streams
5. **Error Handling**: Handle errors gracefully during streaming

## Limitations

- Cannot iterate collections multiple times
- No random access to items
- References may behave differently
- Some operations require eager mode

## Next Steps

- [Java API](../../api/java-api.md) - Full API documentation
- [Custom Generators](./custom-generators.md) - Create your own generators
- [DSL Reference](../../dsl-reference/overview.md) - Complete DSL syntax
