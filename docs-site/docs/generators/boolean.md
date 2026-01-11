# Boolean Generator

Generates random boolean (true/false) values with configurable probability.

## Basic Usage

```json
{
  "isActive": {"gen": "boolean"}
}
```

**Output:** `true` or `false` (50/50 chance)

## Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `probability` | double | 0.5 | Probability of generating `true` (0.0 to 1.0) |

## Examples

### Equal Probability

```json
{
  "isActive": {"gen": "boolean"}
}
```

**Result:** 50% true, 50% false

### High Probability of True

```json
{
  "isVerified": {"gen": "boolean", "probability": 0.8}
}
```

**Result:** 80% true, 20% false

### Low Probability of True

```json
{
  "isPremium": {"gen": "boolean", "probability": 0.1}
}
```

**Result:** 10% true, 90% false

### Always True

```json
{
  "isEnabled": {"gen": "boolean", "probability": 1.0}
}
```

### Always False

```json
{
  "isDeleted": {"gen": "boolean", "probability": 0.0}
}
```

## Filtering Support

Boolean is the **only generator** that supports built-in filtering:

```json
{
  "isActive": {
    "gen": "boolean",
    "probability": 0.8,
    "filter": [false]
  }
}
```

**Result:** Only generates `true` values (filters out `false`)

## Common Patterns

### User Flags

```json
{
  "users": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "isActive": {"gen": "boolean", "probability": 0.9},
      "isVerified": {"gen": "boolean", "probability": 0.7},
      "isPremium": {"gen": "boolean", "probability": 0.15}
    }
  }
}
```

### Feature Flags

```json
{
  "features": {
    "count": 1,
    "item": {
      "darkMode": {"gen": "boolean", "probability": 0.6},
      "betaFeatures": {"gen": "boolean", "probability": 0.2},
      "notifications": {"gen": "boolean", "probability": 0.85},
      "analytics": {"gen": "boolean", "probability": 0.95}
    }
  }
}
```

### Product Attributes

```json
{
  "products": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "inStock": {"gen": "boolean", "probability": 0.85},
      "onSale": {"gen": "boolean", "probability": 0.2},
      "featured": {"gen": "boolean", "probability": 0.1},
      "freeShipping": {"gen": "boolean", "probability": 0.4}
    }
  }
}
```

### Account Settings

```json
{
  "accounts": {
    "count": 200,
    "item": {
      "id": {"gen": "uuid"},
      "emailNotifications": {"gen": "boolean", "probability": 0.7},
      "smsNotifications": {"gen": "boolean", "probability": 0.3},
      "twoFactorAuth": {"gen": "boolean", "probability": 0.4},
      "publicProfile": {"gen": "boolean", "probability": 0.6}
    }
  }
}
```

## Best Practices

1. **Use Realistic Probabilities**: Match real-world distributions
2. **Document Probabilities**: Comment why specific probabilities were chosen
3. **Consider Defaults**: Use high probability for default-enabled features
4. **Use Filtering**: Filter out unwanted values when needed
5. **Avoid 0.5**: Use specific probabilities that match your use case

## Comparison with Choice

For boolean values with weights, you can also use Choice:

```json
{
  "isActive": {
    "gen": "choice",
    "options": [true, false],
    "weights": [80, 20]
  }
}
```

This is equivalent to:

```json
{
  "isActive": {"gen": "boolean", "probability": 0.8}
}
```

Use Boolean generator for simplicity, Choice for consistency with other weighted selections.

## Next Steps

- [Choice Generator](./choice.md) - Select from multiple options
- [Filtering](../dsl-reference/filtering.md) - Learn about filtering
