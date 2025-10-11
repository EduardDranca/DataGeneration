# Installation

Add DataGeneration to your Java project using Maven or Gradle.

## Maven

Add this dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.eduarddranca</groupId>
    <artifactId>data-generation</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Gradle

Add this to your `build.gradle`:

```groovy
dependencies {
    implementation 'io.github.eduarddranca:data-generation:0.1.0'
}
```

Or for Kotlin DSL (`build.gradle.kts`):

```kotlin
dependencies {
    implementation("io.github.eduarddranca:data-generation:0.1.0")
}
```

## Requirements

- **Java 17** or higher
- **Maven 3.6+** or **Gradle 7.0+** (for building)

## Verify Installation

Create a simple test to verify the installation:

```java
import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.Generation;

public class InstallationTest {
    public static void main(String[] args) {
        String dsl = """
            {
              "test": {
                "count": 1,
                "item": {
                  "message": "Hello DataGeneration!"
                }
              }
            }
            """;

        Generation generation = DslDataGenerator.create()
            .fromJsonString(dsl)
            .generate();

        System.out.println(generation.toJson());
    }
}
```

Expected output:
```json
{
  "test": [
    {
      "message": "Hello DataGeneration!"
    }
  ]
}
```

## Next Steps

- [Quick Start Guide](./quick-start.md) - Build your first data generator
- [Core Concepts](./core-concepts.md) - Understand the fundamentals
- [DSL Reference](../dsl-reference/overview.md) - Complete syntax guide
