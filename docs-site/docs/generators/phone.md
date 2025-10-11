# Phone Generator

Generates phone numbers in various formats.

## Basic Usage

```json
{
  "phone": {"gen": "phone"}
}
```

**Output:** Standard phone number (e.g., `555-123-4567`)

## Options

| Option | Type | Description |
|--------|------|-------------|
| `format` | string | Phone format type |

## Format Options

| Format | Description | Example |
|--------|-------------|---------|
| `default` (or omitted) | Standard phone number | `555-123-4567` |
| `international` | International format | `+1-555-123-4567` |
| `cell` or `mobile` | Mobile phone | `555-123-4567` |
| `extension` | Phone extension | `x1234` |

## Examples

### Standard Phone

```json
{
  "phone": {"gen": "phone"}
}
```

### International Format

```json
{
  "phone": {"gen": "phone", "format": "international"}
}
```

**Output:** `+1-555-123-4567`

### Mobile Phone

```json
{
  "mobile": {"gen": "phone", "format": "cell"}
}
```

### Extension

```json
{
  "extension": {"gen": "phone", "format": "extension"}
}
```

**Output:** `x1234`

## Common Patterns

### Contact Information

```json
{
  "contacts": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "phone": {"gen": "phone"},
      "mobile": {"gen": "phone", "format": "cell"},
      "email": {"gen": "internet.emailAddress"}
    }
  }
}
```

### Business Directory

```json
{
  "businesses": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "company.name"},
      "phone": {"gen": "phone"},
      "fax": {"gen": "phone"},
      "extension": {"gen": "phone", "format": "extension"}
    }
  }
}
```

### User Profiles

```json
{
  "users": {
    "count": 200,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "email": {"gen": "internet.emailAddress"},
      "homePhone": {"gen": "phone"},
      "workPhone": {"gen": "phone"},
      "mobile": {"gen": "phone", "format": "mobile"}
    }
  }
}
```

## Best Practices

1. **Use Appropriate Format**: Match format to use case
2. **International for Global**: Use international format for multi-country data
3. **Mobile for Cells**: Use cell/mobile format for mobile numbers
4. **Multiple Numbers**: Generate different types for realistic data

## Next Steps

- [Internet Generator](./internet.md) - For emails and URLs
- [Address Generator](./address.md) - For addresses
- [Name Generator](./name.md) - For names
