# Name Generator

Generates realistic name-related data.

## Basic Usage

```json
{
  "name": {"gen": "name"}
}
```

**Output:** Returns an object with all name fields:
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "fullName": "John Doe",
  "prefix": "Mr.",
  "suffix": "Jr.",
  "title": "Chief Executive Officer"
}
```

## Available Fields

Access specific name components using dot notation:

| Field | Description | Example |
|-------|-------------|---------|
| `firstName` | First name | `John` |
| `lastName` | Last name | `Doe` |
| `fullName` | Full name (first + last) | `John Doe` |
| `prefix` | Name prefix | `Mr.`, `Mrs.`, `Dr.` |
| `suffix` | Name suffix | `Jr.`, `Sr.`, `III` |
| `title` | Professional title | `Chief Executive Officer` |

## Options

This generator has **no options**.

## Examples

### Individual Fields

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "firstName": {"gen": "name.firstName"},
      "lastName": {"gen": "name.lastName"}
    }
  }
}
```

### Full Name

```json
{
  "contacts": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "email": {"gen": "internet.emailAddress"}
    }
  }
}
```

### With Prefix and Suffix

```json
{
  "doctors": {
    "count": 15,
    "item": {
      "id": {"gen": "uuid"},
      "prefix": {"gen": "name.prefix"},
      "firstName": {"gen": "name.firstName"},
      "lastName": {"gen": "name.lastName"},
      "suffix": {"gen": "name.suffix"},
      "title": {"gen": "name.title"}
    }
  }
}
```

**Output:**
```json
{
  "id": "550e8400-...",
  "prefix": "Dr.",
  "firstName": "Jane",
  "lastName": "Smith",
  "suffix": "Jr.",
  "title": "Chief Medical Officer"
}
```

### Self-Reference Pattern

Use name fields to derive other fields:

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "firstName": {"gen": "name.firstName"},
      "lastName": {"gen": "name.lastName"},
      "displayName": {"ref": "this.firstName"},
      "username": {"ref": "this.lastName"}
    }
  }
}
```

## Common Patterns

### User Profile

```json
{
  "users": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "firstName": {"gen": "name.firstName"},
      "lastName": {"gen": "name.lastName"},
      "email": {"gen": "internet.emailAddress"},
      "phone": {"gen": "phone.phoneNumber"}
    }
  }
}
```

### Employee Directory

```json
{
  "employees": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "fullName": {"gen": "name.fullName"},
      "title": {"gen": "name.title"},
      "department": {"gen": "company.industry"},
      "email": {"gen": "internet.emailAddress"}
    }
  }
}
```

### Contact List

```json
{
  "contacts": {
    "count": 30,
    "item": {
      "id": {"gen": "uuid"},
      "prefix": {"gen": "name.prefix"},
      "firstName": {"gen": "name.firstName"},
      "lastName": {"gen": "name.lastName"},
      "suffix": {"gen": "name.suffix"},
      "company": {"gen": "company.name"},
      "jobTitle": {"gen": "name.title"}
    }
  }
}
```

## Best Practices

1. **Use Specific Fields**: Access only the fields you need (e.g., `name.firstName`)
2. **Consistent Naming**: Use `firstName`/`lastName` or `fullName`, not both
3. **Professional Context**: Use `prefix`, `suffix`, and `title` for professional scenarios
4. **Self-References**: Derive usernames or display names from name fields

## Localization

The Name generator uses Datafaker's default locale. Names are primarily English but include diverse cultural names.

## Next Steps

- [Internet Generator](./internet.md) - Generate emails and URLs
- [Company Generator](./company.md) - Generate company names and titles
- [Self References](../dsl-reference/references.md#self-reference) - Use name fields in other fields
