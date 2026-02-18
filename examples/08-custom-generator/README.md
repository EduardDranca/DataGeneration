# Custom Generator Example

This example demonstrates how to create and use custom generators to extend the library's functionality beyond the built-in generators.

## Features Demonstrated

- **Custom Generator Implementation**: Create your own data generators
- **Generator Registration**: Register custom generators with the DSL system
- **Business Logic Integration**: Implement domain-specific data generation rules
- **Parameterized Generators**: Accept configuration options from DSL

## Custom Generators Included

1. **Employee ID Generator**: Generates formatted employee IDs (EMP-XXXX)
2. **Department Code Generator**: Creates department codes based on department names  
3. **Job Level Info Generator**: Creates complete job level objects with multiple fields using spread operator

## Key Benefits

- **Domain-Specific Data**: Generate data that matches your business rules
- **Consistency**: Ensure generated data follows your organization's patterns
- **Flexibility**: Combine custom generators with built-in ones
- **Complex Objects**: Generate complete objects with multiple related fields
- **Spread Operator**: Use `"..."` to spread generated object fields into the parent
- **Reusability**: Create generators once, use across multiple DSL files

## Usage

```java
// Create a complex generator that returns multiple fields
Generator jobLevelInfoGenerator = (options) -> {
    String[] levels = {"Junior", "Mid", "Senior", "Lead"};
    String[] codes = {"L1", "L2", "L3", "L4"};
    String[] salaryRanges = {"$40,000 - $60,000", "$60,000 - $85,000", 
                            "$85,000 - $120,000", "$120,000 - $160,000"};
    
    int index = faker.number().numberBetween(0, levels.length);
    
    // Return complete object that will be spread
    var jobLevel = mapper.createObjectNode();
    jobLevel.put("level", levels[index]);
    jobLevel.put("code", codes[index]);
    jobLevel.put("salary_range", salaryRanges[index]);
    jobLevel.put("years_experience_min", (index + 1) * 2);
    
    return jobLevel;
};

// Use with spread operator in DSL
{
    "job_levels": {
        "item": {
            "id": {"gen": "uuid"},
            "...": {"gen": "jobLevelInfo"}  // Spreads all fields from generator
        }
    }
}
```

This approach allows you to create highly specialized data generators that understand your business domain and generate realistic, consistent test data.
