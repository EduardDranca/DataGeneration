package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ComparisonCondition")
class ComparisonConditionTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Should match with EQUALS operator for strings")
    void shouldMatchEqualsString() throws Exception {
        String json = "{\"status\": \"active\"}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("status", ComparisonOperator.EQUALS, "active");
        
        assertThat(condition.matches(item)).isTrue();
    }

    @Test
    @DisplayName("Should not match with EQUALS operator for different strings")
    void shouldNotMatchEqualsDifferentString() throws Exception {
        String json = "{\"status\": \"inactive\"}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("status", ComparisonOperator.EQUALS, "active");
        
        assertThat(condition.matches(item)).isFalse();
    }

    @Test
    @DisplayName("Should match with NOT_EQUALS operator")
    void shouldMatchNotEquals() throws Exception {
        String json = "{\"status\": \"inactive\"}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("status", ComparisonOperator.NOT_EQUALS, "active");
        
        assertThat(condition.matches(item)).isTrue();
    }

    @Test
    @DisplayName("Should match with LESS_THAN operator")
    void shouldMatchLessThan() throws Exception {
        String json = "{\"age\": 15}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("age", ComparisonOperator.LESS_THAN, 18);
        
        assertThat(condition.matches(item)).isTrue();
    }

    @Test
    @DisplayName("Should match with LESS_THAN_OR_EQUAL operator")
    void shouldMatchLessThanOrEqual() throws Exception {
        String json = "{\"age\": 18}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("age", ComparisonOperator.LESS_THAN_OR_EQUAL, 18);
        
        assertThat(condition.matches(item)).isTrue();
    }

    @Test
    @DisplayName("Should match with GREATER_THAN operator")
    void shouldMatchGreaterThan() throws Exception {
        String json = "{\"age\": 25}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("age", ComparisonOperator.GREATER_THAN, 18);
        
        assertThat(condition.matches(item)).isTrue();
    }

    @Test
    @DisplayName("Should match with GREATER_THAN_OR_EQUAL operator")
    void shouldMatchGreaterThanOrEqual() throws Exception {
        String json = "{\"age\": 21}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("age", ComparisonOperator.GREATER_THAN_OR_EQUAL, 21);
        
        assertThat(condition.matches(item)).isTrue();
    }

    @Test
    @DisplayName("Should match boolean values")
    void shouldMatchBoolean() throws Exception {
        String json = "{\"active\": true}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("active", ComparisonOperator.EQUALS, true);
        
        assertThat(condition.matches(item)).isTrue();
    }

    @Test
    @DisplayName("Should match integer values")
    void shouldMatchInteger() throws Exception {
        String json = "{\"count\": 42}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("count", ComparisonOperator.EQUALS, 42);
        
        assertThat(condition.matches(item)).isTrue();
    }

    @Test
    @DisplayName("Should match long values")
    void shouldMatchLong() throws Exception {
        String json = "{\"id\": 9999999999}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("id", ComparisonOperator.EQUALS, 9999999999L);
        
        assertThat(condition.matches(item)).isTrue();
    }

    @Test
    @DisplayName("Should match double values")
    void shouldMatchDouble() throws Exception {
        String json = "{\"price\": 19.99}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("price", ComparisonOperator.EQUALS, 19.99);
        
        assertThat(condition.matches(item)).isTrue();
    }

    @Test
    @DisplayName("Should match float values")
    void shouldMatchFloat() throws Exception {
        String json = "{\"rating\": 4.5}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("rating", ComparisonOperator.EQUALS, 4.5f);
        
        assertThat(condition.matches(item)).isTrue();
    }

    @Test
    @DisplayName("Should match null values")
    void shouldMatchNull() throws Exception {
        String json = "{\"middleName\": null}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("middleName", ComparisonOperator.EQUALS, null);
        
        assertThat(condition.matches(item)).isTrue();
    }

    @Test
    @DisplayName("Should match missing field as null")
    void shouldMatchMissingFieldAsNull() throws Exception {
        String json = "{\"name\": \"John\"}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("middleName", ComparisonOperator.EQUALS, null);
        
        assertThat(condition.matches(item)).isTrue();
    }

    @Test
    @DisplayName("Should not match null with non-null value")
    void shouldNotMatchNullWithValue() throws Exception {
        String json = "{\"name\": \"John\"}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("name", ComparisonOperator.EQUALS, null);
        
        assertThat(condition.matches(item)).isFalse();
    }

    @Test
    @DisplayName("Should extract nested field for comparison")
    void shouldExtractNestedField() throws Exception {
        String json = "{\"user\": {\"profile\": {\"age\": 25}}}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("user.profile.age", ComparisonOperator.GREATER_THAN, 18);
        
        assertThat(condition.matches(item)).isTrue();
    }

    @Test
    @DisplayName("Should throw exception for numeric comparison on null")
    void shouldThrowExceptionForNumericComparisonOnNull() throws Exception {
        String json = "{\"age\": null}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("age", ComparisonOperator.GREATER_THAN, 18);
        
        assertThatThrownBy(() -> condition.matches(item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot perform numeric comparison on null");
    }

    @Test
    @DisplayName("Should throw exception for numeric comparison on missing field")
    void shouldThrowExceptionForNumericComparisonOnMissing() throws Exception {
        String json = "{\"name\": \"John\"}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("age", ComparisonOperator.GREATER_THAN, 18);
        
        assertThatThrownBy(() -> condition.matches(item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot perform numeric comparison on null");
    }

    @Test
    @DisplayName("Should throw exception for numeric comparison on non-numeric value")
    void shouldThrowExceptionForNumericComparisonOnString() throws Exception {
        String json = "{\"age\": \"twenty\"}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("age", ComparisonOperator.GREATER_THAN, 18);
        
        assertThatThrownBy(() -> condition.matches(item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot perform numeric comparison on non-numeric value");
    }

    @Test
    @DisplayName("Should throw exception when expected value is not numeric for comparison")
    void shouldThrowExceptionWhenExpectedValueNotNumeric() throws Exception {
        String json = "{\"age\": 25}";
        JsonNode item = mapper.readTree(json);
        
        ComparisonCondition condition = new ComparisonCondition("age", ComparisonOperator.GREATER_THAN, "eighteen");
        
        assertThatThrownBy(() -> condition.matches(item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected value must be numeric");
    }

    @Test
    @DisplayName("Should generate condition string with string value")
    void shouldGenerateConditionStringWithString() {
        ComparisonCondition condition = new ComparisonCondition("status", ComparisonOperator.EQUALS, "active");
        
        assertThat(condition.toConditionString()).isEqualTo("status='active'");
    }

    @Test
    @DisplayName("Should generate condition string with null value")
    void shouldGenerateConditionStringWithNull() {
        ComparisonCondition condition = new ComparisonCondition("middleName", ComparisonOperator.EQUALS, null);
        
        assertThat(condition.toConditionString()).isEqualTo("middleName=null");
    }

    @Test
    @DisplayName("Should generate condition string with numeric value")
    void shouldGenerateConditionStringWithNumber() {
        ComparisonCondition condition = new ComparisonCondition("age", ComparisonOperator.GREATER_THAN, 18);
        
        assertThat(condition.toConditionString()).isEqualTo("age>18");
    }

    @Test
    @DisplayName("Should generate condition string with boolean value")
    void shouldGenerateConditionStringWithBoolean() {
        ComparisonCondition condition = new ComparisonCondition("active", ComparisonOperator.EQUALS, true);
        
        assertThat(condition.toConditionString()).isEqualTo("active=true");
    }

    @Test
    @DisplayName("Should return referenced paths")
    void shouldReturnReferencedPaths() {
        ComparisonCondition condition = new ComparisonCondition("user.profile.age", ComparisonOperator.GREATER_THAN, 18);
        
        Set<String> paths = condition.getReferencedPaths();
        
        assertThat(paths).containsExactly("user.profile.age");
    }

    @Test
    @DisplayName("Should provide getters")
    void shouldProvideGetters() {
        ComparisonCondition condition = new ComparisonCondition("status", ComparisonOperator.EQUALS, "active");
        
        assertThat(condition.getFieldPath()).isEqualTo("status");
        assertThat(condition.getOperator()).isEqualTo(ComparisonOperator.EQUALS);
        assertThat(condition.getExpectedValue()).isEqualTo("active");
    }
}
