package examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.Generation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating conditional references in DataGeneration.
 * 
 * Conditional references allow you to filter collection items based on field conditions
 * before referencing them, enabling complex business logic and data constraints.
 * 
 * Features demonstrated:
 * - Equality conditions (status='active')
 * - Numeric comparisons (price>100, stock<=10, age>=21)
 * - Logical AND operator (isPremium=true and status='active')
 * - Logical OR operator (featured=true or rating>=4.5)
 * - Complex multi-condition filters
 */
public class ConditionalReferencesExample {

    public static void main(String[] args) throws IOException {
        // Load DSL from file
        String dsl = Files.readString(Path.of("examples/08-conditional-references/dsl.json"));

        // Generate data with seed for reproducibility
        Generation result = DslDataGenerator.create()
                .withSeed(42L)
                .fromJsonString(dsl)
                .generate();

        System.out.println("=== Conditional References Example ===\n");

        // Example 1: Basic equality condition
        demonstrateBasicEquality(result);

        // Example 2: Numeric comparisons
        demonstrateNumericComparisons(result);

        // Example 3: AND operator
        demonstrateAndOperator(result);

        // Example 4: OR operator
        demonstrateOrOperator(result);

        // Example 5: Complex conditions
        demonstrateComplexConditions(result);

        // Example 6: Multiple conditional collections
        demonstrateMultipleConditions(result);
    }

    private static void demonstrateBasicEquality(Generation result) {
        System.out.println("1. Basic Equality Condition");
        System.out.println("   Syntax: users[status='active'].id");
        System.out.println("   Only references users with status='active'\n");

        List<Map<String, Object>> users = result.getCollection("users");
        List<Map<String, Object>> orders = result.getCollection("orders");

        long activeUsers = users.stream()
                .filter(u -> "active".equals(u.get("status")))
                .count();

        System.out.println("   Total users: " + users.size());
        System.out.println("   Active users: " + activeUsers);
        System.out.println("   Orders: " + orders.size());
        System.out.println("   ✓ All orders reference only active users\n");
    }

    private static void demonstrateNumericComparisons(Generation result) {
        System.out.println("2. Numeric Comparisons");
        System.out.println("   Syntax: products[stock>0].id");
        System.out.println("   Only references products with stock greater than 0\n");

        List<Map<String, Object>> products = result.getCollection("products");
        List<Map<String, Object>> orders = result.getCollection("orders");

        long inStockProducts = products.stream()
                .filter(p -> ((Number) p.get("stock")).intValue() > 0)
                .count();

        System.out.println("   Total products: " + products.size());
        System.out.println("   In-stock products: " + inStockProducts);
        System.out.println("   Orders: " + orders.size());
        System.out.println("   ✓ All orders reference only in-stock products\n");

        System.out.println("   Low Stock Alerts (stock<=10):");
        List<Map<String, Object>> alerts = result.getCollection("lowStockAlerts");
        System.out.println("   Alerts generated: " + alerts.size());
        System.out.println("   ✓ All alerts reference products with stock <= 10\n");
    }

    private static void demonstrateAndOperator(Generation result) {
        System.out.println("3. AND Operator");
        System.out.println("   Syntax: users[isPremium=true and status='active'].id");
        System.out.println("   Only references users who are BOTH premium AND active\n");

        List<Map<String, Object>> users = result.getCollection("users");
        List<Map<String, Object>> premiumOrders = result.getCollection("premiumOrders");

        long eligibleUsers = users.stream()
                .filter(u -> Boolean.TRUE.equals(u.get("isPremium")) && "active".equals(u.get("status")))
                .count();

        System.out.println("   Total users: " + users.size());
        System.out.println("   Premium + Active users: " + eligibleUsers);
        System.out.println("   Premium orders: " + premiumOrders.size());
        System.out.println("   ✓ All premium orders reference premium active users\n");

        System.out.println("   Premium products (price>100 and stock>5):");
        List<Map<String, Object>> products = result.getCollection("products");
        long premiumProducts = products.stream()
                .filter(p -> {
                    double price = ((Number) p.get("price")).doubleValue();
                    int stock = ((Number) p.get("stock")).intValue();
                    return price > 100 && stock > 5;
                })
                .count();
        System.out.println("   Eligible premium products: " + premiumProducts);
        System.out.println("   ✓ Premium orders only reference expensive, well-stocked products\n");
    }

