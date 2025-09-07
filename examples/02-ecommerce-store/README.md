# E-commerce Store Example

This example demonstrates a complete e-commerce data model with relationships between entities.

## What it generates

- **3 product categories** (Electronics, Clothing, Books)
- **8 products** with pricing and inventory
- **5 customers** with addresses
- **10 orders** linking customers to products

## Key features demonstrated

- **Entity relationships** using references (`ref`)
- **Complex objects** (nested address structure)
- **Commerce generators** for product names and pricing
- **Choice generators** with predefined options
- **Cross-collection references** (orders → customers → products)

## Data model

```
Categories (3) ← Products (8) ← Orders (10) → Customers (5)
```

## Use cases

- Testing e-commerce applications
- Database seeding for online stores
- Performance testing with realistic data
- API testing with complete product catalogs
