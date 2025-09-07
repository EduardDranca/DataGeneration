package com.github.eddranca.datagenerator.node;

/**
 * Base interface for all DSL nodes in the abstract syntax tree.
 * Implements the Visitor pattern to allow different operations on the tree.
 */
public interface DslNode {
    /**
     * Accept a visitor and return the result of the visit operation.
     *
     * @param visitor the visitor to accept
     * @param <T>     the return type of the visitor
     * @return the result of the visitor operation
     */
    <T> T accept(DslNodeVisitor<T> visitor);
}
