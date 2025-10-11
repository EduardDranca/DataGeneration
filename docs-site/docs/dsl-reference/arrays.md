# Arrays

Generate arrays of values within your items.

## Basic Array

```json
{
  "products": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "tags": {
        "array": {
          "size": 5,
          "item": {"gen": "lorem.word"}
        }
      }
    }
  }
}
```

**Syntax:**
```json
{
  "fieldName": {
    "array": {
      "size": <number>,
      "item": <field definition>
    }
  }
}
```

**Output:**
```json
{
  "products": [
    {
      "id": "550e8400-...",
      "name": "widget",
      "tags": ["lorem", "ipsum", "dolor", "sit", "amet"]
    }
  ]
}
```

## Variable Size Arrays

Use `minSize` and `maxSize` for random array lengths:

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "skills": {
        "array": {
          "minSize": 2,
          "maxSize": 8,
          "item": {
            "gen": "choice",
            "options": ["Java", "Python", "JavaScript", "Go", "Rust", "C++"]
          }
        }
      }
    }
  }
}
```

Each user will have between 2 and 8 skills.

## Arrays with Generators

Any generator can be used in arrays:

```json
{
  "company": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "phoneNumbers": {
        "array": {
          "size": 3,
          "item": {"gen": "phone.phoneNumber"}
        }
      },
      "scores": {
        "array": {
          "size": 10,
          "item": {"gen": "number", "min": 0, "max": 100}
        }
      },
      "prices": {
        "array": {
          "size": 5,
          "item": {"gen": "float", "min": 9.99, "max": 999.99", "decimals": 2}
        }
      }
    }
  }
}
```

## Arrays with References

Reference other collections in arrays:

```json
{
  "categories": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"}
    }
  },
  "products": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "categoryIds": {
        "array": {
          "size": 3,
          "item": {"ref": "categories[*].id"}
        }
      }
    }
  }
}
```

Each product will have 3 random category IDs.

## Arrays with Static Values

Mix static and generated values:

```json
{
  "users": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "roles": {
        "array": {
          "size": 2,
          "item": {
            "gen": "choice",
            "options": ["admin", "user", "moderator"]
          }
        }
      },
      "permissions": {
        "array": {
          "size": 3,
          "item": "read"
        }
      }
    }
  }
}
```

## Nested Arrays

Arrays can contain objects with nested structures:

```json
{
  "orders": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "items": {
        "array": {
          "size": 3,
          "item": {
            "productId": {"gen": "uuid"},
            "quantity": {"gen": "number", "min": 1, "max": 10},
            "price": {"gen": "float", "min": 9.99, "max": 99.99", "decimals": 2}
          }
        }
      }
    }
  }
}
```

**Output:**
```json
{
  "orders": [
    {
      "id": "550e8400-...",
      "items": [
        {
          "productId": "6ba7b810-...",
          "quantity": 3,
          "price": 29.99
        },
        {
          "productId": "7c9e6679-...",
          "quantity": 1,
          "price": 49.99
        }
      ]
    }
  ]
}
```

## Arrays of Arrays

Create multi-dimensional arrays:

```json
{
  "matrices": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "grid": {
        "array": {
          "size": 3,
          "item": {
            "array": {
              "size": 3,
              "item": {"gen": "number", "min": 0, "max": 9}
            }
          }
        }
      }
    }
  }
}
```

Creates 3x3 grids of numbers.

## Sequential References in Arrays

Use sequential references for predictable distribution:

```json
{
  "teams": {
    "count": 3,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "company.name"}
    }
  },
  "projects": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "teamIds": {
        "array": {
          "size": 2,
          "item": {
            "ref": "teams[*].id",
            "sequential": true
          }
        }
      }
    }
  }
}
```

## Common Patterns

### Tags/Labels

```json
{
  "tags": {
    "array": {
      "minSize": 1,
      "maxSize": 5,
      "item": {
        "gen": "choice",
        "options": ["urgent", "bug", "feature", "enhancement", "documentation"]
      }
    }
  }
}
```

### Contact Methods

```json
{
  "contacts": {
    "array": {
      "size": 2,
      "item": {
        "type": {
          "gen": "choice",
          "options": ["email", "phone", "address"]
        },
        "value": {"gen": "internet.emailAddress"}
      }
    }
  }
}
```

### Order Line Items

```json
{
  "lineItems": {
    "array": {
      "minSize": 1,
      "maxSize": 10,
      "item": {
        "productId": {"ref": "products[*].id"},
        "quantity": {"gen": "number", "min": 1, "max": 5},
        "unitPrice": {"gen": "float", "min": 9.99, "max": 999.99", "decimals": 2}
      }
    }
  }
}
```

### Time Series Data

```json
{
  "measurements": {
    "array": {
      "size": 24,
      "item": {
        "hour": {"gen": "sequence", "start": 0, "increment": 1},
        "temperature": {"gen": "float", "min": 15.0, "max": 30.0", "decimals": 1},
        "humidity": {"gen": "number", "min": 30, "max": 80}
      }
    }
  }
}
```

## Best Practices

1. **Use Variable Sizes**: `minSize`/`maxSize` creates more realistic variation
2. **Reasonable Array Sizes**: Keep arrays small (< 100 items) for performance
3. **Sequential for Distribution**: Use sequential references when you want even distribution
4. **Choice for Variety**: Use Choice generator to limit array item variety
5. **Nested Objects**: Use objects in arrays for complex structures

## Limitations

- Array size must be determined at definition time
- Cannot have dynamic array sizes based on other fields (see [issue #28](https://github.com/EduardDranca/DataGeneration/issues))
- Very large arrays (> 1000 items) may impact performance

## Next Steps

- [References](./references.md) - Use references in arrays
- [Filtering](./filtering.md) - Filter array item references
- [Generators](../generators/overview.md) - Explore all generators
