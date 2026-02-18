package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import com.github.eddranca.datagenerator.generator.GeneratorOptionSpec;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.exception.DslValidationException;
import com.github.eddranca.datagenerator.validation.DslTreeBuildResult;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneratorOptionValidationTest {

    private DslTreeBuilder builder;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        GeneratorRegistry registry = GeneratorRegistry.withDefaultGenerators(new Faker());
        builder = new DslTreeBuilder(registry);
        mapper = new ObjectMapper();
    }

    // --- Strict generators reject unknown options ---

    @Test
    void testStrictGeneratorRejectsUnknownOption() throws Exception {
        String dsl = """
            {
                "users": {
                    "count": 1,
                    "item": {
                        "id": {"gen": "uuid", "format": "short"}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).toString())
            .contains("unknown option 'format'")
            .contains("uuid");
    }

    @Test
    void testStrictGeneratorRejectsMultipleUnknownOptions() throws Exception {
        String dsl = """
            {
                "users": {
                    "count": 1,
                    "item": {
                        "name": {"gen": "name", "locale": "en", "style": "formal"}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getErrors().toString()).contains("unknown option 'locale'");
        assertThat(result.getErrors().toString()).contains("unknown option 'style'");
    }

    @Test
    void testNoOptionsGeneratorRejectsAnyOption() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "company": {"gen": "company", "size": "large"},
                        "addr": {"gen": "address", "country": "US"},
                        "book": {"gen": "book", "genre": "fiction"},
                        "fin": {"gen": "finance", "currency": "USD"},
                        "net": {"gen": "internet", "domain": "example.com"},
                        "ctry": {"gen": "country", "continent": "Europe"}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(6);
    }

    // --- Generators with optional params accept valid options ---

    @Test
    void testNumberGeneratorAcceptsValidOptions() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "age": {"gen": "number", "min": 18, "max": 65}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void testNumberGeneratorRejectsUnknownOption() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "age": {"gen": "number", "min": 18, "max": 65, "step": 5}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).toString())
            .contains("unknown option 'step'")
            .contains("number");
    }

    @Test
    void testFloatGeneratorAcceptsValidOptions() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "price": {"gen": "float", "min": 0, "max": 100, "decimals": 2}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void testStringGeneratorAcceptsValidOptions() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "code": {"gen": "string", "length": 8, "allowedChars": "ABC123"}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void testStringGeneratorAcceptsRegex() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "code": {"gen": "string", "regex": "[A-Z]{3}-[0-9]{4}"}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void testStringGeneratorAcceptsMinMaxLength() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "text": {"gen": "string", "minLength": 5, "maxLength": 20}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void testDateGeneratorAcceptsValidOptions() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "created": {"gen": "date", "from": "2020-01-01", "to": "2025-12-31", "format": "iso"}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void testBooleanGeneratorAcceptsValidOptions() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "active": {"gen": "boolean", "probability": 0.8}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void testLoremGeneratorAcceptsValidOptions() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "title": {"gen": "lorem", "words": 5},
                        "body": {"gen": "lorem", "sentences": 3},
                        "essay": {"gen": "lorem", "paragraphs": 2}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void testSequenceGeneratorAcceptsValidOptions() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "idx": {"gen": "sequence", "start": 100, "increment": 10}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void testPhoneGeneratorAcceptsValidOptions() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "phone": {"gen": "phone", "format": "cell"}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void testCsvGeneratorRequiresFile() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "data": {"gen": "csv"}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).toString())
            .contains("missing required option 'file'")
            .contains("csv");
    }

    @Test
    void testCsvGeneratorAcceptsValidOptions() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "data": {"gen": "csv", "file": "data.csv", "sequential": false}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isFalse();
    }

    // --- Generators with no options work without any ---

    @Test
    void testNoOptionGeneratorsWorkWithoutOptions() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "id": {"gen": "uuid"},
                        "name": {"gen": "name"},
                        "company": {"gen": "company"},
                        "address": {"gen": "address"},
                        "internet": {"gen": "internet"},
                        "country": {"gen": "country"},
                        "book": {"gen": "book"},
                        "finance": {"gen": "finance"}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isFalse();
    }

    // --- Filter keyword is not treated as a generator option ---

    @Test
    void testFilterKeywordNotTreatedAsOption() throws Exception {
        String dsl = """
            {
                "users": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "uuid"}
                    }
                },
                "orders": {
                    "count": 10,
                    "item": {
                        "userId": {
                            "gen": "uuid",
                            "filter": [{"ref": "users[0].id"}]
                        }
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isFalse();
    }

    // --- Custom non-strict generators accept any options ---

    @Test
    void testNonStrictCustomGeneratorAcceptsAnyOptions() throws Exception {
        GeneratorRegistry registry = GeneratorRegistry.withDefaultGenerators(new Faker());
        registry.register("myGen", new Generator() {
            @Override
            public JsonNode generate(GeneratorContext context) {
                return context.mapper().valueToTree("custom");
            }
            // Default getOptionSpec() returns nonStrict()
        });

        DslTreeBuilder customBuilder = new DslTreeBuilder(registry);
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "val": {"gen": "myGen", "foo": "bar", "baz": 42, "anything": true}
                    }
                }
            }
            """;

        DslTreeBuildResult result = customBuilder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isFalse();
    }

    // --- Custom strict generators validate options ---

    @Test
    void testStrictCustomGeneratorRejectsUnknownOptions() throws Exception {
        GeneratorRegistry registry = GeneratorRegistry.withDefaultGenerators(new Faker());
        registry.register("strictGen", new Generator() {
            @Override
            public JsonNode generate(GeneratorContext context) {
                return context.mapper().valueToTree("strict");
            }

            @Override
            public GeneratorOptionSpec getOptionSpec() {
                return GeneratorOptionSpec.builder()
                    .required("template")
                    .optional("locale")
                    .build();
            }
        });

        DslTreeBuilder customBuilder = new DslTreeBuilder(registry);
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "val": {"gen": "strictGen", "template": "hello", "unknown": true}
                    }
                }
            }
            """;

        DslTreeBuildResult result = customBuilder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).toString())
            .contains("unknown option 'unknown'");
    }

    @Test
    void testStrictCustomGeneratorRequiresMissingOption() throws Exception {
        GeneratorRegistry registry = GeneratorRegistry.withDefaultGenerators(new Faker());
        registry.register("strictGen", new Generator() {
            @Override
            public JsonNode generate(GeneratorContext context) {
                return context.mapper().valueToTree("strict");
            }

            @Override
            public GeneratorOptionSpec getOptionSpec() {
                return GeneratorOptionSpec.builder()
                    .required("template")
                    .optional("locale")
                    .build();
            }
        });

        DslTreeBuilder customBuilder = new DslTreeBuilder(registry);
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "val": {"gen": "strictGen", "locale": "en"}
                    }
                }
            }
            """;

        DslTreeBuildResult result = customBuilder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).toString())
            .contains("missing required option 'template'");
    }

    // --- Multiple errors across fields ---

    @Test
    void testMultipleValidationErrorsAcrossFields() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "id": {"gen": "uuid", "version": 4},
                        "age": {"gen": "number", "min": 0, "max": 100, "precision": 1},
                        "active": {"gen": "boolean", "trueLabel": "yes"}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(3);
    }

    // --- End-to-end: DslValidationException is thrown ---

    @Test
    void testEndToEndValidationExceptionThrown() {
        String dsl = """
            {
                "users": {
                    "count": 1,
                    "item": {
                        "id": {"gen": "uuid", "format": "short"}
                    }
                }
            }
            """;

        assertThatThrownBy(() ->
            DslDataGenerator.create()
                .fromJsonString(dsl)
                .generate()
        )
            .isInstanceOf(DslValidationException.class)
            .hasMessageContaining("unknown option 'format'")
            .hasMessageContaining("uuid");
    }

    // --- Dot notation generators validate against the base generator ---

    @Test
    void testDotNotationGeneratorValidatesBaseSpec() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "first": {"gen": "name.firstName", "locale": "en"}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).toString())
            .contains("unknown option 'locale'")
            .contains("name");
    }

    @Test
    void testDotNotationGeneratorWithNoOptionsIsValid() throws Exception {
        String dsl = """
            {
                "items": {
                    "count": 1,
                    "item": {
                        "first": {"gen": "name.firstName"},
                        "city": {"gen": "address.city"},
                        "email": {"gen": "internet.emailAddress"}
                    }
                }
            }
            """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.hasErrors()).isFalse();
    }
}
