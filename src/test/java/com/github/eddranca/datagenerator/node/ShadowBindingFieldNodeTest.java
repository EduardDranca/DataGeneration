package com.github.eddranca.datagenerator.node;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("ShadowBindingFieldNode")
class ShadowBindingFieldNodeTest {

    @Test
    @DisplayName("stores binding name and field path")
    void storesBindingNameAndFieldPath() {
        ShadowBindingFieldNode node = new ShadowBindingFieldNode("$user", "id", Collections.emptyList(), false);
        
        assertThat(node.getBindingName()).isEqualTo("$user");
        assertThat(node.getFieldPath()).isEqualTo("id");
    }

    @Test
    @DisplayName("getReferenceString() returns binding.field format")
    void getReferenceStringReturnsBindingFieldFormat() {
        ShadowBindingFieldNode node = new ShadowBindingFieldNode("$user", "regionId", Collections.emptyList(), false);
        
        assertThat(node.getReferenceString()).isEqualTo("$user.regionId");
    }

    @Test
    @DisplayName("getCollectionName() returns empty optional")
    void getCollectionNameReturnsEmptyOptional() {
        ShadowBindingFieldNode node = new ShadowBindingFieldNode("$user", "id", Collections.emptyList(), false);
        
        assertThat(node.getCollectionName()).isEmpty();
    }

    @Test
    @DisplayName("accept() calls visitor's visitShadowBindingField method")
    @SuppressWarnings("unchecked")
    void acceptCallsVisitorMethod() {
        ShadowBindingFieldNode node = new ShadowBindingFieldNode("$user", "id", Collections.emptyList(), false);
        DslNodeVisitor<String> visitor = mock(DslNodeVisitor.class);
        
        node.accept(visitor);
        
        verify(visitor).visitShadowBindingField(node);
    }

    @Test
    @DisplayName("resolve() throws UnsupportedOperationException")
    void resolveThrowsUnsupportedOperationException() {
        ShadowBindingFieldNode node = new ShadowBindingFieldNode("$user", "id", Collections.emptyList(), false);
        
        assertThatThrownBy(() -> node.resolve(null, null, null))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("should not be called directly");
    }

    @Test
    @DisplayName("isSequential() returns configured value")
    void isSequentialReturnsConfiguredValue() {
        ShadowBindingFieldNode sequentialNode = new ShadowBindingFieldNode("$user", "id", Collections.emptyList(), true);
        ShadowBindingFieldNode nonSequentialNode = new ShadowBindingFieldNode("$user", "id", Collections.emptyList(), false);
        
        assertThat(sequentialNode.isSequential()).isTrue();
        assertThat(nonSequentialNode.isSequential()).isFalse();
    }
}
