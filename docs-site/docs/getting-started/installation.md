# Installation

Add DataGeneration to your Java project using Maven or Gradle.

## Requirements

- Java 17 or higher
- Maven 3.6+ or Gradle 7.0+

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

## Verify Installation

Create a simple test to verify the installation:

```java
import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.Generation;

public class InstallationTest {
    public static void main(String[] args) {
        String dsl = """
            {
              "users": {
                "count": 3,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.fullName"}
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

If you see JSON output with 3 users, you're all set!

## Next Steps

- [Quick Start Guide](./quick-start.md) - Build your first data generator
- [Core Concepts](./core-concepts.md) - Understand the fundamentals
- [Generators Overview](../generators/overview.md) - Explore available generators
