# Shadow Bindings

Shadow bindings allow you to bind and reuse referenced items within the same item without including them in the final output. Fields prefixed with `$` are resolved during generation but excluded from the generated data.

## Overview

Shadow bindings solve a common problem: when you need to reference the same item multiple times or use its values in conditional references, but don't want those intermediate values in your final data.

**Key benefits:**
- **Cross-entity constraints**: Use bound values in conditional references
- **Clean output**: Keep intermediate values out of generated data
- **Multiple field access**: Access multiple fields from the same bound item
- **Self-exclusion**: Prevent items from referencing themselves

## Syntax

 Shadow binding fields start with `$`:

```json
{
  "collectionName": {
    "count": 10,
    "item": {
      "$bindingName": {"ref": "otherCollection[*]"},
      "field1": {"ref": "$bindingName.fieldName1"},
      "field2": {"ref": "$bindingName.fieldName2"}
    }
  }
}
```

**Components:**
- `$bindingName` - The shadow binding (not included in output)
- `$bindingName.fieldPath` - Access fields from the bound item
- Scope: Available only within the same `item` block

## Basic Usage

### Binding a Random Item

Bind a random user and extract multiple fields:

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "regionId": {"gen": "uuid"}
    }
  },
  "orders": {
    "count": 100,
    "item": {
      "$user": {"ref": "users[*]"},
      "userId": {"ref": "$user.id"},
      "userName": {"ref": "$user.name"}
    }
  }
}
```

**Generated output (no `$user` field):**
```json
{
  "userId": "550e8400-e29b...",
  "userName": "John Doe"
}
```

## Patterns and Use Cases

### 1. Geographic Restrictions

Ensure products are available in the user's region:

```json
{
  "users": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "regionId": {"gen": "uuid"}
    }
  },
  "products": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "regionId": {"gen": "uuid"}
    }
  },
  "orders": {
    "count": 100,
    "item": {
      "$user": {"ref": "users[*]"},
      "userId": {"ref": "$user.id"},
      "productId": {"ref": "products[regionId=$user.regionId].id"}
    }
  }
}
```

**Result:** Each order references a product that exists in the same region as the user.

### 2. Personalized Recommendations

Combine multiple conditions on bound values:

```json
{
  "users": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "regionId": {"gen": "uuid"},
      "preferredCategory": {"gen": "choice", "options": ["electronics", "books", "clothing"]}
    }
  },
  "products": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "regionId": {"gen": "uuid"},
      "category": {"gen": "choice", "options": ["electronics", "books", "clothing"]}
    }
  },
  "personalizedOrders": {
    "count": 50,
    "item": {
      "$user": {"ref": "users[*]"},
      "userId": {"ref": "$user.id"},
      "productId": {
        "ref": "products[regionId=$user.regionId and category=$user.preferredCategory].id"
      }
    }
  }
}
```

**Result:** Products must match both the user's region AND preferred category.

### 3. Self-Exclusion Pattern

Prevent items from referencing themselves:

```json
{
  "users": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"}
    }
  },
  "friendships": {
    "count": 50,
    "item": {
      "$person": {"ref": "users[*]"},
      "userId": {"ref": "$person.id"},
      "friendId": {"ref": "users[id!=$person.id].id"}
    }
  }
}
```

**Result:** No user can be friends with themselves.

### 4. Hierarchical Data with Constraints

Ensure managers are from the same department as employees:

```json
{
  "departments": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "company.industry"}
    }
  },
  "employees": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "departmentId": {"ref": "departments[*].id"}
    }
  },
  "managers": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "departmentId": {"ref": "departments[*].id"}
    }
  },
  "assignments": {
    "count": 100,
    "item": {
      "$employee": {"ref": "employees[*]"},
      "employeeId": {"ref": "$employee.id"},
      "managerId": {
        "ref": "managers[departmentId=$employee.departmentId].id"
      }
    }
  }
}
```

**Result:** Each employee is assigned to a manager from the same department.

## Nested Field Access

Access deeply nested fields in bound items:

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "profile": {
        "settings": {
          "theme": {"gen": "choice", "options": ["light", "dark"]},
          "language": {"gen": "choice", "options": ["en", "es", "fr"]}
        }
      }
    }
  },
  "userPreferences": {
    "count": 10,
    "item": {
      "$user": {"ref": "users[*]"},
      "userId": {"ref": "$user.id"},
      "theme": {"ref": "$user.profile.settings.theme"},
      "language": {"ref": "$user.profile.settings.language"}
    }
  }
}
```

## Advanced Features

### Multiple Bindings

Use multiple shadow bindings in the same item:

```json
{
  "shipments": {
    "count": 50,
    "item": {
      "$category": {"ref": "categories[*]"},
      "$warehouse": {"ref": "warehouses[categoryId=$category.id]"},
      "categoryId": {"ref": "$category.id"},
      "warehouseId": {"ref": "$warehouse.id"},
      "warehouseName": {"ref": "$warehouse.name"}
    }
  }
}
```

Note: When binding to an object, don't include the field path (e.g., `.id`) in the binding reference. The binding should capture the whole object so you can access multiple fields from it.

### Binding Order Matters

Define bindings before using them:

```json
{
  "item": {
    "$user": {"ref": "users[*]"},      // First: define binding
    "userId": {"ref": "$user.id"},     // Then: use binding
    "productId": {
      "ref": "products[regionId=$user.regionId].id"
    }
  }
}
```

