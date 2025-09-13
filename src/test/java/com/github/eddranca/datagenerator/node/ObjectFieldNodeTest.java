package com.github.eddranca.datagenerator.node;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ObjectFieldNodeTest {

    @Mock
    private DslNode mockField1;

    @Mock
    private DslNode mockField2;

    @Test
    void testObjectFieldWithMultipleFields() {
        Map<String, DslNode> fields = new LinkedHashMap<>();
        fields.put("name", mockField1);
        fields.put("age", mockField2);

        ObjectFieldNode node = new ObjectFieldNode(fields);

        assertThat(node.getFields()).hasSize(2);
        assertThat(node.getFields()).containsEntry("name", mockField1);
        assertThat(node.getFields()).containsEntry("age", mockField2);
    }

    @Test
    void testObjectFieldWithEmptyFields() {
        Map<String, DslNode> fields = new LinkedHashMap<>();
        ObjectFieldNode node = new ObjectFieldNode(fields);

        assertThat(node.getFields()).isEmpty();
    }

    @Test
    void testObjectFieldPreservesOrder() {
        Map<String, DslNode> fields = new LinkedHashMap<>();
        fields.put("first", mockField1);
        fields.put("second", mockField2);

        ObjectFieldNode node = new ObjectFieldNode(fields);

        assertThat(node.getFields().keySet()).containsExactly("first", "second");
    }

    @Test
    void testObjectFieldIsImmutable() {
        Map<String, DslNode> originalFields = new LinkedHashMap<>();
        originalFields.put("name", mockField1);

        ObjectFieldNode node = new ObjectFieldNode(originalFields);

        // Modify original map
        originalFields.put("age", mockField2);

        // Node should not be affected
        assertThat(node.getFields()).hasSize(1);
        assertThat(node.getFields()).containsOnlyKeys("name");
    }
}
