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
- [References](./references.md) - Create relationships between collections
- [Arrays](./arrays.md) - Generate arrays of values
- [Filtering](./filtering.md) - Exclude specific values
- [Static Values](./static-values.md) - Use literal values

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
