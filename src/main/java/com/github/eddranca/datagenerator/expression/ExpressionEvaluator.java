package com.github.eddranca.datagenerator.expression;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.function.Function;

/**
 * Evaluates an expression AST at runtime, resolving references and applying functions.
 * References are resolved by building temporary reference nodes and visiting them
 * through the existing visitor infrastructure.
 */
public class ExpressionEvaluator {
    private final ExpressionFunctionRegistry functionRegistry;
    private final Function<String, JsonNode> referenceResolver;

    /**
     * @param functionRegistry  the registry of expression functions
     * @param referenceResolver resolves a reference string (e.g., "this.firstName") to a JsonNode value
     */
    public ExpressionEvaluator(ExpressionFunctionRegistry functionRegistry,
                               Function<String, JsonNode> referenceResolver) {
        this.functionRegistry = functionRegistry;
        this.referenceResolver = referenceResolver;
    }

    /**
     * Evaluates an expression tree and returns the result as a string.
     */
    public String evaluate(ExpressionNode node) {
        if (node instanceof LiteralExprNode literal) {
            return literal.value();
        } else if (node instanceof ReferenceExprNode ref) {
            return resolveReference(ref.reference());
        } else if (node instanceof ConcatExprNode concat) {
            return evaluateConcat(concat);
        } else if (node instanceof FunctionCallExprNode funcCall) {
            return evaluateFunction(funcCall);
        }
        throw new IllegalArgumentException("Unknown expression node type: " + node.getClass().getSimpleName());
    }

    /**
     * Evaluates an expression tree and returns the result as a TextNode.
     */
    public JsonNode evaluateToJsonNode(ExpressionNode node) {
        return new TextNode(evaluate(node));
    }

    private String resolveReference(String reference) {
        JsonNode resolved = referenceResolver.apply(reference);
        if (resolved == null || resolved.isNull() || resolved.isMissingNode()) {
            return "";
        }
        return resolved.isTextual() ? resolved.asText() : resolved.toString();
    }

    private String evaluateConcat(ConcatExprNode concat) {
        StringBuilder sb = new StringBuilder();
        for (ExpressionNode part : concat.parts()) {
            sb.append(evaluate(part));
        }
        return sb.toString();
    }

    private String evaluateFunction(FunctionCallExprNode funcCall) {
        ExpressionFunction function = functionRegistry.get(funcCall.functionName());
        if (function == null) {
            throw new IllegalArgumentException("Unknown expression function: '" + funcCall.functionName() + "'");
        }

        String value = evaluate(funcCall.argument());
        return function.apply(value, funcCall.extraArgs());
    }
}
