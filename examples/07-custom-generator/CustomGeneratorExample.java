package examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.Generation;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;

import java.io.File;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Example demonstrating custom generator implementation and usage.
 * Shows how to extend the library with domain-specific generators.
 */
public class CustomGeneratorExample {

    public static void main(String[] args) {
        try {
            // Initialize dependencies
            ObjectMapper mapper = new ObjectMapper();
            Faker faker = new Faker(new Random(42));

            // Create custom employee ID generator
            Generator employeeIdGenerator = (GeneratorContext context) -> {
                String prefix = context.getStringOption("prefix");
                if (prefix == null) prefix = "EMP";
                int number = context.getFaker().number().numberBetween(1000, 9999);
                return mapper.valueToTree(prefix + "-" + String.format("%04d", number));
            };

            // Create custom department code generator
            Generator departmentCodeGenerator = (GeneratorContext context) -> {
                String deptName = context.getStringOption("department");
                if (deptName != null) {
                    String code;
                    switch (deptName.toLowerCase()) {
                        case "engineering":
                            code = "ENG";
                            break;
                        case "marketing":
                            code = "MKT";
                            break;
                        case "sales":
                            code = "SLS";
                            break;
                        case "human resources":
                        case "hr":
                            code = "HR";
                            break;
                        default:
                            code = deptName.substring(0, Math.min(3, deptName.length())).toUpperCase();
                            break;
                    }
                    return mapper.valueToTree(code);
                }
                return mapper.valueToTree("UNK");
            };

            // Create complex job level info generator that creates complete objects
            Generator jobLevelInfoGenerator = (GeneratorContext context) -> {
                String[] levels = {"Junior", "Mid", "Senior", "Lead"};
                String[] codes = {"L1", "L2", "L3", "L4"};
                String[] titles = {"Software Engineer", "Senior Software Engineer", "Staff Engineer", "Principal Engineer"};
                String[] salaryRanges = {"$40,000 - $60,000", "$60,000 - $85,000", "$85,000 - $120,000", "$120,000 - $160,000"};
                String[] descriptions = {
                    "Entry-level position with mentorship and learning opportunities",
                    "Experienced professional with independent project ownership",
                    "Senior contributor with technical leadership responsibilities",
                    "Strategic technical leader driving architectural decisions"
                };

                int index = context.getFaker().number().numberBetween(0, levels.length);

                // Create a complete job level object that will be spread using "..."
                var jobLevel = mapper.createObjectNode();
                jobLevel.put("level", levels[index]);
                jobLevel.put("code", codes[index]);
                jobLevel.put("title", titles[index]);
                jobLevel.put("salary_range", salaryRanges[index]);
                jobLevel.put("description", descriptions[index]);
                jobLevel.put("years_experience_min", (index + 1) * 2);
                jobLevel.put("years_experience_max", (index + 1) * 4);

                return jobLevel;
            };

            System.out.println("=== Custom Generator Example ===");
            System.out.println("Generating employee data with custom business logic...");

            // Generate data using custom generators
            Generation result = DslDataGenerator.create()
                    .withSeed(12345L)
                    .withCustomGenerator("employeeId", employeeIdGenerator)
                    .withCustomGenerator("departmentCode", departmentCodeGenerator)
                    .withCustomGenerator("jobLevelInfo", jobLevelInfoGenerator)
                    .fromFile(new File("dsl.json"))
                    .generate();

            System.out.println("\n=== Generated JSON ===");
            // Convert streams to JSON for display
            ObjectMapper mapper2 = new ObjectMapper();
            var jsonResult = mapper2.createObjectNode();
            result.asJsonNodes().forEach((collectionName, stream) -> {
                var items = stream.collect(Collectors.toList());
                jsonResult.set(collectionName, mapper2.valueToTree(items));
            });
            System.out.println(mapper2.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResult));

            System.out.println("\n=== Generated SQL ===");
            result.asSqlInserts().forEach((table, sqlStream) -> {
                System.out.println("-- Table: " + table);
                sqlStream.forEach(System.out::println);
                System.out.println();
            });

            System.out.println("=== Custom Generator Features Demonstrated ===");
            System.out.println("✓ Employee IDs follow company format (EMP-XXXX)");
            System.out.println("✓ Department codes are business-appropriate abbreviations");
            System.out.println("✓ Job level info generator creates complete objects with spread operator");
            System.out.println("✓ Complex generators can return multiple fields at once");
            System.out.println("✓ Custom generators integrate seamlessly with built-in ones");

        } catch (Exception e) {
            System.err.println("Error running custom generator example: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
