# Custom Generators

Create your own generators to extend DataGeneration with domain-specific logic.

## Creating a Custom Generator

Implement the `Generator` interface:

```java
package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;

public class CustomGenerator implements Generator {
    @Override
    public JsonNode generate(GeneratorContext context) {
        // Your generation logic here
        return context.mapper().valueToTree("custom value");
    }
}
```

## Registering Your Generator

Register it with the builder:

```java
Generation result = DslDataGenerator.create()
    .withCustomGenerator("custom", new CustomGenerator())
    .fromJsonString(dsl)
    .generate();
```

## Using in DSL

```json
{
  "items": {
    "count": 10,
    "item": {
      "id": {"gen": "uuid"},
      "customField": {"gen": "custom"}
    }
  }
}
```

## Accessing Options

Use `GeneratorContext` to access options (it's a Java record):

```java
public class ConfigurableGenerator implements Generator {
    @Override
    public JsonNode generate(GeneratorContext context) {
        int min = context.getIntOption("min", 0);
        int max = context.getIntOption("max", 100);
        String format = context.getStringOption("format");  // Returns null if not set
        if (format == null) format = "default";
        
        // Your logic using options
        return context.mapper().valueToTree(generateValue(min, max, format));
    }
}
```

**DSL with options:**
```json
{
  "value": {
    "gen": "configurable",
    "min": 10,
    "max": 50,
    "format": "hex"
  }
}
```

## Using Faker

Access the Faker instance for realistic data:

```java
public class CustomNameGenerator implements Generator {
    @Override
    public JsonNode generate(GeneratorContext context) {
        Faker faker = context.faker();
        String value = faker.name().fullName() + " " + faker.number().digits(4);
        return context.mapper().valueToTree(value);
    }
}
```

## Example: SKU Generator

```java
public class SkuGenerator implements Generator {
    @Override
    public JsonNode generate(GeneratorContext context) {
        String prefix = context.getStringOption("prefix");
        if (prefix == null) prefix = "PROD";
        int digits = context.getIntOption("digits", 6);
        
        Faker faker = context.faker();
        String number = faker.number().digits(digits);
        
        return context.mapper().valueToTree(prefix + "-" + number);
    }
}
```

**Usage:**
```json
{
  "products": {
    "count": 100,
    "item": {
      "id": {"gen": "uuid"},
      "sku": {"gen": "sku", "prefix": "WIDGET", "digits": 8}
    }
  }
}
```

## Example: Weighted Status Generator

```java
public class StatusGenerator implements Generator {
    private static final Map<String, Integer> STATUS_WEIGHTS = Map.of(
        "active", 70,
        "inactive", 20,
        "pending", 10
    );
    
    @Override
    public JsonNode generate(GeneratorContext context) {
        Faker faker = context.faker();
        int total = STATUS_WEIGHTS.values().stream().mapToInt(Integer::intValue).sum();
        int random = faker.number().numberBetween(0, total);
        
        int cumulative = 0;
        for (Map.Entry<String, Integer> entry : STATUS_WEIGHTS.entrySet()) {
            cumulative += entry.getValue();
            if (random < cumulative) {
                return context.mapper().valueToTree(entry.getKey());
            }
        }
        
        return context.mapper().valueToTree("active");
    }
}
```

## Best Practices

1. **Use GeneratorContext**: Access options and Faker through context
2. **Provide Defaults**: Always provide default values for options
3. **Document Options**: Document what options your generator accepts
4. **Use Faker**: Leverage Faker for realistic data
5. **Keep It Simple**: Generators should do one thing well
6. **Thread Safe**: Generators should be stateless and thread-safe

## Next Steps

- [Generators Overview](../../generators/overview.md) - See built-in generators
- [Java API](../../api/java-api.md) - Full API documentation
