package examples.basic_users;

import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.Generation;

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
            System.out.println(result.asJson());

            System.out.println("\n=== Generated SQL ===");
            result.asSqlInserts().forEach((table, sql) -> {
                System.out.println("-- Table: " + table);
                System.out.println(sql);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
