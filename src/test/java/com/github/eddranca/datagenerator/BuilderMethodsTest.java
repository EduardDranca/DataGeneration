package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the new Builder convenience methods.
 */
class BuilderMethodsTest extends ParameterizedGenerationTest {

    @BothImplementations
    void testBuilderGenerateAsJsonMethods(boolean memoryOptimized) throws Exception {
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

        var builder = createGenerator(memoryOptimized).fromJsonString(dsl);

        // Test generateAsJson() - all collections
        Map<String, Stream<JsonNode>> allJsonStreams = builder.generateAsJson();
        assertThat(allJsonStreams).containsKeys("users", "posts");
        assertThat(allJsonStreams.get("users").count()).isEqualTo(2);

        // Test generateAsJson(String...) - specific collections
        Map<String, Stream<JsonNode>> specificJsonStreams = builder.generateAsJson("users");
        assertThat(specificJsonStreams).containsOnlyKeys("users");
        assertThat(specificJsonStreams.get("users").count()).isEqualTo(2);
    }

    @BothImplementations
    void testBuilderStreamMethods(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
              "products": {
                "count": 3,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "lorem", "options": {"length": 10}},
                  "price": {"gen": "number.numberBetween", "options": {"min": 10, "max": 100}}
                }
              }
            }
            """;

        var builder = createGenerator(memoryOptimized).fromJsonString(dsl);

        // Test streamJsonNodes()
        Stream<JsonNode> jsonStream = builder.streamJsonNodes("products");
        assertThat(jsonStream.count()).isEqualTo(3);

        // Test streamSqlInserts()
        Stream<String> sqlStream = builder.streamSqlInserts("products");
        assertThat(sqlStream.count()).isEqualTo(3);
    }

    @BothImplementations
    void testIGenerationConvenienceMethods(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
              "users": {
                "count": 2,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.firstName"}
                }
              },
              "orders": {
                "count": 5,
                "item": {
                  "id": {"gen": "uuid"},
                  "userId": {"ref": "users[*].id"}
                }
              }
            }
            """;

        IGeneration generation = generateFromDsl(dsl, memoryOptimized);

        // Test hasCollection()
        assertThat(generation.hasCollection("users")).isTrue();
        assertThat(generation.hasCollection("orders")).isTrue();
        assertThat(generation.hasCollection("nonexistent")).isFalse();

        // Test getTotalItemCount()
        assertThat(generation.getTotalItemCount()).isEqualTo(7); // 2 users + 5 orders
    }
}
