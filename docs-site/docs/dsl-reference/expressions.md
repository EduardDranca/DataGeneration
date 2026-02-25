# Expressions

Expressions let you build computed string values by combining references, literal text, and functions.

## Basic Syntax

Use the `expr` keyword to define an expression field:

```json
{
  "greeting": {"expr": "Hello ${this.firstName}!"}
}
```

The `${}` placeholders resolve references. Everything outside them is literal text.

## Reference Placeholders

Inside `${}` you can use any reference type:

```json
{
  "users": {
    "count": 5,
    "item": {
      "firstName": {"gen": "name.firstName"},
      "lastName": {"gen": "name.lastName"},
      "email": {"expr": "${this.firstName}.${this.lastName}@example.com"}
    }
  }
}
```

### Supported Reference Types

| Reference | Example |
|-----------|---------|
| Self reference | `${this.firstName}` |
| Shadow binding | `${$user.firstName}` |
| Pick reference | `${admin.name}` |
| Collection reference | `${users[*].name}` |

:::note
Expressions resolve **references only**, not generators. If you need a generated value, assign it to a field first and reference it with `this.fieldName`.
:::

## Built-in Functions

Four functions are available out of the box:

### lowercase

Converts the entire result to lowercase.

```json
{"expr": "lowercase(${this.firstName}.${this.lastName}@example.com)"}
```

### uppercase

Converts the entire result to uppercase.

```json
{"expr": "uppercase(${this.code})"}
```

### trim

Removes leading and trailing whitespace.

```json
{"expr": "trim(${this.rawInput})"}
```

### substring

Extracts a portion of the string. Takes two extra arguments: start index and end index.

```json
{"expr": "substring(${this.id}, 0, 8)"}
```

## Nesting Functions

Functions can be nested:

```json
{"expr": "uppercase(trim(${this.name}))"}
```

## Examples

### Email Generation

```json
{
  "users": {
    "count": 10,
    "item": {
      "firstName": {"gen": "name.firstName"},
      "lastName": {"gen": "name.lastName"},
      "email": {"expr": "lowercase(${this.firstName}.${this.lastName}@company.com)"}
    }
  }
}
```

### Short ID

```json
{
  "items": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "shortId": {"expr": "uppercase(substring(${this.id}, 0, 8))"}
    }
  }
}
```

### With Shadow Bindings

```json
{
  "users": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "firstName": {"gen": "name.firstName"},
      "lastName": {"gen": "name.lastName"}
    }
  },
  "emails": {
    "count": 5,
    "item": {
      "$user": {"ref": "users[*]"},
      "userId": {"ref": "$user.id"},
      "email": {"expr": "lowercase(${$user.firstName}.${$user.lastName}@company.com)"}
    }
  }
}
```

### With Pick References

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.firstName"}
    },
    "pick": {"admin": 0}
  },
  "logs": {
    "count": 20,
    "item": {
      "message": {"expr": "Action performed by ${admin.name}"}
    }
  }
}
```

## Custom Functions

Register custom expression functions via the Java API:

```java
Generation result = DslDataGenerator.create()
    .withSeed(123L)
    .withExpressionFunction("slug", (value, args) ->
        value.toLowerCase()
             .replaceAll("\\s+", "-")
             .replaceAll("[^a-z0-9-]", ""))
    .fromJsonString(dsl)
    .generate();
```

Then use in DSL:

```json
{
  "articles": {
    "count": 10,
    "item": {
      "title": {"gen": "lorem.sentence"},
      "slug": {"expr": "slug(${this.title})"}
    }
  }
}
```

The `ExpressionFunction` interface:

```java
@FunctionalInterface
public interface ExpressionFunction {
    String apply(String value, List<String> args);
}
```

- `value` — the evaluated first argument of the function call
- `args` — any extra arguments (e.g., `0` and `8` in `substring(${this.id}, 0, 8)`)

## Limitations

- `expr` cannot be combined with `gen`, `ref`, or `array` on the same field
- Only references are supported inside `${}` — not inline generators
- Expression results are always strings
