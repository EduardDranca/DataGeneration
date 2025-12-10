# Shadow Bindings Example

This example demonstrates how to use shadow bindings to create cross-entity constraints without polluting the output.

## Overview

Shadow bindings are fields prefixed with `$` that are resolved during generation but excluded from the output. They allow you to:

- **Bind references**: Store a referenced item for reuse within the same item
- **Extract fields**: Access multiple fields from the bound item
- **Cross-entity constraints**: Use bound values in conditional references
- **Clean output**: Keep intermediate values out of generated data

## Features Demonstrated

### 1. Basic Shadow Binding
```json
{
  "orders": {
    "count": 100,
    "item": {
      "$user": {"ref": "users[*]"},
      "userId": {"ref": "$user.id"},
      "userName": {"ref": "$user.name"}
    }
  }
}
```
- `$user` binds a random user (not in output)
- `$user.id` and `$user.name` extract fields from the same user
- Output only contains `userId` and `userName`

### 2. Geographic Restrictions
```json
{
  "orders": {
    "count": 100,
    "item": {
      "$user": {"ref": "users[*]"},
      "userId": {"ref": "$user.id"},
      "productId": {"ref": "products[regionId=$user.regionId].id"}
    }
  }
}
```
- Products are filtered to match the user's region
- Ensures orders only contain products available in the user's area

### 3. Multiple Conditions with Shadow Bindings
```json
{
  "personalizedOrders": {
    "count": 50,
    "item": {
      "$user": {"ref": "users[*]"},
      "userId": {"ref": "$user.id"},
      "productId": {"ref": "products[regionId=$user.regionId and category=$user.preferredCategory].id"}
    }
  }
}
```
- Combines region matching with category preferences
- Products must match both the user's region AND preferred category

### 4. Multiple Shadow Bindings
```json
{
  "shipments": {
    "count": 80,
    "item": {
      "$user": {"ref": "users[*]"},
      "$warehouse": {"ref": "warehouses[regionId=$user.regionId]"},
      "userId": {"ref": "$user.id"},
      "warehouseId": {"ref": "$warehouse.id"},
      "warehouseName": {"ref": "$warehouse.name"}
    }
  }
}
```
- Chain multiple bindings for complex constraints
- `$warehouse` depends on `$user.regionId`
- Both bindings are excluded from output

### 5. Self-Exclusion Pattern
```json
{
  "friendships": {
    "count": 50,
    "item": {
      "$person": {"ref": "users[*]"},
      "userId": {"ref": "$person.id"},
      "friendId": {"ref": "users[id!=$person.id].id"}
    }
  }
}
```
- Ensures a user cannot be friends with themselves
- Uses `!=` operator with shadow binding reference

## Running the Example

```bash
# From project root
mvn clean compile
mvn exec:java -Dexec.mainClass="examples.ShadowBindingsExample"
```

## Use Cases

### E-commerce
- **Regional inventory**: Products available in user's region
- **Personalized recommendations**: Match user preferences
- **Local warehouses**: Ship from nearest warehouse

### Social Networks
- **Friend suggestions**: Exclude self from friend list
- **Mutual connections**: Find friends of friends
- **Regional groups**: Match users by location

### HR/Assignments
- **Skill matching**: Assign employees to projects by skill
- **Department constraints**: Keep assignments within department
- **Manager exclusion**: Employees can't report to themselves

### Logistics
- **Route optimization**: Match drivers to local deliveries
- **Warehouse selection**: Pick warehouse in customer's region
- **Carrier matching**: Use carriers that serve the destination

## Syntax Rules

1. **Prefix**: Shadow bindings must start with `$`
2. **Order**: Must be defined before use (top-to-bottom)
3. **Scope**: Available only within the same `item` block
4. **Access**: Use `$name.field` or `$name.nested.field`
5. **Output**: Never included in generated data

## Important Notes

- Shadow bindings work with both eager and lazy generation modes
- Bindings must be defined before they're referenced
- Nested field access is supported: `$user.profile.settings.theme`
- Can be combined with all conditional operators: `=`, `!=`, `<`, `>`, `<=`, `>=`, `and`, `or`

## Related Examples

- [09-conditional-references](../09-conditional-references/) - Conditional filtering without bindings
- [10-runtime-computed-options](../10-runtime-computed-options/) - Dynamic generator options
