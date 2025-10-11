# Choice Generator

Randomly selects from a list of options, with optional weighted probabilities.

## Basic Usage

```json
{
  "status": {
    "gen": "choice",
    "options": ["active", "inactive", "pending"]
  }
}
```

**Output:** One of: `"active"`, `"inactive"`, or `"pending"` (equal probability)

## Options

| Option | Type | Required | Description |
|--------|------|----------|-------------|
| `options` | array | Yes | List of values to choose from |
| `weights` | array of integers | No | Relative weights for each option |

## Examples

### Simple Choice

```json
{
  "role": {
    "gen": "choice",
    "options": ["admin", "user", "guest"]
  }
}
```

### Weighted Choice

```json
{
  "role": {
    "gen": "choice",
    "options": ["admin", "user", "guest"],
    "weights": [10, 80, 10]
  }
}
```

**Result:** 10% admin, 80% user, 10% guest

### Boolean Alternative

```json
{
  "isActive": {
    "gen": "choice",
    "options": [true, false],
    "weights": [80, 20]
  }
}
```

**Result:** 80% true, 20% false

### Nullable Values

```json
{
  "middleName": {
    "gen": "choice",
    "options": [null, {"gen": "name.firstName"}],
    "weights": [30, 70]
  }
}
```

**Result:** 30% null, 70% generated name

### Numeric Choices

```json
{
  "priority": {
    "gen": "choice",
    "options": [1, 2, 3, 4, 5],
    "weights": [5, 10, 50, 25, 10]
  }
}
```

## Common Patterns

### Status Fields

```json
{
  "orders": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "status": {
        "gen": "choice",
        "options": ["pending", "processing", "shipped", "delivered", "cancelled"],
        "weights": [20, 15, 30, 30, 5]
      }
    }
  }
}
```

### User Roles

```json
{
  "users": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "role": {
        "gen": "choice",
        "options": ["admin", "moderator", "user"],
        "weights": [5, 15, 80]
      }
    }
  }
}
```

### Priority Levels

```json
{
  "tickets": {
    "count": 200,
    "item": {
      "id": {"gen": "uuid"},
      "priority": {
        "gen": "choice",
        "options": ["low", "medium", "high", "critical"],
        "weights": [50, 30, 15, 5]
      }
    }
  }
}
```

### Product Categories

```json
{
  "products": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "category": {
        "gen": "choice",
        "options": ["Electronics", "Clothing", "Books", "Home", "Sports"]
      }
    }
  }
}
```

### Subscription Tiers

```json
{
  "users": {
    "count": 1000,
    "item": {
      "id": {"gen": "uuid"},
      "email": {"gen": "internet.emailAddress"},
      "tier": {
        "gen": "choice",
        "options": ["free", "basic", "premium", "enterprise"],
        "weights": [70, 20, 8, 2]
      }
    }
  }
}
```

### Payment Methods

```json
{
  "transactions": {
    "count": 500,
    "item": {
      "id": {"gen": "uuid"},
      "paymentMethod": {
        "gen": "choice",
        "options": ["credit_card", "debit_card", "paypal", "bank_transfer"],
        "weights": [50, 30, 15, 5]
      }
    }
  }
}
```

## Weighted Choice Explained

Weights are **relative**, not percentages:

```json
{
  "gen": "choice",
  "options": ["A", "B", "C"],
  "weights": [1, 2, 7]
}
```

**Total weight:** 1 + 2 + 7 = 10
- A: 1/10 = 10%
- B: 2/10 = 20%
- C: 7/10 = 70%

Same result with different weights:

```json
{
  "gen": "choice",
  "options": ["A", "B", "C"],
  "weights": [10, 20, 70]
}
```

## Filtering

Choice generator supports filtering:

```json
{
  "status": {
    "gen": "choice",
    "options": ["active", "inactive", "banned", "pending"],
    "filter": ["banned"]
  }
}
```

**Result:** Only generates "active", "inactive", or "pending"

## Best Practices

1. **Use Weights for Realism**: Match real-world distributions
2. **Document Weights**: Comment why specific weights were chosen
3. **Consider Null**: Use null in options for optional fields
4. **Enum Alternative**: Use Choice for enum-like values
5. **Test Distributions**: Verify weights produce expected distributions

## Comparison with Other Generators

| Generator | Use Case |
|-----------|----------|
| **Choice** | Select from specific options |
| [Boolean](./boolean.md) | True/false with probability |
| [Number](./number.md) | Random integers in range |
| [String](./string.md) | Random strings |

## Sequential Choice

Choice generator can cycle through options sequentially:

```json
{
  "teams": {
    "count": 3,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "choice", "options": ["Red", "Blue", "Green"]}
    }
  },
  "players": {
    "count": 30,
    "item": {
      "id": {"gen": "uuid"},
      "teamName": {
        "gen": "choice",
        "options": ["Red", "Blue", "Green"],
        "sequential": true
      }
    }
  }
}
```

Note: Sequential behavior is primarily for references, not the Choice generator itself.

## Next Steps

- [Boolean Generator](./boolean.md) - For true/false values
- [Filtering](../dsl-reference/filtering.md) - Filter choice options
- [Weighted Patterns](../guides/patterns/weighted-choices.md) - Advanced weighted choice patterns
