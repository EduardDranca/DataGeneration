# Memory Optimization Example

This example demonstrates the memory optimization feature for generating large datasets efficiently.

## Overview

Memory optimization is particularly useful when:
- Generating large datasets (thousands of records)
- Only some fields are referenced by other collections
- Memory usage is a concern during generation
- You need to stream data without loading everything into memory

## How It Works

The memory optimization feature implements lazy generation:

1. **Dependency Analysis**: Analyzes the DSL to identify which fields are referenced by other collections
2. **Selective Generation**: Only generates referenced fields initially (e.g., `id`, `name` if they're referenced)
3. **On-Demand Materialization**: Other fields are generated only when accessed during streaming or JSON export
4. **Memory Savings**: Can reduce memory usage by up to 90% for large datasets

## Usage

```java
Generation generation = DslDataGenerator.create()
    .withMemoryOptimization()  // Enable memory optimization
    .fromJsonString(dsl)
    .generate();
```

## Example DSL

```json
{
  "users": {
    "count": 10000,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.firstName"},
      "email": {"gen": "internet.emailAddress"},
      "bio": {"gen": "lorem", "options": {"length": 500}},
      "address": {"gen": "address.fullAddress"},
      "phone": {"gen": "phone.phoneNumber"},
      "company": {"gen": "company.name"}
    }
  },
  "posts": {
    "count": 5000,
    "item": {
      "id": {"gen": "uuid"},
      "title": {"gen": "lorem", "options": {"length": 50}},
      "content": {"gen": "lorem", "options": {"length": 1000}},
      "authorId": {"ref": "users[*].id"},      // References users.id
      "authorName": {"ref": "users[*].name"}   // References users.name
    }
  }
}
```

In this example:
- Only `users.id` and `users.name` are generated initially (because they're referenced)
- Fields like `email`, `bio`, `address`, `phone`, `company` are generated on-demand
- This results in significant memory savings during generation

## Key Benefits

- **Lazy Field Generation**: Only referenced fields are initially created
- **On-Demand Materialization**: Other fields generated when accessed
- **Streaming Support**: Process large datasets without loading everything in memory
- **API Compatibility**: Same API, just add `.withMemoryOptimization()`
- **Automatic Reference Detection**: Analyzes DSL to determine which fields to pre-generate

## Running the Example

```bash
cd examples/04-memory-optimization
javac -cp "../../target/classes:../../target/dependency/*" MemoryOptimizationExample.java
java -cp ".:../../target/classes:../../target/dependency/*" examples.MemoryOptimizationExample
```

## Performance Comparison

| Approach | Memory Usage | Generation Speed | Best For |
|----------|--------------|------------------|----------|
| Normal | High         | Fast | Small to medium datasets |
| Memory Optimized | Low          | Slightly slower | Large datasets, memory-constrained environments |

## Important: Consistency Behavior

**Lazy generation is not consistent like eager generation:**

- **Multiple streams**: Streaming the same collection twice will yield different results since data is generated on-demand
- **Order dependency**: Streaming collections in different orders can produce different results
- **Parallel processing**: Using parallel streams will produce inconsistent results

### Ensuring Consistency

If you need consistent results with memory optimization:

```java
// CONSISTENT: Generate all results sequentially in the same order
Generation result = DslDataGenerator.create()
    .withMemoryOptimization()
    .withSeed(123L)  // Always use a seed for reproducibility
    .fromJsonString(dsl)
    .generate();

// Process collections in a consistent order
result.getCollectionNames().stream()
    .sorted()  // Ensure consistent ordering
    .forEach(collectionName -> {
        result.streamSqlInserts(collectionName)
            .forEach(sql -> database.execute(sql));
    });

// INCONSISTENT: Multiple streams of the same collection
Stream<String> firstStream = result.streamSqlInserts("users");
Stream<String> secondStream = result.streamSqlInserts("users"); // Different results!

// INCONSISTENT: Parallel processing
result.streamJsonNodes("users")
    .parallel()  // Will produce inconsistent results
    .forEach(this::processUser);
```

### When to Use Each Approach

- **Use Eager Generation** when you need consistent, repeatable results or multiple access to the same data
- **Use Memory Optimization** when processing large datasets once in a streaming fashion where memory efficiency is more important than consistency

The memory optimization is most beneficial when generating large datasets where only a subset of fields are referenced by other collections.
