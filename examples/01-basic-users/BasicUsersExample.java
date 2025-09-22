package examples.basic_users;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.Generation;

import java.io.File;
import java.util.stream.Collectors;

/**
 * Basic example showing simple user data generation
 */
public class BasicUsersExample {
    public static void main(String[] args) {
        try {
            // Load DSL from external JSON file for better readability
            Generation result = DslDataGenerator.create()
                    .withSeed(12345L)
                    .fromFile("dsl.json")
                    .generate();

            System.out.println("=== Generated JSON ===");
            // Convert streams to JSON for display
            ObjectMapper mapper = new ObjectMapper();
            var jsonResult = mapper.createObjectNode();
            result.asJsonNodes().forEach((collectionName, stream) -> {
                var items = stream.collect(Collectors.toList());
                jsonResult.set(collectionName, mapper.valueToTree(items));
            });
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResult));

            System.out.println("\n=== Generated SQL ===");
            result.asSqlInserts().forEach((table, sqlStream) -> {
                System.out.println("-- Table: " + table);
                sqlStream.forEach(System.out::println);
                System.out.println();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
