package com.github.eddranca.datagenerator.node;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ComparisonOperator")
class ComparisonOperatorTest {

    @Test
    @DisplayName("Should have correct symbol for EQUALS")
    void shouldHaveCorrectSymbolForEquals() {
        assertThat(ComparisonOperator.EQUALS.getSymbol()).isEqualTo("=");
    }

    @Test
    @DisplayName("Should have correct symbol for NOT_EQUALS")
    void shouldHaveCorrectSymbolForNotEquals() {
        assertThat(ComparisonOperator.NOT_EQUALS.getSymbol()).isEqualTo("!=");
    }

    @Test
    @DisplayName("Should have correct symbol for LESS_THAN")
    void shouldHaveCorrectSymbolForLessThan() {
        assertThat(ComparisonOperator.LESS_THAN.getSymbol()).isEqualTo("<");
    }

    @Test
    @DisplayName("Should have correct symbol for LESS_THAN_OR_EQUAL")
    void shouldHaveCorrectSymbolForLessThanOrEqual() {
        assertThat(ComparisonOperator.LESS_THAN_OR_EQUAL.getSymbol()).isEqualTo("<=");
    }

    @Test
    @DisplayName("Should have correct symbol for GREATER_THAN")
    void shouldHaveCorrectSymbolForGreaterThan() {
        assertThat(ComparisonOperator.GREATER_THAN.getSymbol()).isEqualTo(">");
    }

    @Test
    @DisplayName("Should have correct symbol for GREATER_THAN_OR_EQUAL")
    void shouldHaveCorrectSymbolForGreaterThanOrEqual() {
        assertThat(ComparisonOperator.GREATER_THAN_OR_EQUAL.getSymbol()).isEqualTo(">=");
    }

    @Test
    @DisplayName("Should have all six operators")
    void shouldHaveAllSixOperators() {
        ComparisonOperator[] operators = ComparisonOperator.values();
        assertThat(operators).hasSize(6);
    }

    @Test
    @DisplayName("Should be able to get operator by name")
    void shouldGetOperatorByName() {
        ComparisonOperator operator = ComparisonOperator.valueOf("EQUALS");
        assertThat(operator).isEqualTo(ComparisonOperator.EQUALS);
    }
}
