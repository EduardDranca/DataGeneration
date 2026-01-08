package com.github.eddranca.datagenerator.node;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("ShadowBindingNode")
class ShadowBindingNodeTest {

    @Test
    @DisplayName("stores binding name and reference node")
    void storesBindingNameAndReferenceNode() {
        DslNode mockReference = mock(DslNode.class);
        ShadowBindingNode node = new ShadowBindingNode("$user", mockReference);
        
        assertThat(node.getBindingName()).isEqualTo("$user");
        assertThat(node.getReferenceNode()).isSameAs(mockReference);
    }

    @Test
    @DisplayName("accept() calls visitor's visitShadowBinding method")
    @SuppressWarnings("unchecked")
    void acceptCallsVisitorMethod() {
        DslNode mockReference = mock(DslNode.class);
        ShadowBindingNode node = new ShadowBindingNode("$user", mockReference);
        DslNodeVisitor<String> visitor = mock(DslNodeVisitor.class);
        
        node.accept(visitor);
        
        verify(visitor).visitShadowBinding(node);
    }
}
