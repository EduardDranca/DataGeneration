// Quick test for new methods
import com.github.eddranca.datagenerator.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public class TestNewMethods {
    public static void main(String[] args) throws Exception {
        String dsl = """
        {
          "users": {
            "count": 2,
            "item": {
              "id": {"gen": "uuid"},
              "name": {"gen": "name.firstName"}
            }
          }
        }
        """;
        
        Generation result = DslDataGenerator.create()
            .fromJsonString(dsl)
            .generate();
            
        // Test new methods
        System.out.println("Collection names: " + result.getCollectionNames());
        System.out.println("Has users: " + result.hasCollection("users"));
        System.out.println("Has posts: " + result.hasCollection("posts"));
        System.out.println("Users size: " + result.getCollectionSize("users"));
        
        List<JsonNode> users = result.getCollection("users");
        System.out.println("Users: " + users.size());
        
        // Test with memory optimization
        Generation lazyResult = DslDataGenerator.create()
            .withMemoryOptimization()
            .fromJsonString(dsl)
            .generate();
            
        System.out.println("Lazy - Has users: " + lazyResult.hasCollection("users"));
        System.out.println("Lazy - Users size: " + lazyResult.getCollectionSize("users"));
        
        List<JsonNode> lazyUsers = lazyResult.getCollection("users");
        System.out.println("Lazy users: " + lazyUsers.size());
    }
}