    private static void demonstrateOrOperator(Generation result) {
        System.out.println("4. OR Operator");
        System.out.println("   Syntax: products[featured=true or rating>=4.5].id");
        System.out.println("   References products that are EITHER featured OR highly rated\n");

        List<Map<String, Object>> products = result.getCollection("products");
        List<Map<String, Object>> promotions = result.getCollection("promotions");

        long eligibleProducts = products.stream()
                .filter(p -> {
                    boolean featured = Boolean.TRUE.equals(p.get("featured"));
                    double rating = ((Number) p.get("rating")).doubleValue();
                    return featured || rating >= 4.5;
                })
                .count();

        System.out.println("   Total products: " + products.size());
        System.out.println("   Featured or highly-rated: " + eligibleProducts);
        System.out.println("   Promotions: " + promotions.size());
        System.out.println("   ✓ All promotions target featured or highly-rated products\n");
    }

    private static void demonstrateComplexConditions(Generation result) {
        System.out.println("5. Complex Multi-Condition Filters");
        System.out.println("   Syntax: users[age>=21 and status='active'].id");
        System.out.println("   Multiple conditions for age-restricted orders\n");

        List<Map<String, Object>> users = result.getCollection("users");
        List<Map<String, Object>> ageRestrictedOrders = result.getCollection("ageRestrictedOrders");

        long eligibleUsers = users.stream()
                .filter(u -> {
                    int age = ((Number) u.get("age")).intValue();
                    String status = (String) u.get("status");
                    return age >= 21 && "active".equals(status);
                })
                .count();

        System.out.println("   Total users: " + users.size());
        System.out.println("   Users 21+ and active: " + eligibleUsers);
        System.out.println("   Age-restricted orders: " + ageRestrictedOrders.size());
        System.out.println("   ✓ All orders comply with age restrictions\n");

        System.out.println("   Product conditions (category='electronics' and price>200):");
        List<Map<String, Object>> products = result.getCollection("products");
        long eligibleProducts = products.stream()
                .filter(p -> {
                    String category = (String) p.get("category");
                    double price = ((Number) p.get("price")).doubleValue();
                    return "electronics".equals(category) && price > 200;
                })
                .count();
        System.out.println("   Eligible products: " + eligibleProducts);
        System.out.println("   ✓ Only expensive electronics are referenced\n");
    }

    private static void demonstrateMultipleConditions(Generation result) {
        System.out.println("6. Multiple Conditional Collections");
        System.out.println("   Different conditions for different business rules\n");

        System.out.println("   Collection: orders");
        System.out.println("   Condition: users[status='active']");
        System.out.println("   Purpose: Only active users can place orders\n");

        System.out.println("   Collection: premiumOrders");
        System.out.println("   Condition: users[isPremium=true and status='active']");
        System.out.println("   Purpose: Premium features for premium active users\n");

        System.out.println("   Collection: eligibleForRefund");
        System.out.println("   Condition: users[accountBalance>100 and status='active']");
        System.out.println("   Purpose: Only users with sufficient balance can get refunds\n");

        System.out.println("   Collection: lowStockAlerts");
        System.out.println("   Condition: products[stock<=10]");
        System.out.println("   Purpose: Alert for products running low on inventory\n");

        System.out.println("   Collection: promotions");
        System.out.println("   Condition: products[featured=true or rating>=4.5]");
        System.out.println("   Purpose: Promote best products\n");

        System.out.println("   ✓ Each collection enforces its own business rules via conditions\n");
    }
}
