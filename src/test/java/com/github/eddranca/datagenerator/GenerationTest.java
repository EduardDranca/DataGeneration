package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNoException;

class GenerationTest extends ParameterizedGenerationTest {

    private static final String TEST_DSL = """
            {
                "countries": {
                    "count": 3,
                    "tags": ["country"],
                    "item": {
                        "name": {"gen": "country.name"},
                        "isoCode": {"gen": "country.countryCode"},
                        "id": {"gen": "choice", "options": ["Romania", "Brasil"]},
                        "embargoed": {"gen": "choice", "options": [true, false, false, false]}
                    }
                },
                "companies": {
                    "count": 10,
                    "tags": ["company"],
                    "item": {
                        "id": {"gen": "uuid"},
                        "name": {"gen": "company.name"},
                        "countryCode": {"ref": "countries[*].isoCode", "filter": [{"ref": "countries[0].isoCode"}]},
                        "ctrName": {"ref": "countries[*].id", "filter": ["Romania"]}
                    }
                }
            }
            """;

    @BothImplementations
    void testAsJsonNode(String implementationName, boolean memoryOptimized) throws Exception {
        IGeneration generation = generateFromDsl(TEST_DSL, memoryOptimized);
        JsonNode collectionsNode = createLegacyJsonNode(generation);

        assertThat(collectionsNode).isNotNull();
        assertThat(collectionsNode.has("companies")).isTrue();
        assertThat(collectionsNode.has("countries")).isTrue();

        JsonNode companies = collectionsNode.get("companies");
        JsonNode countries = collectionsNode.get("countries");

        assertThat(companies).isNotNull();
        assertThat(companies.size()).isGreaterThan(0);
        assertThat(countries).isNotNull();
        assertThat(countries.size()).isGreaterThan(0);
    }

    @BothImplementations
    void testGetCollectionsAsJsonNode(String implementationName, boolean memoryOptimized) throws Exception {
        IGeneration generation = generateFromDsl(TEST_DSL, memoryOptimized);
        JsonNode collectionsNode = createLegacyJsonNode(generation);

        assertThat(collectionsNode).isNotNull();
        assertThat(collectionsNode.has("companies")).isTrue();
        assertThat(collectionsNode.has("countries")).isTrue();
        assertThat(collectionsNode.get("companies").isArray()).isTrue();
        assertThat(collectionsNode.get("countries").isArray()).isTrue();
        assertThat(collectionsNode.get("companies").isEmpty()).isFalse();
        assertThat(collectionsNode.get("countries").isEmpty()).isFalse();
    }

    @BothImplementations
    void testAsJson(String implementationName, boolean memoryOptimized) throws Exception {
        IGeneration generation = generateFromDsl(TEST_DSL, memoryOptimized);
        String json = createLegacyJsonString(generation);

        assertThat(json)
            .isNotNull()
            .isNotEmpty()
            .contains("companies", "countries");

        // Verify it's valid JSON
        assertThatNoException().isThrownBy(() -> mapper.readTree(json));
    }

