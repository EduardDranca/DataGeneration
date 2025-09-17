package com.github.eddranca.datagenerator.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.IGeneration;
import com.github.eddranca.datagenerator.Generation;
import com.github.eddranca.datagenerator.generator.Generator;
import net.datafaker.Faker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple test to validate that all example DSL files work as expected.
 * This ensures examples don't break when the library evolves.
 */
@DisplayName("Examples DSL Validation")
class ExamplesValidationTest {

    private static final String EXAMPLES_DIR = "examples";

    @Test
    @DisplayName("01-basic-users should generate users with proper structure")
    void shouldValidateBasicUsersStructure() throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "01-basic-users", "dsl.json");
        File dslFile = dslPath.toFile();


        // When
        IGeneration generation = DslDataGenerator.create()
            .withSeed(12345L)
            .fromFile(dslFile)
            .generate();

        // Then

        assertThat(generation.getCollections()).containsKey("users");
        assertThat(generation.getCollections().get("users")).isNotEmpty();
    }

    @Test
    @DisplayName("02-ecommerce-store should generate store data")
    void shouldValidateEcommerceStoreStructure() throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "02-ecommerce-store", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        IGeneration generation = DslDataGenerator.create()
            .withSeed(12345L)
            .fromFile(dslFile)
            .generate();
        // Then
        assertThat(generation.getCollections()).isNotEmpty();
    }

    @Test
    @DisplayName("03-company-employees should generate company data")
    void shouldValidateCompanyEmployeesStructure() throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "03-company-employees", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        IGeneration generation = DslDataGenerator.create()
            .withSeed(12345L)
            .fromFile(dslFile)
            .generate();

        // Then

        assertThat(generation.getCollections()).isNotEmpty();
        assertThat(generation.getCollections()).containsKey("departments");
        assertThat(generation.getCollections()).containsKey("employees");
        assertThat(generation.getCollections()).containsKey("projects");
    }

    @Test
    @DisplayName("04-social-media should generate social media data")
    void shouldValidateSocialMediaStructure() throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "04-social-media", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        IGeneration generation = DslDataGenerator.create()
            .withSeed(12345L)
            .fromFile(dslFile)
            .generate();

        // Then

        assertThat(generation.getCollections()).isNotEmpty();
        assertThat(generation.getCollections()).containsKey("users");
        assertThat(generation.getCollections()).containsKey("posts");
        assertThat(generation.getCollections()).containsKey("comments");
        assertThat(generation.getCollections()).containsKey("follows");
    }

    @Test
    @DisplayName("05-financial-transactions should generate financial data")
    void shouldValidateFinancialTransactionsStructure() throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "05-financial-transactions", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        IGeneration generation = DslDataGenerator.create()
            .withSeed(12345L)
            .fromFile(dslFile)
            .generate();

        // Then

        assertThat(generation.getCollections()).isNotEmpty();
        assertThat(generation.getCollections()).containsKey("banks");
        assertThat(generation.getCollections()).containsKey("accounts");
        assertThat(generation.getCollections()).containsKey("merchants");
        assertThat(generation.getCollections()).containsKey("transactions");
    }

    @Test
    @DisplayName("06-educational-system should generate educational data")
    void shouldValidateEducationalSystemStructure() throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "06-educational-system", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        IGeneration generation = DslDataGenerator.create()
            .withSeed(12345L)
            .fromFile(dslFile)
            .generate();

        // Then

        assertThat(generation.getCollections()).isNotEmpty();
        assertThat(generation.getCollections()).containsKey("schools");
        assertThat(generation.getCollections()).containsKey("courses");
        assertThat(generation.getCollections()).containsKey("students");
        assertThat(generation.getCollections()).containsKey("enrollments");
    }

    @Test
    @DisplayName("07-custom-generator should work with custom generators")
    void shouldValidateCustomGeneratorStructure() throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "07-custom-generator", "dsl.json");
        File dslFile = dslPath.toFile();
        ObjectMapper mapper = new ObjectMapper();
        Faker faker = new Faker(new Random(4202331));

        // Create the same custom generators as in the example
        Generator employeeIdGenerator = options -> {
            String prefix = options.has("prefix") ? options.get("prefix").asText() : "EMP";
            int number = faker.number().numberBetween(1000, 9999);
            return mapper.valueToTree(prefix + "-" + String.format("%04d", number));
        };

        // Complex generator that creates complete job level information (except UUID)
        Generator jobLevelInfoGenerator = options -> {
            String[] levels = {"Junior", "Mid", "Senior", "Lead"};
            String[] codes = {"L1", "L2", "L3", "L4"};
            String[] salaryRanges = {"$40,000 - $60,000", "$60,000 - $85,000", "$85,000 - $120,000", "$120,000 - $160,000"};

            int index = faker.number().numberBetween(0, levels.length);

            // Create a complete job level object that will be spread using "..."
            var jobLevel = mapper.createObjectNode();
            jobLevel.put("level", levels[index]);
            jobLevel.put("code", codes[index]);
            jobLevel.put("salary_range", salaryRanges[index]);

            return jobLevel;
        };

        // When
        IGeneration generation = DslDataGenerator.create()
            .withSeed(12345L)
            .withCustomGenerator("employeeId", employeeIdGenerator)
            .withCustomGenerator("jobLevelInfo", jobLevelInfoGenerator)
            .fromFile(dslFile)
            .generate();

        // Then

        assertThat(generation.getCollections()).isNotEmpty();
        assertThat(generation.getCollections()).containsKey("departments");
        assertThat(generation.getCollections()).containsKey("job_levels");
        assertThat(generation.getCollections()).containsKey("employees");
    }
}
