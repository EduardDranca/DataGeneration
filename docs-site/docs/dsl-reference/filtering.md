# Filtering

Filtering allows you to exclude specific values from references, enabling you to enforce business rules and constraints.

## Basic Filtering

Exclude specific values from a reference:

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"}
    }
  },
  "orders": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "userId": {
        "ref": "users[*].id",
        "filter": [{"ref": "users[0].id"}]
      }
    }
  }
}
```

**Syntax:** `"filter": [<values to exclude>]`

Orders will never reference the first user (index 0).

## Filtering with Static Values

Filter out literal values:

```json
{
  "categories": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {
        "gen": "choice",
        "options": ["Electronics", "Clothing", "Books", "Weapons", "Drugs"]
      }
    }
  },
  "products": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "categoryName": {
        "ref": "categories[*].name",
        "filter": ["Weapons", "Drugs"]
      }
    }
  }
}
```

Products will never be in "Weapons" or "Drugs" categories.

## Filtering with Pick References

Use named items in filters:

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "status": {
        "gen": "choice",
        "options": ["active", "inactive", "banned"]
      }
    },
    "pick": {
      "bannedUser": 5,
      "testUser": 8
    }
  },
  "orders": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "userId": {
        "ref": "users[*].id",
        "filter": [
          {"ref": "bannedUser.id"},
          {"ref": "testUser.id"}
        ]
      }
    }
  }
}
```

Orders will never reference the banned user or test user.

## Multiple Filters

Exclude multiple values:

```json
{
  "userId": {
    "ref": "users[*].id",
    "filter": [
      {"ref": "users[0].id"},
      {"ref": "users[1].id"},
      {"ref": "admin.id"}
    ]
  }
}
```

## Filtering with Sequential References

Combine filtering with sequential access:

```json
{
  "teams": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "company.name"}
    },
    "pick": {
      "archivedTeam": 4
    }
  },
  "employees": {
    "count": 40,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "teamId": {
        "ref": "teams[*].id",
        "sequential": true,
        "filter": [{"ref": "archivedTeam.id"}]
      }
    }
  }
}
```

**Behavior:** 40 employees distributed evenly across 4 teams (excluding the archived team).

## Filtering in Arrays

Filter references within arrays:

```json
{
  "categories": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"}
    },
    "pick": {
      "restricted": 5
    }
  },
  "products": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "categoryIds": {
        "array": {
          "size": 3,
          "item": {
            "ref": "categories[*].id",
            "filter": [{"ref": "restricted.id"}]
          }
        }
      }
    }
  }
}
```

## Filtering Behavior Configuration

Control what happens when all values are filtered:

```java
Generation generation = DslDataGenerator.create()
    .withFilteringBehavior(FilteringBehavior.THROW_EXCEPTION)  // Default
    .fromJsonString(dsl)
    .generate();
```

**Options:**
- `THROW_EXCEPTION` (default) - Throws `FilteringException` if all values filtered
- `RETURN_NULL` - Returns `null` if all values filtered

### Example with RETURN_NULL

```java
String dsl = """
    {
      "users": {
        "count": 1,
        "item": {"id": {"gen": "uuid"}}
      },
      "orders": {
        "count": 5,
        "item": {
          "userId": {
            "ref": "users[*].id",
            "filter": [{"ref": "users[0].id"}]
          }
        }
      }
    }
    """;

Generation generation = DslDataGenerator.create()
    .withFilteringBehavior(FilteringBehavior.RETURN_NULL)
    .fromJsonString(dsl)
    .generate();

// All orders will have userId: null
```

## Generator-Specific Filtering

Only the **Boolean** generator supports built-in filtering:

```json
{
  "isActive": {
    "gen": "boolean",
    "probability": 0.8,
    "filter": [false]
  }
}
```

This will only generate `true` values (filtering out `false`).

For other generators, use the Choice generator:

```json
{
  "status": {
    "gen": "choice",
    "options": ["active", "inactive", "pending", "banned"],
    "filter": ["banned"]
  }
}
```

## Common Patterns

### Exclude Admin Users

```json
{
  "users": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "role": {
        "gen": "choice",
        "options": ["admin", "user"],
        "weights": [10, 90]
      }
    },
    "pick": {
      "admin": 0
    }
  },
  "auditLogs": {
    "count": 100,
    "item": {
      "userId": {
        "ref": "users[*].id",
        "filter": [{"ref": "admin.id"}]
      },
      "action": {"gen": "lorem.word"}
    }
  }
}
```

### Exclude Inactive Items

```json
{
  "products": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "status": {
        "gen": "choice",
        "options": ["active", "inactive", "discontinued"]
      }
    }
  },
  "orders": {
    "count": 200,
    "item": {
      "productId": {
        "ref": "products[*].id",
        "filter": [
          {"ref": "products[*].status", "value": "inactive"},
          {"ref": "products[*].status", "value": "discontinued"}
        ]
      }
    }
  }
}
```

Note: The above pattern is conceptual - current implementation requires filtering by specific item references, not by field values.

### Self-Exclusion

Prevent items from referencing themselves:

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "friendId": {
        "ref": "users[*].id",
        "filter": [{"ref": "this.id"}]
      }
    }
  }
}
```

## Best Practices

1. **Use Pick for Clarity**: Name items you want to filter for better readability
2. **Filter Early**: Define filters at the point of reference
3. **Test Edge Cases**: Ensure you don't filter out all possible values
4. **Choose Behavior**: Select appropriate `FilteringBehavior` for your use case
5. **Document Filters**: Comment why specific values are filtered

## Limitations

- Cannot filter based on complex conditions (e.g., "all users where age > 30")
- Cannot filter based on computed values
- Filtering all values throws exception (unless configured otherwise)
- Boolean generator is the only generator with built-in filtering support

## Error Handling

### FilteringException

Thrown when all possible values are filtered:

```json
{
  "users": {
    "count": 1,
    "item": {"id": {"gen": "uuid"}}
  },
  "orders": {
    "count": 5,
    "item": {
      "userId": {
        "ref": "users[*].id",
        "filter": [{"ref": "users[0].id"}]
      }
    }
  }
}
```

**Error:** `FilteringException: All values have been filtered out`

**Solution:** Either reduce filters or use `FilteringBehavior.RETURN_NULL`

## Next Steps

- [References](./references.md) - Learn about reference types
- [Collections](./collections.md) - Understand pick syntax
- [Common Patterns](../guides/patterns/filtering.md) - See filtering patterns
