# Guides Overview

Learn common patterns and best practices for DataGeneration.

## How-To Guides

Step-by-step guides for specific tasks:

- [Custom Generators](./how-to/custom-generators.md) - Create your own generators
- [Memory Optimization](./how-to/memory-optimization.md) - Handle large datasets efficiently
- [Runtime-Computed Options](./how-to/runtime-computed-options.md) - Dynamic generator options based on other fields

## Patterns

Learn how to handle advanced scenarios:

- [Complex Scenarios](./patterns/complex-scenarios.md) - Multi-tier systems, hierarchies, and skill matching

## Best Practices

### Use Seeds for Reproducibility

```java
Generation gen = DslDataGenerator.create()
    .withSeed(123L)
    .fromJsonString(dsl)
    .generate();
```

Using seeds ensures the same data is generated every time, perfect for testing.

### Enable Memory Optimization for Large Datasets

For datasets with more than 100,000 items, enable memory optimization:

```java
Generation gen = DslDataGenerator.create()
    .withMemoryOptimization()
    .fromJsonString(dsl)
    .generate();
```

### Use Sequential References for Even Distribution

When you want balanced relationships (e.g., equal employees per team):

```json
{
  "teamId": {
    "ref": "teams[*].id",
    "sequential": true
  }
}
```

### Leverage Weighted Choices for Realistic Data

Use weights to match real-world probability distributions:

```json
{
  "status": {
    "gen": "choice",
    "options": ["active", "inactive", "pending"],
    "weights": [70, 20, 10]
  }
}
```

### Filter References to Enforce Business Rules

Exclude specific values from references:

```json
{
  "userId": {
    "ref": "users[*].id",
    "filter": [{"ref": "bannedUser.id"}]
  }
}
```

### Use Conditional References for Field-Based Filtering

Filter collections by field values before referencing:

```json
{
  "orders": {
    "count": 50,
    "item": {
      "userId": {"ref": "users[status='active' and age>=21].id"},
      "productId": {"ref": "products[stock>0].id"}
    }
  }
}
```

### Use Range References for Segmented Data

Reference specific subsets by index:

```json
{
  "regionalManagers": {
    "count": 4,
    "item": {
      "managedEmployeeId": {"ref": "employees[0:4].id"}
    }
  }
}
```

## Common Patterns

### User-Order Relationship

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
      "userId": {"ref": "users[*].id"},
      "total": {"gen": "float", "min": 10, "max": 1000, "decimals": 2}
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

### Admin User Pattern

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "email": {"gen": "internet.emailAddress"},
      "role": {"gen": "choice", "options": ["admin", "user"], "weights": [10, 90]}
    },
    "pick": {
      "admin": 0
    }
  },
  "auditLogs": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "userId": {
        "ref": "users[*].id",
        "filter": [{"ref": "admin.id"}]
      },
      "action": {"gen": "lorem.word"}
    }
  }
}
```

## Next Steps

- [DSL Reference](../dsl-reference/overview.md) - Complete DSL syntax
- [Generators](../generators/overview.md) - Explore all generators
- [Custom Generators](./how-to/custom-generators.md) - Extend with your own logic
