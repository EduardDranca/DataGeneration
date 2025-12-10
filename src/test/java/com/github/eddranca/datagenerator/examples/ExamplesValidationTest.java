package com.github.eddranca.datagenerator.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.Generation;
import com.github.eddranca.datagenerator.ParameterizedGenerationTest;
import com.github.eddranca.datagenerator.generator.Generator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple test to validate that all example DSL files work as expected.
 * This ensures examples don't break when the library evolves.
 * Tests run with both eager and memory-optimized modes.
 */
@DisplayName("Examples DSL Validation")
class ExamplesValidationTest extends ParameterizedGenerationTest {

    private static final String EXAMPLES_DIR = "examples";

    @BothImplementationsTest
    @DisplayName("01-basic-users should generate users with proper structure")
    void shouldValidateBasicUsersStructure(boolean memoryOptimized) throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "01-basic-users", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        Generation generation = createGenerator(memoryOptimized)
            .fromFile(dslFile)
            .generate();

        // Then
        assertThat(generation.getCollectionNames()).contains("users");
        assertThat(generation.getCollectionSize("users")).isEqualTo(5);

        var users = collectAllJsonNodes(generation).get("users");
        assertThat(users).hasSize(5)
            .allSatisfy(user -> {
                assertThat(user.has("id")).isTrue();
                assertThat(user.has("firstName")).isTrue();
                assertThat(user.has("lastName")).isTrue();
                assertThat(user.has("email")).isTrue();
                assertThat(user.has("age")).isTrue();
                assertThat(user.has("isActive")).isTrue();
                assertThat(user.get("age").asInt()).isBetween(18, 65);
                assertThat(user.get("isActive").isBoolean()).isTrue();
            });
    }

    @BothImplementationsTest
    @DisplayName("02-ecommerce-store should generate store data")
    void shouldValidateEcommerceStoreStructure(boolean memoryOptimized) throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "02-ecommerce-store", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        Generation generation = createGenerator(memoryOptimized)
            .fromFile(dslFile)
            .generate();

        // Then
        assertThat(generation.getCollectionNames()).containsExactlyInAnyOrder("categories", "products", "customers", "orders");
        assertThat(generation.getCollectionSize("categories")).isEqualTo(3);
        assertThat(generation.getCollectionSize("products")).isEqualTo(8);
        assertThat(generation.getCollectionSize("customers")).isEqualTo(5);
        assertThat(generation.getCollectionSize("orders")).isEqualTo(10);

        var collections = collectAllJsonNodes(generation);
        var categories = collections.get("categories");
        var products = collections.get("products");
        var customers = collections.get("customers");
        var orders = collections.get("orders");

        var categoryIds = categories.stream().map(c -> c.get("id").asText()).toList();
        var productIds = products.stream().map(p -> p.get("id").asText()).toList();
        var customerIds = customers.stream().map(c -> c.get("id").asText()).toList();

        // Validate products reference categories
        assertThat(products).allSatisfy(product -> {
            assertThat(product.has("categoryId")).isTrue();
            assertThat(categoryIds).contains(product.get("categoryId").asText());
            assertThat(product.get("price").asDouble()).isBetween(9.99, 999.99);
        });

        // Validate customers have nested address
        assertThat(customers).allSatisfy(customer -> {
            assertThat(customer.has("address")).isTrue();
            assertThat(customer.get("address").has("street")).isTrue();
            assertThat(customer.get("address").has("city")).isTrue();
            assertThat(customer.get("address").has("zipCode")).isTrue();
        });

        // Validate orders reference customers and products
        assertThat(orders).allSatisfy(order -> {
            assertThat(order.has("customerId")).isTrue();
            assertThat(order.has("productId")).isTrue();
            assertThat(customerIds).contains(order.get("customerId").asText());
            assertThat(productIds).contains(order.get("productId").asText());
            assertThat(order.get("quantity").asInt()).isBetween(1, 5);
        });
    }

    @BothImplementationsTest
    @DisplayName("03-company-employees should generate company data")
    void shouldValidateCompanyEmployeesStructure(boolean memoryOptimized) throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "03-company-employees", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        Generation generation = createGenerator(memoryOptimized)
            .fromFile(dslFile)
            .generate();

        // Then
        assertThat(generation.getCollectionNames()).containsExactlyInAnyOrder("departments", "employees", "projects");
        assertThat(generation.getCollectionSize("departments")).isEqualTo(4);
        assertThat(generation.getCollectionSize("employees")).isEqualTo(15);
        assertThat(generation.getCollectionSize("projects")).isEqualTo(6);

        var collections = collectAllJsonNodes(generation);
        var departments = collections.get("departments");
        var employees = collections.get("employees");
        var projects = collections.get("projects");

        // Validate departments
        assertThat(departments).allSatisfy(dept -> {
            assertThat(dept.has("id")).isTrue();
            assertThat(dept.has("name")).isTrue();
            assertThat(dept.has("budget")).isTrue();
            assertThat(dept.has("location")).isTrue();
            assertThat(dept.get("budget").asInt()).isBetween(50000, 500000);
        });

        // Validate employees have references to departments
        var departmentIds = departments.stream().map(d -> d.get("id").asText()).toList();
        assertThat(employees).allSatisfy(emp -> {
            assertThat(emp.has("departmentId")).isTrue();
            assertThat(departmentIds).contains(emp.get("departmentId").asText());
            assertThat(emp.has("skills")).isTrue();
            assertThat(emp.get("skills").isArray()).isTrue();
            assertThat(emp.get("skills")).hasSize(4);
        });

        // Validate projects reference both departments and employees
        var employeeIds = employees.stream().map(e -> e.get("id").asText()).toList();
        assertThat(projects).allSatisfy(proj -> {
            assertThat(proj.has("departmentId")).isTrue();
            assertThat(proj.has("leadEmployeeId")).isTrue();
            assertThat(departmentIds).contains(proj.get("departmentId").asText());
            assertThat(employeeIds).contains(proj.get("leadEmployeeId").asText());
        });
    }

    @BothImplementationsTest
    @DisplayName("04-social-media should generate social media data")
    void shouldValidateSocialMediaStructure(boolean memoryOptimized) throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "04-social-media", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        Generation generation = createGenerator(memoryOptimized)
            .fromFile(dslFile)
            .generate();

        // Then
        assertThat(generation.getCollectionNames()).containsExactlyInAnyOrder("users", "posts", "comments", "follows");
        assertThat(generation.getCollectionSize("users")).isEqualTo(8);
        assertThat(generation.getCollectionSize("posts")).isEqualTo(20);
        assertThat(generation.getCollectionSize("comments")).isEqualTo(35);
        assertThat(generation.getCollectionSize("follows")).isEqualTo(25);

        var collections = collectAllJsonNodes(generation);
        var users = collections.get("users");
        var posts = collections.get("posts");
        var comments = collections.get("comments");

        var userIds = users.stream().map(u -> u.get("id").asText()).toList();
        var postIds = posts.stream().map(p -> p.get("id").asText()).toList();

        // Validate posts reference users
        assertThat(posts).allSatisfy(post -> {
            assertThat(post.has("authorId")).isTrue();
            assertThat(userIds).contains(post.get("authorId").asText());
            assertThat(post.has("hashtags")).isTrue();
            assertThat(post.get("hashtags").isArray()).isTrue();
            assertThat(post.get("hashtags")).hasSize(3);
        });

        // Validate comments reference both posts and users
        assertThat(comments).allSatisfy(comment -> {
            assertThat(comment.has("postId")).isTrue();
            assertThat(comment.has("authorId")).isTrue();
            assertThat(postIds).contains(comment.get("postId").asText());
            assertThat(userIds).contains(comment.get("authorId").asText());
        });
    }

    @BothImplementationsTest
    @DisplayName("05-financial-transactions should generate financial data")
    void shouldValidateFinancialTransactionsStructure(boolean memoryOptimized) throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "05-financial-transactions", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        Generation generation = createGenerator(memoryOptimized)
            .fromFile(dslFile)
            .generate();

        // Then
        assertThat(generation.getCollectionNames()).containsExactlyInAnyOrder("banks", "accounts", "merchants", "transactions");
        assertThat(generation.getCollectionSize("banks")).isEqualTo(3);
        assertThat(generation.getCollectionSize("accounts")).isEqualTo(12);
        assertThat(generation.getCollectionSize("merchants")).isEqualTo(8);
        assertThat(generation.getCollectionSize("transactions")).isEqualTo(50);

        var collections = collectAllJsonNodes(generation);
        var banks = collections.get("banks");
        var accounts = collections.get("accounts");
        var merchants = collections.get("merchants");
        var transactions = collections.get("transactions");

        var bankIds = banks.stream().map(b -> b.get("id").asText()).toList();
        var accountIds = accounts.stream().map(a -> a.get("id").asText()).toList();
        var merchantIds = merchants.stream().map(m -> m.get("id").asText()).toList();

        // Validate accounts reference banks
        assertThat(accounts).allSatisfy(account -> {
            assertThat(account.has("bankId")).isTrue();
            assertThat(bankIds).contains(account.get("bankId").asText());
            assertThat(account.get("balance").asDouble()).isBetween(100.0, 50000.0);
        });

        // Validate transactions reference accounts and merchants
        assertThat(transactions).allSatisfy(txn -> {
            assertThat(txn.has("accountId")).isTrue();
            assertThat(txn.has("merchantId")).isTrue();
            assertThat(accountIds).contains(txn.get("accountId").asText());
            assertThat(merchantIds).contains(txn.get("merchantId").asText());
            assertThat(txn.get("amount").asDouble()).isBetween(5.0, 2000.0);
        });
    }

    @BothImplementationsTest
    @DisplayName("06-educational-system should generate educational data")
    void shouldValidateEducationalSystemStructure(boolean memoryOptimized) throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "06-educational-system", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        Generation generation = createGenerator(memoryOptimized)
            .fromFile(dslFile)
            .generate();

        // Then
        assertThat(generation.getCollectionNames()).containsExactlyInAnyOrder("schools", "courses", "students", "enrollments");
        assertThat(generation.getCollectionSize("schools")).isEqualTo(2);
        assertThat(generation.getCollectionSize("courses")).isEqualTo(10);
        assertThat(generation.getCollectionSize("students")).isEqualTo(25);
        assertThat(generation.getCollectionSize("enrollments")).isEqualTo(60);

        var collections = collectAllJsonNodes(generation);
        var schools = collections.get("schools");
        var students = collections.get("students");
        var courses = collections.get("courses");
        var enrollments = collections.get("enrollments");

        var schoolIds = schools.stream().map(s -> s.get("id").asText()).toList();
        var studentIds = students.stream().map(s -> s.get("id").asText()).toList();
        var courseIds = courses.stream().map(c -> c.get("id").asText()).toList();

        // Validate schools have nested address
        assertThat(schools).allSatisfy(school -> {
            assertThat(school.has("address")).isTrue();
            assertThat(school.get("address").has("street")).isTrue();
            assertThat(school.get("address").has("city")).isTrue();
            assertThat(school.get("address").has("state")).isTrue();
            assertThat(school.get("address").has("zipCode")).isTrue();
        });

        // Validate students reference schools
        assertThat(students).allSatisfy(student -> {
            assertThat(student.has("schoolId")).isTrue();
            assertThat(schoolIds).contains(student.get("schoolId").asText());
            assertThat(student.get("gpa").asDouble()).isBetween(2.0, 4.0);
        });

        // Validate enrollments reference students and courses
        assertThat(enrollments).allSatisfy(enrollment -> {
            assertThat(enrollment.has("studentId")).isTrue();
            assertThat(enrollment.has("courseId")).isTrue();
            assertThat(studentIds).contains(enrollment.get("studentId").asText());
            assertThat(courseIds).contains(enrollment.get("courseId").asText());
        });
    }

    @BothImplementationsTest
    @DisplayName("07-custom-generator should work with custom generators")
    void shouldValidateCustomGeneratorStructure(boolean memoryOptimized) throws IOException {
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
        Generation generation = createGenerator(memoryOptimized)
            .withCustomGenerator("employeeId", employeeIdGenerator)
            .withCustomGenerator("jobLevelInfo", jobLevelInfoGenerator)
            .fromFile(dslFile)
            .generate();

        // Then
        assertThat(generation.getCollectionNames()).containsExactlyInAnyOrder("departments", "job_levels", "employees");
        assertThat(generation.getCollectionSize("departments")).isEqualTo(3);
        assertThat(generation.getCollectionSize("job_levels")).isEqualTo(4);
        assertThat(generation.getCollectionSize("employees")).isEqualTo(12);

        var collections = collectAllJsonNodes(generation);
        var departments = collections.get("departments");
        var jobLevels = collections.get("job_levels");
        var employees = collections.get("employees");

        var departmentIds = departments.stream().map(d -> d.get("id").asText()).toList();
        var jobLevelIds = jobLevels.stream().map(j -> j.get("id").asText()).toList();

        // Validate job levels have spread fields from custom generator
        assertThat(jobLevels).allSatisfy(jobLevel -> {
            assertThat(jobLevel.has("id")).isTrue();
            assertThat(jobLevel.has("level")).isTrue();
            assertThat(jobLevel.has("code")).isTrue();
            assertThat(jobLevel.has("salary_range")).isTrue();
        });

        // Validate employees use custom generator and reference other collections
        assertThat(employees).allSatisfy(employee -> {
            assertThat(employee.has("employee_id")).isTrue();
            assertThat(employee.get("employee_id").asText()).startsWith("EMP-");
            assertThat(employee.has("department_id")).isTrue();
            assertThat(employee.has("job_level_id")).isTrue();
            assertThat(departmentIds).contains(employee.get("department_id").asText());
            assertThat(jobLevelIds).contains(employee.get("job_level_id").asText());
        });
    }

    @BothImplementationsTest
    @DisplayName("08-range-references should generate data with range references")
    void shouldValidateRangeReferencesStructure(boolean memoryOptimized) throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "08-range-references", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        Generation generation = createGenerator(memoryOptimized)
            .fromFile(dslFile)
            .generate();

        // Then
        assertThat(generation.getCollectionNames()).containsExactlyInAnyOrder("employees", "regionalManagers", "performanceReviews");
        assertThat(generation.getCollectionSize("employees")).isEqualTo(20);
        assertThat(generation.getCollectionSize("regionalManagers")).isEqualTo(4);
        assertThat(generation.getCollectionSize("performanceReviews")).isEqualTo(10);

        var collections = collectAllJsonNodes(generation);
        var employees = collections.get("employees");
        var regionalManagers = collections.get("regionalManagers");

        // Validate that regional managers reference a range of employees (first 5: indices 0-4)
        var firstFiveEmployeeIds = employees.stream().limit(5).map(e -> e.get("id").asInt()).toList();
        assertThat(regionalManagers).allSatisfy(manager -> {
            assertThat(manager.has("managedEmployeeId")).isTrue();
            assertThat(firstFiveEmployeeIds).contains(manager.get("managedEmployeeId").asInt());
        });
    }

    @BothImplementationsTest
    @DisplayName("09-conditional-references should generate data with conditional filtering")
    void shouldValidateConditionalReferencesStructure(boolean memoryOptimized) throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "09-conditional-references", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        Generation generation = createGenerator(memoryOptimized)
            .fromFile(dslFile)
            .generate();

        // Then
        assertThat(generation.getCollectionNames()).containsExactlyInAnyOrder(
            "users", "products", "orders", "premiumOrders", "promotions",
            "lowStockAlerts", "eligibleForRefund", "ageRestrictedOrders"
        );

        var collections = collectAllJsonNodes(generation);
        var users = collections.get("users");
        var products = collections.get("products");
        var orders = collections.get("orders");
        var premiumOrders = collections.get("premiumOrders");

        // Validate orders only reference active users
        var activeUserIds = users.stream()
            .filter(u -> "active".equals(u.get("status").asText()))
            .map(u -> u.get("id").asInt())
            .toList();
        assertThat(orders).allSatisfy(order -> {
            assertThat(order.has("userId")).isTrue();
            assertThat(activeUserIds).contains(order.get("userId").asInt());
        });

        // Validate premium orders reference premium AND active users
        var premiumActiveUserIds = users.stream()
            .filter(u -> u.get("isPremium").asBoolean() && "active".equals(u.get("status").asText()))
            .map(u -> u.get("id").asInt())
            .toList();
        assertThat(premiumOrders).allSatisfy(order -> {
            assertThat(order.has("userId")).isTrue();
            assertThat(premiumActiveUserIds).contains(order.get("userId").asInt());
        });

        // Validate products in orders have stock > 0
        var productsWithStock = products.stream()
            .filter(p -> p.get("stock").asInt() > 0)
            .map(p -> p.get("id").asInt())
            .toList();
        assertThat(orders).allSatisfy(order -> {
            assertThat(order.has("productId")).isTrue();
            assertThat(productsWithStock).contains(order.get("productId").asInt());
        });
    }

    @BothImplementationsTest
    @DisplayName("10-runtime-computed-options should generate data with runtime-computed generator options")
    void shouldValidateRuntimeComputedOptionsStructure(boolean memoryOptimized) throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "10-runtime-computed-options", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        Generation generation = createGenerator(memoryOptimized)
            .fromFile(dslFile)
            .generate();

        // Then
        assertThat(generation.getCollectionNames()).containsExactlyInAnyOrder("employees", "products", "dynamicStrings");
        assertThat(generation.getCollectionSize("employees")).isEqualTo(5);
        assertThat(generation.getCollectionSize("products")).isEqualTo(10);
        assertThat(generation.getCollectionSize("dynamicStrings")).isEqualTo(8);

        var collections = collectAllJsonNodes(generation);
        var employees = collections.get("employees");
        var products = collections.get("products");
        var dynamicStrings = collections.get("dynamicStrings");

        // Validate employees have retirement age >= start age
        assertThat(employees).allSatisfy(employee -> {
            int startAge = employee.get("startAge").asInt();
            int retirementAge = employee.get("retirementAge").asInt();
            int yearsToRetirement = employee.get("yearsToRetirement").asInt();
            assertThat(startAge).isBetween(22, 35);
            assertThat(retirementAge).isBetween(startAge, 65);
            assertThat(yearsToRetirement).isBetween(0, retirementAge);
        });

        // Validate products have price ranges based on category
        assertThat(products).allSatisfy(product -> {
            String category = product.get("category").asText();
            double price = product.get("price").asDouble();
            switch (category) {
                case "budget" -> assertThat(price).isBetween(10.0, 50.0);
                case "premium" -> assertThat(price).isBetween(100.0, 500.0);
                case "luxury" -> assertThat(price).isBetween(1000.0, 5000.0);
                default -> throw new AssertionError("Unexpected category: " + category);
            }
        });

        // Validate dynamic strings have correct lengths
        assertThat(dynamicStrings).allSatisfy(item -> {
            int baseLength = item.get("baseLength").asInt();
            int dynamicLength = item.get("dynamicLength").asInt();
            String referenceBasedString = item.get("referenceBasedString").asText();
            String dynamicBasedString = item.get("dynamicBasedString").asText();

            assertThat(baseLength).isBetween(5, 15);
            assertThat(dynamicLength).isIn(3, 5, 7, 10);
            assertThat(referenceBasedString).hasSize(baseLength);
            assertThat(dynamicBasedString).hasSize(dynamicLength);
        });
    }

    @BothImplementationsTest
    @DisplayName("11-sql-projections should generate SQL with projections and schema parsing")
    void shouldValidateSqlProjectionsStructure(boolean memoryOptimized) throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "11-sql-projections", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        Generation generation = createGenerator(memoryOptimized)
            .fromFile(dslFile)
            .generate();

        // Then
        assertThat(generation.getCollectionNames()).containsExactlyInAnyOrder("categories", "products");
        assertThat(generation.getCollectionSize("categories")).isEqualTo(3);
        assertThat(generation.getCollectionSize("products")).isEqualTo(10);

        var collections = collectAllJsonNodes(generation);
        var categories = collections.get("categories");
        var products = collections.get("products");

        var categoryIds = categories.stream().map(c -> c.get("id").asInt()).toList();

        // Validate categories structure
        assertThat(categories).allSatisfy(category -> {
            assertThat(category.has("id")).isTrue();
            assertThat(category.has("name")).isTrue();
            assertThat(category.get("id").asInt()).isGreaterThan(0);
        });

        // Validate products structure including helper field
        assertThat(products).allSatisfy(product -> {
            assertThat(product.has("id")).isTrue();
            assertThat(product.has("name")).isTrue();
            assertThat(product.has("category_id")).isTrue();
            assertThat(product.has("price")).isTrue();
            assertThat(product.has("in_stock")).isTrue();
            assertThat(product.has("created_at")).isTrue();
            assertThat(product.has("_tempCategoryName")).isTrue(); // Helper field exists in data

            // Validate references
            assertThat(categoryIds).contains(product.get("category_id").asInt());
            assertThat(product.get("price").asInt()).isBetween(10, 1000);
            assertThat(product.get("in_stock").isBoolean()).isTrue();
        });

        // Validate sequential reference distribution
        var productCategoryIds = products.stream().map(p -> p.get("category_id").asInt()).toList();
        assertThat(productCategoryIds).containsAll(categoryIds); // All categories should be referenced
    }

    @BothImplementationsTest
    @DisplayName("12-shadow-bindings should generate data with shadow bindings excluded from output")
    void shouldValidateShadowBindingsStructure(boolean memoryOptimized) throws IOException {
        // Given
        Path dslPath = Paths.get(EXAMPLES_DIR, "12-shadow-bindings", "dsl.json");
        File dslFile = dslPath.toFile();

        // When
        Generation generation = createGenerator(memoryOptimized)
            .withSeed(12345L) // Use seed for reproducible results
            .fromFile(dslFile)
            .generate();

        // Then
        assertThat(generation.getCollectionNames()).containsExactlyInAnyOrder(
            "regions", "users", "products", "orders", "personalizedOrders", "friendships"
        );
        assertThat(generation.getCollectionSize("regions")).isEqualTo(5);
        assertThat(generation.getCollectionSize("users")).isEqualTo(20);
        assertThat(generation.getCollectionSize("products")).isEqualTo(50);
        assertThat(generation.getCollectionSize("orders")).isEqualTo(100);
        assertThat(generation.getCollectionSize("personalizedOrders")).isEqualTo(50);
        assertThat(generation.getCollectionSize("friendships")).isEqualTo(50);

        var collections = collectAllJsonNodes(generation);
        var users = collections.get("users");
        var products = collections.get("products");
        var orders = collections.get("orders");
        var personalizedOrders = collections.get("personalizedOrders");
        var friendships = collections.get("friendships");

        var userIds = users.stream().map(u -> u.get("id").asText()).toList();
        var productIds = products.stream().map(p -> p.get("id").asText()).toList();

        // Validate shadow bindings are NOT in output
        assertThat(orders).allSatisfy(order -> {
            assertThat(order.has("$user")).isFalse();
            assertThat(order.has("orderId")).isTrue();
            assertThat(order.has("userId")).isTrue();
            assertThat(order.has("userName")).isTrue();
            assertThat(order.has("productId")).isTrue();
            assertThat(userIds).contains(order.get("userId").asText());
            assertThat(productIds).contains(order.get("productId").asText());
        });

        assertThat(personalizedOrders).allSatisfy(order -> {
            assertThat(order.has("$user")).isFalse();
            assertThat(order.has("orderId")).isTrue();
            assertThat(order.has("userId")).isTrue();
            assertThat(order.has("productId")).isTrue();
            assertThat(userIds).contains(order.get("userId").asText());
            // productId may be null if no product matches the user's region AND category
            if (!order.get("productId").isNull()) {
                assertThat(productIds).contains(order.get("productId").asText());
            }
        });

        assertThat(friendships).allSatisfy(friendship -> {
            assertThat(friendship.has("$person")).isFalse();
            assertThat(friendship.has("userId")).isTrue();
            assertThat(friendship.has("friendId")).isTrue();
            assertThat(userIds).contains(friendship.get("userId").asText());
            assertThat(userIds).contains(friendship.get("friendId").asText());
            // Self-exclusion: userId != friendId
            assertThat(friendship.get("userId").asText()).isNotEqualTo(friendship.get("friendId").asText());
        });

        // Validate geographic constraints: orders have products from same region as user
        assertThat(orders).allSatisfy(order -> {
            String userId = order.get("userId").asText();
            String productId = order.get("productId").asText();

            var user = users.stream().filter(u -> u.get("id").asText().equals(userId)).findFirst().orElseThrow();
            var product = products.stream().filter(p -> p.get("id").asText().equals(productId)).findFirst().orElseThrow();

            assertThat(user.get("regionId").asInt()).isEqualTo(product.get("regionId").asInt());
        });
    }
}
