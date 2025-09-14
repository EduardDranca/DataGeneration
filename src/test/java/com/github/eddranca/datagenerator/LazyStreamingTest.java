package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for lazy streaming generation functionality.
 * Verifies that items are generated on-demand during streaming.
 */
class LazyStreamingTest {

    @Test
    void testLazyStreamingWithReferences() throws Exception {
        String dsl = """
            {
              "users": {
                "count": 50,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.firstName"},
                  "email": {"gen": "internet.emailAddress"},
                  "bio": {"gen": "lorem", "options": {"length": 100}}
                }
              },
              "posts": {
                "count": 30,
                "item": {
                  "id": {"gen": "uuid"},
                  "title": {"gen": "lorem", "options": {"length": 50}},
                  "authorId": {"ref": "users[*].id"},
                  "authorName": {"ref": "users[*].name"}
                }
              }
            }
            """;

        // Test with memory optimization enabled
        Generation generation = DslDataGenerator.create()
            .withMemoryOptimization()
            .fromJsonString(dsl)
            .generate();

        System.out.println(Runtime.getRuntime().totalMemory() / 1024 / 1024.f + " MB");

        // Verify structure
        assertTrue(generation.getCollections().containsKey("users"));
        assertTrue(generation.getCollections().containsKey("posts"));
//        assertEquals(5, generation.getCollections().get("users").size());
//        assertEquals(3, generation.getCollections().get("posts").size());
    }

    @Test
    void testLazyStreamingWithoutOptimization() throws Exception {
        String dsl = """
            {
              "users": {
                "count": 3,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.firstName"},
                  "email": {"gen": "internet.emailAddress"}
                }
              }
            }
            """;

        // Test without memory optimization (default)
        Generation generation = DslDataGenerator.create()
            .fromJsonString(dsl)
            .generate();

        // Should work normally
        assertEquals(3, generation.getCollections().get("users").size());

        // Test streaming
        List<String> sqlStatements = generation.streamSqlInserts("users")
            .collect(Collectors.toList());

        assertEquals(3, sqlStatements.size());

        for (String sql : sqlStatements) {
            assertTrue(sql.contains("INSERT INTO users"));
            assertTrue(sql.contains("id"));
            assertTrue(sql.contains("name"));
            assertTrue(sql.contains("email"));
        }
    }

    @Test
    public void testMemoryOptimizationWithSelectiveFieldGeneration() throws Exception {
        String dsl = """
            {
              "users": {
                "count": 3,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.firstName"},
                  "email": {"gen": "internet.emailAddress"},
                  "bio": {"gen": "lorem", "options": {"length": 200}},
                  "address": {"gen": "address.fullAddress"}
                }
              },
              "posts": {
                "count": 2,
                "item": {
                  "id": {"gen": "uuid"},
                  "title": {"gen": "lorem", "options": {"length": 50}},
                  "content": {"gen": "lorem", "options": {"length": 500}},
                  "authorId": {"ref": "users[*].id"},
                  "authorName": {"ref": "users[*].name"}
                }
              }
            }
            """;

        // Test with memory optimization - only id and name should be generated for users
        Generation optimizedGeneration = DslDataGenerator.create()
            .withMemoryOptimization()
            .fromJsonString(dsl)
            .generate();

        // Test without memory optimization - all fields should be generated
        Generation fullGeneration = DslDataGenerator.create()
            .fromJsonString(dsl)
            .generate();

        // Both should have the same structure
        assertEquals(3, optimizedGeneration.getCollections().get("users").size());
        assertEquals(2, optimizedGeneration.getCollections().get("posts").size());
        assertEquals(3, fullGeneration.getCollections().get("users").size());
        assertEquals(2, fullGeneration.getCollections().get("posts").size());

        // Verify that posts reference users correctly in both cases
        JsonNode optimizedJson = optimizedGeneration.asJsonNode();
        JsonNode fullJson = fullGeneration.asJsonNode();

        JsonNode optimizedFirstPost = optimizedJson.get("posts").get(0);
        JsonNode fullFirstPost = fullJson.get("posts").get(0);

        assertTrue(optimizedFirstPost.has("authorId"));
        assertTrue(optimizedFirstPost.has("authorName"));
        assertTrue(fullFirstPost.has("authorId"));
        assertTrue(fullFirstPost.has("authorName"));

        assertNotNull(optimizedFirstPost.get("authorId").asText());
        assertNotNull(optimizedFirstPost.get("authorName").asText());
        assertNotNull(fullFirstPost.get("authorId").asText());
        assertNotNull(fullFirstPost.get("authorName").asText());
    }

