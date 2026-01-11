# Core Concepts

Understanding the fundamental concepts of DataGeneration.

## Collections

A **collection** is a group of generated items. Each collection has:
- A name (the key in your JSON)
- A count (how many items to generate)
- An item definition (the structure of each item)

```json
{
  "users": {           // Collection name
    "count": 10,       // Generate 10 items
    "item": {          // Item structure
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"}
    }
  }
}
```

## Generators

**Generators** create data values. DataGeneration includes 18 built-in generators:

- **Data**: UUID, Name, Internet, Address, Company, Country, Book, Finance, Phone
- **Primitives**: Number, Float, Boolean, String, Date
- **Utilities**: Lorem, Sequence, Choice, CSV

Each generator has its own options. For example:

```json
{
  "age": {"gen": "number", "min": 18, "max": 65},
  "price": {"gen": "float", "min": 9.99, "max": 999.99, "decimals": 2},
  "status": {"gen": "choice", "options": ["active", "inactive"]}
}
```

## References

**References** create relationships between collections:

```json
{
  "users": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"}
    }
  },
  "orders": {
    "count": 20,
    "item": {
      "userId": {"ref": "users[*].id"}  // Random user reference
    }
  }
}
```

### Reference Types

- `users[*].id` - Random item from collection
- `users[0].id` - Specific item by index
- `users[*].id` with `"sequential": true` - Cycle through items
- `this.fieldName` - Reference another field in the same item

## Static Values

Any field without `"gen"` is treated as a static value:

```json
{
  "users": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "role": "user",           // Static string
      "active": true,           // Static boolean
      "metadata": {             // Static object
        "version": 1,
        "type": "standard"
      }
    }
  }
}
```

## Arrays

Generate arrays of values:

```json
{
  "tags": {
    "array": {
      "size": 5,                    // Fixed size
      "item": {"gen": "lorem.word"}
    }
  },
  "scores": {
    "array": {
      "minSize": 2,                 // Variable size
      "maxSize": 10,
      "item": {"gen": "number", "min": 0, "max": 100}
    }
  }
}
```

## Filtering

Exclude specific values from references:

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "status": {"gen": "choice", "options": ["active", "banned"]}
    },
    "pick": {
      "bannedUser": 5  // Name the 6th user (index 5)
    }
  },
  "orders": {
    "count": 50,
    "item": {
      "userId": {
        "ref": "users[*].id",
        "filter": [{"ref": "bannedUser.id"}]  // Exclude banned user
      }
    }
  }
}
```

## Generation Modes

### Eager Mode (Default)

All data is generated upfront and stored in memory:

```java
Generation generation = DslDataGenerator.create()
    .fromJsonString(dsl)
    .generate();

// Stream as JsonNode
generation.streamJsonNodes("users").forEach(user -> {
    System.out.println(user.get("name").asText());
});
```

**Pros**: Fast access, can call streaming methods multiple times
**Cons**: High memory usage for large datasets

### Lazy Mode

Data is generated on-demand:

```java
Generation generation = DslDataGenerator.create()
    .withMemoryOptimization()
    .fromJsonString(dsl)
    .generate();

generation.streamJsonNodes("users").forEach(user -> {
    // Process user as JsonNode
    System.out.println(user.get("name").asText());
});
```

**Pros**: Low memory usage, handles huge datasets
**Cons**: Streaming same collection multiple times yields different results

## Seeds

Use seeds for reproducible data:

```java
Generation gen1 = DslDataGenerator.create()
    .withSeed(123L)
    .fromJsonString(dsl)
    .generate();

Generation gen2 = DslDataGenerator.create()
    .withSeed(123L)
    .fromJsonString(dsl)
    .generate();

// gen1 and gen2 contain identical data
```

Perfect for:
- Unit tests that need consistent data
- Debugging with reproducible scenarios
- Demos that should look the same every time

## Next Steps

- [DSL Reference](../dsl-reference/overview.md) - Complete syntax guide
- [Generators](../generators/overview.md) - Explore all generators
- [Common Patterns](../guides/overview.md) - Learn best practices
