# DSL Reference (Quick Guide)

## Basic Structure

```json
{
  "collectionName": {
    "count": 10,
    "item": {
      "field": {"gen": "generatorName"},
      "static": "value",
      "nested": {
        "field": {"gen": "generator"}
      }
    }
  }
}
```

## Generators (18 total)

**No options**: `uuid`, `name`, `internet`, `address`, `company`, `country`, `book`, `finance`

**With options**:
- `phone`: `format` (default, international, cell, extension)
- `number`: `min`, `max`
- `float`: `min`, `max`, `decimals` (default: 2, max: 10)
- `boolean`: `probability` (0.0-1.0, default: 0.5) - supports filtering
- `string`: `length` OR `minLength`/`maxLength`, `allowedChars`, `regex`
- `date`: `from`, `to` (ISO format), `format` (iso, iso_datetime, timestamp, epoch, custom pattern)
- `lorem`: `words`, `sentences`, `paragraphs`
- `sequence`: `start` (default: 0), `increment` (default: 1)
- `choice`: `options` (required), `weights` (optional)
- `csv`: `file` (required), `sequential` (default: true)

**Field access**: Use dot notation for object generators: `name.firstName`, `internet.emailAddress`, `address.city`

## References

```json
{"ref": "collection[*].field"}           // Random
{"ref": "collection[0].field"}           // Indexed
{"ref": "collection[0:9].field"}         // Range (indices 0-9)
{"ref": "collection[10:].field"}         // Range (from 10 to end)
{"ref": "collection[:10].field"}         // Range (start to 10)
{"ref": "collection[-10:-1].field"}      // Negative indices
{"ref": "collection[*].field", "sequential": true}  // Sequential cycling
{"ref": "this.field"}                    // Self reference
{"ref": "this.nested.field"}             // Nested self reference
{"ref": "pickedName.field"}              // Pick reference
```

**Conditional references**:
```json
{"ref": "users[status='active'].id"}
{"ref": "products[price>50].id"}
{"ref": "users[age>=21 and status='active'].id"}
{"ref": "products[category='electronics' or featured=true].id"}
```

Operators: `=`, `!=`, `<`, `<=`, `>`, `>=`, `and`, `or`

## Arrays

```json
{
  "tags": {
    "array": {
      "size": 5,                         // Fixed size
      "item": {"gen": "lorem.word"}
    }
  },
  "skills": {
    "array": {
      "minSize": 2,                      // Variable size
      "maxSize": 5,
      "item": {"gen": "choice", "options": ["Java", "Python"]}
    }
  }
}
```

## Filtering

```json
{
  "userId": {
    "ref": "users[*].id",
    "filter": [
      {"ref": "users[0].id"},            // Filter specific reference
      "banned-user-id",                  // Filter static value
      {"ref": "bannedUser.id"}           // Filter pick reference
    ]
  }
}
```

## Advanced Features

**Pick** (name specific items):
```json
{
  "users": {
    "count": 10,
    "item": {"id": {"gen": "uuid"}},
    "pick": {
      "admin": 0,
      "testUser": 5
    }
  }
}
// Reference: {"ref": "admin.id"}
```

**Spread**:
```json
{
  "...": {"ref": "templates[*]"}         // Copy all fields from reference
}
```

**Weighted choices**:
```json
{
  "role": {
    "gen": "choice",
    "options": ["admin", "user", "guest"],
    "weights": [10, 70, 20]              // 10%, 70%, 20%
  }
}
```

**Nullable values**:
```json
{
  "middleName": {
    "gen": "choice",
    "options": [null, {"gen": "name.firstName"}],
    "weights": [30, 70]
  }
}
```

## Expressions

```json
{
  "email": {"expr": "lowercase(${this.firstName}.${this.lastName}@example.com)"},
  "slug": {"expr": "slug(${this.title})"},
  "code": {"expr": "uppercase(substring(${this.id}, 0, 8))"},
  "greeting": {"expr": "Hello ${this.firstName}!"}
}
```

- `${}` resolves **references only** (this.*, $binding.*, collection refs, pick refs) — NOT generators
- Everything outside `${}` is literal text (implicit concatenation)
- Built-in functions: `lowercase`, `uppercase`, `trim`, `substring(value, start, end)`
- Functions can nest: `uppercase(trim(${this.name}))`
- Custom functions via builder: `.withExpressionFunction("slug", (value, args) -> ...)`

## Common Patterns

**User-Order relationship**:
```json
{
  "users": {"count": 5, "item": {"id": {"gen": "uuid"}}},
  "orders": {"count": 20, "item": {"userId": {"ref": "users[*].id"}}}
}
```

**Admin user with filtering**:
```json
{
  "users": {
    "count": 10,
    "item": {"id": {"gen": "uuid"}},
    "pick": {"admin": 0}
  },
  "logs": {
    "count": 50,
    "item": {
      "userId": {"ref": "users[*].id", "filter": [{"ref": "admin.id"}]}
    }
  }
}
```

**Hierarchical data with sequential refs**:
```json
{
  "departments": {"count": 3, "item": {"id": {"gen": "uuid"}}},
  "teams": {
    "count": 9,
    "item": {
      "departmentId": {"ref": "departments[*].id", "sequential": true}
    }
  }
}
```

## Limitations

- Cannot reference collections defined later in DSL
- Cannot create circular references
- Filtering all values throws `FilteringException`
- Only Boolean generator has built-in filtering support
- Array sizes must be determined at definition time
- `expr` results are always strings; cannot use generators inside `${}`

## Performance

- Use `sequential: true` for even distribution (more efficient)
- Enable memory optimization for datasets > 10k items
- Use seeds for reproducible, faster repeated runs
