package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Test for the new streaming API functionality.
 */
class StreamingApiTest {

    @Test
    void testStreamJsonNodes() throws Exception {
        String dsl = """
            {
              "users": {
                "count": 3,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.firstName"}
                }
              }
            }
            """;

        IGeneration generation = DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonString(dsl)
            .generate();

        // Test streaming individual collection
        Stream<JsonNode> userStream = generation.streamJsonNodes("users");
        List<JsonNode> users = userStream.collect(Collectors.toList());

        assertThat(users).hasSize(3);
        for (JsonNode user : users) {
            assertThat(user.has("id")).isTrue();
            assertThat(user.has("name")).isTrue();
            assertThat(user.get("id").asText()).isNotEmpty();
            assertThat(user.get("name").asText()).isNotEmpty();
        }
    }

    @Test
    void testAsJsonNodeStreams() throws Exception {
        String dsl = """
            {
              "users": {
                "count": 2,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.firstName"}
                }
              },
              "posts": {
                "count": 3,
                "item": {
                  "id": {"gen": "uuid"},
                  "title": {"gen": "lorem", "options": {"length": 10}}
                }
              }
            }
            """;

        IGeneration generation = DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonString(dsl)
            .generate();

        // Test streaming all collections
        Map<String, Stream<JsonNode>> allStreams = generation.asJsonNodes();
        
        assertThat(allStreams).containsKeys("users", "posts");
        
        List<JsonNode> users = allStreams.get("users").collect(Collectors.toList());
        List<JsonNode> posts = allStreams.get("posts").collect(Collectors.toList());
        
        assertThat(users).hasSize(2);
        assertThat(posts).hasSize(3);
    }

    @Test
    void testAsSqlInsertStreams() throws Exception {
        String dsl = """
            {
              "products": {
                "count": 2,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "lorem", "options": {"length": 10}},
                  "price": {"gen": "number.numberBetween", "options": {"min": 10, "max": 100}}
                }
              }
            }
            """;

        IGeneration generation = DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonString(dsl)
            .generate();

        // Test streaming SQL inserts
        Map<String, Stream<String>> sqlStreams = generation.asSqlInserts();
        
        assertThat(sqlStreams).containsKey("products");
        
        List<String> sqlStatements = sqlStreams.get("products").collect(Collectors.toList());
        
        assertThat(sqlStatements).hasSize(2);
        for (String sql : sqlStatements) {
            assertThat(sql).contains("INSERT INTO products");
            assertThat(sql).contains("id", "name", "price");
        }
    }

    @Test
    void testMemoryOptimizedStreaming() throws Exception {
        String dsl = """
            {
              "users": {
                "count": 5,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.firstName"},
                  "email": {"gen": "internet.emailAddress"}
                }
              }
            }
            """;

        IGeneration generation = DslDataGenerator.create()
            .withMemoryOptimization()
            .withSeed(123L)
            .fromJsonString(dsl)
            .generate();

        // Test that streaming works with memory optimization
        Stream<JsonNode> userStream = generation.streamJsonNodes("users");
        List<JsonNode> users = userStream.collect(Collectors.toList());

        assertThat(users).hasSize(5);
        for (JsonNode user : users) {
            assertThat(user.has("id")).isTrue();
            assertThat(user.has("name")).isTrue();
            assertThat(user.has("email")).isTrue();
        }

        // Test SQL streaming with memory optimization
        Stream<String> sqlStream = generation.streamSqlInserts("users");
        List<String> sqlStatements = sqlStream.collect(Collectors.toList());

        assertThat(sqlStatements).hasSize(5);
        for (String sql : sqlStatements) {
            assertThat(sql).contains("INSERT INTO users");
            assertThat(sql).contains("id", "name", "email");
        }
    }
}