package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.builder.DslTreeBuilder;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.validation.DslTreeBuildResult;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class DataGenerationVisitorTest {

    private GeneratorRegistry registry;
    private DslTreeBuilder builder;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        registry = GeneratorRegistry.withDefaultGenerators(new Faker());
        builder = new DslTreeBuilder(registry);
        mapper = new ObjectMapper();
    }

    @Test
    void testSimpleGeneration() throws Exception {
        JsonNode dsl = mapper.readTree("""
                {
                    "users": {
                        "count": 2,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "name.firstName"},
                            "age": {"gen": "number", "min": 18, "max": 65}
                        }
                    }
                }
                """);

        DslTreeBuildResult buildResult = builder.build(dsl);
        assertThat(buildResult.hasErrors()).isFalse();

        GenerationContext context = new GenerationContext(registry, new Random(123L));
        DataGenerationVisitor visitor = new DataGenerationVisitor(context);

        JsonNode result = buildResult.getTree().accept(visitor);

        assertThat(result).isNotNull();
        assertThat(result.has("users")).isTrue();
        assertThat(result.get("users").isArray()).isTrue();
        assertThat(result.get("users")).hasSize(2);

        JsonNode firstUser = result.get("users").get(0);
        assertThat(firstUser.has("id")).isTrue();
        assertThat(firstUser.has("name")).isTrue();
        assertThat(firstUser.has("age")).isTrue();

        // Verify types
        assertThat(firstUser.get("id").isTextual()).isTrue();
        assertThat(firstUser.get("name").isTextual()).isTrue();
        assertThat(firstUser.get("age").isNumber()).isTrue();
    }

    @Test
    void testChoiceGeneration() throws Exception {
        JsonNode dsl = mapper.readTree("""
                {
                    "items": {
                        "count": 5,
                        "item": {
                            "status": {
                                "gen": "choice",
                                "options": ["active", "inactive", "pending"]
                            }
                        }
                    }
                }
                """);

        DslTreeBuildResult buildResult = builder.build(dsl);
        assertThat(buildResult.hasErrors()).isFalse();

        GenerationContext context = new GenerationContext(registry, new Random(123L));
        DataGenerationVisitor visitor = new DataGenerationVisitor(context);

        JsonNode result = buildResult.getTree().accept(visitor);

        assertThat(result).isNotNull();
        assertThat(result.get("items"))
            .hasSize(5)
            .allSatisfy(item ->
                assertThat(item.get("status").asText()).isIn("active", "inactive", "pending")
            );
    }

    @Test
    void testObjectGeneration() throws Exception {
        JsonNode dsl = mapper.readTree("""
                {
                    "users": {
                        "count": 1,
                        "item": {
                            "profile": {
                                "age": {"gen": "number", "min": 18, "max": 65},
                                "city": {"gen": "address.city"}
                            }
                        }
                    }
                }
                """);

        DslTreeBuildResult buildResult = builder.build(dsl);
        assertThat(buildResult.hasErrors()).isFalse();

        GenerationContext context = new GenerationContext(registry, new Random(123L));
        DataGenerationVisitor visitor = new DataGenerationVisitor(context);

        JsonNode result = buildResult.getTree().accept(visitor);

        assertThat(result).isNotNull();
        JsonNode user = result.get("users").get(0);
        assertThat(user.has("profile")).isTrue();

        JsonNode profile = user.get("profile");
        assertThat(profile.has("age")).isTrue();
        assertThat(profile.has("city")).isTrue();
        assertThat(profile.get("age").isNumber()).isTrue();
        assertThat(profile.get("city").isTextual()).isTrue();
    }

    @Test
    void testReferenceGeneration() throws Exception {
        JsonNode dsl = mapper.readTree("""
                {
                    "countries": {
                        "count": 3,
                        "item": {
                            "name": {"gen": "country.name"},
                            "code": {"gen": "country.countryCode"}
                        }
                    },
                    "users": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "name.firstName"},
                            "country": {"ref": "countries[*].name"}
                        }
                    }
                }
                """);

        DslTreeBuildResult buildResult = builder.build(dsl);
        assertThat(buildResult.hasErrors()).isFalse();

        GenerationContext context = new GenerationContext(registry, new Random(123L));
        DataGenerationVisitor visitor = new DataGenerationVisitor(context);

        JsonNode result = buildResult.getTree().accept(visitor);

        assertThat(result).isNotNull();
        assertThat(result.has("countries")).isTrue();
        assertThat(result.has("users")).isTrue();

        JsonNode countries = result.get("countries");
        JsonNode users = result.get("users");

        assertThat(countries).hasSize(3);
        assertThat(users)
            .hasSize(2)
            .allSatisfy(user -> {
                assertThat(user.has("country")).isTrue();
                assertThat(user.get("country").isTextual()).isTrue();
            });
    }

    @Test
    void testSeedConsistency() throws Exception {
        JsonNode dsl = mapper.readTree("""
                {
                    "users": {
                        "count": 3,
                        "item": {
                            "id": {"gen": "uuid"},
                            "name": {"gen": "name.firstName"}
                        }
                    }
                }
                """);

        // Generate with same seed twice using separate registries
        Random random1 = new Random(456L);
        GeneratorRegistry registry1 = GeneratorRegistry.withDefaultGenerators(new Faker(random1));
        DslTreeBuilder builder1 = new DslTreeBuilder(registry1);
        DslTreeBuildResult buildResult1 = builder1.build(dsl);
        assertThat(buildResult1.hasErrors()).isFalse();

        GenerationContext context1 = new GenerationContext(registry1, random1);
        DataGenerationVisitor visitor1 = new DataGenerationVisitor(context1);
        JsonNode result1 = buildResult1.getTree().accept(visitor1);

        Random random2 = new Random(456L);
        GeneratorRegistry registry2 = GeneratorRegistry.withDefaultGenerators(new Faker(random2));
        DslTreeBuilder builder2 = new DslTreeBuilder(registry2);
        DslTreeBuildResult buildResult2 = builder2.build(dsl);
        assertThat(buildResult2.hasErrors()).isFalse();

        GenerationContext context2 = new GenerationContext(registry2, random2);
        DataGenerationVisitor visitor2 = new DataGenerationVisitor(context2);
        JsonNode result2 = buildResult2.getTree().accept(visitor2);

        assertThat(result1).isEqualTo(result2);
    }
}
