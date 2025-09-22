package examples;

import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.Generation;

/**
 * Financial system example with accounts, transactions, and merchants
 */
public class FinancialTransactionsExample {
    public static void main(String[] args) {
        try {
            // Load DSL from external JSON file for better readability
            Generation result = DslDataGenerator.create()
                    .withSeed(77777L)
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
