# Financial Transactions Example

This example demonstrates a banking system with accounts, merchants, and transactions.

## What it generates

- **3 banks** with routing numbers and SWIFT codes
- **12 accounts** with different types and balances
- **8 merchants** across various categories
- **50 transactions** with detailed financial data

## Key features demonstrated

- **Financial data generators** (IBAN, routing numbers, credit cards)
- **Decimal precision** for monetary amounts
- **Transaction types** and status tracking
- **Regulatory compliance** data (reference numbers)
- **High-volume transaction simulation**

## Data model

```
Banks (3) ← Accounts (12) ← Transactions (50) → Merchants (8)
```

## Use cases

- Banking application testing
- Payment processor development
- Fraud detection system training
- Financial reporting tools
- Compliance testing
- Performance testing with transaction volumes

## Security note

All generated financial data is fake and for testing purposes only. Do not use in production systems.
