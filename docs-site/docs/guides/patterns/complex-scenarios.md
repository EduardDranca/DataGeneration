# Complex Scenarios

Learn how to handle advanced data generation scenarios using DataGeneration's powerful features.

## Multi-Tier User Segmentation

Generate users with different behavior patterns and product preferences.

### Scenario

E-commerce platform with three user tiers:
- VIP users (5%): Many orders, premium products only
- Regular users (70%): Moderate orders, standard products
- Occasional users (25%): Few orders, budget products

### Solution

Use separate collections for each tier with sequential references:

```json
{
  "premiumProducts": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.words", "words": 2},
      "price": {"gen": "float", "min": 500, "max": 5000, "decimals": 2}
    }
  },
  "standardProducts": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.words", "words": 2},
      "price": {"gen": "float", "min": 50, "max": 500, "decimals": 2}
    }
  },
  "budgetProducts": {
    "count": 30,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.words", "words": 2},
      "price": {"gen": "float", "min": 5, "max": 50, "decimals": 2}
    }
  },
  "vipUsers": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "email": {"gen": "internet.emailAddress"},
      "tier": "VIP"
    }
  },
  "regularUsers": {
    "count": 70,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "email": {"gen": "internet.emailAddress"},
      "tier": "Regular"
    }
  },
  "occasionalUsers": {
    "count": 25,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "email": {"gen": "internet.emailAddress"},
      "tier": "Occasional"
    }
  },
  "vipOrders": {
    "count": 500,
    "item": {
      "id": {"gen": "uuid"},
      "userId": {"ref": "vipUsers[*].id", "sequential": true},
      "productId": {"ref": "premiumProducts[*].id"},
      "quantity": {"gen": "number", "min": 1, "max": 3}
    }
  },
  "regularOrders": {
    "count": 700,
    "item": {
      "id": {"gen": "uuid"},
      "userId": {"ref": "regularUsers[*].id", "sequential": true},
      "productId": {"ref": "standardProducts[*].id"},
      "quantity": {"gen": "number", "min": 1, "max": 5}
    }
  },
  "occasionalOrders": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "userId": {"ref": "occasionalUsers[*].id", "sequential": true},
      "productId": {"ref": "budgetProducts[*].id"},
      "quantity": {"gen": "number", "min": 1, "max": 2}
    }
  }
}
```

### Key Techniques

- **Separate collections per tier**: Clear separation ensures correct product-user matching
- **Sequential references**: Distributes orders evenly across users (each VIP gets exactly 100 orders)
- **Price ranges**: Different product collections with appropriate price ranges
- **Static tier field**: Easy to identify user type in generated data

## Geographic Product Restrictions

Ensure users only order products available in their region.

### Scenario

Products are region-specific, and users should only order from their region's catalog.

### Solution

```json
{
  "regions": {
    "count": 3,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "choice", "options": ["North America", "Europe", "Asia"]}
    },
    "pick": {
      "northAmerica": 0,
      "europe": 1,
      "asia": 2
    }
  },
  "productsNA": {
    "count": 30,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "regionId": {"ref": "northAmerica.id"}
    }
  },
  "productsEU": {
    "count": 25,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "regionId": {"ref": "europe.id"}
    }
  },
  "productsAsia": {
    "count": 35,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "regionId": {"ref": "asia.id"}
    }
  },
  "usersNA": {
    "count": 40,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "regionId": {"ref": "northAmerica.id"}
    }
  },
  "usersEU": {
    "count": 35,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "regionId": {"ref": "europe.id"}
    }
  },
  "usersAsia": {
    "count": 25,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "regionId": {"ref": "asia.id"}
    }
  },
  "ordersNA": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "userId": {"ref": "usersNA[*].id"},
      "productId": {"ref": "productsNA[*].id"}
    }
  },
  "ordersEU": {
    "count": 80,
    "item": {
      "id": {"gen": "uuid"},
      "userId": {"ref": "usersEU[*].id"},
      "productId": {"ref": "productsEU[*].id"}
    }
  },
  "ordersAsia": {
    "count": 90,
    "item": {
      "id": {"gen": "uuid"},
      "userId": {"ref": "usersAsia[*].id"},
      "productId": {"ref": "productsAsia[*].id"}
    }
  }
}
```

### Key Techniques

- **Pick references**: Name specific regions for easy reference
- **Region-specific collections**: Separate products and users by region
- **Guaranteed consistency**: Orders can only reference products/users from the same region

## Hierarchical Tree Structures

Generate organizational hierarchies like company departments and teams.

### Scenario

Companies with departments, teams, and employees in a clear hierarchy.

### Solution

```json
{
  "companies": {
    "count": 3,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "company.name"}
    }
  },
  "departments": {
    "count": 9,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "choice", "options": ["Engineering", "Sales", "Marketing"]},
      "companyId": {"ref": "companies[*].id", "sequential": true}
    }
  },
  "teams": {
    "count": 27,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.word"},
      "departmentId": {"ref": "departments[*].id", "sequential": true}
    }
  },
  "employees": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "email": {"gen": "internet.emailAddress"},
      "teamId": {"ref": "teams[*].id"}
    }
  }
}
```

### Key Techniques

