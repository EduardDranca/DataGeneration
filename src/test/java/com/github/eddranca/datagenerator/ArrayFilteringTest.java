package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests demonstrating array generation combined with filtering capabilities.
 * This showcases advanced features that differentiate this library from simple faker tools.
 */
class ArrayFilteringTest extends ParameterizedGenerationTest {

    @BothImplementations
    void testArrayWithExplicitSyntaxAndFiltering(boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                  "excluded": {
                    "count": 1,
                    "item": {
                      "name": {"gen": "choice", "options": ["Java", "Python"]}
                    }
                  },
                  "projects": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "uuid"},
                      "name": {"gen": "company.name"},
                      "technologies": {
                        "array": {
                          "item": {
                            "gen": "choice",
                            "options": ["Java", "Python", "JavaScript", "React", "Docker", "AWS"],
                            "filter": [{"ref": "excluded[0].name"}]
                          },
                          "minSize": 2,
                          "maxSize": 4
                        }
                      }
                    }
                  }
                }
                """;

        JsonNode dslNode = mapper.readTree(dsl);
        IGeneration generation = generateFromDsl(dslNode, memoryOptimized);
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);

        List<JsonNode> excluded = collections.get("excluded");
        List<JsonNode> projects = collections.get("projects");
        assertThat(excluded).hasSize(1);
        assertThat(projects).hasSize(5);

        String excludedTech = excluded.get(0).get("name").asText();
        // Verify filtering: technologies should not contain the excluded technology
        projects.forEach(project -> {
            ArrayNode technologies = (ArrayNode) project.get("technologies");
            assertThat(technologies).hasSizeBetween(2, 4);

            List<String> techList = new ArrayList<>();
            technologies.forEach(tech -> techList.add(tech.asText()));
            assertThat(techList).doesNotContain(excludedTech);
            assertThat(techList).allMatch(tech ->
                List.of("Java", "Python", "JavaScript", "React", "Docker", "AWS").contains(tech)
            );
        });
    }

    @BothImplementations
    void testArrayWithCountSyntaxAndFiltering(boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                  "bannedUser": {
                    "count": 1,
                    "item": {
                      "role": {"gen": "choice", "options": ["admin", "user"]}
                    }
                  },
                  "users": {
                    "count": 8,
                    "item": {
                      "id": {"gen": "uuid"},
                      "name": {"gen": "name.fullName"},
                      "permissions": {
                        "gen": "choice",
                        "options": ["admin", "user", "guest", "moderator"],
                        "count": 3,
                        "filter": [{"ref": "bannedUser[0].role"}]
                      }
                    }
                  }
                }
                """;

        JsonNode dslNode = mapper.readTree(dsl);
        IGeneration generation = generateFromDsl(dslNode, memoryOptimized);
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);

        List<JsonNode> bannedUsers = collections.get("bannedUser");
        List<JsonNode> users = collections.get("users");
        assertThat(bannedUsers).hasSize(1);
        assertThat(users).hasSize(8);

        String bannedRole = bannedUsers.get(0).get("role").asText();
        // Verify filtering: permissions should not contain the banned role
        users.forEach(user -> {
            ArrayNode permissions = (ArrayNode) user.get("permissions");
            assertThat(permissions).hasSize(3);

            List<String> permList = new ArrayList<>();
            permissions.forEach(perm -> permList.add(perm.asText()));
            assertThat(permList).doesNotContain(bannedRole);
            assertThat(permList).allMatch(perm ->
                List.of("admin", "user", "guest", "moderator").contains(perm)
            );
        });
    }

    @BothImplementations
    void testArrayWithMultipleFilters(boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                  "restrictions": {
                    "count": 1,
                    "item": {
                      "tech1": {"gen": "choice", "options": ["Java", "Python"]},
                      "tech2": {"gen": "choice", "options": ["React", "Vue"]}
                    }
                  },
                  "projects": {
                    "count": 3,
                    "item": {
                      "id": {"gen": "uuid"},
                      "technologies": {
                        "array": {
                          "item": {
                            "gen": "choice",
                            "options": ["Java", "Python", "JavaScript", "React", "Vue", "Docker"],
                            "filter": [
                              {"ref": "restrictions[0].tech1"},
                              {"ref": "restrictions[0].tech2"}
                            ]
                          },
                          "minSize": 2,
                          "maxSize": 3
                        }
                      }
                    }
                  }
                }
                """;

        JsonNode dslNode = mapper.readTree(dsl);
        IGeneration generation = generateFromDsl(dslNode, memoryOptimized);
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);

        List<JsonNode> restrictions = collections.get("restrictions");
        List<JsonNode> projects = collections.get("projects");
        assertThat(restrictions).hasSize(1);
        assertThat(projects).hasSize(3);

        String restrictedTech1 = restrictions.get(0).get("tech1").asText();
        String restrictedTech2 = restrictions.get(0).get("tech2").asText();
        // Verify multiple filters: technologies should not contain either restricted technology
        projects.forEach(project -> {
            ArrayNode technologies = (ArrayNode) project.get("technologies");
            assertThat(technologies).hasSizeBetween(2, 3);

            List<String> techList = new ArrayList<>();
            technologies.forEach(tech -> techList.add(tech.asText()));
            assertThat(techList).doesNotContain(restrictedTech1, restrictedTech2);
            assertThat(techList).allMatch(tech ->
                List.of("Java", "Python", "JavaScript", "React", "Vue", "Docker").contains(tech)
            );
        });
    }

    @BothImplementations
    void testArrayWithTagBasedFiltering(boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                  "blacklist": {
                    "count": 1,
                    "item": {
                      "skill": {"gen": "choice", "options": ["Java", "Python"]}
                    },
                    "tags": ["restricted"]
                  },
                  "developers": {
                    "count": 5,
                    "item": {
                      "id": {"gen": "uuid"},
                      "name": {"gen": "name.fullName"},
                      "skills": {
                        "array": {
                          "item": {
                            "gen": "choice",
                            "options": ["Java", "Python", "JavaScript", "React", "Docker", "AWS"],
                            "filter": [{"ref": "byTag[restricted].skill"}]
                          },
                          "minSize": 2,
                          "maxSize": 4
                        }
                      }
                    }
                  }
                }
                """;

        JsonNode dslNode = mapper.readTree(dsl);
        IGeneration generation = generateFromDsl(dslNode, memoryOptimized);
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);

        List<JsonNode> blacklist = collections.get("blacklist");
        List<JsonNode> developers = collections.get("developers");
        assertThat(blacklist).hasSize(1);
        assertThat(developers).hasSize(5);

        String restrictedSkill = blacklist.get(0).get("skill").asText();
        // Verify tag-based filtering: skills should not contain the blacklisted skill
        developers.forEach(developer -> {
            ArrayNode skills = (ArrayNode) developer.get("skills");
            assertThat(skills).hasSizeBetween(2, 4);

            List<String> skillList = new ArrayList<>();
            skills.forEach(skill -> skillList.add(skill.asText()));
            assertThat(skillList).doesNotContain(restrictedSkill);
            assertThat(skillList).allMatch(skill ->
                List.of("Java", "Python", "JavaScript", "React", "Docker", "AWS").contains(skill)
            );
        });
    }

    @BothImplementations
    void testNestedArraysWithFiltering(boolean memoryOptimized) throws IOException {
        String dsl = """
                {
                  "forbidden": {
                    "count": 1,
                    "item": {
                      "framework": {"gen": "choice", "options": ["Spring", "Django"]}
                    }
                  },
                  "teams": {
                    "count": 2,
                    "item": {
                      "name": {"gen": "company.name"},
                      "projects": {
                        "array": {
                          "item": {
                            "name": {"gen": "string", "length": 8},
                            "technologies": {
                              "array": {
                                "item": {
                                  "gen": "choice",
                                  "options": ["Spring", "Django", "Express", "FastAPI", "Rails"],
                                  "filter": [{"ref": "forbidden[0].framework"}]
                                },
                                "minSize": 1,
                                "maxSize": 2
                              }
                            }
                          },
                          "minSize": 1,
                          "maxSize": 3
                        }
                      }
                    }
                  }
                }
                """;

        JsonNode dslNode = mapper.readTree(dsl);
        IGeneration generation = generateFromDsl(dslNode, memoryOptimized);
        Map<String, List<JsonNode>> collections = collectAllJsonNodes(generation);

        List<JsonNode> forbidden = collections.get("forbidden");
        List<JsonNode> teams = collections.get("teams");
        assertThat(forbidden).hasSize(1);
        assertThat(teams).hasSize(2);

        String forbiddenFramework = forbidden.get(0).get("framework").asText();
        // Verify nested filtering works
        teams.forEach(team -> {
            ArrayNode projects = (ArrayNode) team.get("projects");
            assertThat(projects).hasSizeBetween(1, 3);

            projects.forEach(project -> {
                ArrayNode technologies = (ArrayNode) project.get("technologies");
                assertThat(technologies).hasSizeBetween(1, 2);

                List<String> techList = new ArrayList<>();
                technologies.forEach(tech -> techList.add(tech.asText()));
                assertThat(techList).doesNotContain(forbiddenFramework);
                assertThat(techList).allMatch(tech ->
                    List.of("Spring", "Django", "Express", "FastAPI", "Rails").contains(tech)
                );
            });
        });
    }
}
