package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNoException;

class GenerationTest {

    private Generation generation;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        mapper = new ObjectMapper();
        JsonNode dslNode = mapper.readTree("""
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
                """);
        generation = DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(dslNode)
            .generate();
    }

    @Test
    void testGetCollections() {
        Map<String, List<JsonNode>> collections = generation.getCollections();

        assertThat(collections)
            .isNotNull()
            .containsKeys("companies", "countries");

        List<JsonNode> companies = collections.get("companies");
        List<JsonNode> countries = collections.get("countries");

        assertThat(companies).isNotNull().isNotEmpty();
        assertThat(countries).isNotNull().isNotEmpty();
    }

    @Test
    void testGetCollectionsAsJsonNode() {
        JsonNode collectionsNode = generation.getCollectionsAsJsonNode();

        assertThat(collectionsNode).isNotNull();
        assertThat(collectionsNode.has("companies")).isTrue();
        assertThat(collectionsNode.has("countries")).isTrue();
        assertThat(collectionsNode.get("companies").isArray()).isTrue();
        assertThat(collectionsNode.get("countries").isArray()).isTrue();
        assertThat(collectionsNode.get("companies").isEmpty()).isFalse();
        assertThat(collectionsNode.get("countries").isEmpty()).isFalse();
    }

    @Test
    void testAsJson() throws Exception {
        String json = generation.asJson();

        assertThat(json)
            .isNotNull()
            .isNotEmpty()
            .contains("companies", "countries");

        // Verify it's valid JSON
        assertThatNoException().isThrownBy(() -> mapper.readTree(json));
    }

    @Test
    void testAsJsonNode() {
        JsonNode jsonNode = generation.asJsonNode();

        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode.has("companies")).isTrue();
        assertThat(jsonNode.has("countries")).isTrue();
        assertThat(jsonNode.get("companies").isArray()).isTrue();
        assertThat(jsonNode.get("countries").isArray()).isTrue();
    }

    @Test
    void testAsSqlInsertsAll() {
        Map<String, String> sqlMap = generation.asSqlInserts();

        assertThat(sqlMap)
            .isNotNull()
            .isNotEmpty()
            .containsKeys("companies", "countries");

        String companiesSql = sqlMap.get("companies");
        String countriesSql = sqlMap.get("countries");

        assertThat(companiesSql).contains("INSERT INTO companies");
        assertThat(countriesSql).contains("INSERT INTO countries");
    }

    @Test
    void testAsSqlInsertsSpecificTable() {
        Map<String, String> companiesSqlMap = generation.asSqlInserts("companies");
        Map<String, String> countriesSqlMap = generation.asSqlInserts("countries");

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

    @Test
    void testAsSqlInsertsNonExistentTable() {
        Map<String, String> sqlMap = generation.asSqlInserts("nonexistent");

        assertThat(sqlMap)
            .isNotNull()
            .isEmpty();
    }

    @Test
    void testJsonAndJsonNodeConsistency() throws Exception {
        String jsonString = generation.asJson();
        JsonNode jsonNode = generation.asJsonNode();

        assertThat(jsonString).isNotNull();
        assertThat(jsonNode).isNotNull();

        // Both should represent the same data
        assertThatCode(() -> {
            JsonNode parsedString = mapper.readTree(jsonString);
            assertThat(parsedString).isEqualTo(jsonNode);
        }).doesNotThrowAnyException();
    }

    @Test
    void testCollectionsAndJsonNodeConsistency() {
        Map<String, List<JsonNode>> collections = generation.getCollections();
        JsonNode collectionsNode = generation.getCollectionsAsJsonNode();

        assertThat(collections).isNotNull();
        assertThat(collectionsNode).isNotNull();

        // Verify same keys exist - eliminate for loop
        assertThat(collections.keySet())
            .as("Collections and collectionsNode should have same keys")
            .allSatisfy(key -> {
                assertThat(collectionsNode.has(key)).isTrue();
                assertThat(collectionsNode.get(key).size()).isEqualTo(collections.get(key).size());
            });
    }

    @Test
    void testSqlGenerationWithComplexObjects() throws Exception {
        JsonNode dslNode = mapper.readTree("""
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
                """);

        Generation complexGeneration = DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(dslNode)
            .generate();

        Map<String, String> sqlMap = complexGeneration.asSqlInserts("users");
        String sql = sqlMap.get("users");

        assertThat(sql)
            .isNotNull()
            .contains("INSERT INTO users")
            .contains("id", "name", "profile");

        // Should contain JSON representation of complex objects
        assertThat(sql.contains("'{") || sql.contains("\"{")).isTrue();
    }

    @Test
    void testEmptyGeneration() throws Exception {
        JsonNode emptyDsl = mapper.readTree("{}");

        Generation emptyGeneration = DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(emptyDsl)
            .generate();

        Map<String, List<JsonNode>> collections = emptyGeneration.getCollections();
        JsonNode jsonNode = emptyGeneration.asJsonNode();
        String json = emptyGeneration.asJson();
        Map<String, String> sqlMap = emptyGeneration.asSqlInserts();

        assertThat(collections).isNotNull().isEmpty();
        assertThat(jsonNode).isNotNull();
        assertThat(json).isNotNull();
        assertThat(sqlMap).isNotNull().isEmpty();

        assertThat(json.equals("{}") || json.equals("{ }")).isTrue();
    }

    @Test
    void testSingleItemGeneration() throws Exception {
        JsonNode singleItemDsl = mapper.readTree("""
                {
                    "items": {
                        "count": 1,
                        "item": {
                            "id": {"gen": "uuid"},
                            "value": {"gen": "string", "length": 5}
                        }
                    }
                }
                """);

        Generation singleGeneration = DslDataGenerator.create()
            .withSeed(123L)
            .fromJsonNode(singleItemDsl)
            .generate();

        Map<String, List<JsonNode>> collections = singleGeneration.getCollections();

        assertThat(collections)
            .isNotNull()
            .containsKey("items");
        assertThat(collections.get("items"))
            .hasSize(1);
        JsonNode firstItem = collections.get("items").get(0);
        assertThat(firstItem.has("id")).isTrue();
        assertThat(firstItem.has("value")).isTrue();
        assertThat(firstItem.size()).isBetween(0, 50);
    }
}
