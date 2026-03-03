package com.github.eddranca.datagenerator.expression;

/**
 * Base interface for expression AST nodes.
 * An expression is parsed from the {@code expr} DSL keyword into a tree of these nodes.
 */
public sealed interface ExpressionNode
    permits LiteralExprNode, ReferenceExprNode, FunctionCallExprNode, ConcatExprNode {
}
