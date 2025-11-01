# Conditional References Example

This example demonstrates how to use conditional references to filter collection items based on field conditions before referencing them.

## Overview

Conditional references allow you to enforce business rules and data constraints by filtering collections based on field values. This is useful for:

- **Business Logic**: Only reference active users, in-stock products, eligible employees
- **Data Integrity**: Ensure age restrictions, status requirements, balance checks
- **Realistic Data**: Create data that follows real-world constraints and relationships

## Features Demonstrated

### 1. Basic Equality Conditions
```json
{
  "orders": {
    "count": 50,
    "item": {
      "userId": {"ref": "users[status='active'].id"}
    }
  }
}
```
Only references users with `status='active'`.

### 2. Numeric Comparisons
```json
{
  "orders": {
    "count": 50,
    "item": {
      "productId": {"ref": "products[stock>0].id"}
    }
  }
}
```
Only references products with stock greater than 0.

**Supported operators:**
- `=` - Equals
- `!=` - Not equals
- `<` - Less than
- `<=` - Less than or equal
- `>` - Greater than
- `>=` - Greater than or equal

### 3. AND Operator
```json
{
  "premiumOrders": {
    "count": 15,
    "item": {
      "userId": {"ref": "users[isPremium=true and status='active'].id"}
    }
  }
}
```
References users who are **both** premium **and** active.

### 4. OR Operator
```json
{
  "promotions": {
    "count": 10,
    "item": {
      "productId": {"ref": "products[featured=true or rating>=4.5].id"}
    }
  }
}
```
References products that are **either** featured **or** highly rated.

### 5. Complex Multi-Condition Filters
```json
{
  "ageRestrictedOrders": {
    "count": 10,
    "item": {
      "userId": {"ref": "users[age>=21 and status='active'].id"},
      "productId": {"ref": "products[category='electronics' and price>200].id"}
    }
  }
}
```
Multiple conditions on both users and products.

## Running the Example

### Compile and Run
```bash
# From project root
mvn clean compile

# Run the example
mvn exec:java -Dexec.mainClass="examples.ConditionalReferencesExample"
```

### Expected Output
The example will demonstrate:
1. Basic equality filtering (active users only)
2. Numeric comparisons (in-stock products, low stock alerts)
3. AND operator (premium active users, expensive well-stocked products)
4. OR operator (featured or highly-rated products)
5. Complex conditions (age restrictions with multiple criteria)
6. Multiple conditional collections with different business rules

## Use Cases

### E-commerce
- **Active Users Only**: `users[status='active'].id`
- **In-Stock Products**: `products[stock>0].id`
- **Premium Products**: `products[price>100 and rating>=4.0].id`
- **Featured Items**: `products[featured=true or discount>0].id`

### HR/Employee Management
- **Eligible for Bonus**: `employees[salary>80000 and yearsOfService>=5].id`
- **Active Employees**: `employees[status='active' and department='engineering'].id`
- **Senior Staff**: `employees[level='senior' or yearsOfService>=10].id`

### Financial Systems
- **Eligible for Loan**: `customers[creditScore>=700 and accountBalance>5000].id`
- **High-Value Accounts**: `accounts[balance>100000].id`
- **Active Accounts**: `accounts[status='active' and lastActivity<30].id`

### Content Management
- **Published Articles**: `articles[status='published' and views>1000].id`
- **Popular Content**: `content[likes>100 or shares>50].id`
- **Recent Posts**: `posts[status='published' and age<=7].id`

## Value Types in Conditions

### Strings
Use single quotes:
```json
{"ref": "users[status='active'].id"}
{"ref": "products[category='electronics'].id"}
```

### Numbers
No quotes:
```json
{"ref": "products[price>100].id"}
{"ref": "users[age>=21].id"}
{"ref": "items[stock<=10].id"}
```

### Booleans
No quotes:
```json
{"ref": "users[isPremium=true].id"}
{"ref": "products[featured=false].id"}
```

### Null
No quotes:
```json
{"ref": "users[middleName=null].id"}
```

## Important Notes

1. **FilteringException**: If no items match the condition, a `FilteringException` is thrown
2. **Sequential Support**: Can combine with `"sequential": true` for ordered selection
3. **Filter Array**: Can combine with `"filter"` array for additional filtering
4. **Both Modes**: Works with both eager and lazy generation modes
5. **Operator Precedence**: Currently no parentheses support; use multiple references if needed

## DSL File

See [dsl.json](dsl.json) for the complete DSL definition with all conditional reference examples.

## Related Examples

- [02-ecommerce-store](../02-ecommerce-store/) - Basic references without conditions
- [03-company-employees](../03-company-employees/) - Complex relationships
- [04-memory-optimization](../04-memory-optimization/) - Large datasets with conditions

## Documentation

For complete documentation on conditional references, see:
- [DSL Reference Guide](../../.kiro/steering/dsl-reference.md#conditional-references)
- [Main README](../../README.md#conditional-references)
