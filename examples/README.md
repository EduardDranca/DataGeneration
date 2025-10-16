# DataGeneration Examples

This directory contains practical examples demonstrating various use cases of the DataGeneration library. Each example is organized in its own directory with clean separation of code and configuration.

## Available Examples

| Example                                                     | Description          | Key Features                              |
|-------------------------------------------------------------|----------------------|-------------------------------------------|
| **[01-basic-users](01-basic-users/)**                       | Simple user profiles | UUID, names, emails, basic types          |
| **[02-ecommerce-store](02-ecommerce-store/)**               | Online store data    | Entity relationships, nested objects      |
| **[03-company-employees](03-company-employees/)**           | Corporate structure  | Arrays, weighted choices                  |
| **[04-social-media](04-social-media/)**                     | Social platform      | Complex relationships, engagement metrics |
| **[05-financial-transactions](05-financial-transactions/)** | Banking system | Financial data, high-volume transactions  |
| **[06-educational-system](06-educational-system/)**         | School management | Academic data, regex patterns, grading    |
| **[07-custom-generator](07-custom-generator/)**             | Custom business logic | Custom generators, domain-specific rules  |
| **[08-conditional-references](08-conditional-references/)** | Filtered references | Conditional filtering, AND/OR operators   |

## Quick Start

1. **Compile the library:**
   ```bash
   mvn compile
   ```

2. **Choose an example and navigate to its directory:**
   ```bash
   cd examples/01-basic-users
   ```

3. **Follow the README in each directory for specific instructions**

## Example Structure

Each example directory contains:
- `README.md` - Detailed documentation and usage instructions
- `ExampleName.java` - Clean, focused Java program
- `dsl.json` - Beautiful, readable DSL definition
- Clear separation of code and configuration

## Benefits of File-Based DSL

**Clean Code** - Java files focus on logic, not JSON strings  
**Readable JSON** - Proper formatting and syntax highlighting  
**Easy Editing** - Modify DSL without touching Java code  
**Version Control** - Better diffs and merge conflict resolution  
**IDE Support** - JSON validation and auto-completion  
**Reusable** - DSL files can be shared across projects  

## Features Demonstrated

| Feature | Examples Using It |
|---------|-------------------|
| **Entity Relationships** | All examples (using `ref` for cross-collection references) |
| **Conditional References** | Conditional References (filtered collections with AND/OR logic) |
| **Arrays & Collections** | Company (skills), Social Media (hashtags), Education (enrollments) |
| **Weighted Choices** | Company (manager probability), Social Media (verification rates) |
| **Complex Objects** | E-commerce (addresses), Financial (transaction details) |
| **Date Generation** | Company (hire dates), Financial (transaction dates) |
| **Regex Patterns** | Education (course codes, student IDs) |
| **Custom Generators** | Custom Generator (employee IDs, department codes, salary bands) |

## Data Volumes

| Example | Total Records | Collections | Best For |
|---------|---------------|-------------|----------|
| Basic Users | 5 users | 1 | Learning basics |
| E-commerce | 26 records | 4 | Small store testing |
| Company | 25 records | 3 | Team/dept simulation |
| Social Media | 88 records | 4 | Social app testing |
| Financial | 73 records | 4 | Banking simulation |
| Educational | 97 records | 4 | School system testing |
| Custom Generator | 19 records | 3 | Custom business logic |
| Conditional References | 148 records | 8 | Business rule enforcement |

## Integration Patterns

These examples demonstrate common integration patterns:

- **Unit Testing**: Generate test data for specific scenarios
- **Integration Testing**: Create realistic datasets for API testing
- **Database Seeding**: Populate development/staging databases
- **Performance Testing**: Generate large datasets for load testing
- **Demo Data**: Create realistic data for presentations and demos

Start with **01-basic-users** if you're new to the library, then progress through the examples based on your use case complexity needs.

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+

### Using in Your Project

Add DataGeneration to your project:

**Maven:**
```xml
<dependency>
    <groupId>io.github.eduarddranca</groupId>
    <artifactId>data-generator</artifactId>
    <version>0.1.0</version>
</dependency>
```

**Gradle:**
```gradle
implementation 'io.github.eduarddranca:data-generator:0.1.0'
```

## Support

- [Main Documentation](../README.md)
- [API Reference](../README.md#api-reference)
- [Report Issues](https://github.com/eduarddranca/DataGeneration/issues)
