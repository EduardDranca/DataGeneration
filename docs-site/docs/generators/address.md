# Address Generator

Generates address-related data including street addresses, cities, states, ZIP codes, and countries.

## Basic Usage

```json
{
  "address": {"gen": "address"}
}
```

**Output:** Returns an object with all address fields.

## Available Fields

| Field | Description | Example |
|-------|-------------|---------|
| `streetAddress` | Street address | `123 Main St` |
| `city` | City name | `New York` |
| `state` | State/province | `NY` |
| `zipCode` | ZIP/postal code | `10001` |
| `country` | Country name | `United States` |
| `countryCode` | Country code | `US` |
| `fullAddress` | Complete address | `123 Main St, New York, NY 10001` |

## Options

This generator has **no options**.

## Examples

### Basic Address Fields

```json
{
  "users": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "street": {"gen": "address.streetAddress"},
      "city": {"gen": "address.city"},
      "state": {"gen": "address.state"},
      "zip": {"gen": "address.zipCode"}
    }
  }
}
```

### Full Address

```json
{
  "locations": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "address": {"gen": "address.fullAddress"}
    }
  }
}
```

### International Addresses

```json
{
  "offices": {
    "count": 15,
    "item": {
      "id": {"gen": "uuid"},
      "street": {"gen": "address.streetAddress"},
      "city": {"gen": "address.city"},
      "country": {"gen": "address.country"},
      "countryCode": {"gen": "address.countryCode"}
    }
  }
}
```

## Common Patterns

### Shipping Address

```json
{
  "orders": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "shippingAddress": {
        "street": {"gen": "address.streetAddress"},
        "city": {"gen": "address.city"},
        "state": {"gen": "address.state"},
        "zipCode": {"gen": "address.zipCode"},
        "country": {"gen": "address.country"}
      }
    }
  }
}
```

### Store Locations

```json
{
  "stores": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "company.name"},
      "address": {"gen": "address.fullAddress"},
      "city": {"gen": "address.city"},
      "state": {"gen": "address.state"}
    }
  }
}
```

## Next Steps

- [Country Generator](./country.md) - More detailed country information
- [Company Generator](./company.md) - Generate company data
