package com.github.eddranca.datagenerator.expression;

/**
 * A literal text segment in an expression.
 * Represents everything outside {@code ${}} placeholders and function calls.
 */
public record LiteralExprNode(String value) implements ExpressionNode {
}
