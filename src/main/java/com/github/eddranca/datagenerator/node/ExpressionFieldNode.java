package com.github.eddranca.datagenerator.node;

import com.github.eddranca.datagenerator.expression.ExpressionNode;

/**
 * DSL node representing an {@code expr} field.
 * Contains the parsed expression tree that gets evaluated during generation.
 * <p>
 * DSL syntax:
 * <pre>
 * {"expr": "lowercase(${this.firstName}.${this.lastName}@example.com)"}
 * </pre>
 */
public class ExpressionFieldNode implements DslNode {
    private final ExpressionNode expressionTree;
    private final String rawExpression; // for error messages

    public ExpressionFieldNode(ExpressionNode expressionTree, String rawExpression) {
        this.expressionTree = expressionTree;
        this.rawExpression = rawExpression;
    }

    public ExpressionNode getExpressionTree() {
        return expressionTree;
    }

    public String getRawExpression() {
        return rawExpression;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitExpression(this);
    }
}
