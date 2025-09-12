# Contributing to DataGeneration

Thank you for your interest in contributing to DataGeneration! We welcome contributions from the community.

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Git

### Development Setup

1. **Fork the repository**
   ```bash
   # Fork on GitHub, then clone your fork
   git clone https://github.com/YOUR_USERNAME/DataGeneration.git
   cd DataGeneration
   ```

2. **Set up the development environment**
   ```bash
   # Install dependencies and run tests
   mvn clean install
   
   # Verify everything works
   mvn test
   ```

3. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

## Development Guidelines

### Code Style
- Follow standard Java conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public APIs
- Keep methods focused and small
- Use AssertJ for test assertions

### Testing
- Write tests for all new features
- Maintain or improve test coverage
- Use descriptive test names
- Follow the existing test patterns

### Commit Messages
Use conventional commit format:
```
type(scope): description

feat(generator): add new CSV generator
fix(validation): handle null references correctly
docs(readme): update installation instructions
test(array): add filtering test cases
```

## Types of Contributions

### Bug Reports
When reporting bugs, please include:
- Clear description of the issue
- Steps to reproduce
- Expected vs actual behavior
- Java version and OS
- Minimal code example

### Feature Requests
For new features, please:
- Describe the use case
- Explain why it would be valuable
- Provide examples of how it would work
- Consider backward compatibility

### Code Contributions

#### Adding New Generators
1. Create the generator class implementing `Generator` interface
2. Add comprehensive tests
3. Update documentation
4. Add examples to README

Example:
```java
public class CustomGenerator implements Generator {
    @Override
    public JsonNode generate(JsonNode options, String path) {
        // Implementation
    }
    
    @Override
    public boolean supportsFiltering() {
        return true; // If filtering is supported
    }
}
```

#### Improving Existing Features
- Ensure backward compatibility
- Add tests for edge cases
- Update documentation if needed

## Testing

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ArrayFilteringTest

# Run with coverage
mvn test jacoco:report
```

### Writing Good Tests
```java
@Test
void shouldGenerateArrayWithFiltering() {
    // Given
    String dsl = """
        {
            "users": {
                "count": 10,
                "item": {
                    "skills": {
                        "array": {
                            "item": {"gen": "choice", "options": ["Java", "Python"]},
                            "filter": [{"ref": "excluded[0].skill"}]
                        }
                    }
                }
            }
        }
        """;
    
    // When
    Generation result = DslDataGenerator.create()
        .withSeed(123L)
        .fromJsonString(dsl)
        .generate();
    
    // Then
    assertThat(result.getCollections().get("users"))
        .hasSize(10)
        .allSatisfy(user -> {
            List<String> skills = (List<String>) user.get("skills");
            assertThat(skills).doesNotContain("ExcludedSkill");
        });
}
```

## Documentation

### Code Documentation
- Add JavaDoc for all public methods
- Include usage examples in JavaDoc
- Document complex algorithms
- Explain design decisions in comments

### README Updates
When adding features:
- Add examples to the README
- Update the feature comparison table
- Add to the built-in generators table if applicable

## Code Review Process

1. **Submit Pull Request**
   - Fill out the PR template
   - Link related issues
   - Provide clear description

2. **Review Criteria**
   - Code quality and style
   - Test coverage
   - Documentation updates
   - Backward compatibility
   - Performance impact

3. **Addressing Feedback**
   - Respond to all comments
   - Make requested changes
   - Update tests if needed

## Release Process

### Versioning
We follow [Semantic Versioning](https://semver.org/):
- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes

### Release Checklist
- [ ] All tests pass
- [ ] Documentation updated
- [ ] Release notes prepared

## Community Guidelines

### Code of Conduct
- Be respectful and inclusive
- Focus on constructive feedback
- Help newcomers get started
- Celebrate contributions

### Communication
- Use GitHub issues for bugs and features
- Use discussions for questions
- Be patient with response times
- Provide context in communications
## Getting Help

- **Questions**: Use GitHub Discussions
- **Bugs**: Create GitHub Issues
- **Features**: Create GitHub Issues with feature template

Thank you for helping make DataGeneration better!