- **Sequential references**: Ensures even distribution (each company gets 3 departments, each department gets 3 teams)
- **Top-down hierarchy**: Build from companies → departments → teams → employees
- **Clear relationships**: Each level references its parent

## Employee-Manager Relationships

Create self-referential hierarchies where employees report to other employees.

### Scenario

Employees with manager relationships, where managers are also employees.

### Solution

```json
{
  "employees": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "email": {"gen": "internet.emailAddress"},
      "level": {"gen": "choice", "options": ["Executive", "Manager", "Individual Contributor"], "weights": [5, 20, 75]}
    },
    "pick": {
      "ceo": 0
    }
  },
  "reportingRelationships": {
    "count": 45,
    "item": {
      "employeeId": {"ref": "employees[*].id"},
      "managerId": {"ref": "employees[*].id"}
    }
  }
}
```

### Key Techniques

- **Pick for top-level**: Name the CEO for special handling
- **Separate relationships collection**: Cleaner than embedding manager in employee
- **Weighted levels**: Realistic distribution of organizational levels

## Polymorphic Data

Generate different types of items with type-specific fields.

### Scenario

Products with different types (physical, digital, service) that have different attributes.

### Solution

```json
{
  "physicalProducts": {
    "count": 30,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.words", "words": 2},
      "type": "physical",
      "price": {"gen": "float", "min": 10, "max": 500, "decimals": 2},
      "weight": {"gen": "float", "min": 0.1, "max": 50, "decimals": 2},
      "dimensions": {
        "length": {"gen": "number", "min": 5, "max": 100},
        "width": {"gen": "number", "min": 5, "max": 100},
        "height": {"gen": "number", "min": 5, "max": 100}
      }
    }
  },
  "digitalProducts": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.words", "words": 2},
      "type": "digital",
      "price": {"gen": "float", "min": 5, "max": 200, "decimals": 2},
      "downloadUrl": {"gen": "internet.url"},
      "fileSize": {"gen": "number", "min": 1, "max": 5000}
    }
  },
  "services": {
    "count": 15,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.words", "words": 3},
      "type": "service",
      "price": {"gen": "float", "min": 50, "max": 2000, "decimals": 2},
      "duration": {"gen": "number", "min": 30, "max": 480},
      "location": {"gen": "choice", "options": ["on-site", "remote", "hybrid"]}
    }
  }
}
```

### Key Techniques

- **Separate collections per type**: Each type has its own specific fields
- **Type field**: Static field identifies the type
- **Type-specific attributes**: Physical products have weight/dimensions, digital have download URLs, services have duration

## Skill-Based Matching

Match employees to projects based on required skills.

### Scenario

Projects require specific skills, and employees should have those skills.

### Solution

```json
{
  "skills": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "choice", "options": ["Java", "Python", "JavaScript", "React", "AWS", "Docker", "SQL", "MongoDB", "Kubernetes", "GraphQL"]}
    }
  },
  "javaEmployees": {
    "count": 15,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "primarySkill": "Java"
    }
  },
  "pythonEmployees": {
    "count": 12,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "primarySkill": "Python"
    }
  },
  "javaProjects": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.words", "words": 3},
      "requiredSkill": "Java"
    }
  },
  "pythonProjects": {
    "count": 4,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "lorem.words", "words": 3},
      "requiredSkill": "Python"
    }
  },
  "javaAssignments": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "projectId": {"ref": "javaProjects[*].id"},
      "employeeId": {"ref": "javaEmployees[*].id"}
    }
  },
  "pythonAssignments": {
    "count": 16,
    "item": {
      "id": {"gen": "uuid"},
      "projectId": {"ref": "pythonProjects[*].id"},
      "employeeId": {"ref": "pythonEmployees[*].id"}
    }
  }
}
```

### Key Techniques

- **Skill-specific collections**: Separate employees and projects by skill
- **Guaranteed matching**: Assignments only reference employees with the required skill
- **Static skill fields**: Easy to identify requirements

## Best Practices for Complex Scenarios

### 1. Use Sequential References for Even Distribution

```json
{
  "teamId": {"ref": "teams[*].id", "sequential": true}
}
```

This ensures each team gets an equal number of employees.

### 2. Use Pick for Important Items

```json
{
  "users": {
    "count": 100,
    "item": {...},
    "pick": {
      "admin": 0,
      "testUser": 50
    }
  }
}
```

Name specific items for easy reference in filters or special handling.

### 3. Separate Collections for Different Behaviors

Instead of one collection with conditional logic, use multiple collections:

```json
{
  "premiumUsers": {...},
  "regularUsers": {...},
  "premiumOrders": {...},
  "regularOrders": {...}
}
```

### 4. Use Weighted Choices for Realistic Distributions

```json
{
  "priority": {
    "gen": "choice",
    "options": ["low", "medium", "high", "critical"],
    "weights": [50, 30, 15, 5]
  }
}
```

### 5. Combine Static and Generated Fields

```json
{
  "id": {"gen": "uuid"},
  "type": "premium",
  "name": {"gen": "name.fullName"},
  "tier": 1
}
```

## Next Steps

- [References](../../dsl-reference/references.md) - Learn about reference types
- [Filtering](../../dsl-reference/filtering.md) - Exclude specific values
- [Arrays](../../dsl-reference/arrays.md) - Generate arrays of values
