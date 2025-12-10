# Number Generator

Generates random integer values within a specified range.

## Basic Usage

```json
{
  "value": {"gen": "number"}
}
```

Without options, generates any integer value.

## Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `min` | integer | `Integer.MIN_VALUE` | Minimum value (inclusive) |
| `max` | integer | `Integer.MAX_VALUE` | Maximum value (inclusive) |

## Examples

### With Range

```json
{
  "age": {"gen": "number", "min": 18, "max": 65}
}
```

**Output:** Random integer between 18 and 65 (inclusive)

### Quantity

```json
{
  "quantity": {"gen": "number", "min": 1, "max": 100}
}
```

### Score

```json
{
  "score": {"gen": "number", "min": 0, "max": 100}
}
```

### Year

```json
{
  "year": {"gen": "number", "min": 2020, "max": 2024}
}
```

### Negative Numbers

```json
{
  "temperature": {"gen": "number", "min": -20, "max": 40}
}
```

## Common Patterns

### Product Inventory

```json
{
  "products": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "stock": {"gen": "number", "min": 0, "max": 1000},
      "reorderPoint": {"gen": "number", "min": 10, "max": 50}
    }
  }
}
```

### User Demographics

```json
{
  "users": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "age": {"gen": "number", "min": 18, "max": 80},
      "yearsExperience": {"gen": "number", "min": 0, "max": 40}
    }
  }
}
```

### Order Quantities

```json
{
  "orders": {
    "count": 200,
    "item": {
      "id": {"gen": "uuid"},
      "quantity": {"gen": "number", "min": 1, "max": 10},
      "priority": {"gen": "number", "min": 1, "max": 5}
    }
  }
}
```

### Ratings and Scores

```json
{
  "reviews": {
    "count": 500,
    "item": {
      "id": {"gen": "uuid"},
      "rating": {"gen": "number", "min": 1, "max": 5},
      "helpfulVotes": {"gen": "number", "min": 0, "max": 100}
    }
  }
}
```

### Game Statistics

```json
{
  "players": {
    "count": 1000,
    "item": {
      "id": {"gen": "uuid"},
      "level": {"gen": "number", "min": 1, "max": 100},
      "health": {"gen": "number", "min": 50, "max": 100},
      "mana": {"gen": "number", "min": 0, "max": 100},
      "experience": {"gen": "number", "min": 0, "max": 999999}
    }
  }
}
```

## Best Practices

1. **Always Specify Range**: Use `min` and `max` for predictable data
2. **Realistic Ranges**: Choose ranges that match real-world scenarios
3. **Consider Zero**: Decide if zero should be included in your range
4. **Use Float for Decimals**: Use Float generator for decimal values

## Comparison with Other Generators

| Generator | Use Case | Output Type |
|-----------|----------|-------------|
| **Number** | Integer values | `int` |
| [Float](./float.md) | Decimal values | `double` |
| [Sequence](./sequence.md) | Auto-incrementing | `int` |
| [Boolean](./boolean.md) | True/false | `boolean` |

## Next Steps

- [Float Generator](./float.md) - For decimal values
- [Sequence Generator](./sequence.md) - For auto-incrementing IDs
- [Choice Generator](./choice.md) - For selecting from specific values
