package com.github.eddranca.datagenerator.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Parses expression strings from the {@code expr} DSL keyword into an AST.
 * <p>
 * Syntax:
 * <ul>
 *   <li>{@code ${ref}} — reference placeholder (this.field, collection[*].field, etc.)</li>
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
        int parenIndex = Scanner.findTopLevelOpenParen(expr);
        if (parenIndex > 0 && expr.charAt(expr.length() - 1) == ')') {
            String funcName = expr.substring(0, parenIndex).trim();
            if (isValidFunctionName(funcName)) {
                return parseFunctionCall(funcName, expr.substring(parenIndex + 1, expr.length() - 1));
            }
        }
        return parseTemplate(expr);
    }

    private ExpressionNode parseFunctionCall(String funcName, String argsContent) {
        if (!functionRegistry.has(funcName)) {
            throw new ExpressionParseException("Unknown expression function: '" + funcName + "'");
        }
        List<String> rawArgs = Scanner.splitArgs(argsContent);
        if (rawArgs.isEmpty()) {
            throw new ExpressionParseException("Function '" + funcName + "' requires at least one argument");
        }
        ExpressionNode mainArg = parseExpression(rawArgs.get(0).trim());
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
                String literal = template.substring(i);
                if (!literal.isEmpty()) parts.add(new LiteralExprNode(literal));
                break;
            }
            if (refStart > i) {
                parts.add(new LiteralExprNode(template.substring(i, refStart)));
            }
            int refEnd = Scanner.findMatchingBrace(template, refStart + 2);
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
        if (parts.isEmpty()) return new LiteralExprNode("");
        if (parts.size() == 1) return parts.get(0);
        return new ConcatExprNode(parts);
    }

    private boolean isValidFunctionName(String name) {
        if (name.isEmpty() || !Character.isLetter(name.charAt(0))) return false;
        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_') return false;
        }
        return true;
    }

    /**
     * Shared string-scanning utilities that track parenthesis and ${} depth.
     * Centralises the depth-tracking logic used in multiple parse steps.
     */
    private static final class Scanner {

        /** Returns the index of the first '(' not inside a ${} block, or -1. */
        static int findTopLevelOpenParen(String expr) {
            int braceDepth = 0;
            for (int i = 0; i < expr.length(); i++) {
                if (isDollarBrace(expr, i)) { braceDepth++; i++; }
                else if (braceDepth > 0 && expr.charAt(i) == '}') braceDepth--;
                else if (braceDepth == 0 && expr.charAt(i) == '(') return i;
            }
            return -1;
        }

        /** Splits on top-level commas, respecting nested '()' and '${}'.  */
        static List<String> splitArgs(String content) {
            List<String> args = new ArrayList<>();
            int parenDepth = 0, braceDepth = 0, start = 0;
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (isDollarBrace(content, i)) { braceDepth++; i++; }
                else if (braceDepth > 0 && c == '}') braceDepth--;
                else if (c == '(') parenDepth++;
                else if (c == ')') parenDepth--;
                else if (c == ',' && parenDepth == 0 && braceDepth == 0) {
                    args.add(content.substring(start, i));
                    start = i + 1;
                }
            }
            String last = content.substring(start);
            if (!last.trim().isEmpty()) args.add(last);
            return args;
        }

        /** Returns the index of the '}' that closes the '{' at fromIndex, or -1. */
        static int findMatchingBrace(String str, int fromIndex) {
            int depth = 1;
            for (int i = fromIndex; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c == '{') depth++;
                else if (c == '}' && --depth == 0) return i;
            }
            return -1;
        }

        private static boolean isDollarBrace(String s, int i) {
            return s.charAt(i) == '$' && i + 1 < s.length() && s.charAt(i + 1) == '{';
        }
    }

    private static class ExpressionParseException extends RuntimeException {
        ExpressionParseException(String message) { super(message); }
    }
}
