package com.github.eddranca.datagenerator.expression;

/**
 * A reference placeholder in an expression (the content inside {@code ${}}).
 * The reference string uses the same syntax as DSL references:
 * {@code this.field}, {@code collection[*].field}, {@code pickName.field}, {@code $binding.field}.
 */
public record ReferenceExprNode(String reference) implements ExpressionNode {
}
