package examples;

import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.StreamingGeneration;

/**
 * Streaming generation example for handling large datasets efficiently
 * This example demonstrates memory-efficient generation for high-volume data
 */
public class StreamingGenerationExample {
    public static void main(String[] args) {
        try {
            System.out.println("=== Streaming Generation Example ===");
            System.out.println("Generating 1000 records with streaming approach...");
            
            // Load DSL from external JSON file and create streaming generator
            StreamingGeneration streaming = DslDataGenerator.create()
                    .withSeed(99999L)
                    .createStreamingGeneration(new java.io.File("dsl.json"));

            // Demonstrate SQL streaming (this is the main feature)
            System.out.println("\n=== SQL Streaming (First 10 statements) ===");
            streaming.streamSqlInserts("large_dataset")
                    .limit(10)
                    .forEach(System.out::println);

            // Show reference data SQL
            System.out.println("\n=== Reference Data SQL ===");
            streaming.streamSqlInserts("reference_data")
                    .forEach(System.out::println);

            // Demonstrate streaming all collections
            System.out.println("\n=== All Collections SQL (First 15 statements) ===");
            streaming.streamSqlInserts() // Stream all collections
                    .limit(15)
                    .forEach(System.out::println);

            // Show statistics by counting SQL statements
            System.out.println("\n=== Statistics ===");
            long totalLargeDatasetRecords = streaming.streamSqlInserts("large_dataset").count();
            System.out.println("Total SQL statements for large_dataset: " + totalLargeDatasetRecords);
            
            long totalReferenceRecords = streaming.streamSqlInserts("reference_data").count();
            System.out.println("Total SQL statements for reference_data: " + totalReferenceRecords);

            System.out.println("\n=== Memory Efficiency Demo Complete ===");
            System.out.println("Note: Streaming allows processing of datasets much larger than available memory!");
            System.out.println("Each SQL statement is generated on-demand without storing all data in memory.");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
