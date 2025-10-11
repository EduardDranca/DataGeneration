# Welcome to DataGeneration

**DataGeneration** is a Java library for generating complex, realistic test data using a declarative JSON DSL. Create interconnected datasets with relationships, filtering, and advanced constraints for testing, development, and data seeding scenarios.

## Why DataGeneration?

- **Declarative DSL**: Define complex data structures in simple JSON
- **Built-in Generators**: 18 generators for common data types (names, emails, addresses, UUIDs, dates, etc.)
- **Relationships**: Cross-collection references and foreign key relationships
- **Memory Efficient**: Lazy generation mode for streaming large datasets
- **Reproducible**: Seed-based generation for consistent test data
- **Multiple Outputs**: JSON, SQL inserts, and custom formats

## Quick Example

```json
{
  "users": {
    "count": 5,
    "item": {
      "id": {"gen": "uuid"},
      "name": {"gen": "name.fullName"},
      "email": {"gen": "internet.emailAddress"}
    }
  },
  "orders": {
    "count": 20,
    "item": {
      "id": {"gen": "uuid"},
      "userId": {"ref": "users[*].id"},
      "total": {"gen": "float", "min": 10, "max": 1000, "decimals": 2}
    }
  }
}
```

```java
Generation generation = DslDataGenerator.create()
    .withSeed(123L)
    .fromJsonString(dsl)
    .generate();

String json = generation.toJson();
```

## Get Started

<div style={{display: 'flex', gap: '1rem', marginTop: '2rem'}}>
  <a href="/DataGeneration/docs/getting-started/installation" style={{
    padding: '0.75rem 1.5rem',
    background: '#0066cc',
    color: 'white',
    borderRadius: '4px',
    textDecoration: 'none',
    fontWeight: 'bold'
  }}>
    Installation →
  </a>
  <a href="/DataGeneration/docs/getting-started/quick-start" style={{
    padding: '0.75rem 1.5rem',
    border: '2px solid #0066cc',
    color: '#0066cc',
    borderRadius: '4px',
    textDecoration: 'none',
    fontWeight: 'bold'
  }}>
    Quick Start
  </a>
</div>

## Features at a Glance

### Declarative DSL
Define your data structure in JSON - no code required for basic scenarios.

### Relationships
Create realistic relationships between collections with references, sequential access, and filtering.

### 18 Built-in Generators
UUID, Name, Internet, Address, Company, Country, Book, Finance, Phone, Number, Float, Boolean, String, Date, Lorem, Sequence, Choice, CSV.

### Performance
Lazy generation mode streams data without loading everything into memory.

### Reproducible
Use seeds to generate the same data every time - perfect for testing.

### Multiple Outputs
Export as JSON, SQL INSERT statements, or implement custom serializers.

## Use Cases

- **Testing**: Generate realistic test data for unit and integration tests
- **Development**: Populate development databases with meaningful data
- **Demos**: Create convincing demo data for presentations
- **Data Seeding**: Initialize databases with structured, related data
- **Load Testing**: Generate large datasets for performance testing

## Next Steps

1. [Install DataGeneration](/DataGeneration/docs/getting-started/installation)
2. [Follow the Quick Start Guide](/DataGeneration/docs/getting-started/quick-start)
3. [Learn Core Concepts](/DataGeneration/docs/getting-started/core-concepts)
4. [Explore Generators](/DataGeneration/docs/generators/overview)
5. [Browse Common Patterns](/DataGeneration/docs/guides/overview)
