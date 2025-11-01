package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LogicalCondition")
class LogicalConditionTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Should create AND condition with multiple conditions")
    void shouldCreateAndCondition() {
        Condition cond1 = new ComparisonCondition("age", ComparisonOperator.GREATER_THAN, 18);
        Condition cond2 = new ComparisonCondition("status", ComparisonOperator.EQUALS, "active");
        
        LogicalCondition andCondition = new LogicalCondition(LogicalCondition.Type.AND, List.of(cond1, cond2));
        
        assertThat(andCondition.getType()).isEqualTo(LogicalCondition.Type.AND);
        assertThat(andCondition.getConditions()).hasSize(2);
    }

    @Test
    @DisplayName("Should create OR condition with multiple conditions")
    void shouldCreateOrCondition() {
        Condition cond1 = new ComparisonCondition("featured", ComparisonOperator.EQUALS, true);
        Condition cond2 = new ComparisonCondition("rating", ComparisonOperator.GREATER_THAN, 4.5);
        
        LogicalCondition orCondition = new LogicalCondition(LogicalCondition.Type.OR, List.of(cond1, cond2));
        
        assertThat(orCondition.getType()).isEqualTo(LogicalCondition.Type.OR);
        assertThat(orCondition.getConditions()).hasSize(2);
    }

    @Test
    @DisplayName("Should match item when all AND conditions are true")
    void shouldMatchAndCondition() throws Exception {
        String json = "{\"age\": 25, \"status\": \"active\"}";
        JsonNode item = mapper.readTree(json);
        
        Condition cond1 = new ComparisonCondition("age", ComparisonOperator.GREATER_THAN, 18);
        Condition cond2 = new ComparisonCondition("status", ComparisonOperator.EQUALS, "active");
        LogicalCondition andCondition = new LogicalCondition(LogicalCondition.Type.AND, List.of(cond1, cond2));
        
        assertThat(andCondition.matches(item)).isTrue();
    }

    @Test
    @DisplayName("Should not match item when any AND condition is false")
    void shouldNotMatchAndConditionWhenOneFails() throws Exception {
        String json = "{\"age\": 15, \"status\": \"active\"}";
        JsonNode item = mapper.readTree(json);
        
        Condition cond1 = new ComparisonCondition("age", ComparisonOperator.GREATER_THAN, 18);
        Condition cond2 = new ComparisonCondition("status", ComparisonOperator.EQUALS, "active");
        LogicalCondition andCondition = new LogicalCondition(LogicalCondition.Type.AND, List.of(cond1, cond2));
        
        assertThat(andCondition.matches(item)).isFalse();
    }

    @Test
    @DisplayName("Should match item when any OR condition is true")
    void shouldMatchOrCondition() throws Exception {
        String json = "{\"featured\": false, \"rating\": 4.8}";
        JsonNode item = mapper.readTree(json);
        
        Condition cond1 = new ComparisonCondition("featured", ComparisonOperator.EQUALS, true);
        Condition cond2 = new ComparisonCondition("rating", ComparisonOperator.GREATER_THAN, 4.5);
        LogicalCondition orCondition = new LogicalCondition(LogicalCondition.Type.OR, List.of(cond1, cond2));
        
        assertThat(orCondition.matches(item)).isTrue();
    }

    @Test
    @DisplayName("Should not match item when all OR conditions are false")
    void shouldNotMatchOrConditionWhenAllFail() throws Exception {
        String json = "{\"featured\": false, \"rating\": 3.0}";
        JsonNode item = mapper.readTree(json);
        
        Condition cond1 = new ComparisonCondition("featured", ComparisonOperator.EQUALS, true);
        Condition cond2 = new ComparisonCondition("rating", ComparisonOperator.GREATER_THAN, 4.5);
        LogicalCondition orCondition = new LogicalCondition(LogicalCondition.Type.OR, List.of(cond1, cond2));
        
        assertThat(orCondition.matches(item)).isFalse();
    }

    @Test
    @DisplayName("Should generate correct condition string for AND")
    void shouldGenerateAndConditionString() {
        Condition cond1 = new ComparisonCondition("age", ComparisonOperator.GREATER_THAN_OR_EQUAL, 21);
        Condition cond2 = new ComparisonCondition("status", ComparisonOperator.EQUALS, "active");
        LogicalCondition andCondition = new LogicalCondition(LogicalCondition.Type.AND, List.of(cond1, cond2));
        
        assertThat(andCondition.toConditionString()).isEqualTo("(age>=21 and status='active')");
    }

    @Test
    @DisplayName("Should generate correct condition string for OR")
    void shouldGenerateOrConditionString() {
        Condition cond1 = new ComparisonCondition("featured", ComparisonOperator.EQUALS, true);
        Condition cond2 = new ComparisonCondition("rating", ComparisonOperator.GREATER_THAN, 4.5);
        LogicalCondition orCondition = new LogicalCondition(LogicalCondition.Type.OR, List.of(cond1, cond2));
        
        assertThat(orCondition.toConditionString()).isEqualTo("(featured=true or rating>4.5)");
    }

    @Test
    @DisplayName("Should collect all referenced paths from conditions")
    void shouldCollectReferencedPaths() {
        Condition cond1 = new ComparisonCondition("age", ComparisonOperator.GREATER_THAN, 18);
        Condition cond2 = new ComparisonCondition("status", ComparisonOperator.EQUALS, "active");
        Condition cond3 = new ComparisonCondition("balance", ComparisonOperator.GREATER_THAN, 100);
        LogicalCondition andCondition = new LogicalCondition(LogicalCondition.Type.AND, List.of(cond1, cond2, cond3));
        
        Set<String> paths = andCondition.getReferencedPaths();
        
        assertThat(paths).containsExactlyInAnyOrder("age", "status", "balance");
    }

    @Test
    @DisplayName("Should throw exception when created with less than 2 conditions")
    void shouldThrowExceptionWithLessThanTwoConditions() {
        Condition cond1 = new ComparisonCondition("age", ComparisonOperator.GREATER_THAN, 18);
        
        assertThatThrownBy(() -> new LogicalCondition(LogicalCondition.Type.AND, List.of(cond1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires at least 2 conditions");
    }

    @Test
    @DisplayName("Should throw exception when created with null conditions")
    void shouldThrowExceptionWithNullConditions() {
        assertThatThrownBy(() -> new LogicalCondition(LogicalCondition.Type.AND, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires at least 2 conditions");
    }

    @Test
    @DisplayName("Should get operator string from Type enum")
    void shouldGetOperatorFromType() {
        assertThat(LogicalCondition.Type.AND.getOperator()).isEqualTo("and");
        assertThat(LogicalCondition.Type.OR.getOperator()).isEqualTo("or");
    }
}
