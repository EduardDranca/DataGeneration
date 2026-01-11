# Date Generator

Generates random dates and timestamps within a specified range.

## Basic Usage

```json
{
  "createdAt": {"gen": "date"}
}
```

**Output:** Random date between 1970-01-01 and one year from now in ISO format

## Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `from` | string | `"1970-01-01"` | Start date (ISO format) |
| `to` | string | One year from now | End date (ISO format) |
| `format` | string | `"iso"` | Output format |

## Format Options

| Format | Description | Example Output |
|--------|-------------|----------------|
| `iso` | ISO date (YYYY-MM-DD) | `2024-03-15` |
| `iso_datetime` | ISO datetime | `2024-03-15T14:30:00Z` |
| `timestamp` | Unix timestamp (milliseconds) | `1710512400000` |
| `epoch` | Unix timestamp (seconds) | `1710512400` |
| Custom pattern | Java date pattern | `dd/MM/yyyy` → `15/03/2024` |

## Examples

### Basic Date Range

```json
{
  "birthDate": {
    "gen": "date",
    "from": "1950-01-01",
    "to": "2005-12-31"
  }
}
```

### Recent Dates

```json
{
  "createdAt": {
    "gen": "date",
    "from": "2024-01-01",
    "to": "2024-12-31"
  }
}
```

### ISO Datetime

```json
{
  "timestamp": {
    "gen": "date",
    "from": "2024-01-01",
    "to": "2024-12-31",
    "format": "iso_datetime"
  }
}
```

**Output:** `2024-06-15T14:30:45Z`

### Unix Timestamp

```json
{
  "createdAt": {
    "gen": "date",
    "from": "2024-01-01",
    "to": "2024-12-31",
    "format": "timestamp"
  }
}
```

**Output:** `1718462445000`

### Epoch Seconds

```json
{
  "createdAt": {
    "gen": "date",
    "from": "2024-01-01",
    "to": "2024-12-31",
    "format": "epoch"
  }
}
```

**Output:** `1718462445`

### Custom Format

```json
{
  "displayDate": {
    "gen": "date",
    "from": "2024-01-01",
    "to": "2024-12-31",
    "format": "dd/MM/yyyy"
  }
}
```

**Output:** `15/06/2024`

### US Format

```json
{
  "date": {
    "gen": "date",
    "from": "2024-01-01",
    "to": "2024-12-31",
    "format": "MM-dd-yyyy"
  }
}
```

**Output:** `06-15-2024`

## Common Patterns

### User Records

```json
{
  "users": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "birthDate": {
        "gen": "date",
        "from": "1960-01-01",
        "to": "2005-12-31",
        "format": "iso"
      },
      "createdAt": {
        "gen": "date",
        "from": "2020-01-01",
        "to": "2024-12-31",
        "format": "iso_datetime"
      }
    }
  }
}
```

### Event Logs

```json
{
  "events": {
    "count": 1000,
    "item": {
      "id": {"gen": "uuid"},
      "eventType": {"gen": "choice", "options": ["login", "logout", "purchase"]},
      "timestamp": {
        "gen": "date",
        "from": "2024-01-01",
        "to": "2024-12-31",
        "format": "timestamp"
      }
    }
  }
}
```

### Orders

```json
{
  "orders": {
    "count": 500,
    "item": {
      "id": {"gen": "uuid"},
      "orderDate": {
        "gen": "date",
        "from": "2024-01-01",
        "to": "2024-12-31",
        "format": "iso"
      },
      "deliveryDate": {
        "gen": "date",
        "from": "2024-01-01",
        "to": "2024-12-31",
        "format": "iso"
      }
    }
  }
}
```

### Time Series Data

```json
{
  "measurements": {
    "count": 1000,
    "item": {
      "id": {"gen": "uuid"},
      "timestamp": {
        "gen": "date",
        "from": "2024-01-01T00:00:00",
        "to": "2024-01-31T23:59:59",
        "format": "iso_datetime"
      },
      "value": {"gen": "float", "min": 0, "max": 100, "decimals": 2}
    }
  }
}
```

## Custom Date Patterns

Common Java date format patterns:

| Pattern | Description | Example |
|---------|-------------|---------|
| `yyyy-MM-dd` | ISO date | `2024-03-15` |
| `dd/MM/yyyy` | European format | `15/03/2024` |
| `MM-dd-yyyy` | US format | `03-15-2024` |
| `yyyy-MM-dd HH:mm:ss` | Datetime | `2024-03-15 14:30:45` |
| `dd MMM yyyy` | Month name | `15 Mar 2024` |
| `EEEE, MMMM dd, yyyy` | Full format | `Friday, March 15, 2024` |

## Best Practices

1. **Use ISO Format**: Default to ISO for consistency
2. **Realistic Ranges**: Choose date ranges that match your use case
3. **Timestamps for Logs**: Use `timestamp` or `epoch` for event logs
4. **Custom Formats**: Use custom patterns for display dates
5. **Consider Timezones**: ISO datetime includes timezone (Z for UTC)

## Next Steps

- [Number Generator](./number.md) - For numeric values
- [String Generator](./string.md) - For text values
- [Sequence Generator](./sequence.md) - For sequential IDs
