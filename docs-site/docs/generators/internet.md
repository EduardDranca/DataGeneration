# Internet Generator

Generates internet-related data including emails, URLs, domains, and usernames.

## Basic Usage

```json
{
  "internet": {"gen": "internet"}
}
```

**Output:** Returns an object with all internet fields:
```json
{
  "emailAddress": "john.doe@example.com",
  "domainName": "example.com",
  "url": "https://www.example.com",
  "username": "john.doe"
}
```

## Available Fields

Access specific fields using dot notation:

| Field | Description | Example |
|-------|-------------|---------|
| `emailAddress` | Email address | `john.doe@example.com` |
| `domainName` | Domain name | `example.com` |
| `url` | Full URL | `https://www.example.com` |
| `username` | Username | `john.doe` |

## Options

This generator has **no options**.

## Examples

### Email Address

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "email": {"gen": "internet.emailAddress"}
    }
  }
}
```

### Website URL

```json
{
  "companies": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "company.name"},
      "website": {"gen": "internet.url"}
    }
  }
}
```

### Username

```json
{
  "accounts": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "username": {"gen": "internet.username"},
      "email": {"gen": "internet.emailAddress"}
    }
  }
}
```

### Domain Name

```json
{
  "websites": {
    "count": 30,
    "item": {
      "id": {"gen": "uuid"},
      "domain": {"gen": "internet.domainName"},
      "url": {"gen": "internet.url"}
    }
  }
}
```

## Common Patterns

### User Registration

```json
{
  "users": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "firstName": {"gen": "name.firstName"},
      "lastName": {"gen": "name.lastName"},
      "email": {"gen": "internet.emailAddress"},
      "username": {"gen": "internet.username"}
    }
  }
}
```

### Contact Information

```json
{
  "contacts": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "email": {"gen": "internet.emailAddress"},
      "website": {"gen": "internet.url"},
      "phone": {"gen": "phone.phoneNumber"}
    }
  }
}
```

### Company Directory

```json
{
  "companies": {
    "count": 30,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "company.name"},
      "domain": {"gen": "internet.domainName"},
      "website": {"gen": "internet.url"},
      "contactEmail": {"gen": "internet.emailAddress"}
    }
  }
}
```

## Best Practices

1. **Use Specific Fields**: Access only the fields you need
2. **Email for Users**: Use `emailAddress` for user accounts
3. **URL for Links**: Use `url` for website links
4. **Domain for DNS**: Use `domainName` for domain-specific data

## Next Steps

- [Name Generator](./name.md) - Generate names for users
- [Company Generator](./company.md) - Generate company information
- [Phone Generator](./phone.md) - Generate phone numbers
