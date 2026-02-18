# Runtime-Computed Options Example

This example demonstrates how to use runtime-computed generator options, allowing generator parameters to reference other field values for dynamic, context-dependent data generation.

## Features

### Simple References
Reference other fields in the same item:
```json
{
  "startAge": {"gen": "number", "min": 22, "max": 35},
  "retirementAge": {
    "gen": "number",
    "min": {"ref": "this.startAge"},
    "max": 65
  }
}
```

### Mapped References
Map referenced values to different option values:
```json
{
  "category": {"gen": "choice", "options": ["budget", "premium", "luxury"]},
  "price": {
    "gen": "float",
    "min": {"ref": "this.category", "map": {"budget": 10, "premium": 100, "luxury": 1000}},
    "max": {"ref": "this.category", "map": {"budget": 50, "premium": 500, "luxury": 5000}}
  }
}
```

## Use Cases

1. **Age Ranges**: Retirement age depends on starting age
2. **Dynamic Pricing**: Price ranges based on product category
3. **Date Ranges**: End date must be after start date
4. **Content Length**: String length based on content type

## Running the Example

```bash
mvn exec:java -Dexec.mainClass="examples.RuntimeComputedOptionsExample"
```

## Supported Generators

Runtime-computed options work with any generator that accepts the referenced option:
- `number`: min, max
- `float`: min, max
- `date`: from, to
- `lorem`: words, sentences, paragraphs
- `string`: length, minLength, maxLength

## Limitations

- Only self-references (`this.fieldName`) are supported
- Referenced fields must be defined before the field that references them
- Mapping requires exact string matches for keys

## Implementation Notes

- Works in both eager and memory-optimized (lazy) modes
- In lazy mode, referenced fields are automatically materialized before dependent fields
- Circular dependencies are not supported and will cause errors
