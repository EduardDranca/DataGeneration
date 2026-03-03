package com.github.eddranca.datagenerator.expression;

import java.util.List;

/**
 * A concatenation of multiple expression parts.
 * Produced when an expression contains a mix of literals and references,
 * e.g., {@code "Hello ${this.firstName}!"} becomes
 * ConcatExprNode([LiteralExprNode("Hello "), ReferenceExprNode("this.firstName"), LiteralExprNode("!")]).
 */
public record ConcatExprNode(List<ExpressionNode> parts) implements ExpressionNode {
}
