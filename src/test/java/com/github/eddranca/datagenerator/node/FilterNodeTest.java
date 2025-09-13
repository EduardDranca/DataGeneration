package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FilterNodeTest {

    @Mock
    private DslNode mockFilterExpression;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void testFilterNodeWithExpression() {
        FilterNode node = new FilterNode(mockFilterExpression);

        assertThat(node.getFilterExpression()).isEqualTo(mockFilterExpression);
    }

    @Test
    void testFilterNodeWithLiteralExpression() {
        LiteralFieldNode literalExpression = new LiteralFieldNode(mapper.valueToTree("test"));
        FilterNode node = new FilterNode(literalExpression);

        assertThat(node.getFilterExpression()).isEqualTo(literalExpression);
        assertThat(node.getFilterExpression()).isInstanceOf(LiteralFieldNode.class);
    }
}
