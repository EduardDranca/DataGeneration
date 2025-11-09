package examples;

import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.Generation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Example demonstrating runtime-computed generator options.
 * <p>
 * This feature allows generator options to reference other field values,
 * enabling dynamic, context-dependent data generation.
 * <p>
 * Use cases:
 * - Age ranges that depend on other age fields
 * - Price ranges based on product category
 * - Date ranges where end date depends on start date
 * - String lengths based on content type
 */
public class RuntimeComputedOptionsExample {
    public static void main(String[] args) throws IOException {
        String dsl = Files.readString(Paths.get("examples/10-runtime-computed-options/dsl.json"));

        Generation generation = DslDataGenerator.create()
            .withSeed(42L)
            .fromJsonString(dsl)
            .generate();

        System.out.println("=== Runtime-Computed Options Example ===\n");

        System.out.println("Employees with age-dependent retirement ages:");
        generation.asJsonNodes().get("employees").limit(3).forEach(employee -> {
            System.out.printf("  %s (Start: %d, Retirement: %d, Years to retirement: %d)%n",
                employee.get("name").asText(),
                employee.get("startAge").asInt(),
                employee.get("retirementAge").asInt(),
                employee.get("yearsToRetirement").asInt()
            );
        });

        System.out.println("\nProducts with category-based pricing:");
        generation.asJsonNodes().get("products").limit(5).forEach(product -> {
            System.out.printf("  %s (%s): $%.2f%n",
                product.get("name").asText(),
                product.get("category").asText(),
                product.get("price").asDouble()
            );
        });

        System.out.println("\nDynamic strings with generator-based options:");
        generation.asJsonNodes().get("dynamicStrings").limit(3).forEach(item -> {
            System.out.printf("  Item %d: baseLength=%d, dynamicLength=%d%n",
                item.get("id").asInt(),
                item.get("baseLength").asInt(),
                item.get("dynamicLength").asInt()
            );
            System.out.printf("    generatedString='%s' (length=%d)%n",
                item.get("generatedString").asText(),
                item.get("generatedString").asText().length()
            );
            System.out.printf("    referenceBasedString='%s' (length=%d)%n",
                item.get("referenceBasedString").asText(),
                item.get("referenceBasedString").asText().length()
            );
            System.out.printf("    dynamicBasedString='%s' (length=%d)%n",
                item.get("dynamicBasedString").asText(),
                item.get("dynamicBasedString").asText().length()
            );
        });

        System.out.println("\n=== Key Features ===");
        System.out.println("1. Simple references: {\"ref\": \"this.fieldName\"}");
        System.out.println("2. Mapped references: {\"ref\": \"this.category\", \"map\": {...}}");
        System.out.println("3. Generator-based options: {\"gen\": \"choice\", \"options\": [10, 23, 1, 29]}");
        System.out.println("4. Works with all generators that accept min/max options");
        System.out.println("5. Supports both eager and memory-optimized modes");
    }
}
