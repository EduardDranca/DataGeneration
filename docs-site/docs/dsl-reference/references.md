# References

References create relationships between collections, enabling you to generate realistic interconnected data.

## Simple Reference (Random)

Reference a random item from a collection:

```json
{
  "users": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"}
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

**Syntax:** `{"ref": "collectionName[*].fieldName"}`

Each order will reference a random user's ID.

## Indexed Reference

Reference a specific item by index:

```json
{
  "settings": {
    "count": 1,
    "item": {
      "defaultUserId": {"ref": "users[0].id"},
      "adminUserId": {"ref": "users[1].id"}
    }
  }
}
```

**Syntax:** `{"ref": "collectionName[index].fieldName"}`

- `users[0]` - First user
- `users[1]` - Second user
- etc.

## Sequential Reference

Cycle through collection items in order:

```json
{
  "teams": {
    "count": 3,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "company.name"}
    }
  },
  "employees": {
    "count": 30,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "teamId": {
        "ref": "teams[*].id",
        "sequential": true
      }
    }
  }
}
```

**Syntax:** Add `"sequential": true` to any reference

**Behavior:** With 3 teams and 30 employees:
- Employees 0-9 → Team 0
- Employees 10-19 → Team 1
- Employees 20-29 → Team 2

Each team gets exactly 10 employees (30 / 3).

## Self Reference

Reference other fields in the same item:

```json
{
  "users": {
    "count": 5,
    "item": {
      "firstName": {"gen": "name.firstName"},
      "lastName": {"gen": "name.lastName"},
      "email": {"ref": "this.firstName"},
      "username": {"ref": "this.lastName"}
    }
  }
}
```

**Syntax:** `{"ref": "this.fieldName"}`

**Nested Self Reference:**

```json
{
  "users": {
    "count": 5,
    "item": {
      "profile": {
        "firstName": {"gen": "name.firstName"},
        "lastName": {"gen": "name.lastName"}
      },
      "displayName": {"ref": "this.profile.firstName"}
    }
  }
}
```

## Pick References

Reference named collection items:

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"}
    },
    "pick": {
      "admin": 0,
      "testUser": 5
    }
  },
  "settings": {
    "count": 1,
    "item": {
      "adminId": {"ref": "admin.id"},
      "testUserId": {"ref": "testUser.id"}
    }
  }
}
```

**Syntax:** `{"ref": "pickedName.fieldName"}`

## Nested Field References

Reference nested fields:

```json
{
  "users": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "profile": {
        "email": {"gen": "internet.emailAddress"}
      }
    }
  },
  "notifications": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "recipientEmail": {"ref": "users[*].profile.email"}
    }
  }
}
```

**Syntax:** `{"ref": "collectionName[*].nested.field.path"}`

## Spread Operator

Copy all fields from a referenced object:

```json
{
  "templates": {
    "count": 2,
    "item": {
      "type": {"gen": "choice", "options": ["premium", "basic"]},
      "features": {"gen": "lorem.words", "words": 3},
      "price": {"gen": "float", "min": 9.99, "max": 99.99", "decimals": 2}
    }
  },
  "products": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "...": {"ref": "templates[*]"}
    }
  }
}
```

**Syntax:** `"...": {"ref": "collectionName[*]"}`

**Result:** Each product gets all fields from a random template (type, features, price).

## Reference with Filtering

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
      "bannedUser": 5
    }
  },
  "orders": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "userId": {
        "ref": "users[*].id",
        "filter": [{"ref": "bannedUser.id"}]
      }
    }
  }
}
```

See [Filtering](./filtering.md) for more details.

## Common Patterns

### One-to-Many Relationship

```json
{
  "authors": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"}
    }
  },
  "books": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "title": {"gen": "book.title"},
      "authorId": {"ref": "authors[*].id"}
    }
  }
}
```

### Many-to-Many (via Junction Table)

```json
{
  "students": {
    "count": 30,
    "item": {"id": {"gen": "uuid"}}
  },
  "courses": {
    "count": 10,
    "item": {"id": {"gen": "uuid"}}
  },
  "enrollments": {
    "count": 100,
    "item": {
      "studentId": {"ref": "students[*].id"},
      "courseId": {"ref": "courses[*].id"}
    }
  }
}
```

### Hierarchical Data

```json
{
  "departments": {
    "count": 3,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "company.industry"}
    }
  },
  "teams": {
    "count": 9,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "departmentId": {"ref": "departments[*].id", "sequential": true}
    }
  },
  "employees": {
    "count": 30,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "teamId": {"ref": "teams[*].id"}
    }
  }
}
```

## Best Practices

1. **Use Sequential for Even Distribution**: When you want balanced relationships (e.g., equal employees per team)
2. **Use Random for Realistic Variation**: When relationships should be uneven (e.g., popular products get more orders)
3. **Name Important Items with Pick**: Makes filtering and special references easier
4. **Reference Order Matters**: Define collections before referencing them
5. **Self References for Derived Fields**: Use `this` to create fields based on other fields in the same item

## Limitations

- Cannot reference collections defined later in the DSL
- Cannot create circular references
- Pick references only work with indexed items (not random selections)

## Next Steps

- [Filtering](./filtering.md) - Exclude specific values from references
- [Arrays](./arrays.md) - Generate arrays with references
- [Common Patterns](../guides/patterns/relationships.md) - Learn relationship patterns
