package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.builder.DslTreeBuilder;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.node.RootNode;
import com.github.eddranca.datagenerator.validation.DslTreeBuildResult;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PathDependencyAnalyzer to verify correct path analysis
 * for memory optimization.
 */
class PathDependencyAnalyzerTest {

    private PathDependencyAnalyzer analyzer;
    private DslTreeBuilder builder;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        analyzer = new PathDependencyAnalyzer();
        GeneratorRegistry registry = GeneratorRegistry.withDefaultGenerators(new Faker());
        builder = new DslTreeBuilder(registry);
        mapper = new ObjectMapper();
    }

    private RootNode parseAndBuild(String dsl) throws Exception {
        JsonNode dslNode = mapper.readTree(dsl);
        DslTreeBuildResult result = builder.build(dslNode);
        return result.getTree();
    }

    @Test
    void testSimpleFieldReferences() throws Exception {
        String dsl = """
            {
              "users": {
                "count": 5,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.firstName"},
                  "email": {"gen": "internet.emailAddress"}
                }
              },
              "posts": {
                "count": 3,
                "item": {
                  "id": {"gen": "uuid"},
                  "authorId": {"ref": "users[*].id"},
                  "authorName": {"ref": "users[*].name"}
                }
              }
            }
            """;

        RootNode root = parseAndBuild(dsl);
        Map<String, Set<String>> referencedPaths = analyzer.analyzeRoot(root);

        // Users collection should have id and name referenced
        assertThat(referencedPaths).containsKey("users");
        Set<String> userPaths = referencedPaths.get("users");
        assertThat(userPaths).contains("id", "name")
            .doesNotContain("email"); // email is not referenced

        // Posts collection should not have any references
        assertThat(referencedPaths).doesNotContainKey("posts");
    }

    @Test
    void testNestedFieldReferences() throws Exception {
        String dsl = """
            {
              "users": {
                "count": 5,
                "item": {
                  "id": {"gen": "uuid"},
                  "profile": {
                    "bio": {"gen": "lorem"},
                    "social": {
                      "twitter": {"gen": "internet.username"},
                      "linkedin": {"gen": "internet.username"}
                    }
                  },
                  "address": {
                    "street": {"gen": "address.streetAddress"},
                    "city": {"gen": "address.city"}
                  }
                }
              },
              "orders": {
                "count": 2,
                "item": {
                  "id": {"gen": "uuid"},
                  "userId": {"ref": "users[*].id"},
                  "shippingStreet": {"ref": "users[*].address.street"},
                  "socialHandle": {"ref": "users[*].profile.social.twitter"}
                }
              }
            }
            """;

        RootNode root = parseAndBuild(dsl);
        Map<String, Set<String>> referencedPaths = analyzer.analyzeRoot(root);

        assertThat(referencedPaths).containsKey("users");
        Set<String> userPaths = referencedPaths.get("users");

        // Should contain all referenced paths
        assertThat(userPaths).contains("id", "address.street", "profile.social.twitter")
            .doesNotContain("profile.bio", "address.city", "profile.social.linkedin");
    }


    @Test
    void testNoReferences() throws Exception {
        String dsl = """
            {
              "users": {
                "count": 3,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.firstName"}
                }
              },
              "categories": {
                "count": 2,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "lorem"}
                }
              }
            }
            """;

        RootNode root = parseAndBuild(dsl);
        Map<String, Set<String>> referencedPaths = analyzer.analyzeRoot(root);

        // No collections should have references since there are no ref fields
        assertThat(referencedPaths).isEmpty();
    }

    @Test
    void testMultipleReferencesToSameCollection() throws Exception {
        String dsl = """
            {
              "users": {
                "count": 5,
                "item": {
                  "id": {"gen": "uuid"},
                  "name": {"gen": "name.firstName"},
                  "email": {"gen": "internet.emailAddress"},
                  "bio": {"gen": "lorem"}
                }
              },
              "posts": {
                "count": 3,
                "item": {
                  "id": {"gen": "uuid"},
                  "authorId": {"ref": "users[*].id"},
                  "authorName": {"ref": "users[*].name"}
                }
              },
              "comments": {
                "count": 5,
                "item": {
                  "id": {"gen": "uuid"},
                  "userId": {"ref": "users[*].id"},
                  "userEmail": {"ref": "users[*].email"}
                }
              }
            }
            """;

        RootNode root = parseAndBuild(dsl);
        Map<String, Set<String>> referencedPaths = analyzer.analyzeRoot(root);

        assertThat(referencedPaths).containsKey("users");
        Set<String> userPaths = referencedPaths.get("users");

        // Should contain all referenced fields from both posts and comments
        assertThat(userPaths).contains("id", "name", "email")
            .doesNotContain("bio");
    }
}
