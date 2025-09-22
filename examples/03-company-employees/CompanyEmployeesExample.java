package examples;

import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.Generation;

/**
 * Company structure example with departments and employees
 */
public class CompanyEmployeesExample {
    public static void main(String[] args) {
        try {
            // Load DSL from external JSON file for better readability
            Generation result = DslDataGenerator.create()
                    .withSeed(98765L)
                    .fromFile(new File("dsl.json"))
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
