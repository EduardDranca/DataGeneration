package com.github.eddranca.datagenerator.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.DslDataGenerator;
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
        Generation generation = DslDataGenerator.create()
            .withSeed(12345L)
            .fromFile(dslFile)
            .generate();

        // Then

        assertThat(generation.getCollectionNames()).contains("users");
        assertThat(generation.getCollectionSize("users")).isGreaterThan(0);
    }

    @Test
    @DisplayName("02-ecommerce-store should generate store data")
    void shouldValidateEcommerceStoreStructure() throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "02-ecommerce-store", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        Generation generation = DslDataGenerator.create()
            .withSeed(12345L)
            .fromFile(dslFile)
            .generate();
        // Then
        assertThat(generation.getCollectionNames()).isNotEmpty();
    }

    @Test
    @DisplayName("03-company-employees should generate company data")
    void shouldValidateCompanyEmployeesStructure() throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "03-company-employees", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        Generation generation = DslDataGenerator.create()
            .withSeed(12345L)
            .fromFile(dslFile)
            .generate();

        // Then

        assertThat(generation.getCollectionNames()).isNotEmpty();
        assertThat(generation.getCollectionNames()).contains("departments");
        assertThat(generation.getCollectionNames()).contains("employees");
        assertThat(generation.getCollectionNames()).contains("projects");
    }

    @Test
    @DisplayName("04-social-media should generate social media data")
    void shouldValidateSocialMediaStructure() throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "04-social-media", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        Generation generation = DslDataGenerator.create()
            .withSeed(12345L)
            .fromFile(dslFile)
            .generate();

        // Then

        assertThat(generation.getCollectionNames()).isNotEmpty();
        assertThat(generation.getCollectionNames()).contains("users");
        assertThat(generation.getCollectionNames()).contains("posts");
        assertThat(generation.getCollectionNames()).contains("comments");
        assertThat(generation.getCollectionNames()).contains("follows");
    }

    @Test
    @DisplayName("05-financial-transactions should generate financial data")
    void shouldValidateFinancialTransactionsStructure() throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "05-financial-transactions", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        Generation generation = DslDataGenerator.create()
            .withSeed(12345L)
            .fromFile(dslFile)
            .generate();

        // Then

        assertThat(generation.getCollectionNames()).isNotEmpty();
        assertThat(generation.getCollectionNames()).contains("banks");
        assertThat(generation.getCollectionNames()).contains("accounts");
        assertThat(generation.getCollectionNames()).contains("merchants");
        assertThat(generation.getCollectionNames()).contains("transactions");
    }

    @Test
    @DisplayName("06-educational-system should generate educational data")
    void shouldValidateEducationalSystemStructure() throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "06-educational-system", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        Generation generation = DslDataGenerator.create()
            .withSeed(12345L)
            .fromFile(dslFile)
            .generate();

        // Then

        assertThat(generation.getCollectionNames()).isNotEmpty();
        assertThat(generation.getCollectionNames()).contains("schools");
        assertThat(generation.getCollectionNames()).contains("courses");
        assertThat(generation.getCollectionNames()).contains("students");
        assertThat(generation.getCollectionNames()).contains("enrollments");
    }

    @Test
    @DisplayName("07-custom-generator should work with custom generators")
    void shouldValidateCustomGeneratorStructure() throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "07-custom-generator", "dsl.json");
        File dslFile = dslPath.toFile();
        ObjectMapper mapper = new ObjectMapper();

        // Create the same custom generators as in the example
        Generator employeeIdGenerator = context -> {
            String prefix = context.getStringOption("prefix");
            if (prefix == null) prefix = "EMP";
            int number = context.faker().number().numberBetween(1000, 9999);
            return mapper.valueToTree(prefix + "-" + String.format("%04d", number));
        };

        // Complex generator that creates complete job level information (except UUID)
        Generator jobLevelInfoGenerator = context -> {
            String[] levels = {"Junior", "Mid", "Senior", "Lead"};
            String[] codes = {"L1", "L2", "L3", "L4"};
            String[] salaryRanges = {"$40,000 - $60,000", "$60,000 - $85,000", "$85,000 - $120,000", "$120,000 - $160,000"};

            int index = context.faker().number().numberBetween(0, levels.length);

            // Create a complete job level object that will be spread using "..."
            var jobLevel = mapper.createObjectNode();
            jobLevel.put("level", levels[index]);
            jobLevel.put("code", codes[index]);
            jobLevel.put("salary_range", salaryRanges[index]);

            return jobLevel;
        };

        // When
        Generation generation = DslDataGenerator.create()
            .withSeed(12345L)
            .withCustomGenerator("employeeId", employeeIdGenerator)
            .withCustomGenerator("jobLevelInfo", jobLevelInfoGenerator)
            .fromFile(dslFile)
            .generate();

        // Then

        assertThat(generation.getCollectionNames()).isNotEmpty();
        assertThat(generation.getCollectionNames()).contains("departments");
        assertThat(generation.getCollectionNames()).contains("job_levels");
        assertThat(generation.getCollectionNames()).contains("employees");
    }
}
