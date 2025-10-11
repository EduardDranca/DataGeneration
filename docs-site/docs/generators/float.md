# Float Generator

Generates random floating-point (decimal) numbers within a specified range.

## Basic Usage

```json
{
  "price": {"gen": "float"}
}
```

Without options, generates any float value.

## Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `min` | number | `Integer.MIN_VALUE` | Minimum value (inclusive) |
| `max` | number | `Integer.MAX_VALUE` | Maximum value (inclusive) |
| `decimals` | integer | 2 | Number of decimal places (max: 10) |

## Examples

### Price

```json
{
  "price": {"gen": "float", "min": 9.99, "max": 999.99, "decimals": 2}
}
```

**Output:** Random float between 9.99 and 999.99 with 2 decimal places (e.g., `49.99`)

### Percentage

```json
{
  "percentage": {"gen": "float", "min": 0, "max": 100, "decimals": 1}
}
```

**Output:** Random float between 0.0 and 100.0 with 1 decimal place (e.g., `73.5`)

### Temperature

```json
{
  "temperature": {"gen": "float", "min": -20.0, "max": 40.0, "decimals": 1}
}
```

### Rating

```json
{
  "rating": {"gen": "float", "min": 1.0, "max": 5.0, "decimals": 1}
}
```

**Output:** Random rating like `4.2`, `3.7`, etc.

### High Precision

```json
{
  "coordinate": {"gen": "float", "min": -180, "max": 180, "decimals": 6}
}
```

**Output:** Precise coordinates like `123.456789`

## Common Patterns

### Product Pricing

```json
{
  "products": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "price": {"gen": "float", "min": 9.99, "max": 999.99, "decimals": 2},
      "discount": {"gen": "float", "min": 0, "max": 50, "decimals": 0}
    }
  }
}
```

### Financial Data

```json
{
  "transactions": {
    "count": 500,
    "item": {
      "id": {"gen": "uuid"},
      "amount": {"gen": "float", "min": 10.00, "max": 10000.00, "decimals": 2},
      "fee": {"gen": "float", "min": 0.50, "max": 50.00, "decimals": 2},
      "taxRate": {"gen": "float", "min": 0.05, "max": 0.15, "decimals": 3}
    }
  }
}
```

### Measurements

```json
{
  "measurements": {
    "count": 1000,
    "item": {
      "id": {"gen": "uuid"},
      "weight": {"gen": "float", "min": 0.1, "max": 100.0, "decimals": 2},
      "height": {"gen": "float", "min": 0.5, "max": 2.5, "decimals": 2},
      "temperature": {"gen": "float", "min": 15.0, "max": 30.0, "decimals": 1}
    }
  }
}
```

### Ratings and Scores

```json
{
  "reviews": {
    "count": 200,
    "item": {
      "id": {"gen": "uuid"},
      "rating": {"gen": "float", "min": 1.0, "max": 5.0, "decimals": 1},
      "score": {"gen": "float", "min": 0, "max": 100, "decimals": 1}
    }
  }
}
```

### Geographic Coordinates

```json
{
  "locations": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "latitude": {"gen": "float", "min": -90, "max": 90, "decimals": 6},
      "longitude": {"gen": "float", "min": -180, "max": 180, "decimals": 6}
    }
  }
}
```

## Best Practices

1. **Always Specify Range**: Use `min` and `max` for predictable data
2. **Appropriate Decimals**: Use 2 decimals for currency, 1 for percentages, 6 for coordinates
3. **Realistic Ranges**: Choose ranges that match real-world scenarios
4. **Currency Format**: Use 2 decimals for monetary values
5. **Max Decimals**: Maximum of 10 decimal places supported

## Comparison with Number

| Generator | Output Type | Use Case |
|-----------|-------------|----------|
| **Float** | Decimal numbers | Prices, measurements, ratings |
| [Number](./number.md) | Integers | Counts, IDs, quantities |

## Next Steps

- [Number Generator](./number.md) - For integer values
- [String Generator](./string.md) - For text values
- [Date Generator](./date.md) - For dates and timestamps
