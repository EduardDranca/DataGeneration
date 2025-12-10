package com.github.eddranca.datagenerator.node;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConditionalReferenceNode")
class ConditionalReferenceNodeTest {

    @Test
    @DisplayName("Should create node with field name")
    void shouldCreateNodeWithFieldName() {
        Condition condition = new ComparisonCondition("status", ComparisonOperator.EQUALS, "active");
        ConditionalReferenceNode node = new ConditionalReferenceNode(
                "users", "id", condition, new ArrayList<>(), false);

        assertThat(node.hasFieldName()).isTrue();
        assertThat(node.getFieldName()).isEqualTo("id");
    }

    @Test
    @DisplayName("Should create node without field name")
    void shouldCreateNodeWithoutFieldName() {
        Condition condition = new ComparisonCondition("status", ComparisonOperator.EQUALS, "active");
        ConditionalReferenceNode node = new ConditionalReferenceNode(
                "users", null, condition, new ArrayList<>(), false);

        assertThat(node.hasFieldName()).isFalse();
        assertThat(node.getFieldName()).isEmpty();
    }

    @Test
    @DisplayName("Should get collection name")
    void shouldGetCollectionName() {
        Condition condition = new ComparisonCondition("status", ComparisonOperator.EQUALS, "active");
        ConditionalReferenceNode node = new ConditionalReferenceNode(
                "users", "id", condition, new ArrayList<>(), false);

        assertThat(node.getCollectionName()).isEqualTo(Optional.of("users"));
        assertThat(node.getCollectionNameString()).isEqualTo("users");
    }

    @Test
    @DisplayName("Should get condition")
    void shouldGetCondition() {
        Condition condition = new ComparisonCondition("status", ComparisonOperator.EQUALS, "active");
        ConditionalReferenceNode node = new ConditionalReferenceNode(
                "users", "id", condition, new ArrayList<>(), false);

        assertThat(node.getCondition()).isEqualTo(condition);
    }

    @Test
    @DisplayName("Should generate reference string with field name")
    void shouldGenerateReferenceStringWithFieldName() {
        Condition condition = new ComparisonCondition("status", ComparisonOperator.EQUALS, "active");
        ConditionalReferenceNode node = new ConditionalReferenceNode(
                "users", "id", condition, new ArrayList<>(), false);

        assertThat(node.getReferenceString()).isEqualTo("users[status='active'].id");
    }

    @Test
    @DisplayName("Should generate reference string without field name")
    void shouldGenerateReferenceStringWithoutFieldName() {
        Condition condition = new ComparisonCondition("status", ComparisonOperator.EQUALS, "active");
        ConditionalReferenceNode node = new ConditionalReferenceNode(
                "users", null, condition, new ArrayList<>(), false);

        assertThat(node.getReferenceString()).isEqualTo("users[status='active']");
    }

    @Test
    @DisplayName("Should generate reference string with complex condition")
    void shouldGenerateReferenceStringWithComplexCondition() {
        Condition cond1 = new ComparisonCondition("age", ComparisonOperator.GREATER_THAN_OR_EQUAL, 21);
        Condition cond2 = new ComparisonCondition("status", ComparisonOperator.EQUALS, "active");
        Condition andCondition = new LogicalCondition(LogicalCondition.Type.AND, List.of(cond1, cond2));
        
        ConditionalReferenceNode node = new ConditionalReferenceNode(
                "users", "id", andCondition, new ArrayList<>(), false);

        assertThat(node.getReferenceString()).isEqualTo("users[(age>=21 and status='active')].id");
    }

    @Test
    @DisplayName("Should generate reference string with OR condition")
    void shouldGenerateReferenceStringWithOrCondition() {
        Condition cond1 = new ComparisonCondition("featured", ComparisonOperator.EQUALS, true);
        Condition cond2 = new ComparisonCondition("rating", ComparisonOperator.GREATER_THAN, 4.5);
        Condition orCondition = new LogicalCondition(LogicalCondition.Type.OR, List.of(cond1, cond2));
        
        ConditionalReferenceNode node = new ConditionalReferenceNode(
                "products", "id", orCondition, new ArrayList<>(), false);

        assertThat(node.getReferenceString()).isEqualTo("products[(featured=true or rating>4.5)].id");
    }

    @Test
    @DisplayName("Should generate reference string with nested field")
    void shouldGenerateReferenceStringWithNestedField() {
        Condition condition = new ComparisonCondition("status", ComparisonOperator.EQUALS, "active");
        ConditionalReferenceNode node = new ConditionalReferenceNode(
                "users", "profile.name", condition, new ArrayList<>(), false);

        assertThat(node.getReferenceString()).isEqualTo("users[status='active'].profile.name");
    }
}
