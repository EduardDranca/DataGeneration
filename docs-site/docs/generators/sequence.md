# Sequence Generator

Generates sequential (auto-incrementing) numbers.

## Basic Usage

```json
{
  "id": {"gen": "sequence"}
}
```

**Output:** 0, 1, 2, 3, 4, ... (starting from 0, incrementing by 1)

## Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `start` | integer | 0 | Starting value |
| `increment` | integer | 1 | Increment step |

## Examples

### Starting from 1

```json
{
  "id": {"gen": "sequence", "start": 1, "increment": 1}
}
```

**Output:** 1, 2, 3, 4, 5, ...

### Custom Increment

```json
{
  "id": {"gen": "sequence", "start": 100, "increment": 10}
}
```

**Output:** 100, 110, 120, 130, 140, ...

### Even Numbers

```json
{
  "evenNumber": {"gen": "sequence", "start": 0, "increment": 2}
}
```

**Output:** 0, 2, 4, 6, 8, ...

### Odd Numbers

```json
{
  "oddNumber": {"gen": "sequence", "start": 1, "increment": 2}
}
```

**Output:** 1, 3, 5, 7, 9, ...

## Independent Sequences

Each field using sequence has its own independent counter:

```json
{
  "users": {
    "count": 5,
    "item": {
      "userId": {"gen": "sequence", "start": 1, "increment": 1},
      "accountId": {"gen": "sequence", "start": 1000, "increment": 1}
    }
  }
}
```

**Output:**
```json
[
  {"userId": 1, "accountId": 1000},
  {"userId": 2, "accountId": 1001},
  {"userId": 3, "accountId": 1002},
  {"userId": 4, "accountId": 1003},
  {"userId": 5, "accountId": 1004}
]
```

## Common Patterns

### Primary Keys

```json
{
  "users": {
    "count": 100,
    "item": {
      "id": {"gen": "sequence", "start": 1, "increment": 1},
      "name": {"gen": "name.fullName"},
      "email": {"gen": "internet.emailAddress"}
    }
  }
}
```

### Order Numbers

```json
{
  "orders": {
    "count": 500,
    "item": {
      "orderNumber": {"gen": "sequence", "start": 10000, "increment": 1},
      "customerId": {"ref": "customers[*].id"},
      "total": {"gen": "float", "min": 10, "max": 1000, "decimals": 2}
    }
  }
}
```

### Invoice Numbers

```json
{
  "invoices": {
    "count": 200,
    "item": {
      "invoiceNumber": {"gen": "sequence", "start": 2024001, "increment": 1},
      "date": {"gen": "date"},
      "amount": {"gen": "float", "min": 100, "max": 10000, "decimals": 2}
    }
  }
}
```

### Version Numbers

```json
{
  "releases": {
    "count": 20,
    "item": {
      "version": {"gen": "sequence", "start": 1, "increment": 1},
      "name": {"gen": "lorem.word"},
      "releaseDate": {"gen": "date"}
    }
  }
}
```

### Time Series Data

```json
{
  "measurements": {
    "count": 24,
    "item": {
      "hour": {"gen": "sequence", "start": 0, "increment": 1},
      "temperature": {"gen": "float", "min": 15.0, "max": 30.0, "decimals": 1},
      "humidity": {"gen": "number", "min": 30, "max": 80}
    }
  }
}
```

### Pagination

```json
{
  "pages": {
    "count": 50,
    "item": {
      "pageNumber": {"gen": "sequence", "start": 1, "increment": 1},
      "content": {"gen": "lorem.paragraph"}
    }
  }
}
```

## With References

Sequence-generated IDs can be referenced:

```json
{
  "categories": {
    "count": 5,
    "item": {
      "id": {"gen": "sequence", "start": 1, "increment": 1},
      "name": {"gen": "lorem.word"}
    }
  },
  "products": {
    "count": 50,
    "item": {
      "id": {"gen": "sequence", "start": 1, "increment": 1},
      "categoryId": {"ref": "categories[*].id"},
      "name": {"gen": "lorem.word"}
    }
  }
}
```

## Comparison with UUID

| Feature | Sequence | UUID |
|---------|----------|------|
| **Format** | Integer | String (36 chars) |
| **Readability** | High | Low |
| **Sortability** | Natural order | Random |
| **Uniqueness** | Within collection | Globally unique |
| **Use Case** | Numeric IDs, counters | Distributed systems |

**Use Sequence when:**
- You need human-readable IDs
- You want natural ordering
- You're generating SQL for databases with auto-increment

**Use UUID when:**
- You need globally unique identifiers
- You're working with distributed systems
- You want to avoid ID collisions

## Best Practices

1. **Start from 1**: Most databases use 1-based IDs
2. **Use for Counters**: Perfect for sequential counters
3. **Independent Sequences**: Each field gets its own sequence
4. **Combine with References**: Use sequence IDs in relationships
5. **Consider UUID**: Use UUID for distributed systems

## Limitations

- Sequences are per-field, not per-collection
- Cannot share sequence state between fields
- Resets on each generation run (unless using same seed)

## Next Steps

- [UUID Generator](./uuid.md) - Alternative for unique IDs
- [Number Generator](./number.md) - For random integers
- [References](../dsl-reference/references.md) - Use sequence IDs in relationships
