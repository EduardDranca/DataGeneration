---
name: Bug report
about: Create a report to help us improve
title: '[BUG] '
labels: bug
assignees: ''
---

## Bug Description
A clear and concise description of what the bug is.

## Steps to Reproduce
1. Go to '...'
2. Click on '....'
3. Scroll down to '....'
4. See error

## Expected Behavior
A clear and concise description of what you expected to happen.

## Actual Behavior
A clear and concise description of what actually happened.

## Code Example
```java
// Minimal code example that reproduces the issue
String dsl = """
    {
        "users": {
            "count": 5,
            "item": {
                "name": {"gen": "name.fullName"}
            }
        }
    }
    """;

Generation result = DslDataGenerator.create()
    .fromJsonString(dsl)
    .generate();
```

## Screenshots
If applicable, add screenshots to help explain your problem.

## Environment
- **Java Version**: [e.g. Java 17]
- **DataGeneration Version**: [e.g. 0.1.0]
- **OS**: [e.g. macOS 12.0]
- **Build Tool**: [e.g. Maven 3.8.1]

## Additional Context
Add any other context about the problem here.

## Error Logs
```
Paste any relevant error logs here
```
