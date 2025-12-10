# Runtime-Computed Options

Generate dynamic, context-dependent data by referencing other field values in generator options.

## Overview

Runtime-computed options allow generator parameters (like `min`, `max`, `from`, `to`) to reference other fields in the same item. This enables realistic data where values depend on each other.

## Basic Usage

Reference another field using `this.fieldName`:

```json
{
  "employees": {
    "count": 10,
    "item": {
      "startAge": {"gen": "number", "min": 22, "max": 35},
      "retirementAge": {
        "gen": "number",
        "min": {"ref": "this.startAge"},
        "max": 65
      }
    }
  }
}
```

Each employee's `retirementAge` will be between their `startAge` and 65.

## Mapped References

Map referenced values to different option values:

```json
{
  "products": {
    "count": 20,
    "item": {
      "category": {"gen": "choice", "options": ["budget", "premium", "luxury"]},
      "price": {
        "gen": "float",
        "min": {"ref": "this.category", "map": {"budget": 10, "premium": 100, "luxury": 1000}},
        "max": {"ref": "this.category", "map": {"budget": 50, "premium": 500, "luxury": 5000}},
        "decimals": 2
      }
    }
  }
}
```

**Result:**
- Budget products: $10-50
- Premium products: $100-500
- Luxury products: $1000-5000

## Supported Generators

Runtime-computed options work with any generator that accepts numeric or date options:

| Generator | Supported Options |
|-----------|-------------------|
| `number` | `min`, `max` |
| `float` | `min`, `max` |
| `date` | `from`, `to` |
| `lorem` | `words`, `sentences`, `paragraphs` |
| `string` | `length`, `minLength`, `maxLength` |

## Common Patterns

### Date Ranges

Ensure end date is after start date:

```json
{
  "events": {
    "count": 10,
    "item": {
      "startDate": {"gen": "date", "from": "2024-01-01", "to": "2024-06-30"},
      "endDate": {
        "gen": "date",
        "from": {"ref": "this.startDate"},
        "to": "2024-12-31"
      }
    }
  }
}
```

### Content Length by Type

```json
{
  "articles": {
    "count": 10,
    "item": {
      "type": {"gen": "choice", "options": ["tweet", "post", "article"]},
      "content": {
        "gen": "lorem",
        "words": {"ref": "this.type", "map": {"tweet": 20, "post": 100, "article": 500}}
      }
    }
  }
}
```

### Salary Ranges by Level

```json
{
  "employees": {
    "count": 50,
    "item": {
      "level": {"gen": "choice", "options": ["junior", "mid", "senior", "lead"]},
      "salary": {
        "gen": "number",
        "min": {"ref": "this.level", "map": {"junior": 40000, "mid": 60000, "senior": 90000, "lead": 120000}},
        "max": {"ref": "this.level", "map": {"junior": 60000, "mid": 90000, "senior": 120000, "lead": 180000}}
      }
    }
  }
}
```

## Important Notes

1. **Field Order Matters**: Referenced fields must be defined before the field that references them
2. **Self-References Only**: Only `this.fieldName` is supported (not cross-collection references)
3. **Exact Map Keys**: Mapping requires exact string matches for keys
4. **Both Modes**: Works with both eager and memory-optimized (lazy) modes
5. **No Circular Dependencies**: Circular references will cause errors

## Limitations

- Only self-references (`this.fieldName`) are supported
- Cannot reference fields from other collections in options
- Map keys must exactly match the referenced field's value
- Nested field references (`this.nested.field`) are not supported in options

## Next Steps

- [Custom Generators](./custom-generators.md) - Create your own generators
- [References](../../dsl-reference/references.md) - Cross-collection references
- [Generators Overview](../../generators/overview.md) - All available generators
