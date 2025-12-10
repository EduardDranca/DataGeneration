package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.node.ComparisonCondition;
import com.github.eddranca.datagenerator.node.ComparisonOperator;
import com.github.eddranca.datagenerator.node.Condition;
import com.github.eddranca.datagenerator.node.LogicalCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConditionParser")
class ConditionParserTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<String> errors = new ArrayList<>();
    private final ConditionParser parser = new ConditionParser(errors::add);

    @Test
    @DisplayName("Should parse simple equality condition")
    void shouldParseSimpleEquality() {
        Condition condition = parser.parse("status='active'");

        assertThat(condition).isInstanceOf(ComparisonCondition.class);
        ComparisonCondition comparison = (ComparisonCondition) condition;
        assertThat(comparison.getOperator()).isEqualTo(ComparisonOperator.EQUALS);
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should parse string with 'and' inside quotes as simple equality")
    void shouldParseStringWithAndInsideQuotes() throws Exception {
        Condition condition = parser.parse("value='something and something else'");

        assertThat(condition).isInstanceOf(ComparisonCondition.class);
        ComparisonCondition comparison = (ComparisonCondition) condition;
        assertThat(comparison.getOperator()).isEqualTo(ComparisonOperator.EQUALS);
        
        // Test that it matches correctly
        String json = "{\"value\": \"something and something else\"}";
        JsonNode item = mapper.readTree(json);
        assertThat(condition.matches(item)).isTrue();
        
        // Should not match a different value
        String json2 = "{\"value\": \"something\"}";
        JsonNode item2 = mapper.readTree(json2);
        assertThat(condition.matches(item2)).isFalse();
        
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should parse string with 'or' inside quotes as simple equality")
    void shouldParseStringWithOrInsideQuotes() throws Exception {
        Condition condition = parser.parse("description='red or blue'");

        assertThat(condition).isInstanceOf(ComparisonCondition.class);
        ComparisonCondition comparison = (ComparisonCondition) condition;
        assertThat(comparison.getOperator()).isEqualTo(ComparisonOperator.EQUALS);
        
        // Test that it matches correctly
        String json = "{\"description\": \"red or blue\"}";
        JsonNode item = mapper.readTree(json);
        assertThat(condition.matches(item)).isTrue();
        
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should parse actual AND logical condition")
    void shouldParseActualAndCondition() {
        Condition condition = parser.parse("age>=21 and status='active'");

        assertThat(condition).isInstanceOf(LogicalCondition.class);
        LogicalCondition logical = (LogicalCondition) condition;
        assertThat(logical.getType()).isEqualTo(LogicalCondition.Type.AND);
        assertThat(logical.getConditions()).hasSize(2);
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should parse actual OR logical condition")
    void shouldParseActualOrCondition() {
        Condition condition = parser.parse("category='electronics' or featured=true");

        assertThat(condition).isInstanceOf(LogicalCondition.class);
        LogicalCondition logical = (LogicalCondition) condition;
        assertThat(logical.getType()).isEqualTo(LogicalCondition.Type.OR);
        assertThat(logical.getConditions()).hasSize(2);
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should handle complex string with both 'and' and 'or' inside quotes")
    void shouldHandleComplexStringInsideQuotes() throws Exception {
        Condition condition = parser.parse("message='you can have this and that or something else'");

        assertThat(condition).isInstanceOf(ComparisonCondition.class);
        
        // Test that it matches correctly
        String json = "{\"message\": \"you can have this and that or something else\"}";
        JsonNode item = mapper.readTree(json);
        assertThat(condition.matches(item)).isTrue();
        
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should parse number comparison")
    void shouldParseNumberComparison() {
        Condition condition = parser.parse("price>50");

        assertThat(condition).isInstanceOf(ComparisonCondition.class);
        ComparisonCondition comparison = (ComparisonCondition) condition;
        assertThat(comparison.getOperator()).isEqualTo(ComparisonOperator.GREATER_THAN);
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should parse boolean comparison")
    void shouldParseBooleanComparison() {
        Condition condition = parser.parse("isActive=true");

        assertThat(condition).isInstanceOf(ComparisonCondition.class);
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should reject uppercase AND")
    void shouldRejectUppercaseAnd() {
        Condition condition = parser.parse("age>=21 AND status='active'");

        assertThat(condition).isNull();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("lowercase");
    }

    @Test
    @DisplayName("Should reject uppercase OR")
    void shouldRejectUppercaseOr() {
        Condition condition = parser.parse("category='electronics' OR featured=true");

        assertThat(condition).isNull();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("lowercase");
    }

    @Test
    @DisplayName("Should handle mixed case: logical AND with string containing 'or'")
    void shouldHandleMixedLogicalAndStringWithOr() throws Exception {
        Condition condition = parser.parse("status='active' and description='red or blue'");

        assertThat(condition).isInstanceOf(LogicalCondition.class);
        LogicalCondition logical = (LogicalCondition) condition;
        assertThat(logical.getType()).isEqualTo(LogicalCondition.Type.AND);
        assertThat(logical.getConditions()).hasSize(2);
        
        // Test that it matches correctly
        String json = "{\"status\": \"active\", \"description\": \"red or blue\"}";
        JsonNode item = mapper.readTree(json);
        assertThat(condition.matches(item)).isTrue();
        
        // Should not match if status is different
        String json2 = "{\"status\": \"inactive\", \"description\": \"red or blue\"}";
        JsonNode item2 = mapper.readTree(json2);
        assertThat(condition.matches(item2)).isFalse();
        
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should handle mixed case: logical OR with string containing 'and'")
    void shouldHandleMixedLogicalOrStringWithAnd() throws Exception {
        Condition condition = parser.parse("priority='high' or message='this and that'");

        assertThat(condition).isInstanceOf(LogicalCondition.class);
        LogicalCondition logical = (LogicalCondition) condition;
        assertThat(logical.getType()).isEqualTo(LogicalCondition.Type.OR);
        assertThat(logical.getConditions()).hasSize(2);
        
        // Test that it matches when first condition is true
        String json1 = "{\"priority\": \"high\", \"message\": \"something else\"}";
        JsonNode item1 = mapper.readTree(json1);
        assertThat(condition.matches(item1)).isTrue();
        
        // Test that it matches when second condition is true
        String json2 = "{\"priority\": \"low\", \"message\": \"this and that\"}";
        JsonNode item2 = mapper.readTree(json2);
        assertThat(condition.matches(item2)).isTrue();
        
        // Should not match when neither condition is true
        String json3 = "{\"priority\": \"low\", \"message\": \"something else\"}";
        JsonNode item3 = mapper.readTree(json3);
        assertThat(condition.matches(item3)).isFalse();
        
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should handle operators inside quoted strings")
    void shouldHandleOperatorsInsideQuotes() throws Exception {
        Condition condition = parser.parse("formula='x>=5'");

        assertThat(condition).isInstanceOf(ComparisonCondition.class);
        
        // Test that it matches the literal string
        String json = "{\"formula\": \"x>=5\"}";
        JsonNode item = mapper.readTree(json);
        assertThat(condition.matches(item)).isTrue();
        
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty string value")
    void shouldHandleEmptyStringValue() throws Exception {
        Condition condition = parser.parse("value=''");

        assertThat(condition).isInstanceOf(ComparisonCondition.class);
        
        // Test that it matches empty string
        String json = "{\"value\": \"\"}";
        JsonNode item = mapper.readTree(json);
        assertThat(condition.matches(item)).isTrue();
        
        // Should not match non-empty string
        String json2 = "{\"value\": \"something\"}";
        JsonNode item2 = mapper.readTree(json2);
        assertThat(condition.matches(item2)).isFalse();
        
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should handle equals sign inside quoted string")
    void shouldHandleEqualsInsideQuotes() throws Exception {
        Condition condition = parser.parse("equation='a=b'");

        assertThat(condition).isInstanceOf(ComparisonCondition.class);
        
        // Test that it matches the literal string with equals
        String json = "{\"equation\": \"a=b\"}";
        JsonNode item = mapper.readTree(json);
        assertThat(condition.matches(item)).isTrue();
        
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should handle single quote in middle of unquoted value")
    void shouldHandleSingleQuoteInUnquotedValue() {
        // This is an edge case - unquoted value with apostrophe
        // The parser will treat it as starting a quoted string
        Condition condition = parser.parse("name=O'Brien");

        // This will likely fail or behave unexpectedly
        // Just documenting current behavior
        assertThat(condition).isNotNull();
    }

    @Test
    @DisplayName("Should handle comparison with negative number")
    void shouldHandleNegativeNumber() throws Exception {
        Condition condition = parser.parse("temperature<-5");

        assertThat(condition).isInstanceOf(ComparisonCondition.class);
        ComparisonCondition comparison = (ComparisonCondition) condition;
        assertThat(comparison.getOperator()).isEqualTo(ComparisonOperator.LESS_THAN);
        
        // Test that it matches
        String json = "{\"temperature\": -10}";
        JsonNode item = mapper.readTree(json);
        assertThat(condition.matches(item)).isTrue();
        
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should handle whitespace in quoted strings")
    void shouldHandleWhitespaceInQuotes() throws Exception {
        Condition condition = parser.parse("message='  spaces  '");

        assertThat(condition).isInstanceOf(ComparisonCondition.class);
        
        // Test that it preserves spaces
        String json = "{\"message\": \"  spaces  \"}";
        JsonNode item = mapper.readTree(json);
        assertThat(condition.matches(item)).isTrue();
        
        assertThat(errors).isEmpty();
    }
}
