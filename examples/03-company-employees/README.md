# Company & Employees Example

This example shows a corporate structure with departments, employees, and projects.

## What it generates

- **4 departments** with budgets and locations
- **15 employees** with skills and positions
- **6 projects** assigned to departments and employees

## Key features demonstrated

- **Tags system** for grouping collections
- **Array generation** with dynamic counts (employee skills)
- **Weighted choices** for manager probability (20% managers)
- **Multiple references** (projects reference both departments and employees)
- **Date generators** for hire dates and project timelines

## Data relationships

```
Departments (4) ← Employees (15) ← Projects (6)
     ↑                ↓
   Tags           Skills Array
```

## Use cases

- HR system testing
- Organizational chart generation
- Project management tool seeding
- Employee directory creation
- Skills matrix analysis
