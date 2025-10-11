# UUID Generator

Generates UUID v4 (Universally Unique Identifier) strings.

## Basic Usage

```json
{
  "id": {"gen": "uuid"}
}
```

**Output:** `"550e8400-e29b-41d4-a716-446655440000"`

## Options

This generator has **no options**.

## Characteristics

- **Format**: UUID v4 (random)
- **Deterministic**: Yes, when using a seed
- **Uniqueness**: Extremely high probability of uniqueness
- **Length**: Always 36 characters (including hyphens)

## Examples

### Basic ID Field

```json
{
  "users": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"}
    }
  }
}
```

### Multiple UUID Fields

```json
{
  "orders": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "transactionId": {"gen": "uuid"},
      "sessionId": {"gen": "uuid"}
    }
  }
}
```

### With References

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
      "id": {"gen": "uuid"},
      "userId": {"ref": "users[*].id"}
    }
  }
}
```

## Reproducibility

When using a seed, UUIDs are deterministic:

```java
Generation gen1 = DslDataGenerator.create()
    .withSeed(123L)
    .fromJsonString(dsl)
    .generate();

Generation gen2 = DslDataGenerator.create()
    .withSeed(123L)
    .fromJsonString(dsl)
    .generate();

// Both generations produce identical UUIDs
```

## Common Use Cases

- **Primary Keys**: Database primary keys
- **Transaction IDs**: Unique transaction identifiers
- **Session IDs**: User session tracking
- **Request IDs**: API request tracking
- **Correlation IDs**: Distributed system tracing

## Best Practices

1. **Use for IDs**: Perfect for primary and foreign keys
2. **Seed for Tests**: Use seeds in tests for reproducible IDs
3. **Reference Pattern**: Generate UUIDs in parent collections, reference in children
4. **Avoid Duplication**: Don't worry about collisions - UUID v4 is designed for uniqueness

## SQL Output

```java
String sql = generation.toSqlInserts();
```

**Output:**
```sql
INSERT INTO users (id, name) VALUES ('550e8400-e29b-41d4-a716-446655440000', 'John Doe');
```

## Next Steps

- [References](../dsl-reference/references.md) - Use UUIDs in relationships
- [Sequence Generator](./sequence.md) - Alternative for numeric IDs
