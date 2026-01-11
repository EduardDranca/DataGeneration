# Company Generator

Generates company-related data including names, industries, and professions.

## Basic Usage

```json
{
  "company": {"gen": "company"}
}
```

**Output:** Returns an object with all company fields.

## Available Fields

| Field | Description | Example |
|-------|-------------|---------|
| `name` | Company name | `Acme Corporation` |
| `industry` | Industry type | `Technology` |
| `profession` | Profession/job title | `Software Engineer` |
| `buzzword` | Business buzzword | `synergy` |

## Options

This generator has **no options**.

## Examples

### Company Name

```json
{
  "companyName": {"gen": "company.name"}
}
```

### Industry

```json
{
  "industry": {"gen": "company.industry"}
}
```

### Profession

```json
{
  "jobTitle": {"gen": "company.profession"}
}
```

### Buzzword

```json
{
  "buzzword": {"gen": "company.buzzword"}
}
```

## Common Patterns

### Company Directory

```json
{
  "companies": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "company.name"},
      "industry": {"gen": "company.industry"},
      "website": {"gen": "internet.url"},
      "email": {"gen": "internet.emailAddress"}
    }
  }
}
```

### Employee Records

```json
{
  "employees": {
    "count": 200,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "jobTitle": {"gen": "company.profession"},
      "company": {"gen": "company.name"},
      "email": {"gen": "internet.emailAddress"}
    }
  }
}
```

### Job Listings

```json
{
  "jobs": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "title": {"gen": "company.profession"},
      "company": {"gen": "company.name"},
      "industry": {"gen": "company.industry"},
      "description": {"gen": "lorem.paragraph"}
    }
  }
}
```

## Next Steps

- [Name Generator](./name.md) - For person names
- [Internet Generator](./internet.md) - For emails and URLs
- [Address Generator](./address.md) - For addresses