### Sequential with Shadow Bindings

Combine sequential references with shadow bindings:

```json
{
  "orders": {
    "count": 100,
    "item": {
      "$user": {"ref": "users[*]", "sequential": true},
      "userId": {"ref": "$user.id"},
      "productId": {"ref": "products[regionId=$user.regionId].id"}
    }
  }
}
```

**Result:** Users are distributed evenly across orders, but products still match the user's region.

## Rules and Limitations

### Must Follow

1. **Prefix required**: Shadow bindings must start with `$`
2. **Field path required**: Must specify field path (e.g., `$user.name`)
3. **Define before use**: Bindings must be defined before they're referenced
4. **Item scope**: Bindings only work within the same `item` block

### Limitations

1. **No cross-item scope**: Cannot use bindings across different `item` blocks
2. **No circular dependencies**: Cannot create cycles with bindings
3. **Requires field path**: `$user` alone is invalid, must use `$user.field`
4. **Cannot reference collections**: Cannot use `$users[*].field` directly

## Best Practices

### 1. Descriptive Binding Names

Use clear, descriptive names for bindings:

```json
{
  "$currentUser": {"ref": "users[*]"},    // Clear
  "$u": {"ref": "users[*]"}                // Not clear
}
```

### 2. Group Related Fields

Use shadow bindings logically:

```json
{
  "$user": {"ref": "users[*]"},
  "userId": {"ref": "$user.id"},
  "userName": {"ref": "$user.name"},
  "userRegion": {"ref": "$user.regionId"},  // All user-related together
  "productId": {"ref": "products[regionId=$user.regionId].id"}
}
```

### 3. Use for Cross-Collection Constraints

Perfect for enforcing business rules:

```json
{
  "$user": {"ref": "users[*]"},
  "productId": {"ref": "products[regionId=$user.regionId and category='electronics'].id"}
}
```

### 4. Combine with Conditional References

Enable powerful filtering logic:

```json
{
  "$employee": {"ref": "employees[*]"},
  "$manager": {"ref": "managers[departmentId=$employee.departmentId]"},
  "employeeId": {"ref": "$employee.id"},
  "managerId": {"ref": "$manager.id"}
}
```

## Common Use Cases

| Use Case | Pattern |
|----------|---------|
| Regional restrictions | `products[regionId=$user.regionId]` |
| Skill matching | `projects[requiredSkill=$employee.skill]` |
| Self-exclusion | `users[id!=$person.id]` |
| Multi-condition filters | `products[region=$r and category=$c]` |
| Hierarchical constraints | `managers[departmentId=$dept.id]` |

## Troubleshooting

### Error: "shadow binding reference must include field path"

**Invalid:**
```json
{
  "$user": {"ref": "users[*]"},
  "field": {"ref": "$user"}  // Missing field path
}
```

**Valid:**
```json
{
  "$user": {"ref": "users[*]"},
  "field": {"ref": "$user.id"}  // Includes field path
}
```

### Error: "references undeclared collection"

Ensure the referenced collection is defined before the shadow binding.

### Error: "Circular dependency detected"

Check that bindings don't reference each other in a cycle.

## Performance Considerations

Shadow bindings have minimal performance impact:
- Bound items are cached within the same item
- No additional database or file I/O
- Works efficiently with both eager and lazy generation modes

## Related Features

- [References](./references.md) - Cross-collection reference types
- [Conditional References](./references.md#conditional-reference) - Filtering collections
- [Self References](./references.md#self-reference) - Same-item field references
- [Complex Scenarios](../guides/patterns/complex-scenarios.md) - Advanced patterns

## Example: Complete E-commerce Scenario

```json
{
  "regions": {
    "count": 3,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "choice", "options": ["North America", "Europe", "Asia"]}
    }
  },
  "users": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "email": {"gen": "internet.emailAddress"},
      "regionId": {"gen": "uuid"},
      "preferredCategory": {"gen": "choice", "options": ["electronics", "clothing", "books"]}
    }
  },
  "products": {
    "count": 200,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.words", "words": 2},
      "regionId": {"gen": "uuid"},
      "category": {"gen": "choice", "options": ["electronics", "clothing", "books"]},
      "price": {"gen": "float", "min": 10, "max": 500, "decimals": 2},
      "stock": {"gen": "number", "min": 0, "max": 100}
    }
  },
  "orders": {
    "count": 500,
    "item": {
      "$user": {"ref": "users[*]"},
      "$product": {"ref": "products[regionId=$user.regionId and category=$user.preferredCategory and stock>0]"},
      "userId": {"ref": "$user.id"},
      "userName": {"ref": "$user.name"},
      "productId": {"ref": "$product.id"},
      "productName": {"ref": "$product.name"},
      "price": {"ref": "$product.price"},
      "quantity": {"gen": "number", "min": 1, "max": 5}
    }
  }
}
```

**Result:** Orders contain products that:
- Match the user's region
- Match the user's preferred category
- Have stock available
- Include user and product details for readability

## Next Steps

- [Conditional References](./references.md#conditional-reference) - Learn about filtering
- [Complex Scenarios](../guides/patterns/complex-scenarios.md) - Advanced patterns
- [Custom Generators](../guides/how-to/custom-generators.md) - Create your own generators
