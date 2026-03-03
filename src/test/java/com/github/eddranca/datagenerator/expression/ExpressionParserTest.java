package com.github.eddranca.datagenerator.expression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExpressionParserTest {
    private ExpressionFunctionRegistry registry;
    private List<String> errors;
    private ExpressionParser parser;

    @BeforeEach
    void setUp() {
        registry = new ExpressionFunctionRegistry();
        errors = new ArrayList<>();
        parser = new ExpressionParser(errors::add, registry);
    }

    @Test
    void testParseLiteral() {
        ExpressionNode node = parser.parse("hello world");
        assertThat(node).isInstanceOf(LiteralExprNode.class);
        assertThat(((LiteralExprNode) node).value()).isEqualTo("hello world");
    }

    @Test
    void testParseSingleReference() {
        ExpressionNode node = parser.parse("${this.name}");
        assertThat(node).isInstanceOf(ReferenceExprNode.class);
        assertThat(((ReferenceExprNode) node).reference()).isEqualTo("this.name");
    }

    @Test
    void testParseTemplateWithReferences() {
        ExpressionNode node = parser.parse("Hello ${this.firstName} ${this.lastName}!");
        assertThat(node).isInstanceOf(ConcatExprNode.class);
        ConcatExprNode concat = (ConcatExprNode) node;
        assertThat(concat.parts()).hasSize(5);
        assertThat(concat.parts().get(0)).isInstanceOf(LiteralExprNode.class);
        assertThat(concat.parts().get(1)).isInstanceOf(ReferenceExprNode.class);
        assertThat(concat.parts().get(2)).isInstanceOf(LiteralExprNode.class);
        assertThat(concat.parts().get(3)).isInstanceOf(ReferenceExprNode.class);
        assertThat(concat.parts().get(4)).isInstanceOf(LiteralExprNode.class);
    }

    @Test
    void testParseFunctionCall() {
        ExpressionNode node = parser.parse("lowercase(${this.name})");
        assertThat(node).isInstanceOf(FunctionCallExprNode.class);
        FunctionCallExprNode func = (FunctionCallExprNode) node;
        assertThat(func.functionName()).isEqualTo("lowercase");
        assertThat(func.argument()).isInstanceOf(ReferenceExprNode.class);
        assertThat(func.extraArgs()).isEmpty();
    }

    @Test
    void testParseSubstringWithExtraArgs() {
        ExpressionNode node = parser.parse("substring(${this.id}, 0, 8)");
        assertThat(node).isInstanceOf(FunctionCallExprNode.class);
        FunctionCallExprNode func = (FunctionCallExprNode) node;
        assertThat(func.functionName()).isEqualTo("substring");
        assertThat(func.extraArgs()).containsExactly("0", "8");
    }

    @Test
    void testParseNestedFunctions() {
        ExpressionNode node = parser.parse("uppercase(trim(${this.name}))");
        assertThat(node).isInstanceOf(FunctionCallExprNode.class);
        FunctionCallExprNode outer = (FunctionCallExprNode) node;
        assertThat(outer.functionName()).isEqualTo("uppercase");
        assertThat(outer.argument()).isInstanceOf(FunctionCallExprNode.class);
        FunctionCallExprNode inner = (FunctionCallExprNode) outer.argument();
        assertThat(inner.functionName()).isEqualTo("trim");
    }

    @Test
    void testParseFunctionWithTemplateArg() {
        ExpressionNode node = parser.parse("lowercase(${this.first}.${this.last}@example.com)");
        assertThat(node).isInstanceOf(FunctionCallExprNode.class);
        FunctionCallExprNode func = (FunctionCallExprNode) node;
        assertThat(func.functionName()).isEqualTo("lowercase");
        assertThat(func.argument()).isInstanceOf(ConcatExprNode.class);
    }

    @Test
    void testEmptyExpressionReportsError() {
        ExpressionNode node = parser.parse("");
        assertThat(node).isNull();
        assertThat(errors).hasSize(1);
    }

    @Test
    void testNullExpressionReportsError() {
        ExpressionNode node = parser.parse(null);
        assertThat(node).isNull();
        assertThat(errors).hasSize(1);
    }

    @Test
    void testUnknownFunctionReportsError() {
        ExpressionNode node = parser.parse("nonexistent(hello)");
        assertThat(node).isNull();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("nonexistent");
    }

    @Test
    void testUnclosedReferenceReportsError() {
        ExpressionNode node = parser.parse("Hello ${this.name");
        assertThat(node).isNull();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("Unclosed");
    }

    @Test
    void testEmptyReferenceReportsError() {
        ExpressionNode node = parser.parse("Hello ${}!");
        assertThat(node).isNull();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("Empty");
    }

    @Test
    void testShadowBindingReference() {
        ExpressionNode node = parser.parse("${$user.firstName}");
        assertThat(node).isInstanceOf(ReferenceExprNode.class);
        assertThat(((ReferenceExprNode) node).reference()).isEqualTo("$user.firstName");
    }

    @Test
    void testCollectionReference() {
        ExpressionNode node = parser.parse("${users[*].name}");
        assertThat(node).isInstanceOf(ReferenceExprNode.class);
        assertThat(((ReferenceExprNode) node).reference()).isEqualTo("users[*].name");
    }
}
