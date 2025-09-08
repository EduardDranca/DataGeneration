package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StreamingGenerationTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testStreamingWithReferences() throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "categories": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 10},
                            "name": {"gen": "choice", "options": ["Electronics", "Books", "Clothing"]}
                        }
                    },
                    "products": {
                        "count": 10,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 1000},
                            "name": {"gen": "choice", "options": ["Product A", "Product B", "Product C"]},
                            "category_id": {"ref": "categories[*].id"},
                            "price": {"gen": "float", "min": 10.0, "max": 500.0, "decimals": 2}
                        }
                    }
                }
                """);

        StreamingGeneration streaming = DslDataGenerator.create()
            .withSeed(456L)
            .createStreamingGeneration(dslNode);

        // Stream products (which reference categories)
        List<String> productSql = streaming.streamSqlInserts("products")
            .toList();

        // Should have 10 SQL INSERT statements for products
        assertThat(productSql).hasSize(10);

        assertThat(productSql)
            .as("All SQL statements should be valid product inserts")
            .allSatisfy(sql -> {
                assertThat(sql).startsWith("INSERT INTO products");
                assertThat(sql).contains("category_id", "price");
            });
    }

    @Test
    void testStreamingMultipleCollections() throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "users": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 100},
                            "username": {"gen": "choice", "options": ["user1", "user2", "user3"]}
                        }
                    },
                    "posts": {
                        "count": 5,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 1000},
                            "title": {"gen": "choice", "options": ["Post 1", "Post 2", "Post 3"]},
                            "user_id": {"ref": "users[*].id"}
                        }
                    }
                }
                """);

        StreamingGeneration streaming = DslDataGenerator.create()
            .withSeed(789L)
            .createStreamingGeneration(dslNode);

        // Stream all collections
        List<String> allSql = streaming.streamSqlInserts("users", "posts")
            .toList();

        // Should have 8 total statements (3 users + 5 posts)
        assertThat(allSql).hasSize(8);

        // Count statements by table
        long userStatements = allSql.stream()
            .mapToLong(sql -> sql.startsWith("INSERT INTO users") ? 1 : 0)
            .sum();
        long postStatements = allSql.stream()
            .mapToLong(sql -> sql.startsWith("INSERT INTO posts") ? 1 : 0)
            .sum();

        assertThat(userStatements).as("Should have 3 user statements").isEqualTo(3);
        assertThat(postStatements).as("Should have 5 post statements").isEqualTo(5);
    }

    @Test
    void testStreamingWithMultiStepCollections() throws IOException {
        JsonNode dslNode = mapper.readTree("""
                {
                    "admin_users": {
                        "name": "users",
                        "count": 2,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 100},
                            "username": {"gen": "choice", "options": ["admin1", "admin2"]},
                            "role": {"gen": "choice", "options": ["admin"]}
                        }
                    },
                    "regular_users": {
                        "name": "users",
                        "count": 3,
                        "item": {
                            "id": {"gen": "number", "min": 101, "max": 200},
                            "username": {"gen": "choice", "options": ["user1", "user2", "user3"]},
                            "role": {"gen": "choice", "options": ["user"]}
                        }
                    },
                    "orders": {
                        "count": 4,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 1000},
                            "user_id": {"ref": "users[*].id"},
                            "amount": {"gen": "float", "min": 10.0, "max": 100.0, "decimals": 2}
                        }
                    }
                }
                """);

        StreamingGeneration streaming = DslDataGenerator.create()
            .withSeed(999L)
            .createStreamingGeneration(dslNode);

        // Stream orders (which reference the merged users collection)
        List<String> orderSql = streaming.streamSqlInserts("orders")
            .toList();

        // Should have 4 order statements
        assertThat(orderSql).hasSize(4);

        assertThat(orderSql)
            .as("All order SQL statements should be valid")
            .allSatisfy(sql -> {
                assertThat(sql).startsWith("INSERT INTO orders");
                assertThat(sql).contains("user_id", "amount");
            });
    }

    @Test
    void testStreamingMemoryEfficiency() throws IOException {
        // This test demonstrates that streaming doesn't load all data into memory
        JsonNode dslNode = mapper.readTree("""
                {
                    "large_dataset": {
                        "count": 1000,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 100000},
                            "data": {"gen": "string", "length": 100}
                        }
                    }
                }
                """);

        StreamingGeneration streaming = DslDataGenerator.create()
            .withSeed(123L)
            .createStreamingGeneration(dslNode);

        // Process the stream in chunks to demonstrate memory efficiency
        long count = streaming.streamSqlInserts("large_dataset")
            .limit(10) // Only process first 10 items
            .peek(sql -> {
                // Each SQL statement is processed immediately
                assertThat(sql).startsWith("INSERT INTO large_dataset");
            })
            .count();

        assertThat(count).as("Should process exactly 10 items").isEqualTo(10);
    }
}
