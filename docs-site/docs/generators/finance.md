# Finance Generator

Generates financial data including IBAN numbers, BIC codes, and credit card numbers.

## Basic Usage

```json
{
  "finance": {"gen": "finance"}
}
```

**Output:** Returns an object with all finance fields:
```json
{
  "iban": "DE89370400440532013000",
  "bic": "DEUTDEFF",
  "creditCard": "4532015112830366"
}
```

## Available Fields

Access specific fields using dot notation:

| Field | Description | Example |
|-------|-------------|---------|
| `iban` | International Bank Account Number | `DE89370400440532013000` |
| `bic` | Bank Identifier Code (SWIFT) | `DEUTDEFF` |
| `creditCard` | Credit card number | `4532015112830366` |

## Options

This generator has **no options**.

## Examples

### Single Field

```json
{
  "accountNumber": {"gen": "finance.iban"}
}
```

**Output:** `"DE89370400440532013000"`

### Multiple Fields

```json
{
  "iban": {"gen": "finance.iban"},
  "bic": {"gen": "finance.bic"},
  "card": {"gen": "finance.creditCard"}
}
```

### Full Finance Object

```json
{
  "bankingInfo": {"gen": "finance"}
}
```

**Output:**
```json
{
  "bankingInfo": {
    "iban": "GB82WEST12345698765432",
    "bic": "NWBKGB2L",
    "creditCard": "5425233430109903"
  }
}
```

## Common Patterns

### Bank Accounts

```json
{
  "accounts": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "userId": {"gen": "uuid"},
      "iban": {"gen": "finance.iban"},
      "bic": {"gen": "finance.bic"},
      "balance": {"gen": "float", "min": 0, "max": 100000, "decimals": 2},
      "currency": {"gen": "choice", "options": ["USD", "EUR", "GBP"]},
      "accountType": {"gen": "choice", "options": ["checking", "savings"]}
    }
  }
}
```

### Payment Methods

```json
{
  "users": {
    "count": 50,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "email": {"gen": "internet.emailAddress"}
    }
  },
  "paymentMethods": {
    "count": 150,
    "item": {
      "id": {"gen": "uuid"},
      "userId": {"ref": "users[*].id"},
      "type": {"gen": "choice", "options": ["credit_card", "bank_account"]},
      "cardNumber": {"gen": "finance.creditCard"},
      "iban": {"gen": "finance.iban"},
      "isDefault": {"gen": "boolean", "probability": 0.3}
    }
  }
}
```

### Transactions

```json
{
  "accounts": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "iban": {"gen": "finance.iban"},
      "owner": {"gen": "name.fullName"}
    }
  },
  "transactions": {
    "count": 1000,
    "item": {
      "id": {"gen": "uuid"},
      "fromAccount": {"ref": "accounts[*].iban"},
      "toAccount": {"ref": "accounts[*].iban"},
      "amount": {"gen": "float", "min": 10, "max": 10000, "decimals": 2},
      "currency": {"gen": "choice", "options": ["USD", "EUR", "GBP"]},
      "date": {"gen": "date", "from": "2024-01-01", "to": "2024-12-31"},
      "status": {"gen": "choice", "options": ["pending", "completed", "failed"], "weights": [10, 85, 5]}
    }
  }
}
```

### Credit Cards

```json
{
  "creditCards": {
    "count": 200,
    "item": {
      "id": {"gen": "uuid"},
      "cardNumber": {"gen": "finance.creditCard"},
      "cardHolder": {"gen": "name.fullName"},
      "expiryMonth": {"gen": "number", "min": 1, "max": 12},
      "expiryYear": {"gen": "number", "min": 2024, "max": 2030"},
      "cvv": {"gen": "string", "regex": "[0-9]{3}"},
      "cardType": {"gen": "choice", "options": ["Visa", "Mastercard", "Amex"]},
      "creditLimit": {"gen": "float", "min": 1000, "max": 50000, "decimals": 2}
    }
  }
}
```

### Banking System

```json
{
  "customers": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "email": {"gen": "internet.emailAddress"},
      "phone": {"gen": "phone.phoneNumber"}
    }
  },
  "accounts": {
    "count": 250,
    "item": {
      "id": {"gen": "uuid"},
      "customerId": {"ref": "customers[*].id"},
      "iban": {"gen": "finance.iban"},
      "bic": {"gen": "finance.bic"},
      "accountType": {"gen": "choice", "options": ["checking", "savings", "business"]},
      "balance": {"gen": "float", "min": 0, "max": 500000, "decimals": 2},
      "openedDate": {"gen": "date", "from": "2020-01-01", "to": "2024-12-31"}
    }
  }
}
```

### E-commerce Payments

```json
{
  "orders": {
    "count": 500,
    "item": {
      "id": {"gen": "uuid"},
      "customerId": {"gen": "uuid"},
      "total": {"gen": "float", "min": 10, "max": 1000, "decimals": 2},
      "paymentMethod": {"gen": "choice", "options": ["card", "bank_transfer"]},
      "cardNumber": {"gen": "finance.creditCard"},
      "iban": {"gen": "finance.iban"},
      "status": {"gen": "choice", "options": ["pending", "paid", "failed"], "weights": [10, 85, 5]},
      "orderDate": {"gen": "date"}
    }
  }
}
```

## Best Practices

1. **Use Specific Fields**: Access only the fields you need (e.g., `finance.iban`)
2. **Combine with Choice**: Use choice generator for payment types, currencies, statuses
3. **Add Validation Fields**: Combine with string generator for CVV, expiry dates
4. **Create Relationships**: Link accounts to users, transactions to accounts
5. **Realistic Amounts**: Use float generator with appropriate min/max for balances and amounts

## Security Note

⚠️ **Important**: The generated financial data is for **testing purposes only**. These are randomly generated numbers that may not pass real validation algorithms. Do not use this data for:
- Production systems
- Real financial transactions
- Fraud testing without proper authorization
- Any system that processes real money

## Use Cases

- **Banking Applications**: Generate test accounts and transactions
- **Payment Systems**: Create payment method test data
- **E-commerce**: Generate order and payment data
- **Financial Reporting**: Create test data for reports and analytics
- **Testing**: Populate test databases with realistic financial data

## Comparison with Other Generators

| Generator | Use Case |
|-----------|----------|
| **Finance** | Banking and payment data |
| [Number](./number.md) | Generic numeric values |
| [Float](./float.md) | Decimal amounts (prices, balances) |
| [String](./string.md) | Custom formatted strings |
| [UUID](./uuid.md) | Unique identifiers |

## Next Steps

- [Float Generator](./float.md) - For monetary amounts
- [String Generator](./string.md) - For custom financial codes
- [References](../dsl-reference/references.md) - Link accounts to users
