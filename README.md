# DataGeneration

[![CI](https://github.com/EduardDranca/DataGeneration/workflows/CI/badge.svg)](https://github.com/EduardDranca/DataGeneration/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java Version](https://img.shields.io/badge/Java-17%2B-orange)](https://openjdk.org/projects/jdk/17/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.eduarddranca/data-generation.svg)](https://central.sonatype.com/artifact/io.github.eduarddranca/data-generation)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=EduardDranca_DataGeneration&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=EduardDranca_DataGeneration)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=EduardDranca_DataGeneration&metric=coverage)](https://sonarcloud.io/summary/new_code?id=EduardDranca_DataGeneration)
[![GitHub issues](https://img.shields.io/github/issues/EduardDranca/DataGeneration.svg)](https://github.com/EduardDranca/DataGeneration/issues)
[![GitHub stars](https://img.shields.io/github/stars/EduardDranca/DataGeneration.svg)](https://github.com/EduardDranca/DataGeneration/stargazers)

A powerful Java library for generating complex, realistic test data using a declarative JSON DSL. Perfect for testing, development, and data seeding scenarios.

**[Full Documentation](https://eduarddranca.github.io/DataGeneration/)**

## Why DataGeneration?
DataGeneration creates **complex, interconnected datasets** with relationships, filtering, and advanced constraints.

```java
// DataGeneration: Connected, realistic data
String dsl = """
    {
        "departments": {
            "count": 4,
            "item": {
                "id": {"gen": "uuid"},
                "name": {"gen": "choice", "options": ["Engineering", "Marketing", "Sales", "HR"]},
                "budget": {"gen": "number", "min": 50000, "max": 500000}
            }
        },
        "employees": {
            "count": 15,
            "item": {
                "id": {"gen": "uuid"},
                "firstName": {"gen": "name.firstName"},
                "lastName": {"gen": "name.lastName"},
                "email": {"gen": "internet.emailAddress"},
                "departmentId": {"ref": "departments[*].id"},
                "salary": {"gen": "number", "min": 40000, "max": 150000}
            }
        }
    }
    """;
```

## Quick Start

### Installation

**Maven:**
```xml
<dependency>
    <groupId>io.github.eduarddranca</groupId>
    <artifactId>data-generator</artifactId>
    <version>0.2.0</version>
</dependency>
```

**Gradle:**
```gradle
implementation 'io.github.eduarddranca:data-generator:0.2.0'
```

### Basic Usage

```java
String dsl = """
    {
        "users": {
            "count": 5,
            "item": {
                "id": {"gen": "uuid"},
                "firstName": {"gen": "name.firstName"},
                "lastName": {"gen": "name.lastName"},
                "email": {"gen": "internet.emailAddress"},
                "age": {"gen": "number", "min": 18, "max": 65},
                "isActive": {"gen": "choice", "options": [true, false]}
            }
        }
    }
    """;

Generation result = DslDataGenerator.create()
    .withSeed(123L)  // Reproducible results
    .fromJsonString(dsl)
    .generate();

// Multiple output formats
Stream<JsonNode> users = result.streamJsonNodes("users");  // Stream specific collection
Map<String, Stream<JsonNode>> jsonStreams = result.asJsonNodes(); // All collections as streams
Map<String, Stream<String>> sqlInserts = result.asSqlInserts(); // SQL inserts as streams
```

## Key Features

### **Complex Relationships**
Create realistic data with foreign keys and references:

```json
{
    "categories": {
        "count": 3,
        "item": {
            "id": {"gen": "uuid"},
            "name": {"gen": "choice", "options": ["Electronics", "Clothing", "Books"]}
        }
    },
    "products": {
        "count": 8,
        "item": {
            "id": {"gen": "uuid"},
            "name": {"gen": "choice", "options": ["Laptop Pro", "T-Shirt", "Programming Book"]},
            "price": {"gen": "float", "decimals": 2, "min": 9.99, "max": 999.99},
            "categoryId": {"ref": "categories[*].id"}
        }
    }
}
```

### **Advanced Filtering**
Filter generated values based on other data:

```json
{
    "reference_data": {
        "count": 3,
        "item": {
            "id": {"gen": "uuid"},
            "name": {"gen": "choice", "options": ["Alice", "Bob", "Charlie"]}
        }
    },
    "filtered_items": {
        "count": 5,
        "item": {
            "id": {"gen": "uuid"},
            "name": {
                "ref": "reference_data[*].name",
                "filter": ["Alice", "Bob"]
            }
        }
    }
}
```

### **Conditional References**
Filter collections based on field conditions before referencing:

```json
{
    "users": {
        "count": 10,
        "item": {
            "id": {"gen": "sequence", "start": 1},
            "age": {"gen": "number", "min": 18, "max": 70},
            "status": {"gen": "choice", "options": ["active", "inactive", "banned"]}
        }
    },
    "orders": {
        "count": 20,
        "item": {
            "userId": {"ref": "users[status='active' and age>=21].id"}
        }
    }
}
```

**Supported operators:**
- Comparison: `=`, `!=`, `<`, `<=`, `>`, `>=`
- Logical: `and`, `or`

**Example use cases:**
- Only reference active users: `users[status='active'].id`
- Premium products: `products[price>100].id`
- Eligible employees: `employees[salary>80000 and yearsOfService>=5].id`
- Featured or discounted: `products[featured=true or discount>0].id`

### **Arrays & Collections**
Generate arrays with size constraints:

```json
{
    "employees": {
        "count": 15,
        "item": {
            "id": {"gen": "uuid"},
            "firstName": {"gen": "name.firstName"},
            "skills": {
                "array": {
                    "size": 4,
                    "item": {
                        "gen": "choice",
                        "options": ["Java", "Python", "JavaScript", "SQL", "Docker", "AWS", "React", "Spring"]
                    }
                }
            }
        }
    }
}
```



### **Multiple Output Formats**

```java
Generation result = DslDataGenerator.create().fromJsonString(dsl).generate();

// Collection metadata
boolean hasUsers = result.hasCollection("users");
int userCount = result.getCollectionSize("users");
Set<String> collectionNames = result.getCollectionNames();

// Streaming data (memory efficient)
Stream<JsonNode> users = result.streamJsonNodes("users");
Map<String, Stream<JsonNode>> allCollections = result.asJsonNodes();

// SQL Inserts as streams
Map<String, Stream<String>> sqlInserts = result.asSqlInserts();
Stream<String> userSqlInserts = result.streamSqlInserts("users");
```

## Built-in Generators

| Generator | Examples |
|-----------|----------|
| **name** | `firstName`, `lastName`, `fullName` | 
| **internet** | `emailAddress`, `domainName`, `url` | 
| **address** | `streetAddress`, `city`, `zipCode` | 
| **company** | `name`, `profession`, `buzzword` | 
| **country** | `name`, `code` | 
| **book** | `title`, `author`, `genre` | 
| **finance** | `creditCardNumber`, `routingNumber`, `bic` | 
| **phone** | `phoneNumber`, `cellPhone` | 
| **number** | Random integers |
| **float** | Random decimals |
| **string** | Random strings |
| **uuid** | UUID v4 | 
| **choice** | Pick from options |
| **sequence** | Sequential numbers |
| **date** | Random dates |
| **lorem** | Lorem ipsum text |
| **boolean** | Random booleans | 
| **csv** | CSV data | 

## Advanced Features

### Memory Optimization
For large datasets, enable memory optimization to reduce memory usage by not generating unreferenced fields until needed:

```java
Generation result = DslDataGenerator.create()
    .withMemoryOptimization()  // Enable lazy generation
    .withSeed(123L)           // Always use seed for reproducibility
    .fromJsonString(dsl)
    .generate();

// Stream data efficiently without loading everything into memory
result.streamSqlInserts("users")
    .forEach(sql -> database.execute(sql));
```

**Important**: Memory optimization uses lazy generation, which means:
- Streaming the same collection multiple times yields different results
- Processing order affects the generated data
- Not suitable for parallel processing
- Best for one-time streaming of large datasets

See the [Memory Optimization Example](examples/04-memory-optimization/) for detailed usage.

### Custom Generators
Create custom generators with access to the shared Faker instance for consistent randomization:

```java
Generator employeeIdGenerator = context -> {
    String prefix = context.getStringOption("prefix");
    if (prefix == null) prefix = "EMP";
    int number = context.faker().number().numberBetween(1000, 9999);
    return context.mapper().valueToTree(prefix + "-" + String.format("%04d", number));
};

DslDataGenerator.create()
    .withCustomGenerator("employeeId", employeeIdGenerator)
    .fromJsonString(dsl)
    .generate();
```

**Key Benefits:**
- **Shared Faker**: `context.faker()` ensures reproducible results when using seeds
- **Convenient Options**: `context.getStringOption()`, `getIntOption()`, `getBooleanOption()`, `getDoubleOption()`
- **ObjectMapper Access**: `context.mapper()` for creating JsonNode values
- **Type Safety**: Clear contract with `GeneratorContext` parameter

### Weighted Choices
```json
{
    "isManager": {
        "gen": "choice",
        "options": [true, false],
        "weights": [20, 80]
    }
}
```

### Sequential References
```json
{
    "orders": {
        "count": 10,
        "item": {
            "id": {"gen": "uuid"},
            "userId": {"ref": "users[*].id", "sequential": true}
        }
    }
}
```

### Range References
Reference a subset of items by index range:
```json
{
    "employees": {
        "count": 20,
        "item": {"id": {"gen": "sequence", "start": 1001}}
    },
    "regionalManagers": {
        "count": 4,
        "item": {
            "managedEmployeeId": {"ref": "employees[0:4].id"}
        }
    }
}
```

**Range syntax:**
- `[0:9]` - Indices 0-9 (10 items)
- `[10:]` - From index 10 to end
- `[:10]` - From start to index 10
- `[:]` - All items (equivalent to `[*]`)
- `[-10:-1]` - Last 10 items

## Examples

### E-commerce Dataset
```json
{
    "categories": {
        "count": 3,
        "item": {
            "id": {"gen": "uuid"},
            "name": {"gen": "choice", "options": ["Electronics", "Clothing", "Books"]},
            "description": {"gen": "lorem.sentence"}
        }
    },
    "products": {
        "count": 8,
        "item": {
            "id": {"gen": "uuid"},
            "name": {"gen": "choice", "options": ["Laptop Pro", "Wireless Headphones", "T-Shirt", "Programming Book"]},
            "price": {"gen": "float", "decimals": 2, "min": 9.99, "max": 999.99},
            "categoryId": {"ref": "categories[*].id"},
            "inStock": {"gen": "choice", "options": [true, false]},
            "stockQuantity": {"gen": "number", "min": 0, "max": 100}
        }
    },
    "customers": {
        "count": 5,
        "item": {
            "id": {"gen": "uuid"},
            "firstName": {"gen": "name.firstName"},
            "lastName": {"gen": "name.lastName"},
            "email": {"gen": "internet.emailAddress"},
            "address": {
                "street": {"gen": "address.streetAddress"},
                "city": {"gen": "address.city"},
                "zipCode": {"gen": "address.zipCode"}
            }
        }
    },
    "orders": {
        "count": 10,
        "item": {
            "id": {"gen": "uuid"},
            "customerId": {"ref": "customers[*].id"},
            "productId": {"ref": "products[*].id"},
            "quantity": {"gen": "number", "min": 1, "max": 5},
            "orderDate": {"gen": "date"},
            "status": {"gen": "choice", "options": ["pending", "shipped", "delivered", "cancelled"]}
        }
    }
}
```

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup
```bash
git clone https://github.com/EduardDranca/DataGeneration.git
cd DataGeneration
mvn clean test
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
