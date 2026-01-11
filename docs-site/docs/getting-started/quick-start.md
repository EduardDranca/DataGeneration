# Quick Start

Get up and running with DataGeneration in 5 minutes.

## Step 1: Create Your First DSL

Create a JSON file defining your data structure:

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "firstName": {"gen": "name.firstName"},
      "lastName": {"gen": "name.lastName"},
      "email": {"gen": "internet.emailAddress"},
      "age": {"gen": "number", "min": 18, "max": 65}
    }
  }
}
```

## Step 2: Generate Data

```java
import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.Generation;
import com.fasterxml.jackson.databind.JsonNode;

public class QuickStart {
    public static void main(String[] args) throws Exception {
        String dsl = // ... your JSON from above

        Generation generation = DslDataGenerator.create()
            .withSeed(123L)  // Optional: for reproducible data
            .fromJsonString(dsl)
            .generate();

        // Stream as JsonNode
        generation.streamJsonNodes("users").forEach(user -> {
            System.out.println(user.get("firstName").asText());
        });

        // Check collection size
        int userCount = generation.getCollectionSize("users");
        System.out.println("Generated " + userCount + " users");
    }
}
```

## Step 3: Add Relationships

Let's add orders that reference users:

```json
{
  "users": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "email": {"gen": "internet.emailAddress"}
    }
  },
  "orders": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "userId": {"ref": "users[*].id"},
      "product": {"gen": "lorem.word"},
      "total": {"gen": "float", "min": 10, "max": 500, "decimals": 2},
      "status": {
        "gen": "choice",
        "options": ["pending", "shipped", "delivered"],
        "weights": [30, 50, 20]
      }
    }
  }
}
```

The `{"ref": "users[*].id"}` syntax creates a foreign key relationship - each order will reference a random user's ID.

## Step 4: Export as SQL

```java
Generation generation = DslDataGenerator.create()
    .fromJsonString(dsl)
    .generate();

// Stream SQL INSERT statements for each collection
generation.streamSqlInserts("users").forEach(System.out::println);
generation.streamSqlInserts("orders").forEach(System.out::println);

// Or get all collections as SQL streams
Map<String, Stream<String>> allSql = generation.asSqlInserts();
allSql.forEach((table, sqlStream) -> {
    sqlStream.forEach(System.out::println);
});
```

Output:
```sql
INSERT INTO users (id, name, email) VALUES ('550e8400-...', 'John Doe', 'john@example.com');
INSERT INTO users (id, name, email) VALUES ('6ba7b810-...', 'Jane Smith', 'jane@example.com');
...
INSERT INTO orders (id, userId, product, total, status) VALUES ('7c9e6679-...', '550e8400-...', 'widget', 125.50, 'shipped');
...
```

## Common Patterns

### Static Values

Mix generated and static values:

```json
{
  "users": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "role": "user",
      "active": true
    }
  }
}
```

### Arrays

Generate arrays of values:

```json
{
  "products": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "tags": {
        "array": {
          "size": 3,
          "item": {"gen": "lorem.word"}
        }
      }
    }
  }
}
```

### Sequential References

Distribute references evenly:

```json
{
  "teams": {
    "count": 3,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "company.name"}
    }
  },
  "employees": {
    "count": 30,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "teamId": {
        "ref": "teams[*].id",
        "sequential": true
      }
    }
  }
}
```

This ensures each team gets exactly 10 employees (30 / 3).

## Memory Optimization

For large datasets, use lazy generation:

```java
Generation generation = DslDataGenerator.create()
    .withMemoryOptimization()  // Enable lazy mode
    .fromJsonString(dsl)
    .generate();

// Stream collections one at a time
generation.streamJsonNodes("users").forEach(user -> {
    // Process each user as JsonNode
    System.out.println(user.get("name").asText());
});
```

## Next Steps

- [Core Concepts](./core-concepts.md) - Understand collections, references, and generators
- [DSL Reference](../dsl-reference/overview.md) - Complete DSL syntax guide
- [Generators](../generators/overview.md) - Explore all 18 built-in generators
- [Common Patterns](../guides/overview.md) - Learn best practices and patterns
