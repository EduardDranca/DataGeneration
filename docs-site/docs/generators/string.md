# String Generator

Generates random strings with configurable length and character sets.

## Basic Usage

```json
{
  "code": {"gen": "string"}
}
```

**Output:** Random alphanumeric string (default length 1-20 characters)

## Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `length` | integer | - | Exact string length (overrides min/max) |
| `minLength` | integer | 1 | Minimum length |
| `maxLength` | integer | 20 | Maximum length |
| `allowedChars` | string | alphanumeric | Characters to use |
| `regex` | string | - | Regex pattern to generate from (overrides other options) |

## Examples

### Fixed Length

```json
{
  "code": {"gen": "string", "length": 10}
}
```

**Output:** Exactly 10 characters (e.g., `aB3xK9mP2q`)

### Variable Length

```json
{
  "description": {"gen": "string", "minLength": 5, "maxLength": 15}
}
```

**Output:** Between 5 and 15 characters

### Custom Characters

```json
{
  "hexCode": {"gen": "string", "length": 6, "allowedChars": "0123456789ABCDEF"}
}
```

**Output:** 6-character hex string (e.g., `A3F2B9`)

### Uppercase Only

```json
{
  "code": {"gen": "string", "length": 8, "allowedChars": "ABCDEFGHIJKLMNOPQRSTUVWXYZ"}
}
```

### Numbers Only

```json
{
  "pin": {"gen": "string", "length": 4, "allowedChars": "0123456789"}
}
```

**Output:** 4-digit PIN (e.g., `7392`)

### Regex Pattern

```json
{
  "productCode": {"gen": "string", "regex": "[A-Z]{3}-[0-9]{4}"}
}
```

**Output:** Pattern like `ABC-1234`

### License Plate

```json
{
  "licensePlate": {"gen": "string", "regex": "[A-Z]{2}[0-9]{2}[A-Z]{3}"}
}
```

**Output:** Pattern like `AB12XYZ`

## Common Patterns

### Product Codes

```json
{
  "products": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "sku": {"gen": "string", "regex": "[A-Z]{3}-[0-9]{5}"},
      "barcode": {"gen": "string", "length": 13, "allowedChars": "0123456789"}
    }
  }
}
```

### Access Codes

```json
{
  "accessCodes": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "code": {"gen": "string", "length": 8, "allowedChars": "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"},
      "pin": {"gen": "string", "length": 6, "allowedChars": "0123456789"}
    }
  }
}
```

### Tracking Numbers

```json
{
  "shipments": {
    "count": 200,
    "item": {
      "id": {"gen": "uuid"},
      "trackingNumber": {"gen": "string", "regex": "[A-Z]{2}[0-9]{9}[A-Z]{2}"}
    }
  }
}
```

### Voucher Codes

```json
{
  "vouchers": {
    "count": 1000,
    "item": {
      "id": {"gen": "uuid"},
      "code": {"gen": "string", "length": 12, "allowedChars": "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"}
    }
  }
}
```

### Color Codes

```json
{
  "colors": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "hex": {"gen": "string", "length": 6, "allowedChars": "0123456789ABCDEF"}
    }
  }
}
```

## Best Practices

1. **Use Regex for Patterns**: Regex is powerful for specific formats
2. **Exclude Ambiguous Characters**: For codes, exclude O/0, I/1, etc.
3. **Fixed Length for Codes**: Use `length` for consistent code formats
4. **Appropriate Character Sets**: Match your use case (hex, alphanumeric, etc.)
5. **Consider Readability**: For user-facing codes, avoid confusing characters

## Regex Patterns

Common regex patterns:

```json
// Product code: ABC-1234
{"regex": "[A-Z]{3}-[0-9]{4}"}

// Phone format: (123) 456-7890
{"regex": "\\([0-9]{3}\\) [0-9]{3}-[0-9]{4}"}

// Date format: 2024-01-15
{"regex": "20[0-9]{2}-[0-1][0-9]-[0-3][0-9]"}

// Hex color: #A3F2B9
{"regex": "#[0-9A-F]{6}"}

// Email-like: user123@domain.com
{"regex": "[a-z]{4,8}[0-9]{1,3}@[a-z]{5,10}\\.com"}
```

## Next Steps

- [Lorem Generator](./lorem.md) - For placeholder text
- [UUID Generator](./uuid.md) - For unique identifiers
- [Number Generator](./number.md) - For numeric values
