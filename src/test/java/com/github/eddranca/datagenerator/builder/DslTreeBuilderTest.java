package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.ValidationError;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.node.ArrayFieldNode;
import com.github.eddranca.datagenerator.node.ChoiceFieldNode;
import com.github.eddranca.datagenerator.node.CollectionNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.GeneratedFieldNode;
import com.github.eddranca.datagenerator.node.ItemNode;
import com.github.eddranca.datagenerator.node.LiteralFieldNode;
import com.github.eddranca.datagenerator.node.ObjectFieldNode;
import com.github.eddranca.datagenerator.node.ReferenceFieldNode;
import com.github.eddranca.datagenerator.node.RootNode;
import com.github.eddranca.datagenerator.node.SpreadFieldNode;
import com.github.eddranca.datagenerator.validation.DslTreeBuildResult;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DslTreeBuilderTest {

    private DslTreeBuilder builder;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        GeneratorRegistry registry = GeneratorRegistry.withDefaultGenerators(new Faker());
        builder = new DslTreeBuilder(registry);
        mapper = new ObjectMapper();
    }

    @Test
    void testBuildSimpleCollection() throws Exception {
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

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result).isNotNull();
        assertThat(result.hasErrors()).isFalse();

        RootNode root = result.getTree();
        assertThat(root).isNotNull();
        assertThat(root.getCollections()).hasSize(1);

        CollectionNode users = root.getCollections().get("users");
        assertThat(users).isNotNull();
        assertThat(users.getName()).isEqualTo("users");
        assertThat(users.getCount()).isEqualTo(3);

        ItemNode item = users.getItem();
        assertThat(item).isNotNull();
        assertThat(item.getFields()).hasSize(2);

        DslNode idField = item.getFields().get("id");
        assertThat(idField).isInstanceOf(GeneratedFieldNode.class);
        GeneratedFieldNode genId = (GeneratedFieldNode) idField;
        assertThat(genId.getGeneratorName()).isEqualTo("uuid");
        assertThat(genId.hasPath()).isFalse();

        DslNode nameField = item.getFields().get("name");
        assertThat(nameField).isInstanceOf(GeneratedFieldNode.class);
        GeneratedFieldNode genName = (GeneratedFieldNode) nameField;
        assertThat(genName.getGeneratorName()).isEqualTo("name");
        assertThat(genName.getPath()).isEqualTo("firstName");
        assertThat(genName.hasPath()).isTrue();
    }

    @Test
    void testBuildWithChoice() throws Exception {
        JsonNode dsl = mapper.readTree("""
            {
                "items": {
                    "count": 2,
                    "item": {
                        "status": {
                            "gen": "choice",
                            "options": ["active", "inactive", {"gen": "string", "length": 5}]
                        }
                    }
                }
            }
            """);

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result).isNotNull();
        assertThat(result.hasErrors()).isFalse();

        CollectionNode items = result.getTree().getCollections().get("items");
        DslNode statusField = items.getItem().getFields().get("status");

        assertThat(statusField).isInstanceOf(ChoiceFieldNode.class);
        ChoiceFieldNode choice = (ChoiceFieldNode) statusField;
        assertThat(choice.getOptions()).hasSize(3);

        // First two should be literals
        assertThat(choice.getOptions().get(0)).isInstanceOf(LiteralFieldNode.class);
        assertThat(choice.getOptions().get(1)).isInstanceOf(LiteralFieldNode.class);

        // Third should be generated
        assertThat(choice.getOptions().get(2)).isInstanceOf(GeneratedFieldNode.class);
    }

    @Test
    void testBuildWithReference() throws Exception {
        JsonNode dsl = mapper.readTree("""
                {
                    "countries": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "country.name"},
                            "code": {"gen": "country.countryCode"}
                        }
                    },
                    "users": {
                        "count": 3,
                        "item": {
                            "name": {"gen": "name.firstName"},
                            "country": {"ref": "countries[*].name"}
                        }
                    }
                }
                """);

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result).isNotNull();
        assertThat(result.hasErrors()).isFalse();

        CollectionNode users = result.getTree().getCollections().get("users");
        DslNode countryField = users.getItem().getFields().get("country");

        assertThat(countryField).isInstanceOf(ReferenceFieldNode.class);
        ReferenceFieldNode ref = (ReferenceFieldNode) countryField;
        assertThat(ref.getReference()).isEqualTo("countries[*].name");
        assertThat(ref.getFilters()).isEmpty();
    }

    @Test
    void testBuildWithValidationErrors() throws Exception {
        JsonNode dsl = mapper.readTree("""
                {
                    "users": {
                        "count": 2,
                        "item": {
                            "name": {"gen": "nonexistent.generator"},
                            "country": {"ref": "nonexistent[*].name"}
                        }
                    }
                }
                """);

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result).isNotNull();
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(2);

        // Check that we have both expected errors using AssertJ's extracting
        assertThat(result.getErrors())
            .extracting(ValidationError::toString)
            .anyMatch(error -> error.contains("Unknown generator: nonexistent"))
            .anyMatch(error -> error.contains("references undeclared collection or pick: nonexistent"));
    }

    @Test
    void testBuildWithObject() throws Exception {
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

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result).isNotNull();
        assertThat(result.hasErrors()).isFalse();

        CollectionNode users = result.getTree().getCollections().get("users");
        DslNode profileField = users.getItem().getFields().get("profile");

        assertThat(profileField).isInstanceOf(ObjectFieldNode.class);
        ObjectFieldNode obj = (ObjectFieldNode) profileField;
        assertThat(obj.getFields()).hasSize(2);

        assertThat(obj.getFields().get("age")).isInstanceOf(GeneratedFieldNode.class);
        assertThat(obj.getFields().get("city")).isInstanceOf(GeneratedFieldNode.class);
    }

    @Test
    void testBuildWithSpreadField() throws Exception {
        JsonNode dsl = mapper.readTree("""
                {
                    "users": {
                        "count": 1,
                        "item": {
                            "...nameDetails": {
                                "gen": "name",
                                "fields": ["firstName", "lastName:surname"]
                            },
                            "id": {"gen": "uuid"}
                        }
                    }
                }
                """);

        DslTreeBuildResult result = builder.build(dsl);

        assertThat(result).isNotNull();
        assertThat(result.hasErrors()).isFalse();

        CollectionNode users = result.getTree().getCollections().get("users");
        ItemNode item = users.getItem();

        // Should have spread field and regular field
        assertThat(item.getFields()).hasSize(2);

        DslNode spreadField = item.getFields().get("...nameDetails");
        assertThat(spreadField).isInstanceOf(SpreadFieldNode.class);

        SpreadFieldNode spread = (SpreadFieldNode) spreadField;
        assertThat(spread.getGeneratorName()).isEqualTo("name");
        assertThat(spread.getFields()).hasSize(2);
        assertThat(spread.getFields()).contains("firstName");
        assertThat(spread.getFields()).contains("lastName:surname");

        DslNode idField = item.getFields().get("id");
        assertThat(idField).isInstanceOf(GeneratedFieldNode.class);
    }

    @Test
    void testArrayFieldParsing() throws Exception {
        String dsl = """
                {
                    "users": {
                        "count": 1,
                        "item": {
                            "fixedArray": {
                                "array": {
                                    "size": 3,
                                    "item": "value"
                                }
                            },
                            "variableArray": {
                                "array": {
                                    "minSize": 1,
                                    "maxSize": 5,
                                    "item": {"gen": "uuid"}
                                }
                            }
                        }
                    }
                }
                """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));

        assertThat(result.getErrors()).isEmpty();

        RootNode root = result.getTree();
        CollectionNode users = root.getCollections().get("users");
        ItemNode item = users.getItem();

        // Test fixed size array
        DslNode fixedArrayField = item.getFields().get("fixedArray");
        assertThat(fixedArrayField).isInstanceOf(ArrayFieldNode.class);

        ArrayFieldNode fixedArray = (ArrayFieldNode) fixedArrayField;
        assertThat(fixedArray.hasFixedSize()).isTrue();
        assertThat(fixedArray.getSize()).isEqualTo(3);
        assertThat(fixedArray.getItemNode()).isInstanceOf(LiteralFieldNode.class);

        // Test variable size array
        DslNode variableArrayField = item.getFields().get("variableArray");
        assertThat(variableArrayField).isInstanceOf(ArrayFieldNode.class);

        ArrayFieldNode variableArray = (ArrayFieldNode) variableArrayField;
        assertThat(variableArray.hasFixedSize()).isFalse();
        assertThat(variableArray.getMinSize()).isEqualTo(1);
        assertThat(variableArray.getMaxSize()).isEqualTo(5);
        assertThat(variableArray.getItemNode()).isInstanceOf(GeneratedFieldNode.class);
    }

    @Test
    void testArrayFieldValidationMissingItem() throws Exception {
        String dsl = """
                {
                    "users": {
                        "count": 1,
                        "item": {
                            "badArray": {
                                "array": {
                                    "size": 3
                                }
                            }
                        }
                    }
                }
                """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).toString()).contains("must have an 'item' definition");
    }

    @Test
    void testArrayFieldValidationConflictingSize() throws Exception {
        String dsl = """
                {
                    "users": {
                        "count": 1,
                        "item": {
                            "badArray": {
                                "array": {
                                    "size": 3,
                                    "minSize": 1,
                                    "maxSize": 5,
                                    "item": "value"
                                }
                            }
                        }
                    }
                }
                """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).toString()).contains("cannot have both 'size' and 'minSize/maxSize'");
    }

    @Test
    void testArrayFieldValidationNegativeSize() throws Exception {
        String dsl = """
                {
                    "users": {
                        "count": 1,
                        "item": {
                            "badArray": {
                                "array": {
                                    "size": -1,
                                    "item": "value"
                                }
                            }
                        }
                    }
                }
                """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).toString()).contains("size must be non-negative");
    }

    @Test
    void testArrayFieldValidationInvalidRange() throws Exception {
        String dsl = """
                {
                    "users": {
                        "count": 1,
                        "item": {
                            "badArray": {
                                "array": {
                                    "minSize": 5,
                                    "maxSize": 2,
                                    "item": "value"
                                }
                            }
                        }
                    }
                }
                """;

        DslTreeBuildResult result = builder.build(mapper.readTree(dsl));
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).toString()).contains("maxSize must be >= minSize");
    }
}
