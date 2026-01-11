# Static Values

Any field without a `"gen"` keyword is treated as a static (literal) value.

## Basic Static Values

```json
{
  "users": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "role": "user",
      "active": true,
      "version": 1
    }
  }
}
```

**Output:**
```json
{
  "users": [
    {
      "id": "550e8400-...",
      "role": "user",
      "active": true,
      "version": 1
    }
  ]
}
```

## Supported Types

### Strings

```json
{
  "status": "pending",
  "message": "Hello World",
  "empty": ""
}
```

### Numbers

```json
{
  "count": 42,
  "price": 19.99,
  "negative": -10,
  "zero": 0
}
```

### Booleans

```json
{
  "active": true,
  "deleted": false,
  "verified": true
}
```

### Null

```json
{
  "middleName": null,
  "deletedAt": null
}
```

### Objects

```json
{
  "metadata": {
    "version": 1,
    "type": "standard",
    "features": {
      "premium": false,
      "trial": true
    }
  }
}
```

### Arrays

```json
{
  "roles": ["user", "viewer"],
  "permissions": ["read", "write"],
  "tags": [],
  "scores": [10, 20, 30, 40, 50]
}
```

## Mixing Static and Generated Values

Combine static and generated values in the same item:

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "role": "user",
      "active": true,
      "createdAt": "2024-01-01T00:00:00Z",
      "metadata": {
        "version": 1,
        "source": "api"
      }
    }
  }
}
```

## Static Values in Nested Objects

```json
{
  "products": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "pricing": {
        "currency": "USD",
        "taxRate": 0.08,
        "amount": {"gen": "float", "min": 9.99, "max": 999.99, "decimals": 2}
      },
      "metadata": {
        "version": 1,
        "schema": "v2",
        "flags": {
          "featured": false,
          "onSale": false
        }
      }
    }
  }
}
```

## Static Arrays with Generated Items

Arrays can have static structure with generated content:

```json
{
  "users": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "roles": ["user", "viewer"],
      "contacts": [
        {
          "type": "email",
          "value": {"gen": "internet.emailAddress"},
          "primary": true
        },
        {
          "type": "phone",
          "value": {"gen": "phone.phoneNumber"},
          "primary": false
        }
      ]
    }
  }
}
```

## Common Patterns

### Default Values

```json
{
  "users": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "role": "user",
      "status": "active",
      "emailVerified": false,
      "phoneVerified": false,
      "twoFactorEnabled": false
    }
  }
}
```

### Configuration Objects

```json
{
  "applications": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "config": {
        "timeout": 30000,
        "retries": 3,
        "debug": false,
        "endpoints": {
          "api": "https://api.example.com",
          "cdn": "https://cdn.example.com"
        }
      }
    }
  }
}
```

### Versioning

```json
{
  "documents": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "content": {"gen": "lorem.paragraph"},
      "version": 1,
      "schemaVersion": "2.0",
      "format": "markdown"
    }
  }
}
```

### Timestamps

```json
{
  "records": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "data": {"gen": "lorem.sentence"},
      "createdAt": "2024-01-01T00:00:00Z",
      "updatedAt": {"gen": "date", "format": "iso_datetime"},
      "deletedAt": null
    }
  }
}
```

### Feature Flags

```json
{
  "users": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "features": {
        "darkMode": {"gen": "boolean", "probability": 0.7},
        "betaAccess": false,
        "premiumTrial": false,
        "notifications": true
      }
    }
  }
}
```

## When to Use Static Values

Use static values when:

1. **Default Values**: All items should have the same default value
2. **Configuration**: Fixed configuration that doesn't change
3. **Versioning**: Schema or API version numbers
4. **Type Indicators**: Fixed type or category identifiers
5. **Flags**: Boolean flags that are initially false
6. **Null Placeholders**: Fields that are initially null

Use generators when:

1. **Variation Needed**: Each item should have different values
2. **Realistic Data**: Need realistic-looking data (names, emails, etc.)
3. **Random Values**: Need random numbers, dates, or choices
4. **Relationships**: Need to reference other collections

## Combining with Choice Generator

For controlled variation, use Choice with static options:

```json
{
  "users": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "role": {
        "gen": "choice",
        "options": ["user", "admin", "moderator"],
        "weights": [85, 10, 5]
      },
      "status": "active",
      "tier": {
        "gen": "choice",
        "options": ["free", "premium"],
        "weights": [80, 20]
      }
    }
  }
}
```

## Best Practices

1. **Use Static for Constants**: Don't generate values that should be constant
2. **Mix Appropriately**: Combine static and generated values naturally
3. **Document Defaults**: Comment why certain values are static
4. **Consider Choice**: Use Choice generator for controlled variation
5. **Null for Optional**: Use `null` for optional fields that may be populated later

## Next Steps

- [Generators](../generators/overview.md) - Explore all generators
- [Choice Generator](../generators/choice.md) - Controlled variation
- [References](./references.md) - Create relationships
