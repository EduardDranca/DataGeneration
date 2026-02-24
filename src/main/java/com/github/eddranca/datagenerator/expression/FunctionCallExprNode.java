package com.github.eddranca.datagenerator.expression;

import java.util.List;

/**
 * A function call in an expression.
 * The first argument is the main expression (the value to transform).
 * Additional arguments are function-specific parameters (e.g., start/end for substring).
 * <p>
 * Examples:
 * <ul>
 *   <li>{@code lowercase(${this.name})} → functionName="lowercase", argument=ReferenceExprNode, extraArgs=[]</li>
 *   <li>{@code substring(${this.id}, 0, 8)} → functionName="substring", argument=ReferenceExprNode, extraArgs=["0", "8"]</li>
 *   <li>{@code uppercase(trim(${this.name}))} → nested: uppercase wraps trim wraps reference</li>
 * </ul>
 */
public record FunctionCallExprNode(
    String functionName,
    ExpressionNode argument,
    List<String> extraArgs
) implements ExpressionNode {
}
