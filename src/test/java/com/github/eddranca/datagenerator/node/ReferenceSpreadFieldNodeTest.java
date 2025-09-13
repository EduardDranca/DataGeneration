package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceSpreadFieldNodeTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void testReferenceSpreadFieldWithSimpleReference() {
        SimpleReferenceNode simpleRef = new SimpleReferenceNode("users", "name", List.of(), false);
        List<String> fields = List.of("name", "age");

        ReferenceSpreadFieldNode node = new ReferenceSpreadFieldNode(simpleRef, fields);

        assertThat(node.getReferenceNode()).isEqualTo(simpleRef);
        assertThat(node.getFields()).containsExactly("name", "age");
        assertThat(node.getFilters()).isEmpty();
        assertThat(node.isSequential()).isFalse();
    }

    @Test
    void testReferenceSpreadFieldWithIndexedReference() {
        IndexedReferenceNode indexedRef = new IndexedReferenceNode("users", "*", "name", List.of(), true);
        List<String> fields = List.of("id", "name", "email");

        ReferenceSpreadFieldNode node = new ReferenceSpreadFieldNode(indexedRef, fields);

        assertThat(node.getReferenceNode()).isEqualTo(indexedRef);
        assertThat(node.getFields()).containsExactly("id", "name", "email");
        assertThat(node.getFilters()).isEmpty();
        assertThat(node.isSequential()).isTrue();
    }

    @Test
    void testReferenceSpreadFieldWithTagReference() {
        TagReferenceNode tagRef = new TagReferenceNode("userTag", "name", List.of(), false);
        List<String> fields = List.of("name");

        ReferenceSpreadFieldNode node = new ReferenceSpreadFieldNode(tagRef, fields);

        assertThat(node.getReferenceNode()).isEqualTo(tagRef);
        assertThat(node.getFields()).containsExactly("name");
        assertThat(node.isSequential()).isFalse();
    }

    @Test
    void testReferenceSpreadFieldWithFilters() {
        FilterNode filter = new FilterNode(new LiteralFieldNode(mapper.valueToTree("test")));
        List<FilterNode> filters = List.of(filter);
        SimpleReferenceNode simpleRef = new SimpleReferenceNode("users", "name", filters, true);
        List<String> fields = List.of("name", "age");

        ReferenceSpreadFieldNode node = new ReferenceSpreadFieldNode(simpleRef, fields);

        assertThat(node.getReferenceNode()).isEqualTo(simpleRef);
        assertThat(node.getFields()).containsExactly("name", "age");
        assertThat(node.getFilters()).hasSize(1);
        assertThat(node.getFilters().get(0)).isEqualTo(filter);
        assertThat(node.isSequential()).isTrue();
    }

    @Test
    void testReferenceSpreadFieldWithEmptyFields() {
        SimpleReferenceNode simpleRef = new SimpleReferenceNode("users", null, List.of(), false);
        List<String> fields = List.of();

        ReferenceSpreadFieldNode node = new ReferenceSpreadFieldNode(simpleRef, fields);

        assertThat(node.getReferenceNode()).isEqualTo(simpleRef);
        assertThat(node.getFields()).isEmpty();
        assertThat(node.isSequential()).isFalse();
    }

    @Test
    void testReferenceSpreadFieldWithNullReference() {
        List<String> fields = List.of("name", "age");

        ReferenceSpreadFieldNode node = new ReferenceSpreadFieldNode(null, fields);

        assertThat(node.getReferenceNode()).isNull();
        assertThat(node.getFields()).containsExactly("name", "age");
        assertThat(node.getFilters()).isEmpty();
        assertThat(node.isSequential()).isFalse();
    }

    @Test
    void testReferenceSpreadFieldIsImmutable() {
        SimpleReferenceNode simpleRef = new SimpleReferenceNode("users", null, List.of(), false);
        List<String> originalFields = new ArrayList<>();
        originalFields.add("name");

        ReferenceSpreadFieldNode node = new ReferenceSpreadFieldNode(simpleRef, originalFields);

        // Modify original list - should not affect the node
        originalFields.add("age");

        assertThat(node.getFields()).hasSize(1);
        assertThat(node.getFields()).containsExactly("name");
    }
}
