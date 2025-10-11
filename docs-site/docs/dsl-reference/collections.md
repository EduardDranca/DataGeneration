# Collections

Collections are the fundamental building blocks of the DataGeneration DSL. Each collection represents a group of generated items.

## Basic Collection

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"}
    }
  }
}
```

**Structure:**
- `"users"` - Collection name (becomes the key in output JSON)
- `"count"` - Number of items to generate
- `"item"` - Template defining the structure of each item

## Multiple Collections

Define multiple collections in one DSL:

```json
{
  "users": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"}
    }
  },
  "products": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "price": {"gen": "float", "min": 9.99, "max": 999.99", "decimals": 2}
    }
  }
}
```

## Nested Objects

Items can contain nested objects:

```json
{
  "users": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "profile": {
        "firstName": {"gen": "name.firstName"},
        "lastName": {"gen": "name.lastName"},
        "contact": {
          "email": {"gen": "internet.emailAddress"},
          "phone": {"gen": "phone.phoneNumber"}
        }
      },
      "settings": {
        "notifications": {"gen": "boolean", "probability": 0.8},
        "theme": {"gen": "choice", "options": ["light", "dark"]}
      }
    }
  }
}
```

## Pick (Named Items)

Give specific collection items names for easy reference:

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "role": {"gen": "choice", "options": ["admin", "user"]}
    },
    "pick": {
      "admin": 0,
      "testUser": 5,
      "lastUser": 9
    }
  }
}
```

**Usage:**
- `"admin": 0` - Names the first user (index 0) as "admin"
- Can reference picked items: `{"ref": "admin.id"}`
- Useful for filtering and special references

**Example with references:**

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"}
    },
    "pick": {
      "admin": 0
    }
  },
  "settings": {
    "count": 1,
    "item": {
      "adminId": {"ref": "admin.id"}
    }
  }
}
```

## Collection Order

Collections are generated in the order they appear in the DSL. This matters for references:

```json
{
  "categories": {
    "count": 5,
    "item": {"id": {"gen": "uuid"}}
  },
  "products": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "categoryId": {"ref": "categories[*].id"}  // ✅ Works - categories defined first
    }
  }
}
```

**Invalid:**
```json
{
  "products": {
    "count": 20,
    "item": {
      "categoryId": {"ref": "categories[*].id"}  // ❌ Error - categories not defined yet
    }
  },
  "categories": {
    "count": 5,
    "item": {"id": {"gen": "uuid"}}
  }
}
```

## Output Format

Collections are output as JSON arrays:

```json
{
  "users": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "John Doe"
    },
    {
      "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "name": "Jane Smith"
    }
  ]
}
```

## Accessing Collections in Java

```java
Generation generation = DslDataGenerator.create()
    .fromJsonString(dsl)
    .generate();

// Get entire collection
List<Map<String, Object>> users = generation.getCollection("users");

// Get specific item
Map<String, Object> firstUser = users.get(0);
String userId = (String) firstUser.get("id");

// Stream collection (lazy mode)
generation.streamCollection("users").forEach(user -> {
    System.out.println(user);
});
```

## Best Practices

1. **Meaningful Names**: Use descriptive collection names that reflect your domain
2. **Order Matters**: Define collections before referencing them
3. **Use Pick**: Name important items (admin users, default categories) for easy reference
4. **Reasonable Counts**: Start small (5-10 items) for testing, scale up as needed
5. **Nested Structure**: Use nested objects to organize related fields

## Next Steps

- [References](./references.md) - Create relationships between collections
- [Arrays](./arrays.md) - Generate arrays of values
- [Static Values](./static-values.md) - Use literal values
