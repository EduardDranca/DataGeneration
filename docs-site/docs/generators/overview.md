# Generators Overview

DataGeneration includes 18 built-in generators for creating realistic test data.

## Data Generators

Generate realistic domain-specific data:

| Generator | Description | Example Output |
|-----------|-------------|----------------|
| [UUID](./uuid.md) | UUID v4 strings | `550e8400-e29b-41d4-a716-446655440000` |
| [Name](./name.md) | Names and titles | `John Doe`, `Dr.`, `Smith` |
| [Internet](./internet.md) | Email, URLs, domains | `john@example.com`, `https://example.com` |
| [Address](./address.md) | Addresses and locations | `123 Main St`, `New York`, `10001` |
| [Company](./company.md) | Company names and industries | `Acme Corp`, `Technology` |
| [Country](./country.md) | Countries and codes | `United States`, `US`, `USD` |
| [Book](./book.md) | Book titles, authors, genres | `The Great Gatsby`, `F. Scott Fitzgerald` |
| [Finance](./finance.md) | IBAN, BIC, credit cards | `DE89370400440532013000`, `DEUTDEFF` |
| [Phone](./phone.md) | Phone numbers | `+1-555-123-4567` |

## Primitive Generators

Generate basic data types:

| Generator | Description | Options |
|-----------|-------------|---------|
| [Number](./number.md) | Random integers | `min`, `max` |
| [Float](./float.md) | Floating-point numbers | `min`, `max`, `decimals` |
| [Boolean](./boolean.md) | True/false values | `probability` |
| [String](./string.md) | Random strings | `length`, `allowedChars`, `regex` |
| [Date](./date.md) | Dates and timestamps | `from`, `to`, `format` |

## Utility Generators

Special-purpose generators:

| Generator | Description | Use Case |
|-----------|-------------|----------|
| [Lorem](./lorem.md) | Lorem ipsum text | Placeholder content |
| [Sequence](./sequence.md) | Auto-incrementing numbers | IDs, counters |
| [Choice](./choice.md) | Pick from options | Enums, statuses |
| [CSV](./csv.md) | Read from CSV files | External data |

## Usage Patterns

### Basic Generator

```json
{
  "id": {"gen": "uuid"},
  "name": {"gen": "name.fullName"}
}
```

### Generator with Options

```json
{
  "age": {"gen": "number", "min": 18, "max": 65},
  "price": {"gen": "float", "min": 9.99, "max": 999.99, "decimals": 2}
}
```

### Accessing Sub-fields

Some generators return objects with multiple fields:

```json
{
  "firstName": {"gen": "name.firstName"},
  "lastName": {"gen": "name.lastName"},
  "fullName": {"gen": "name.fullName"}
}
```

### Weighted Choices

```json
{
  "priority": {
    "gen": "choice",
    "options": ["low", "medium", "high", "critical"],
    "weights": [50, 30, 15, 5]
  }
}
```

## Custom Generators

You can create custom generators by implementing the `Generator` interface:

```java
public class CustomGenerator implements Generator {
    @Override
    public Object generate(GeneratorContext context) {
        // Your generation logic
        return "custom value";
    }
}

// Register it
DslDataGenerator.create()
    .registerGenerator("custom", new CustomGenerator())
    .fromJsonString(dsl)
    .generate();
```

Then use it in your DSL:

```json
{
  "customField": {"gen": "custom"}
}
```

## Generator Compatibility

### Filtering Support

Only the **Boolean** generator currently supports filtering:

```json
{
  "isActive": {
    "gen": "boolean",
    "probability": 0.8,
    "filter": [false]  // Only generate true values
  }
}
```

For other generators, use the Choice generator with filtering:

```json
{
  "status": {
    "gen": "choice",
    "options": ["active", "inactive", "banned"],
    "filter": ["banned"]
  }
}
```

### Sequential Support

Generators that support sequential access:
- **Sequence** - Built-in sequential behavior
- **CSV** - Can read sequentially with `"sequential": true`
- **Choice** - Can cycle through options sequentially

## Next Steps

- Explore individual generator documentation
- [Learn about References](../dsl-reference/references.md)
- [See Common Patterns](../guides/overview.md)