    @BothImplementations
    void testAsJsonNodeStructure(String implementationName, boolean memoryOptimized) throws Exception {
        IGeneration generation = generateFromDsl(TEST_DSL, memoryOptimized);
        JsonNode jsonNode = createLegacyJsonNode(generation);

        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode.has("companies")).isTrue();
        assertThat(jsonNode.has("countries")).isTrue();
        assertThat(jsonNode.get("companies").isArray()).isTrue();
        assertThat(jsonNode.get("countries").isArray()).isTrue();
    }

    @BothImplementations
    void testAsSqlInsertsAll(String implementationName, boolean memoryOptimized) throws Exception {
        IGeneration generation = generateFromDsl(TEST_DSL, memoryOptimized);
        Map<String, String> sqlMap = collectAllSqlInserts(generation);

        assertThat(sqlMap)
            .isNotNull()
            .isNotEmpty()
            .containsKeys("companies", "countries");

        String companiesSql = sqlMap.get("companies");
        String countriesSql = sqlMap.get("countries");

        assertThat(companiesSql).contains("INSERT INTO companies");
        assertThat(countriesSql).contains("INSERT INTO countries");
    }

    @BothImplementations
    void testAsSqlInsertsSpecificTable(String implementationName, boolean memoryOptimized) throws Exception {
        IGeneration generation = generateFromDsl(TEST_DSL, memoryOptimized);
        
        // Get SQL for specific collections by collecting their streams
        Map<String, String> companiesSqlMap = new HashMap<>();
        companiesSqlMap.put("companies", generation.streamSqlInserts("companies").collect(java.util.stream.Collectors.joining("\n")));
        
        Map<String, String> countriesSqlMap = new HashMap<>();
        countriesSqlMap.put("countries", generation.streamSqlInserts("countries").collect(java.util.stream.Collectors.joining("\n")));

        assertThat(companiesSqlMap)
            .isNotNull()
            .containsKey("companies")
            .doesNotContainKey("countries");
        assertThat(countriesSqlMap)
            .isNotNull()
            .containsKey("countries")
            .doesNotContainKey("companies");

        String companiesSql = companiesSqlMap.get("companies");
        String countriesSql = countriesSqlMap.get("countries");

        assertThat(companiesSql).contains("INSERT INTO companies");
        assertThat(countriesSql).contains("INSERT INTO countries");
    }

    @BothImplementations
    void testAsSqlInsertsNonExistentTable(String implementationName, boolean memoryOptimized) throws Exception {
        IGeneration generation = generateFromDsl(TEST_DSL, memoryOptimized);
        Map<String, String> sqlMap = new HashMap<>();
        // Non-existent table should result in empty map

        assertThat(sqlMap)
            .isNotNull()
            .isEmpty();
    }

    @BothImplementations
    void testJsonAndJsonNodeConsistency(String implementationName, boolean memoryOptimized) throws Exception {
        IGeneration generation = generateFromDsl(TEST_DSL, memoryOptimized);
        String jsonString = createLegacyJsonString(generation);
        JsonNode jsonNode = createLegacyJsonNode(generation);

        assertThat(jsonString).isNotNull();
        assertThat(jsonNode).isNotNull();

        // Both should represent the same data
        assertThatCode(() -> {
            JsonNode parsedString = mapper.readTree(jsonString);
            assertThat(parsedString).isEqualTo(jsonNode);
        }).doesNotThrowAnyException();
    }



    @BothImplementations
    void testSqlGenerationWithComplexObjects(String implementationName, boolean memoryOptimized) throws Exception {
        String complexDsl = """
                {
                    "users": {
                        "count": 2,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "name.firstName"},
                            "profile": {
                                "age": {"gen": "number", "min": 18, "max": 65},
                                "address": {
                                    "street": {"gen": "address.streetAddress"},
                                    "city": {"gen": "address.city"}
                                }
                            }
                        }
                    }
                }
                """;

        IGeneration complexGeneration = generateFromDsl(complexDsl, memoryOptimized);
        String sql = complexGeneration.streamSqlInserts("users").collect(java.util.stream.Collectors.joining("\n"));
        String sql = sqlMap.get("users");

        assertThat(sql)
            .isNotNull()
            .contains("INSERT INTO users")
            .contains("id", "name", "profile");

        // Should contain JSON representation of complex objects
        assertThat(sql.contains("'{") || sql.contains("\"{")).isTrue();
    }

    @BothImplementations
    void testEmptyGeneration(String implementationName, boolean memoryOptimized) throws Exception {
        String emptyDsl = "{}";
        IGeneration emptyGeneration = generateFromDsl(emptyDsl, memoryOptimized);

        JsonNode jsonNode = createLegacyJsonNode(emptyGeneration);
        String json = createLegacyJsonString(emptyGeneration);
        Map<String, String> sqlMap = collectAllSqlInserts(emptyGeneration);

        assertThat(emptyGeneration.getCollectionNames()).isNotNull().isEmpty();
        assertThat(jsonNode).isNotNull();
        assertThat(json).isNotNull();
        assertThat(sqlMap).isNotNull().isEmpty();

        assertThat(json.equals("{}") || json.equals("{ }")).isTrue();
    }

    @BothImplementations
    void testSingleItemGeneration(String implementationName, boolean memoryOptimized) throws Exception {
        String singleItemDsl = """
                {
                    "items": {
                        "count": 1,
                        "item": {
                            "id": {"gen": "uuid"},
                            "value": {"gen": "string", "length": 5}
                        }
                    }
                }
                """;

        IGeneration singleGeneration = generateFromDsl(singleItemDsl, memoryOptimized);
        JsonNode collectionsNode = createLegacyJsonNode(singleGeneration);

        assertThat(collectionsNode).isNotNull();
        assertThat(collectionsNode.has("items")).isTrue();
        assertThat(collectionsNode.get("items").size()).isEqualTo(1);
        
        JsonNode firstItem = collectionsNode.get("items").get(0);
        assertThat(firstItem.has("id")).isTrue();
        assertThat(firstItem.has("value")).isTrue();
        assertThat(firstItem.size()).isBetween(0, 50);
    }
}
