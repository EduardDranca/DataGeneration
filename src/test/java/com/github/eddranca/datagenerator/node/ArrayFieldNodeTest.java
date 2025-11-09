package com.github.eddranca.datagenerator.node;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;

class ArrayFieldNodeTest {

    @Mock
    private DslNode mockItemNode;

    @Test
    void testFixedSizeArrayField() {
        ArrayFieldNode node = new ArrayFieldNode(5, mockItemNode);

        assertThat(node.hasFixedSize()).isTrue();
        assertThat(node.getSize()).isEqualTo(5);
        assertThat(node.getMinSize()).isZero();
        assertThat(node.getMaxSize()).isZero();
        assertThat(node.getItemNode()).isEqualTo(mockItemNode);
    }

    @Test
    void testVariableSizeArrayField() {
        ArrayFieldNode node = new ArrayFieldNode(2, 10, mockItemNode);

        assertThat(node.hasFixedSize()).isFalse();
        assertThat(node.getSize()).isZero();
        assertThat(node.getMinSize()).isEqualTo(2);
        assertThat(node.getMaxSize()).isEqualTo(10);
        assertThat(node.getItemNode()).isEqualTo(mockItemNode);
    }
}
