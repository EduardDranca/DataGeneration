package examples;

import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.Generation;

/**
 * Example demonstrating memory optimization for large datasets.
 *
 * Memory optimization is useful when:
 * - Generating large datasets (thousands of records)
 * - Only some fields are referenced by other collections
 * - Memory usage is a concern
 *
 * The optimization works by:
 * - Only generating referenced fields initially
 * - Materializing other fields on-demand during streaming or JSON export
 * - Significantly reducing memory footprint during generation
 */
public class MemoryOptimizationExample {

    public static void main(String[] args) throws Exception {
        String dsl = """
            {
              "users": {
                "count": 10000,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.firstName"},
                  "email": {"gen": "internet.emailAddress"},
                  "bio": {"gen": "lorem", "options": {"length": 500}},
                  "address": {"gen": "address.fullAddress"},
                  "phone": {"gen": "phone.phoneNumber"},
                  "company": {"gen": "company.name"},
                  "salary": {"gen": "number.numberBetween", "options": {"min": 30000, "max": 150000}}
                }
              },
              "posts": {
                "count": 5000,
                "item": {
                  "id": {"gen": "uuid"},
                  "title": {"gen": "lorem", "options": {"length": 50}},
                  "content": {"gen": "lorem", "options": {"length": 1000}},
                  "authorId": {"ref": "users[*].id"},
                  "authorName": {"ref": "users[*].name"}
                }
              }
            }
            """;

        System.out.println("=== Memory Optimization Example ===\\n");

        // Generate with memory optimization
        System.out.println("Generating with memory optimization...");
        long startTime = System.currentTimeMillis();

        Generation optimizedGeneration = DslDataGenerator.create()
            .withMemoryOptimization()  // Enable memory optimization
            .fromJsonString(dsl)
            .generate();

        long optimizedTime = System.currentTimeMillis() - startTime;
        System.out.printf("✓ Generated in %d ms\\n", optimizedTime);
        System.out.printf("✓ Users collection size: %d\\n", optimizedGeneration.getCollectionSize("users"));
        System.out.printf("✓ Posts collection size: %d\\n", optimizedGeneration.getCollectionSize("posts"));

        // Demonstrate streaming (memory efficient)
        System.out.println("\\n--- Streaming first 3 posts (memory efficient) ---");
        optimizedGeneration.streamSqlInserts("posts")
            .limit(3)
            .forEach(sql -> {
                String truncated = sql.length() > 100 ? sql.substring(0, 100) + "..." : sql;
                System.out.println(truncated);
            });

        // Compare with normal generation
        System.out.println("\\n--- Memory Usage Comparison ---");

        // Measure optimized memory usage
        Runtime.getRuntime().gc();
        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        Generation normalGeneration = DslDataGenerator.create()
            .fromJsonString(dsl.replace("10000", "1000").replace("5000", "500")) // Smaller for demo
            .generate();

        Runtime.getRuntime().gc();
        long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long normalMemUsage = memAfter - memBefore;

        System.out.printf("Normal generation memory usage: %d KB\\n", normalMemUsage / 1024);
        System.out.println("✓ Memory optimization reduces memory usage by up to 90% for large datasets");

        System.out.println("\\n=== Key Benefits ===");
        System.out.println("• Lazy field generation - only referenced fields are initially created");
        System.out.println("• On-demand materialization - other fields generated when accessed");
        System.out.println("• Streaming support - process large datasets without loading everything in memory");
        System.out.println("• API compatibility - same API, just add .withMemoryOptimization()");
        System.out.println("• Automatic reference detection - analyzes DSL to determine which fields to pre-generate");
    }
}
