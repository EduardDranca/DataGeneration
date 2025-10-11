# Memory Optimization

Handle large datasets efficiently with lazy generation mode.

## When to Use Memory Optimization

Enable memory optimization when:
- Generating more than 10,000 items
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
generation.streamCollection("users").forEach(user -> {
    // Process each user
    System.out.println(user);
    // Or save to database, write to file, etc.
});
```

## Eager vs Lazy Mode

### Eager Mode (Default)

```java
Generation generation = DslDataGenerator.create()
    .fromJsonString(dsl)
    .generate();

// All data generated upfront
List<Map<String, Object>> users = generation.getCollection("users");

// Can iterate multiple times
users.forEach(user -> process(user));
users.forEach(user -> validate(user));
```

**Pros:**
- Fast random access
- Can iterate multiple times
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
generation.streamCollection("users").forEach(user -> {
    process(user);
});
```

**Pros:**
- Low memory usage
- Handles huge datasets
- Efficient streaming

**Cons:**
- Can only stream once
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
List<Map<String, Object>> batch = new ArrayList<>();
generation.streamCollection("users").forEach(user -> {
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
    
    generation.streamCollection("users").forEach(user -> {
        try {
            stmt.setString(1, (String) user.get("id"));
            stmt.setString(2, (String) user.get("name"));
            stmt.setString(3, (String) user.get("email"));
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
    generation.streamCollection("users").forEach(user -> {
        try {
            if (!first.get()) {
                writer.write(",\n");
            }
            first.set(false);
            
            String json = new ObjectMapper().writeValueAsString(user);
            writer.write("  " + json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    });
    
    writer.write("\n]");
}
```

## Best Practices

1. **Use for Large Datasets**: Enable for > 10,000 items
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

- [Streaming Guide](./streaming.md) - Advanced streaming patterns
- [Performance Tuning](./performance-tuning.md) - Optimize generation speed
