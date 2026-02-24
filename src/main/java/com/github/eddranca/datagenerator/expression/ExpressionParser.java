package com.github.eddranca.datagenerator.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Parses expression strings from the {@code expr} DSL keyword into an AST.
 * <p>
 * Syntax:
 * <ul>
 *   <li>{@code ${ref}} — reference placeholder (this.field, collection[*].field, $binding.field, etc.)</li>
 *   <li>{@code functionName(...)} — function call wrapping an expression</li>
 *   <li>Everything else — literal text</li>
 *   <li>Function calls can nest: {@code uppercase(trim(${this.name}))}</li>
 *   <li>Extra args after first expression arg: {@code substring(${this.id}, 0, 8)}</li>
 * </ul>
 */
public class ExpressionParser {
    private final Consumer<String> errorHandler;
    private final ExpressionFunctionRegistry functionRegistry;

    public ExpressionParser(Consumer<String> errorHandler, ExpressionFunctionRegistry functionRegistry) {
        this.errorHandler = errorHandler;
        this.functionRegistry = functionRegistry;
    }

    /**
     * Parses an expression string into an AST node.
     *
     * @param expression the raw expression string
     * @return the parsed expression tree, or null if parsing fails
     */
    public ExpressionNode parse(String expression) {
        if (expression == null || expression.isEmpty()) {
            errorHandler.accept("Expression cannot be empty");
            return null;
        }

        try {
            return parseExpression(expression.trim());
        } catch (ExpressionParseException e) {
            errorHandler.accept(e.getMessage());
            return null;
        }
    }

    private ExpressionNode parseExpression(String expr) {
        // Check if the entire expression is a function call: functionName(...)
        int parenIndex = findTopLevelOpenParen(expr);
        if (parenIndex > 0 && expr.charAt(expr.length() - 1) == ')') {
            String funcName = expr.substring(0, parenIndex).trim();
            if (isValidFunctionName(funcName)) {
                return parseFunctionCall(funcName, expr.substring(parenIndex + 1, expr.length() - 1));
            }
        }

        // Otherwise, parse as a template with ${} placeholders and literal text
        return parseTemplate(expr);
    }

    private ExpressionNode parseFunctionCall(String funcName, String argsContent) {
        if (!functionRegistry.has(funcName)) {
            throw new ExpressionParseException("Unknown expression function: '" + funcName + "'");
        }

        // Split arguments: first arg is the main expression, rest are extra string args
        List<String> rawArgs = splitFunctionArgs(argsContent);
        if (rawArgs.isEmpty()) {
            throw new ExpressionParseException("Function '" + funcName + "' requires at least one argument");
        }

        // First argument is parsed as an expression (can contain ${}, nested functions, etc.)
        ExpressionNode mainArg = parseExpression(rawArgs.get(0).trim());

        // Remaining arguments are treated as literal string values
        List<String> extraArgs = new ArrayList<>();
        for (int i = 1; i < rawArgs.size(); i++) {
            extraArgs.add(rawArgs.get(i).trim());
        }

        return new FunctionCallExprNode(funcName, mainArg, extraArgs);
    }

    private ExpressionNode parseTemplate(String template) {
        List<ExpressionNode> parts = new ArrayList<>();
        int i = 0;

        while (i < template.length()) {
            int refStart = template.indexOf("${", i);
            if (refStart == -1) {
                // Rest is literal
                String literal = template.substring(i);
                if (!literal.isEmpty()) {
                    parts.add(new LiteralExprNode(literal));
                }
                break;
            }

            // Add literal before the reference
            if (refStart > i) {
                parts.add(new LiteralExprNode(template.substring(i, refStart)));
            }

            // Find matching closing brace
            int refEnd = findMatchingBrace(template, refStart + 2);
            if (refEnd == -1) {
                throw new ExpressionParseException("Unclosed ${} in expression: " + template);
            }

            String reference = template.substring(refStart + 2, refEnd).trim();
            if (reference.isEmpty()) {
                throw new ExpressionParseException("Empty ${} reference in expression");
            }
            parts.add(new ReferenceExprNode(reference));
            i = refEnd + 1;
        }

        if (parts.isEmpty()) {
            return new LiteralExprNode("");
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        return new ConcatExprNode(parts);
    }

    /**
     * Finds the index of the top-level opening parenthesis, skipping any that are
     * inside ${} placeholders.
     */
    private int findTopLevelOpenParen(String expr) {
        int depth = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '$' && i + 1 < expr.length() && expr.charAt(i + 1) == '{') {
                depth++;
                i++; // skip '{'
            } else if (depth > 0 && c == '}') {
                depth--;
            } else if (depth == 0 && c == '(') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Splits function arguments at top-level commas, respecting nested parentheses and ${}.
     */
    private List<String> splitFunctionArgs(String argsContent) {
        List<String> args = new ArrayList<>();
        int parenDepth = 0;
        int braceDepth = 0;
        int start = 0;

        for (int i = 0; i < argsContent.length(); i++) {
            char c = argsContent.charAt(i);
            if (c == '$' && i + 1 < argsContent.length() && argsContent.charAt(i + 1) == '{') {
                braceDepth++;
                i++;
            } else if (braceDepth > 0 && c == '}') {
                braceDepth--;
            } else if (c == '(') {
                parenDepth++;
            } else if (c == ')') {
                parenDepth--;
            } else if (c == ',' && parenDepth == 0 && braceDepth == 0) {
                args.add(argsContent.substring(start, i));
                start = i + 1;
            }
        }
        // Add last argument
        String last = argsContent.substring(start);
        if (!last.trim().isEmpty()) {
            args.add(last);
        }
        return args;
    }

    private int findMatchingBrace(String str, int fromIndex) {
        int depth = 1;
        for (int i = fromIndex; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isValidFunctionName(String name) {
        if (name.isEmpty()) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') return false;
        }
        return Character.isLetter(name.charAt(0));
    }

    private static class ExpressionParseException extends RuntimeException {
        ExpressionParseException(String message) {
            super(message);
        }
    }
}
