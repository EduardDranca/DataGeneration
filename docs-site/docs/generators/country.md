# Country Generator

Generates country-related data including names, codes, capitals, and currencies.

## Basic Usage

```json
{
  "country": {"gen": "country"}
}
```

**Output:** Returns an object with all country fields.

## Available Fields

| Field | Description | Example |
|-------|-------------|---------|
| `name` | Country name | `United States` |
| `countryCode` | 2-letter country code | `US` |
| `capital` | Capital city | `Washington D.C.` |
| `currency` | Currency name | `US Dollar` |
| `currencyCode` | Currency code | `USD` |

## Options

This generator has **no options**.

## Examples

### Country Name

```json
{
  "country": {"gen": "country.name"}
}
```

### Country Code

```json
{
  "code": {"gen": "country.countryCode"}
}
```

### Capital City

```json
{
  "capital": {"gen": "country.capital"}
}
```

### Currency

```json
{
  "currency": {"gen": "country.currency"},
  "currencyCode": {"gen": "country.currencyCode"}
}
```

## Common Patterns

### International Addresses

```json
{
  "addresses": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "street": {"gen": "address.streetAddress"},
      "city": {"gen": "address.city"},
      "country": {"gen": "country.name"},
      "countryCode": {"gen": "country.countryCode"}
    }
  }
}
```

### Currency Exchange

```json
{
  "rates": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "fromCurrency": {"gen": "country.currencyCode"},
      "toCurrency": {"gen": "country.currencyCode"},
      "rate": {"gen": "float", "min": 0.1, "max": 10.0, "decimals": 4}
    }
  }
}
```

### Travel Destinations

```json
{
  "destinations": {
    "count": 30,
    "item": {
      "id": {"gen": "uuid"},
      "country": {"gen": "country.name"},
      "capital": {"gen": "country.capital"},
      "description": {"gen": "lorem.paragraph"}
    }
  }
}
```

## Next Steps

- [Address Generator](./address.md) - For addresses
- [Company Generator](./company.md) - For company data
