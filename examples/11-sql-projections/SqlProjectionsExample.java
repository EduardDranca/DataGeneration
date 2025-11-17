package com.github.eddranca.datagenerator.examples;

import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.util.SqlProjection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Example demonstrating SQL projections and schema-based generation.
 * Shows three approaches:
 * 1. Field projections - excluding helper fields
 * 2. Data type specifications - formatting values correctly
 * 3. Automatic schema parsing - using CREATE TABLE scripts
 */
public class SqlProjectionsExample {

    public static void main(String[] args) throws IOException {
        System.out.println("=== SQL Projections Example ===\n");

        // Read DSL
        String dslPath = "examples/11-sql-projections/dsl.json";
        String dsl = Files.readString(Paths.get(dslPath));

        // Example 1: Field Projection - Exclude Helper Fields
        System.out.println("1. FIELD PROJECTION - Excluding _tempCategoryName helper field:");
        System.out.println("-".repeat(80));
        
        SqlProjection productsProjection = SqlProjection.builder()
            .includeFields(Set.of("id", "name", "category_id", "price", "in_stock", "created_at"))
            .build();

        Stream<String> productInserts = DslDataGenerator.create()
            .withSeed(12345L)
            .fromJsonString(dsl)
            .streamSqlInsertsWithProjection("products", productsProjection);

        productInserts.limit(3).forEach(System.out::println);
        System.out.println("...\n");

        // Example 2: Data Type Specifications
        System.out.println("2. DATA TYPE SPECIFICATIONS - Boolean as TINYINT (0/1):");
        System.out.println("-".repeat(80));

        SqlProjection typedProjection = SqlProjection.builder()
            .includeFields(Set.of("id", "name", "category_id", "price", "in_stock", "created_at"))
            .withFieldType("id", "BIGINT")
            .withFieldType("category_id", "BIGINT")
            .withFieldType("price", "DECIMAL(10,2)")
            .withFieldType("in_stock", "TINYINT")  // Boolean as 0/1
            .withFieldType("created_at", "TIMESTAMP")
            .build();

        Stream<String> typedInserts = DslDataGenerator.create()
            .withSeed(12345L)
            .fromJsonString(dsl)
            .streamSqlInsertsWithProjection("products", typedProjection);

        typedInserts.limit(3).forEach(System.out::println);
        System.out.println("...\n");

        // Example 3: Automatic Schema Parsing
        System.out.println("3. AUTOMATIC SCHEMA PARSING - From CREATE TABLE:");
        System.out.println("-".repeat(80));

        String createTableProducts = """
            CREATE TABLE products (
                id BIGINT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                category_id BIGINT NOT NULL,
                price DECIMAL(10,2),
                in_stock TINYINT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createTableCategories = """
            CREATE TABLE categories (
                id INT PRIMARY KEY,
                name VARCHAR(100) NOT NULL
            )
            """;

        Map<String, String> schemas = Map.of(
            "products", createTableProducts,
            "categories", createTableCategories
        );

        Map<String, Stream<String>> allInserts = DslDataGenerator.create()
            .withSeed(12345L)
            .fromJsonString(dsl)
            .generateAsSqlFromSchemas(schemas);

        System.out.println("Categories:");
        allInserts.get("categories").forEach(sql -> System.out.println("  " + sql));
        
        System.out.println("\nProducts (first 3):");
        allInserts.get("products").limit(3).forEach(sql -> System.out.println("  " + sql));
        System.out.println("  ...");

        System.out.println("\n=== Key Features ===");
        System.out.println("✓ Helper fields excluded from SQL output");
        System.out.println("✓ Booleans formatted as 0/1 for TINYINT columns");
        System.out.println("✓ Automatic type extraction from CREATE TABLE");
        System.out.println("✓ Consistent formatting across all inserts");
    }
}