    @Test
    public void testMemoryUsageComparison() throws Exception {
        String dsl = """
            {
              "users": {
                "count": 100000000,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.firstName"},
                  "email": {"gen": "internet.emailAddress"},
                  "bio": {"gen": "lorem", "options": {"length": 500}},
                  "address": {"gen": "address.fullAddress"},
                  "phone": {"gen": "phone.phoneNumber"},
                  "company": {"gen": "company.name"}
                }
              },
              "posts": {
                "count": 100,
                "item": {
                  "id": {"gen": "uuid"},
                  "title": {"gen": "lorem", "options": {"length": 50}},
                  "authorId": {"ref": "users[*].id"},
                  "authorName": {"ref": "users[*].name"}
                }
              }
            }
            """;

        // Measure memory usage with optimization
        Runtime.getRuntime().gc();
        long memoryBeforeOptimized = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        Generation optimizedGeneration = DslDataGenerator.create()
            .withMemoryOptimization()
            .fromJsonString(dsl)
            .generate();

        Runtime.getRuntime().gc();
        long memoryAfterOptimized = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long optimizedMemoryUsed = memoryAfterOptimized - memoryBeforeOptimized;
        System.out.println("Optimized memory used: " + optimizedMemoryUsed);

        optimizedGeneration.getCollections().get("users");
        final int[] i = {0};
        AtomicLong totalLengthChars = new AtomicLong(0);
        var iterator = optimizedGeneration.streamSqlInserts("users")
            .iterator();

        while (iterator.hasNext()) {
            var s = iterator.next();
            if (++i[0] % 100000 == 0) {
                System.out.println(s);
                System.out.println("Generated SQL for " + i[0] + " users so far...");
            }
            totalLengthChars.addAndGet(s.length());
        }
//            .parallel()
//            .peek((a) -> {
//                if (++i[0] % 100000 == 0) {
//                    System.out.println(a);
//                    System.out.println("Generated SQL for " + i[0] + " users so far...");
//                }
//            })
//            .forEach(s -> totalLengthChars.addAndGet(s.length()));

//        IntStream.range(0, 10000000).parallel()
//                .mapToObj(aadsf -> new Faker().lorem().characters(200))
//            .peek((a) -> {
//                if (++i[0] % 100000 == 0) {
//                    System.out.println("Generated SQL for " + i[0] + " users so far...");
//                }
//            })
//                    .forEach(s -> totalLengthChars.addAndGet(s.length()));

        System.out.println("Total length of SQL inserts for users: " + totalLengthChars.get() + " characters");

        // Measure memory usage without optimization
//        Runtime.getRuntime().gc();
//        long memoryBeforeNormal = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
//
//        Generation normalGeneration = DslDataGenerator.create()
//            .fromJsonString(dsl)
//            .generate();
//
//        Runtime.getRuntime().gc();
//        long memoryAfterNormal = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
//        long normalMemoryUsed = memoryAfterNormal - memoryBeforeNormal;
//
//        System.out.printf("Memory usage - Optimized: %d KB, Normal: %d KB%n",
//            optimizedMemoryUsed / 1024, normalMemoryUsed / 1024);
//        if (normalMemoryUsed > optimizedMemoryUsed) {
//            System.out.printf("Memory savings: %.1f%%%n",
//                ((double)(normalMemoryUsed - optimizedMemoryUsed) / normalMemoryUsed) * 100);
//        }
//
//        // Both should produce the same final result when fully materialized
//        JsonNode optimizedJson = optimizedGeneration.asJsonNode();
//        JsonNode normalJson = normalGeneration.asJsonNode();
//
//        assertEquals(10000, optimizedJson.get("users").size());
//        assertEquals(100, optimizedJson.get("posts").size());
//        assertEquals(10000, normalJson.get("users").size());
//        assertEquals(100, normalJson.get("posts").size());
    }
}
