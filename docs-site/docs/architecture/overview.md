# Architecture Overview

Understanding the internal architecture of DataGeneration.

## High-Level Architecture

```
DSL JSON → Parser → DSL Tree → Visitor → Generated Data
```

1. **DSL Parsing**: JSON is parsed into a tree of `DslNode` objects
2. **Validation**: Tree is validated for correctness
3. **Visitor Pattern**: `DataGenerationVisitor` traverses the tree
4. **Generation**: Generators create data based on node types
5. **Output**: Data is serialized to JSON, SQL, or custom formats

## Core Components

### DSL Tree

The DSL is represented as a tree of nodes:

- `RootNode` - Top-level container
- `CollectionNode` - Represents a collection
- `ItemNode` - Template for collection items
- `GeneratedFieldNode` - Field with generator
- `LiteralFieldNode` - Static value field
- `ArrayFieldNode` - Array field
- `ReferenceNode` - Reference to another collection

### Visitor Pattern

The `DataGenerationVisitor` traverses the DSL tree and generates data:

```java
public interface DslNodeVisitor {
    void visit(RootNode node);
    void visit(CollectionNode node);
    void visit(GeneratedFieldNode node);
    // ... other node types
}
```

### Generator Registry

All generators are registered in `GeneratorRegistry`:

```java
GeneratorRegistry registry = new GeneratorRegistry();
registry.register("uuid", new UuidGenerator());
registry.register("name", new NameGenerator());
// ... etc
```

### Generation Contexts

Two context implementations:

- `EagerGenerationContext` - Generates all data upfront
- `LazyGenerationContext` - Generates data on-demand

## Design Patterns

### Visitor Pattern

Used for traversing and processing the DSL tree without modifying node classes.

### Builder Pattern

`DslDataGenerator.Builder` provides fluent API for configuration.

### Strategy Pattern

`FilteringBehavior` enum allows different filtering strategies.

### Proxy Pattern

Lazy generation uses proxy objects for on-demand field materialization.

### Registry Pattern

`GeneratorRegistry` manages all available generators.

## Next Steps

- [Custom Generators](../guides/how-to/custom-generators.md) - Extend the library
- [Memory Optimization](../guides/how-to/memory-optimization.md) - Lazy vs eager generation
- [Java API](../api/java-api.md) - Full API documentation
