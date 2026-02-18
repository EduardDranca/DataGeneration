package com.github.eddranca.datagenerator.examples;

import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.Generation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Example demonstrating range references - referencing a subset of items by index range.
 * <p>
 * This example shows:
 * - Basic range syntax: employees[0:4] references indices 0 through 4
 * - Open-ended ranges: employees[10:] references from 10 to end
 * - Practical use cases: regional managers overseeing employee ranges
 */
public class RangeReferencesExample {

    public static void main(String[] args) throws IOException {
        // Load DSL from file
        String dsl = Files.readString(Path.of("examples/08-range-references/dsl.json"));

        // Generate data with seed for reproducibility
        Generation generation = DslDataGenerator.create()
                .withSeed(42L)
                .fromJsonString(dsl)
                .generate();

        // Output as formatted JSON
        System.out.println("=== Range References Example ===\n");
        System.out.println(generation.toJson());

        // Demonstrate the relationships
        System.out.println("\n=== Explanation ===");
        System.out.println("- 20 employees generated with sequential IDs (1001-1020)");
        System.out.println("- 4 regional managers, each overseeing 5 employees:");
        System.out.println("  * Manager 1: Oversees employees[0:4] (IDs 1001-1005)");
        System.out.println("  * Manager 2: Oversees employees[5:9] (IDs 1006-1010)");
        System.out.println("  * Manager 3: Oversees employees[10:14] (IDs 1011-1015)");
        System.out.println("  * Manager 4: Oversees employees[15:19] (IDs 1016-1020)");
        System.out.println("- 10 performance reviews for employees[0:9] (first 10 employees)");
    }
}
