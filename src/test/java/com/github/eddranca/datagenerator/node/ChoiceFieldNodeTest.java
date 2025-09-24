package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ChoiceFieldNodeTest {

    @Mock
    private DslNode mockOption1;

    @Mock
    private DslNode mockOption2;

    @Mock
    private DslNode mockOption3;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void testChoiceFieldWithEqualWeights() {
        List<DslNode> options = List.of(mockOption1, mockOption2, mockOption3);
        ChoiceFieldNode node = new ChoiceFieldNode(options);

        assertThat(node.getOptions()).hasSize(3);
        assertThat(node.getOptions()).containsExactly(mockOption1, mockOption2, mockOption3);
        assertThat(node.hasWeights()).isFalse();
        assertThat(node.getWeights()).isNull();
        assertThat(node.hasFilters()).isFalse();
        assertThat(node.getFilters()).isEmpty();
    }

    @Test
    void testChoiceFieldWithWeights() {
        List<DslNode> options = List.of(mockOption1, mockOption2);
        List<Double> weights = List.of(0.7, 0.3);
        ChoiceFieldNode node = new ChoiceFieldNode(options, weights, List.of());

        assertThat(node.getOptions()).hasSize(2);
        assertThat(node.hasWeights()).isTrue();
        assertThat(node.getWeights()).containsExactly(0.7, 0.3);
    }

    @Test
    void testChoiceFieldWithFilters() {
        List<DslNode> options = List.of(mockOption1, mockOption2);
        FilterNode filter = new FilterNode(new LiteralFieldNode(mapper.valueToTree("exclude")));
        List<FilterNode> filters = List.of(filter);

        ChoiceFieldNode node = ChoiceFieldNode.withFilters(options, filters);

        assertThat(node.hasFilters()).isTrue();
        assertThat(node.getFilters()).hasSize(1);
        assertThat(node.getFilters().get(0)).isEqualTo(filter);
    }

    @Test
    void testChoiceFieldWithWeightsAndFilters() {
        List<DslNode> options = List.of(mockOption1, mockOption2);
        List<Double> weights = List.of(0.6, 0.4);
        FilterNode filter = new FilterNode(new LiteralFieldNode(mapper.valueToTree("exclude")));
        List<FilterNode> filters = List.of(filter);

        ChoiceFieldNode node = ChoiceFieldNode.withWeightsAndFilters(options, weights, filters);

        assertThat(node.hasWeights()).isTrue();
        assertThat(node.getWeights()).containsExactly(0.6, 0.4);
        assertThat(node.hasFilters()).isTrue();
        assertThat(node.getFilters()).hasSize(1);
    }

    @Test
    void testChoiceFieldWithMismatchedWeights() {
        List<DslNode> options = List.of(mockOption1, mockOption2);
        List<Double> weights = List.of(0.7); // Only one weight for two options

        assertThatThrownBy(() -> new ChoiceFieldNode(options, weights, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Options and weights lists must have the same size");
    }

    @Test
    void testChoiceFieldWithNegativeWeight() {
        List<DslNode> options = List.of(mockOption1, mockOption2);
        List<Double> weights = List.of(0.7, -0.3);

        assertThatThrownBy(() -> new ChoiceFieldNode(options, weights, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("All weights must be positive numbers");
    }

    @Test
    void testChoiceFieldWithZeroWeight() {
        List<DslNode> options = List.of(mockOption1, mockOption2);
        List<Double> weights = List.of(0.7, 0.0);

        assertThatThrownBy(() -> new ChoiceFieldNode(options, weights, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("All weights must be positive numbers");
    }

    @Test
    void testChoiceFieldWithNullWeight() {
        List<DslNode> options = List.of(mockOption1, mockOption2);
        List<Double> weights = new ArrayList<>();
        weights.add(0.7);
        weights.add(null);

        assertThatThrownBy(() -> new ChoiceFieldNode(options, weights, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("All weights must be positive numbers");
    }

    @Test
    void testChoiceFieldIsImmutable() {
        List<DslNode> originalOptions = new ArrayList<>();
        originalOptions.add(mockOption1);
        List<FilterNode> originalFilters = new ArrayList<>();
        ChoiceFieldNode node = new ChoiceFieldNode(originalOptions, null, originalFilters);

        // Modify original lists - should not affect the node
        originalOptions.add(mockOption2);

        assertThat(node.getOptions()).hasSize(1);
    }
}
