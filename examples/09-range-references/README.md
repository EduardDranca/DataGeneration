# Range References Example

This example demonstrates **range references** - referencing a subset of items from a collection by specifying an index range.

## What it generates

- **20 employees** with IDs, names, and departments
- **4 regional managers** each overseeing a specific range of employees
- **10 performance reviews** for employees in specific ranges

## Key features demonstrated

- **Range references** - `users[0:9]` references items 0 through 9
- **Open-ended ranges** - `users[10:]` references from index 10 to the end
- **Multiple range patterns** for different use cases
- **Practical scenarios** showing when range references are useful

## Data relationships

```
Employees (20)
    ↓
Regional Managers (4) → Oversee specific employee ranges
    ↓
Performance Reviews (10) → Review employees in specific ranges
```

## Range Reference Syntax

```json
{"ref": "collection[0:9].field"}     // Indices 0-9 (10 items)
{"ref": "collection[10:19].field"}   // Indices 10-19 (10 items)
{"ref": "collection[0:4].field"}     // Indices 0-4 (5 items)
{"ref": "collection[15:].field"}     // From index 15 to end
{"ref": "collection[:10].field"}     // From start to index 10
{"ref": "collection[:].field"}       // All items (equivalent to [*])
```

## Use cases

- **Regional management** - Managers oversee specific employee ranges
- **Tiered access** - Early adopters (first 100 users) get beta features
- **Geographic distribution** - Stores in specific regions
- **Time-based segmentation** - Q1 reports reference Q1 events
- **Performance tiers** - Top performers get bonuses

## Running the example

```bash
# From project root
mvn compile exec:java -Dexec.mainClass="com.github.eddranca.datagenerator.examples.RangeReferencesExample"

# Or compile and run directly
javac -cp "target/classes:..." examples/08-range-references/RangeReferencesExample.java
java -cp "target/classes:..." com.github.eddranca.datagenerator.examples.RangeReferencesExample
```

## Example output

```json
{
  "employees": [...],
  "regionalManagers": [
    {
      "id": "...",
      "name": "John Smith",
      "region": "North",
      "employeeId": "..." // From employees[0:4]
    },
    {
      "id": "...",
      "name": "Jane Doe",
      "region": "South",
      "employeeId": "..." // From employees[5:9]
    }
  ],
  "performanceReviews": [...]
}
```

## When to use range references

**Use range references when:**
- You need to reference a specific subset of items
- Items have natural ordering or segmentation
- You want to model hierarchical or regional structures
- Early/late items have special meaning

**Alternatives:**
- **Sequential references** - If you just need even distribution
- **Conditional references** - If filtering by field values
- **Pick references** - If you need specific named items
