# DSL Reference

Complete reference for the DataGeneration DSL syntax.

## Basic Structure

```json
{
  "collectionName": {
    "count": <number>,
    "item": {
      "fieldName": <field definition>
    }
  }
}
```

## Topics

- [Collections](./collections.md) - Define collections and items
- [References](./references.md) - Create relationships between collections (including range and conditional references)
- [Shadow Bindings](./shadow-bindings.md) - Bind and reuse referenced items for cross-entity constraints
- [Arrays](./arrays.md) - Generate arrays of values
- [Filtering](./filtering.md) - Exclude specific values
- [Static Values](./static-values.md) - Use literal values

## Key Features

### Reference Types
- **Simple** `[*]` - Random item
- **Indexed** `[0]` - Specific item
- **Range** `[0:9]` - Subset by index range
- **Conditional** `[status='active']` - Filter by field values
- **Sequential** - Cycle through items in order
- **Self** `this.field` - Reference same item
- **Shadow Bindings** `$name` - Bind items for reuse in conditions

## Quick Examples

### Simple Collection

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

### With References

```json
{
  "users": {
    "count": 5,
    "item": {"id": {"gen": "uuid"}}
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

### With Arrays

```json
{
  "products": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "tags": {
        "array": {
          "size": 3,
          "item": {"gen": "lorem.word"}
        }
      }
    }
  }
}
```

## Next Steps

Explore each topic in detail using the sidebar navigation.
