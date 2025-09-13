package com.github.eddranca.datagenerator.node;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ItemNodeTest {

    @Mock
    private DslNode mockField1;

    @Mock
    private DslNode mockField2;

    @Mock
    private DslNodeVisitor<String> mockVisitor;

    @Test
    void testItemNodeWithMultipleFields() {
        Map<String, DslNode> fields = new LinkedHashMap<>();
        fields.put("name", mockField1);
        fields.put("age", mockField2);

        ItemNode node = new ItemNode(fields);

        assertThat(node.getFields()).hasSize(2);
        assertThat(node.getFields().get("name")).isEqualTo(mockField1);
        assertThat(node.getFields().get("age")).isEqualTo(mockField2);
    }

    @Test
    void testItemNodeWithEmptyFields() {
        Map<String, DslNode> fields = new LinkedHashMap<>();
        ItemNode node = new ItemNode(fields);

        assertThat(node.getFields()).isEmpty();
    }

    @Test
    void testItemNodePreservesFieldOrder() {
        Map<String, DslNode> fields = new LinkedHashMap<>();
        fields.put("first", mockField1);
        fields.put("second", mockField2);

        ItemNode node = new ItemNode(fields);

        assertThat(node.getFields().keySet()).containsExactly("first", "second");
    }

    @Test
    void testItemNodeIsImmutable() {
        Map<String, DslNode> originalFields = new LinkedHashMap<>();
        originalFields.put("name", mockField1);

        ItemNode node = new ItemNode(originalFields);

        // Modify original map
        originalFields.put("age", mockField2);

        // Node should not be affected
        assertThat(node.getFields()).hasSize(1);
        assertThat(node.getFields()).containsOnlyKeys("name");
    }
}
