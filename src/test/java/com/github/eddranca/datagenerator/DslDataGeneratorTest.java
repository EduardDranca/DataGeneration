package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.builder.DslTreeBuilder;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.validation.DslTreeBuildResult;
import net.datafaker.Faker;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.insert.Insert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.eddranca.datagenerator.ParameterizedGenerationTest.LegacyApiHelper.asJson;
import static com.github.eddranca.datagenerator.ParameterizedGenerationTest.LegacyApiHelper.asJsonNode;
import static com.github.eddranca.datagenerator.ParameterizedGenerationTest.LegacyApiHelper.asSqlInserts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class DslDataGeneratorTest extends ParameterizedGenerationTest {

    @Nested
    class SeedConsistencyTests extends ParameterizedGenerationTest {

        @BothImplementationsTest
        void testSeedConsistencyBasic(boolean memoryOptimized) throws IOException {
            JsonNode dslNode = mapper.readTree(
                """
                    {
                        "countries": {
                            "count": 3,
                            "item": {
                                "name": {"gen": "country.name"},
                                "isoCode": {"gen": "country.countryCode"},
                                "id": {"gen": "choice", "options": ["Romania", "Brasil"]},
                                "embargoed": {"gen": "choice", "options": [true, false, false, false]}
                            }
                        },
                        "companies": {
                            "count": 10,
                            "item": {
                                "id": {"gen": "uuid"},
                                "name": {"gen": "company.name"},
                                "countryCode": {"ref": "countries[*].isoCode", "filter": [{"ref": "countries[0].isoCode"}]},
                                "ctrName": {"ref": "countries[*].id", "filter": ["Romania"]}
                            }
                        }
                    }
                    """);

            Generation generation1 = generateFromDsl(dslNode, memoryOptimized);
            Generation generation2 = generateFromDsl(dslNode, memoryOptimized);

            assertThat(asJson(generation1)).isEqualTo(asJson(generation2));
        }

        @BothImplementationsTest
        void testSeedConsistencyWithCustomGenerators(boolean memoryOptimized) throws IOException {
            Generator customGenerator = options -> mapper.valueToTree("CUSTOM_FIXED_VALUE");

            JsonNode dslNode = mapper.readTree("""
                {
                    "items": {
                        "count": 5,
                        "item": {
                            "id": {"gen": "uuid"},
                            "customField": {"gen": "customTest"},
                            "name": {"gen": "name.firstName"}
                        }
                    }
                }
                """);

            DslDataGenerator.Builder builder1 = DslDataGenerator.create()
                .withSeed(456L)
                .withCustomGenerator("customTest", customGenerator);
            if (memoryOptimized) {
                builder1 = builder1.withMemoryOptimization();
            }
            Generation generation1 = builder1.fromJsonNode(dslNode).generate();

            DslDataGenerator.Builder builder2 = DslDataGenerator.create()
                .withSeed(456L)
                .withCustomGenerator("customTest", customGenerator);
            if (memoryOptimized) {
                builder2 = builder2.withMemoryOptimization();
            }
            Generation generation2 = builder2.fromJsonNode(dslNode).generate();

            assertThat(asJson(generation1)).isEqualTo(asJson(generation2));
        }

        @BothImplementationsTest
        void testDifferentSeedsProduceDifferentResults(boolean memoryOptimized) throws IOException {
            JsonNode dslNode = mapper.readTree("""
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
                """);

            Generation generation1 = generateFromDsl(dslNode, memoryOptimized);
            Generation generation2 = generateFromDslWithSeed(dslNode, 456L, memoryOptimized);

            assertThat(asJson(generation1)).isNotEqualTo(asJson(generation2));
        }

        @BothImplementationsTest
        void testSeedConsistencyWithFluentAPI(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree("""
                {
                    "users": {
                        "count": 5,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "name.firstName"},
                            "age": {"gen": "number", "min": 18, "max": 65}
                        }
                    }
                }
                """);

            Generation generation1 = generateFromDslWithSeed(dslNode, 789L, memoryOptimized);
            Generation generation2 = generateFromDslWithSeed(dslNode, 789L, memoryOptimized);

            String json1 = asJson(generation1);
            String json2 = asJson(generation2);

            assertThat(json1).isEqualTo(json2);
        }

        @Test
        void testSeedConsistencyWithLazySuppliers() throws Exception {
            JsonNode dslNode = mapper.readTree("""
                {
                    "items": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "uuid"},
                            "value": {"gen": "string", "length": 10}
                        }
                    }
                }
                """);

            Generation generation1 = generateFromDslWithSeed(dslNode, 999L, false);
            Generation generation2 = generateFromDslWithSeed(dslNode, 999L, false);

            assertThat(asJson(generation1)).isEqualTo(asJson(generation2));
        }

        @Test
        void testSeedConsistencyWithComplexNesting() throws Exception {
            JsonNode dslNode = mapper.readTree("""
                {
                    "companies": {
                        "count": 2,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "company.name"},
                            "address": {
                                "street": {"gen": "address.streetAddress"},
                                "city": {"gen": "address.city"}
                            }
                        }
                    }
                }
                """);

            Generation generation1 = generateFromDslWithSeed(dslNode, 111L, false);
            Generation generation2 = generateFromDslWithSeed(dslNode, 111L, false);

            String json1 = asJson(generation1);
            String json2 = asJson(generation2);

            assertThat(json1).isEqualTo(json2);
        }
    }

    @Nested
    class FilteringTests extends ParameterizedGenerationTest {

        @BothImplementationsTest
        void testOriginalSpreadTestFiltering(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree(
                """
                    {
                        "countries": {
                            "count": 3,
                            "item": {
                                "name": {"gen": "country.name"},
                                "isoCode": {"gen": "country.countryCode"},
                                "id": {"gen": "choice", "options": ["Romania", "Brasil"]},
                                "embargoed": {"gen": "choice", "options": [true, false, false, false]}
                            }
                        },
                        "companies": {
                            "count": 10,
                            "item": {
                                "id": {"gen": "uuid"},
                                "name": {"gen": "company.name"},
                                "countryCode": {"ref": "countries[*].isoCode", "filter": [{"ref": "countries[0].isoCode"}]},
                                "ctrName": {"ref": "countries[*].id", "filter": ["Romania"]}
                            }
                        }
                    }
                    """);

            Generation generation = generateFromDsl(dslNode, memoryOptimized);

            JsonNode collectionsNode = asJsonNode(generation);
            JsonNode countries = collectionsNode.get("countries");
            JsonNode companies = collectionsNode.get("companies");

            assertThat(countries).isNotNull();
            assertThat(companies).isNotNull();
            assertThat(countries.size()).isGreaterThan(0);

            String firstCountryCode = countries.get(0).get("isoCode").asText();
            for (JsonNode comp : companies) {
                assertThat(comp.get("countryCode").asText())
                    .as("Company countryCode should not match the filtered first country isoCode")
                    .isNotEqualTo(firstCountryCode);
            }
        }

        @BothImplementationsTest
        void testDirectReferenceWithFiltering(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree(
                """
                    {
                        "locations": {
                            "count": 5,
                            "item": {
                                "name": {"gen": "choice", "options": ["New York", "London", "Tokyo", "Paris", "Berlin"]},
                                "country": {"gen": "choice", "options": ["USA", "UK", "Japan", "France", "Germany"]},
                                "continent": {"gen": "choice", "options": ["North America", "Europe", "Asia", "Europe", "Europe"]}
                            }
                        },
                        "events": {
                            "count": 10,
                            "item": {
                                "name": {"gen": "choice", "options": ["Conference", "Workshop", "Meetup"]},
                                "location": {"ref": "locations[*]"},
                                "filteredLocationName": {"ref": "locations[*].name", "filter": ["New York", "London"]},
                                "continentBasedLocation": {"ref": "locations[*].continent", "filter": ["Asia"]}
                            }
                        }
                    }
                    """);

            Generation generation = generateFromDsl(dslNode, memoryOptimized);

            JsonNode collectionsNode = asJsonNode(generation);
            JsonNode locations = collectionsNode.get("locations");
            JsonNode events = collectionsNode.get("events");

            assertThat(locations)
                .isNotNull()
                .hasSize(5);
            assertThat(events)
                .isNotNull()
                .hasSize(10)
                .allSatisfy(event -> {
                    String filteredLocationName = event.get("filteredLocationName").asText();
                    assertThat(filteredLocationName)
                        .as("Filtered location should be one of: Tokyo, Paris, Berlin")
                        .isIn("Tokyo", "Paris", "Berlin");
                })
                .allSatisfy(event -> {
                    String continentBasedLocation = event.get("continentBasedLocation").asText();
                    assertThat(continentBasedLocation)
                        .isNotNull()
                        .as("Continent-based location should not be Asia")
                        .isNotEqualTo("Asia")
                        .as("Continent-based location should be North America or Europe")
                        .isIn("North America", "Europe");
                });

            // Verify that regular location reference (without filtering) can be any
            // location
            Set<String> allLocationNames = new HashSet<>();
            for (JsonNode loc : locations) {
                allLocationNames.add(loc.get("name").asText());
            }

            Set<String> eventLocationNames = new HashSet<>();
            for (JsonNode event : events) {
                eventLocationNames.add(event.get("location").get("name").asText());
            }

            // Regular location references should potentially include all locations
            assertThat(eventLocationNames)
                .as("Should have at least some location references")
                .isNotEmpty();

            // Verify that the locations collection contains valid location names
            List<String> expectedLocationNames = List.of("New York", "London", "Tokyo", "Paris", "Berlin");
            assertThat(allLocationNames)
                .as("All location names should be from expected values")
                .isNotEmpty()
                .allSatisfy(locationName -> assertThat(expectedLocationNames)
                    .as("Location name should be one of the expected values: " + locationName)
                    .contains(locationName));
        }
    }

    @Nested
    class SelfReferenceTests extends ParameterizedGenerationTest {
        @BothImplementationsTest
        void testSelfReferenceValidation(boolean memoryOptimized) throws Exception {
            // Test valid simple self-reference
            JsonNode validDsl = mapper.readTree("""
                {
                    "users": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "name.firstName"},
                            "email": {"gen": "internet.emailAddress"},
                            "displayName": {"ref": "this.name"}
                        }
                    }
                }
                """);

            // This should work without validation errors
            Generation generation = generateFromDsl(validDsl, memoryOptimized);

            assertThat(generation).isNotNull();
            JsonNode collectionsNode = asJsonNode(generation);
            JsonNode users = collectionsNode.get("users");

            assertThat(users).isNotNull();
            assertThat(users.size()).isEqualTo(2);

            for (JsonNode user : users) {
                assertThat(user.get("displayName"))
                    .as("displayName should match name via self-reference")
                    .isEqualTo(user.get("name"));
            }
        }

        @Test
        void testInvalidSelfReferenceValidation() throws Exception {
            // Test invalid self-reference to non-existent field
            JsonNode invalidDsl = mapper.readTree("""
                {
                    "users": {
                        "count": 1,
                        "item": {
                            "name": {"gen": "name.firstName"},
                            "invalidRef": {"ref": "this.nonExistentField"}
                        }
                    }
                }
                """);

            // Test using DslTreeBuilder directly to check validation
            DslTreeBuilder builder = new DslTreeBuilder(GeneratorRegistry.withDefaultGenerators(new Faker()));

            DslTreeBuildResult result = builder.build(invalidDsl);

            // Should have validation errors
            assertThat(result.hasErrors())
                .as("Should have validation errors for invalid self-reference")
                .isTrue();

            // Check that the error message contains information about the invalid reference
            assertThat(result.getErrors())
                .as("Should have error about nonExistentField reference. Errors: " +
                    result.getErrors().stream().map(Object::toString).toList())
                .anyMatch(error -> error.toString().contains("nonExistentField"));
        }

        @Test
        void testInvalidSelfReferenceToSubfield() throws Exception {
            // Test invalid self-reference to subfield of non-object field
            JsonNode invalidDsl = mapper.readTree("""
                {
                    "users": {
                        "count": 1,
                        "item": {
                            "name": {"gen": "name.firstName"},
                            "invalidRef": {"ref": "this.name.subfield"}
                        }
                    }
                }
                """);

            // This should throw a validation exception
            assertThatThrownBy(() -> DslDataGenerator.create()
                .fromJsonNode(invalidDsl)
                .generate())
                .isInstanceOf(Exception.class);
        }
    }

    @Nested
    class SpreadOperatorTests extends ParameterizedGenerationTest {

        @BothImplementationsTest
        void testSpreadOperatorBasic(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree("""
                {
                    "countries": {
                        "count": 3,
                        "item": {
                            "...countryDetails": {
                                "gen": "country",
                                "fields": ["name", "countryCode"]
                            },
                            "id": {"gen": "uuid"}
                        }
                    }
                }
                """);

            Generation generation = generateFromDsl(dslNode, memoryOptimized);

            assertThat(asJson(generation))
                .isNotNull()
                .contains("countries");

            JsonNode collectionsNode = asJsonNode(generation);
            JsonNode countries = collectionsNode.get("countries");

            // Verify spread fields are present
            assertThat(countries).isNotNull();
            assertThat(countries.size()).isEqualTo(3);

            for (JsonNode country : countries) {
                assertThat(country.has("name")).as("Country should have name field").isTrue();
                assertThat(country.has("countryCode")).as("Country should have countryCode field").isTrue();
                assertThat(country.has("id")).as("Country should have id field").isTrue();
                assertThat(country.get("name")).as("Country name should not be null").isNotNull();
                assertThat(country.get("countryCode")).as("Country countryCode should not be null").isNotNull();
                assertThat(country.get("id")).as("Country id should not be null").isNotNull();
            }
        }

        @BothImplementationsTest
        void testSpreadOperatorWithFieldMapping(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree("""
                {
                    "countries": {
                        "count": 2,
                        "item": {
                            "...countryDetails": {
                                "gen": "country",
                                "fields": ["name", "isoCode:countryCode"]
                            },
                            "id": {"gen": "uuid"},
                            "active": {"gen": "choice", "options": [true, false]}
                        }
                    }
                }
                """);

            Generation generation = generateFromDslWithSeed(dslNode, 456L, memoryOptimized);

            JsonNode collectionsNode = asJsonNode(generation);
            JsonNode countries = collectionsNode.get("countries");

            assertThat(countries)
                .as("All countries should have mapped fields with non-null values")
                .hasSize(2)
                .allSatisfy(country -> {
                    assertThat(country.has("name")).isTrue();
                    assertThat(country.has("isoCode")).isTrue();
                    assertThat(country.has("id")).isTrue();
                    assertThat(country.has("active")).isTrue();
                    assertThat(country.has("countryCode")).isFalse(); // Should not contain the original field name

                    assertThat(country.get("name")).isNotNull();
                    assertThat(country.get("isoCode")).isNotNull();
                    assertThat(country.get("id")).isNotNull();
                    assertThat(country.get("active")).isNotNull();
                });
        }

        @BothImplementationsTest
        void testSpreadOperatorWithComplexObjects(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree("""
                {
                    "companies": {
                        "count": 1,
                        "item": {
                            "companyName": {"gen": "company.name"},
                            "address": {
                                "street": {"gen": "address.streetAddress"},
                                "city": {"gen": "address.city"},
                                "zipCode": {"gen": "address.zipCode"},
                                "foo": {
                                    "bar": {"gen": "country.name"},
                                    "baz": {"gen": "book.title"}
                                }
                            },
                            "contact": {
                                "name": {
                                    "firstName": {"gen": "name.firstName"},
                                    "lastName": {"gen": "name.lastName"},
                                    "fullName": {"gen": "name.fullName"},
                                    "prefix": {"gen": "name.prefix"},
                                    "suffix": {"gen": "name.suffix"},
                                    "title": {"gen": "name.title"}
                                },
                                "email": {"gen": "internet.emailAddress"}
                            }
                        }
                    }
                }
                """);

            Generation generation = generateFromDsl(dslNode, memoryOptimized);

            String json = asJson(generation);
            assertThat(json)
                .isNotNull()
                .contains("companyName", "address", "contact");
        }

        @BothImplementationsTest
        void testMultipleSpreadOperators(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree("""
                {
                    "users": {
                        "count": 2,
                        "item": {
                            "...nameDetails": {
                                "gen": "name",
                                "fields": ["firstName", "lastName"]
                            },
                            "...contactDetails": {
                                "gen": "internet",
                                "fields": ["email:emailAddress"]
                            },
                            "id": {"gen": "uuid"},
                            "age": {"gen": "number", "min": 18, "max": 65}
                        }
                    }
                }
                """);

            Generation generation = generateFromDslWithSeed(dslNode, 789L, memoryOptimized);

            JsonNode collectionsNode = asJsonNode(generation);
            JsonNode users = collectionsNode.get("users");

            assertThat(users).isNotNull();
            assertThat(users.size()).isEqualTo(2);

            for (JsonNode user : users) {
                // From name spread and internet spread with mapping, plus regular fields
                assertThat(user.has("firstName")).isTrue();
                assertThat(user.has("lastName")).isTrue();
                assertThat(user.has("email")).isTrue();
                assertThat(user.has("id")).isTrue();
                assertThat(user.has("age")).isTrue();
                assertThat(user.has("emailAddress")).isFalse(); // Should not contain original field name

                // Verify all values are not null
                assertThat(user.get("firstName")).isNotNull();
                assertThat(user.get("lastName")).isNotNull();
                assertThat(user.get("email")).isNotNull();
                assertThat(user.get("id")).isNotNull();
                assertThat(user.get("age")).isNotNull();
            }
        }

        @BothImplementationsTest
        void testSpreadOperatorWithoutFieldsUsesAllFields(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree("""
                {
                    "users": {
                        "count": 2,
                        "item": {
                            "...nameDetails": {
                                "gen": "name"
                            },
                            "id": {"gen": "uuid"}
                        }
                    }
                }
                """);

            Generation generation = generateFromDsl(dslNode, memoryOptimized);

            JsonNode collectionsNode = asJsonNode(generation);
            JsonNode users = collectionsNode.get("users");

            assertThat(users).isNotNull();
            assertThat(users.size()).isEqualTo(2);

            for (JsonNode user : users) {
                // These are the fields that the name generator provides plus regular field
                assertThat(user.has("firstName")).isTrue();
                assertThat(user.has("lastName")).isTrue();
                assertThat(user.has("fullName")).isTrue();
                assertThat(user.has("title")).isTrue();
                assertThat(user.has("prefix")).isTrue();
                assertThat(user.has("suffix")).isTrue();
                assertThat(user.has("id")).isTrue();

                // Verify all values are not null
                assertThat(user.get("firstName")).isNotNull();
                assertThat(user.get("lastName")).isNotNull();
                assertThat(user.get("fullName")).isNotNull();
                assertThat(user.get("title")).isNotNull();
                assertThat(user.get("prefix")).isNotNull();
                assertThat(user.get("suffix")).isNotNull();
                assertThat(user.get("id")).isNotNull();
            }
        }

        @BothImplementationsTest
        void testSpreadOperatorSqlGeneration(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree("""
                {
                    "companies": {
                        "count": 1,
                        "item": {
                            "companyName": {"gen": "company.name"},
                            "address": {
                                "street": {"gen": "address.streetAddress"},
                                "city": {"gen": "address.city"},
                                "zipCode": {"gen": "address.zipCode"}
                            }
                        }
                    }
                }
                """);

            Generation generation = generateFromDsl(dslNode, memoryOptimized);
            Map<String, String> sqlInserts = asSqlInserts(generation);

            assertThat(sqlInserts)
                .isNotNull()
                .containsKey("companies");

            String companiesSql = sqlInserts.get("companies");
            assertThat(companiesSql)
                .contains("INSERT INTO companies", "companyName", "address");
        }

        @BothImplementationsTest
        void testCsvGeneratorSpreadIntegration(boolean memoryOptimized) throws Exception {
            String csvPath = "src/test/resources/test.csv";

            JsonNode dslNode = mapper.readTree(String.format("""
                {
                    "rows": {
                        "count": 3,
                        "item": {
                            "...csvRow": {
                                "gen": "csv",
                                "file": "%s",
                                "sequential": true
                            }
                        }
                    }
                }
                """, csvPath));

            Generation generation = generateFromDsl(dslNode, memoryOptimized);

            JsonNode collectionsNode = asJsonNode(generation);
            JsonNode rows = collectionsNode.get("rows");

            assertThat(rows).isNotNull();
            assertThat(rows.size()).isEqualTo(3);

            // Validate first two CSV rows match test.csv content and that sequential wraps
            assertThat(rows.get(0).get("header1").asText()).isEqualTo("value1");
            assertThat(rows.get(0).get("header2").asText()).isEqualTo("value2");

            assertThat(rows.get(1).get("header1").asText()).isEqualTo("value3");
            assertThat(rows.get(1).get("header2").asText()).isEqualTo("value4");

            // Third row should wrap back to the first row when sequential
            assertThat(rows.get(2).get("header1").asText()).isEqualTo("value1");
            assertThat(rows.get(2).get("header2").asText()).isEqualTo("value2");
        }
    }

    @Nested
    class SqlGenerationTests extends ParameterizedGenerationTest {

        @BothImplementationsTest
        void testSqlInsertsWithComplexObjects(boolean memoryOptimized) throws Exception {
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

            Generation generation = generateFromDsl(dslNode, memoryOptimized);
            Map<String, String> sqlInserts = asSqlInserts(generation);

            assertThat(sqlInserts)
                .isNotNull()
                .containsKey("users");

            String usersSql = sqlInserts.get("users");
            assertThat(usersSql).isNotNull();

            // Validate SQL structure and syntax
            validateSqlInsertStructure(usersSql, "users");

            // Validate that all expected columns are present
            assertThat(usersSql)
                .as("SQL should contain all expected columns")
                .contains("id", "name", "profile");

            // Validate that we have exactly 2 valid INSERT statements (count: 2)
            validateAllSqlStatements(usersSql, "users", 2);
        }

        @BothImplementationsTest
        void testSqlSubsetGeneration(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree("""
                {
                    "countries": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "country.name"},
                            "isoCode": {"gen": "country.countryCode"}
                        }
                    },
                    "companies": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "company.name"}
                        }
                    }
                }
                """);

            Generation generation = generateFromDsl(dslNode, memoryOptimized);

            // Test subset generation for companies only
            Map<String, Stream<String>> companiesSqlStreamMap = generation.asSqlInserts("companies");
            Map<String, String> companiesSqlMap = new HashMap<>();
            for (Map.Entry<String, Stream<String>> entry : companiesSqlStreamMap.entrySet()) {
                companiesSqlMap.put(entry.getKey(), entry.getValue().collect(Collectors.joining("\n")));
            }
            assertThat(companiesSqlMap)
                .isNotNull()
                .hasSize(1)
                .as("Should only contain companies table")
                .containsKey("companies");

            String companiesSql = companiesSqlMap.get("companies");
            validateAllSqlStatements(companiesSql, "companies", 3);

            // Test subset generation for countries only
            Map<String, Stream<String>> countriesSqlStreamMap = generation.asSqlInserts("countries");
            Map<String, String> countriesSqlMap = new HashMap<>();
            for (Map.Entry<String, Stream<String>> entry : countriesSqlStreamMap.entrySet()) {
                countriesSqlMap.put(entry.getKey(), entry.getValue().collect(Collectors.joining("\n")));
            }
            assertThat(countriesSqlMap)
                .isNotNull()
                .hasSize(1)
                .as("Should only contain countries table")
                .containsKey("countries");

            String countriesSql = countriesSqlMap.get("countries");
            validateAllSqlStatements(countriesSql, "countries", 2);

            // Validate column structure using JSqlParser
            validateSqlColumns(countriesSql, "countries", "name", "isoCode");
            validateSqlColumns(companiesSql, "companies", "id", "name");
        }

        @BothImplementationsTest
        void testSqlGenerationWithAllTables(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree("""
                {
                    "countries": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "country.name"},
                            "isoCode": {"gen": "country.countryCode"}
                        }
                    },
                    "companies": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "company.name"}
                        }
                    }
                }
                """);

            Generation generation = generateFromDsl(dslNode, memoryOptimized);

            Map<String, String> allSqlMap = asSqlInserts(generation);
            assertThat(allSqlMap)
                .isNotNull()
                .hasSize(2)
                .as("Should contain exactly 2 tables")
                .containsKeys("companies", "countries");

            String companiesSql = allSqlMap.get("companies");
            String countriesSql = allSqlMap.get("countries");

            // Validate both SQL statements with correct counts
            validateAllSqlStatements(companiesSql, "companies", 3);
            validateAllSqlStatements(countriesSql, "countries", 2);
        }

        @BothImplementationsTest
        void testSqlGenerationWithSpecialCharacters(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree(
                """
                    {
                        "products": {
                            "count": 1,
                            "item": {
                                "name": {"gen": "choice", "options": ["Product's Name", "Product Quote", "Product Newline"]},
                                "description": {"gen": "choice", "options": ["It's great!", "Contains quotes", "Multi Line Text"]}
                            }
                        }
                    }
                    """);

            Generation generation = generateFromDsl(dslNode, memoryOptimized);
            Map<String, String> sqlInserts = asSqlInserts(generation);

            String productsSql = sqlInserts.get("products");
            assertThat(productsSql).isNotNull();

            // Validate that special characters are properly escaped
            validateSqlInsertStructure(productsSql, "products");

            // Check that quotes are properly escaped (should be doubled in SQL)
            if (productsSql.contains("Product's Name")) {
                assertThat(productsSql)
                    .as("Single quotes should be properly escaped")
                    .satisfiesAnyOf(
                        sql -> assertThat(sql).contains("Product''s Name"),
                        sql -> assertThat(sql).contains("'Product''s Name'"));
            }
        }

        @BothImplementationsTest
        void testSqlGenerationWithNullValues(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree("""
                {
                    "items": {
                        "count": 1,
                        "item": {
                            "id": {"gen": "uuid"},
                            "optionalField": {"gen": "choice", "options": [null, "value"]}
                        }
                    }
                }
                """);

            Generation generation = generateFromDsl(dslNode, memoryOptimized);
            Map<String, String> sqlInserts = asSqlInserts(generation);

            String itemsSql = sqlInserts.get("items");
            assertThat(itemsSql).isNotNull();

            validateSqlInsertStructure(itemsSql, "items");

            // Check that null values are properly handled
            assertThat(itemsSql)
                .as("Both columns should be present")
                .contains("id", "optionalField");
        }

        @BothImplementationsTest
        void testSqlGenerationValidatesWithParser(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree("""
                {
                    "employees": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "uuid"},
                            "firstName": {"gen": "name.firstName"},
                            "lastName": {"gen": "name.lastName"},
                            "email": {"gen": "internet.emailAddress"},
                            "age": {"gen": "number", "min": 18, "max": 65},
                            "salary": {"gen": "float", "min": 30000.0, "max": 150000.0},
                            "active": {"gen": "choice", "options": [true, false]}
                        }
                    }
                }
                """);

            Generation generation = generateFromDsl(dslNode, memoryOptimized);
            Map<String, String> sqlInserts = asSqlInserts(generation);

            String employeesSql = sqlInserts.get("employees");
            assertThat(employeesSql).isNotNull();

            // Use JSqlParser to validate structure and content
            validateAllSqlStatements(employeesSql, "employees", 3);

            // Validate all expected columns are present
            validateSqlColumns(employeesSql, "employees",
                "id", "firstName", "lastName", "email", "age", "salary", "active");
        }

        @BothImplementationsTest
        void testSqlGenerationSyntaxValidation(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree(
                """
                    {
                        "test_table": {
                            "count": 2,
                            "item": {
                                "string_field": {"gen": "name.firstName"},
                                "number_field": {"gen": "number", "min": 1, "max": 100},
                                "float_field": {"gen": "float", "min": 0.0, "max": 1.0},
                                "boolean_field": {"gen": "choice", "options": [true, false]},
                                "null_field": {"gen": "choice", "options": [null, "value"]},
                                "special_chars": {"gen": "choice", "options": ["O'Reilly", "Smith & Co", "Test\\"Quote"]}
                            }
                        }
                    }
                    """);

            Generation generation = generateFromDsl(dslNode, memoryOptimized);
            Map<String, String> sqlInserts = asSqlInserts(generation);

            String testTableSql = sqlInserts.get("test_table");
            assertThat(testTableSql).isNotNull();

            // Validate using JSqlParser - this will catch any syntax errors
            validateAllSqlStatements(testTableSql, "test_table", 2);

            // Validate all expected columns
            validateSqlColumns(testTableSql, "test_table",
                "string_field", "number_field", "float_field", "boolean_field", "null_field", "special_chars");
        }

        @BothImplementationsTest
        void testSqlGenerationWithNumericValues(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree("""
                {
                    "metrics": {
                        "count": 2,
                        "item": {
                            "id": {"gen": "number", "min": 1, "max": 1000},
                            "score": {"gen": "float", "min": 0.0, "max": 100.0},
                            "active": {"gen": "choice", "options": [true, false]}
                        }
                    }
                }
                """);

            Generation generation = generateFromDsl(dslNode, memoryOptimized);
            Map<String, String> sqlInserts = asSqlInserts(generation);

            String metricsSql = sqlInserts.get("metrics");
            assertThat(metricsSql).isNotNull();

            // Validate using JSqlParser
            validateAllSqlStatements(metricsSql, "metrics", 2);
            validateSqlColumns(metricsSql, "metrics", "id", "score", "active");
        }

        @BothImplementationsTest
        void testFloatGeneratorWithDecimalsConfiguration(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree("""
                {
                    "products": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "uuid"},
                            "price": {"gen": "float", "min": 10.0, "max": 100.0, "decimals": 2},
                            "weight": {"gen": "float", "min": 0.1, "max": 5.0, "decimals": 3},
                            "rating": {"gen": "float", "min": 1.0, "max": 5.0, "decimals": 1},
                            "discount": {"gen": "float", "min": 0.0, "max": 1.0, "decimals": 0}
                        }
                    }
                }
                """);

            Generation generation = generateFromDsl(dslNode, memoryOptimized);

            // Test JSON generation
            JsonNode collectionsNode = asJsonNode(generation);
            JsonNode products = collectionsNode.get("products");

            assertThat(products).isNotNull();
            assertThat(products.size()).isEqualTo(3);

            for (JsonNode product : products) {
                // Verify all required fields are present and not null
                assertThat(product.get("price")).isNotNull();
                assertThat(product.get("weight")).isNotNull();
                assertThat(product.get("rating")).isNotNull();
                assertThat(product.get("discount")).isNotNull();

                // Verify ranges using fluent assertions
                double price = product.get("price").doubleValue();
                double weight = product.get("weight").doubleValue();
                double rating = product.get("rating").doubleValue();
                double discount = product.get("discount").doubleValue();

                assertThat(price).isBetween(10.0, 100.0);
                assertThat(weight).isBetween(0.1, 5.0);
                assertThat(rating).isBetween(1.0, 5.0);
                assertThat(discount).isBetween(0.0, 1.0);

                // Verify discount is a whole number (0 decimals)
                assertThat(discount)
                    .as("Discount should be a whole number")
                    .isCloseTo(Math.floor(discount), within(0.0001));
            }
        }

        /**
         * Helper method to parse SQL statement and wrap exceptions
         */
        private Statement parseSqlStatement(String sql) {
            try {
                return CCJSqlParserUtil.parse(sql);
            } catch (JSQLParserException e) {
                throw new IllegalStateException("Failed to parse SQL: " + sql + ". Error: " + e.getMessage(), e);
            }
        }

        /**
         * Helper method to validate SQL INSERT structure using JSqlParser
         */
        private void validateSqlInsertStructure(String sql, String tableName) {
            assertThat(sql)
                .as("SQL should not be null")
                .isNotNull();
            assertThat(sql.trim())
                .as("SQL should not be empty")
                .isNotBlank();

            // Parse each SQL statement using JSqlParser
            String[] statements = sql.split(";");
            int validInsertCount = 0;

            for (String statement : statements) {
                String trimmedStatement = statement.trim();
                if (trimmedStatement.isEmpty()) {
                    continue;
                }

                // Parse the SQL statement
                Statement parsedStatement = parseSqlStatement(trimmedStatement);

                // Verify it's an INSERT statement
                assertThat(parsedStatement)
                    .as("Statement should be an INSERT: " + trimmedStatement)
                    .isInstanceOf(Insert.class);

                Insert insertStatement = (Insert) parsedStatement;

                // Verify table name
                assertThat(insertStatement.getTable().getName())
                    .as("Table name should match expected: " + tableName)
                    .isEqualTo(tableName);

                // Verify it has columns
                assertThat(insertStatement.getColumns())
                    .as("INSERT should have columns defined")
                    .isNotNull()
                    .as("INSERT should have at least one column")
                    .isNotEmpty();

                // Verify it has values
                assertThat(insertStatement.getValues())
                    .as("INSERT should have VALUES clause")
                    .isNotNull();

                validInsertCount++;
            }

            assertThat(validInsertCount)
                .as("Should have at least one valid INSERT statement")
                .isPositive();
        }

        /**
         * Helper method to validate and parse all SQL statements in a multi-statement
         * SQL string
         */
        private void validateAllSqlStatements(String sql, String expectedTableName, int expectedCount) {
            String[] statements = sql.split(";");
            int validInsertCount = 0;

            for (String statement : statements) {
                String trimmedStatement = statement.trim();
                if (trimmedStatement.isEmpty()) {
                    continue;
                }

                Statement parsedStatement = parseSqlStatement(trimmedStatement);
                assertThat(parsedStatement)
                    .as("Statement should be an INSERT: " + trimmedStatement)
                    .isInstanceOf(Insert.class);

                Insert insertStatement = (Insert) parsedStatement;
                assertThat(insertStatement.getTable().getName())
                    .as("Table name should match: " + expectedTableName)
                    .isEqualTo(expectedTableName);

                // Validate that columns and values count match
                int columnCount = insertStatement.getColumns().size();
                assertThat(columnCount)
                    .as("Should have at least one column")
                    .isPositive();

                validInsertCount++;
            }

            assertThat(validInsertCount)
                .as("Should have exactly " + expectedCount + " valid INSERT statements")
                .isEqualTo(expectedCount);
        }

        /**
         * Helper method to extract and validate column names from SQL INSERT statements
         */
        private void validateSqlColumns(String sql, String tableName, String... expectedColumns) {
            String[] statements = sql.split(";");

            for (String statement : statements) {
                String trimmedStatement = statement.trim();
                if (trimmedStatement.isEmpty()) {
                    continue;
                }

                Statement parsedStatement = parseSqlStatement(trimmedStatement);
                assertThat(parsedStatement).isInstanceOf(Insert.class);

                Insert insertStatement = (Insert) parsedStatement;
                assertThat(insertStatement.getTable().getName()).isEqualTo(tableName);

                // Extract column names
                List<String> actualColumns = insertStatement.getColumns().stream()
                    .map(Column::getColumnName)
                    .toList();

                // Verify expected columns are present
                assertThat(actualColumns)
                    .as("All expected columns should be present")
                    .containsAll(Arrays.asList(expectedColumns));
            }
        }
    }

    @Nested
    class FluentApiTests extends ParameterizedGenerationTest {

        @Test
        void testFluentApiWithCustomGenerator() throws Exception {
            Generator customGenerator = node -> mapper.valueToTree("CUSTOM_VALUE_" + System.currentTimeMillis());
            JsonNode dslNode = mapper.readTree("""
                {
                    "items": {
                        "count": 1,
                        "item": {
                            "id": {"gen": "uuid"},
                            "customField": {"gen": "customTest"}
                        }
                    }
                }
                """);

            Generation generation = DslDataGenerator.create()
                .withSeed(456L)
                .withCustomGenerator("customTest", customGenerator)
                .fromJsonNode(dslNode)
                .generate();
            String json = asJson(generation);

            assertThat(json)
                .isNotNull()
                .contains("CUSTOM_VALUE_", "customField");
        }

        @BothImplementationsTest
        void testJsonNodeMethods(boolean memoryOptimized) throws Exception {
            JsonNode dslNode = mapper.readTree("""
                {
                    "countries": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "country.name"},
                            "isoCode": {"gen": "country.countryCode"}
                        }
                    },
                    "companies": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "company.name"}
                        }
                    }
                }
                """);

            Generation generation = generateFromDsl(dslNode, memoryOptimized);

            assertThat(asJsonNode(generation)).isNotNull();
            assertThat(asJsonNode(generation)).isNotNull();

            JsonNode jsonNode = asJsonNode(generation);
            assertThat(jsonNode.has("countries")).isTrue();
            assertThat(jsonNode.has("companies")).isTrue();

            String jsonFromNode = asJsonNode(generation).toString();
            String jsonFromMap = asJson(generation);

            assertThat(jsonFromNode).isNotNull();
            assertThat(jsonFromMap).isNotNull();
        }
    }

    @Nested
    class FileBasedTests {

        @Test
        void testGenerationFromFile(@TempDir Path tempDir) throws IOException {
            // Create a temporary JSON file
            String jsonContent = """
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

            Path tempFile = tempDir.resolve("test.json");
            Files.writeString(tempFile, jsonContent);

            // Test fromFile(File)
            Generation generation1 = DslDataGenerator.create()
                .withSeed(123L)
                .fromFile(tempFile.toFile())
                .generate();

            assertThat(generation1)
                .isNotNull();
            assertThat(asJson(generation1))
                .contains("users");

            // Test fromFile(String)
            Generation generation2 = DslDataGenerator.create()
                .withSeed(123L)
                .fromFile(tempFile.toString())
                .generate();

            assertThat(generation2)
                .isNotNull();
            assertThat(asJson(generation2))
                .isEqualTo(asJson(generation1));

            // Test fromFile(Path)
            Generation generation3 = DslDataGenerator.create()
                .withSeed(123L)
                .fromFile(tempFile)
                .generate();

            assertThat(generation3)
                .isNotNull();
            assertThat(asJson(generation3))
                .isEqualTo(asJson(generation1));
        }

        @Test
        void testGenerationFromJsonString() throws IOException {
            String jsonString = """
                {
                    "products": {
                        "count": 2,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "string", "length": 10},
                            "price": {"gen": "number", "min": 1, "max": 100}
                        }
                    }
                }
                """;

            Generation generation1 = generateFromDslWithSeed(jsonString, 456L, false);
            Generation generation2 = generateFromDslWithSeed(jsonString, 456L, false);

            assertThat(generation1).isNotNull();
            assertThat(generation2).isNotNull();
            assertThat(asJson(generation2)).isEqualTo(asJson(generation1));
            assertThat(asJson(generation1)).contains("products");
        }

        @Test
        void testFileNotFound() {
            assertThatThrownBy(() -> DslDataGenerator.create()
                .fromFile("nonexistent.json")
                .generate())
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void testGenerationFromFileWithComplexStructure(@TempDir Path tempDir) throws IOException {
            String complexJson = """
                {
                    "countries": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "country.name"},
                            "isoCode": {"gen": "country.countryCode"},
                            "id": {"gen": "choice", "options": ["Romania", "Brasil"]}
                        }
                    },
                    "companies": {
                        "count": 5,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "company.name"},
                            "countryCode": {"ref": "countries[*].isoCode"}
                        }
                    }
                }
                """;

            Path tempFile = tempDir.resolve("complex.json");
            Files.writeString(tempFile, complexJson);

            Generation generation = DslDataGenerator.create()
                .withSeed(789L)
                .fromFile(tempFile)
                .generate();

            assertThat(generation).isNotNull();
            JsonNode collectionsNode = asJsonNode(generation);

            assertThat(collectionsNode.has("countries")).isTrue();
            assertThat(collectionsNode.has("companies")).isTrue();
            assertThat(collectionsNode.get("countries").size()).isEqualTo(2);
            assertThat(collectionsNode.get("companies").size()).isEqualTo(5);

            JsonNode companies = collectionsNode.get("companies");
            assertThat(companies).isNotNull();
            assertThat(companies.size()).isGreaterThan(0);

            for (JsonNode company : companies) {
                assertThat(company.get("countryCode")).isNotNull();
            }
        }
    }
}
