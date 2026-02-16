package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for multiple DSL keys mapping to the same collection name via the "name" property.
 * This enables generating separate collections that merge under the same output name
 * (e.g., for SQL table mapping).
 */
class DuplicateCollectionNameTest extends ParameterizedGenerationTest {

    @BothImplementationsTest
    void testTwoCollectionsWithSameNameMergeInOutput(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
                "usersKey1": {
                    "name": "users",
                    "count": 3,
                    "item": {
                        "id": {"gen": "sequence", "start": 1},
                        "type": "admin"
                    }
                },
                "usersKey2": {
                    "name": "users",
                    "count": 2,
                    "item": {
                        "id": {"gen": "sequence", "start": 100},
                        "type": "regular"
                    }
                }
            }
            """;

        Generation result = generateFromDsl(dsl, memoryOptimized);
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(result);

        // Output should have one "users" key with 5 merged items
        assertThat(collections).containsKey("users");
        assertThat(collections.get("users")).hasSize(5);
    }

    @BothImplementationsTest
    void testReferenceByDslKeyWithSharedName(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
                "admins": {
                    "name": "users",
                    "count": 3,
                    "item": {
                        "id": {"gen": "uuid"},
                        "type": "admin"
                    }
                },
                "regularUsers": {
                    "name": "users",
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"},
                        "type": "regular"
                    }
                },
                "adminLogs": {
                    "count": 10,
                    "item": {
                        "userId": {"ref": "admins[*].id"}
                    }
                }
            }
            """;

        Generation result = generateFromDsl(dsl, memoryOptimized);
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(result);

        // Collect admin IDs
        Set<String> adminIds = collections.get("users").stream()
            .filter(user -> "admin".equals(user.get("type").asText()))
            .map(user -> user.get("id").asText())
            .collect(Collectors.toSet());
        assertThat(adminIds).hasSize(3);

        // All adminLog userIds should reference admin IDs only
        assertThat(collections.get("adminLogs")).allSatisfy(log ->
            assertThat(adminIds).contains(log.get("userId").asText())
        );
    }

    @BothImplementationsTest
    void testReferenceBySharedNamePullsFromMergedCollection(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
                "admins": {
                    "name": "users",
                    "count": 3,
                    "item": {
                        "id": {"gen": "uuid"},
                        "type": "admin"
                    }
                },
                "regularUsers": {
                    "name": "users",
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"},
                        "type": "regular"
                    }
                },
                "allUserLogs": {
                    "count": 20,
                    "item": {
                        "userId": {"ref": "users[*].id"}
                    }
                }
            }
            """;

        Generation result = generateFromDsl(dsl, memoryOptimized);
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(result);

        // Collect all user IDs (both admin and regular)
        Set<String> allUserIds = collections.get("users").stream()
            .map(user -> user.get("id").asText())
            .collect(Collectors.toSet());
        assertThat(allUserIds).hasSize(8);

        // All log userIds should come from the merged collection
        assertThat(collections.get("allUserLogs")).allSatisfy(log ->
            assertThat(allUserIds).contains(log.get("userId").asText())
        );
    }

    @BothImplementationsTest
    void testSqlOutputUsesSharedName(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
                "admins": {
                    "name": "users",
                    "count": 2,
                    "item": {
                        "id": {"gen": "sequence", "start": 1},
                        "role": "admin"
                    }
                },
                "regularUsers": {
                    "name": "users",
                    "count": 3,
                    "item": {
                        "id": {"gen": "sequence", "start": 100},
                        "role": "user"
                    }
                }
            }
            """;

        Generation result = generateFromDsl(dsl, memoryOptimized);
        Map<String, Stream<String>> sqlStreams = result.asSqlInserts();

        // SQL should use the shared "users" table name
        assertThat(sqlStreams).containsKey("users");
        List<String> sqlStatements = sqlStreams.get("users").toList();
        assertThat(sqlStatements).hasSize(5);

        assertThat(sqlStatements).allSatisfy(sql ->
            assertThat(sql).startsWith("INSERT INTO users")
        );
    }
}
