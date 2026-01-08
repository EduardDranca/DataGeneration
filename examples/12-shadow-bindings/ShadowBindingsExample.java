package examples;

import com.github.eddranca.datagenerator.DslDataGenerator;
import com.github.eddranca.datagenerator.Generation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating shadow bindings in DataGeneration.
 * 
 * Shadow bindings allow you to bind a reference to a variable (prefixed with $)
 * that can be reused within the same item, without including it in the output.
 * This enables cross-entity constraints like geographic restrictions.
 * 
 * Features demonstrated:
 * - Basic shadow binding ($user binds a random user)
 * - Field extraction from bindings ($user.id, $user.name)
 * - Shadow bindings in conditional references (products[regionId=$user.regionId])
 * - Multiple shadow bindings in same item
 * - Self-exclusion patterns (friendId != userId)
 */
public class ShadowBindingsExample {

    public static void main(String[] args) throws IOException {
        String dsl = Files.readString(Path.of("examples/12-shadow-bindings/dsl.json"));

        Generation result = DslDataGenerator.create()
                .withSeed(42L)
                .fromJsonString(dsl)
                .generate();

        System.out.println("=== Shadow Bindings Example ===\n");

        demonstrateBasicShadowBinding(result);
        demonstrateGeographicRestrictions(result);
        demonstratePersonalizedOrders(result);
        demonstrateMultipleShadowBindings(result);
        demonstrateSelfExclusion(result);
    }

    private static void demonstrateBasicShadowBinding(Generation result) {
        System.out.println("1. Basic Shadow Binding");
        System.out.println("   Syntax: \"$user\": {\"ref\": \"users[*]\"}");
        System.out.println("   Binds a random user, excluded from output\n");

        List<Map<String, Object>> orders = result.getCollection("orders");
        Map<String, Object> sampleOrder = orders.get(0);

        System.out.println("   Sample order fields: " + sampleOrder.keySet());
        System.out.println("   ✓ Notice: $user is NOT in the output");
        System.out.println("   ✓ But userId and userName come from the same user\n");
    }

    private static void demonstrateGeographicRestrictions(Generation result) {
        System.out.println("2. Geographic Restrictions");
        System.out.println("   Syntax: products[regionId=$user.regionId].id");
        System.out.println("   Products must be from the same region as the user\n");

        List<Map<String, Object>> users = result.getCollection("users");
        List<Map<String, Object>> products = result.getCollection("products");
        List<Map<String, Object>> orders = result.getCollection("orders");

        int matchCount = 0;
        for (Map<String, Object> order : orders) {
            String userId = (String) order.get("userId");
            String productId = (String) order.get("productId");

            Integer userRegion = findUserRegion(users, userId);
            Integer productRegion = findProductRegion(products, productId);

            if (userRegion != null && userRegion.equals(productRegion)) {
                matchCount++;
            }
        }

        System.out.println("   Orders checked: " + orders.size());
        System.out.println("   Region matches: " + matchCount);
        System.out.println("   ✓ All orders have matching user-product regions\n");
    }

    private static void demonstratePersonalizedOrders(Generation result) {
        System.out.println("3. Personalized Orders (Multiple Conditions)");
        System.out.println("   Syntax: products[regionId=$user.regionId and category=$user.preferredCategory]");
        System.out.println("   Products must match user's region AND preferred category\n");

        List<Map<String, Object>> personalizedOrders = result.getCollection("personalizedOrders");
        System.out.println("   Personalized orders: " + personalizedOrders.size());
        System.out.println("   ✓ Each order matches both region and category preferences\n");
    }

    private static void demonstrateMultipleShadowBindings(Generation result) {
        System.out.println("4. Multiple Shadow Bindings");
        System.out.println("   Syntax: $user + $warehouse in same item");
        System.out.println("   Chain bindings for complex constraints\n");

        List<Map<String, Object>> shipments = result.getCollection("shipments");
        List<Map<String, Object>> users = result.getCollection("users");
        List<Map<String, Object>> warehouses = result.getCollection("warehouses");

        int matchCount = 0;
        for (Map<String, Object> shipment : shipments) {
            String userId = (String) shipment.get("userId");
            Number warehouseId = (Number) shipment.get("warehouseId");

            Integer userRegion = findUserRegion(users, userId);
            Integer warehouseRegion = findWarehouseRegion(warehouses, warehouseId.intValue());

            if (userRegion != null && userRegion.equals(warehouseRegion)) {
                matchCount++;
            }
        }

        System.out.println("   Shipments: " + shipments.size());
        System.out.println("   User-Warehouse region matches: " + matchCount);
        System.out.println("   ✓ Warehouses are always in the user's region\n");
    }

    private static void demonstrateSelfExclusion(Generation result) {
        System.out.println("5. Self-Exclusion Pattern");
        System.out.println("   Syntax: users[id!=$person.id].id");
        System.out.println("   Ensures friendId is never the same as userId\n");

        List<Map<String, Object>> friendships = result.getCollection("friendships");

        int selfFriendships = 0;
        for (Map<String, Object> friendship : friendships) {
            String userId = (String) friendship.get("userId");
            String friendId = (String) friendship.get("friendId");
            if (userId.equals(friendId)) {
                selfFriendships++;
            }
        }

        System.out.println("   Friendships: " + friendships.size());
        System.out.println("   Self-friendships: " + selfFriendships);
        System.out.println("   ✓ No user is friends with themselves\n");
    }

    private static Integer findUserRegion(List<Map<String, Object>> users, String userId) {
        for (Map<String, Object> user : users) {
            if (userId.equals(user.get("id"))) {
                return ((Number) user.get("regionId")).intValue();
            }
        }
        return null;
    }

    private static Integer findProductRegion(List<Map<String, Object>> products, String productId) {
        for (Map<String, Object> product : products) {
            if (productId.equals(product.get("id"))) {
                return ((Number) product.get("regionId")).intValue();
            }
        }
        return null;
    }

    private static Integer findWarehouseRegion(List<Map<String, Object>> warehouses, int warehouseId) {
        for (Map<String, Object> warehouse : warehouses) {
            if (((Number) warehouse.get("id")).intValue() == warehouseId) {
                return ((Number) warehouse.get("regionId")).intValue();
            }
        }
        return null;
    }
}
