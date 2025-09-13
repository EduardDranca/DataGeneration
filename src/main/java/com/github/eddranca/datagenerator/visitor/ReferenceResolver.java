package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.node.ReferenceFieldNode;

/**
 * Functional interface for resolving references of different types.
 */
@FunctionalInterface
public interface ReferenceResolver {
    /**
     * Resolves a reference based on the context.
     *
     * @param reference    the reference string to resolve
     * @param context      context containing the current item and any filtered collections
     * @param node         the reference field node in the DSL
     * @param sequential   whether to use sequential access instead of random
     * @return the resolved JSON node
     */
    JsonNode resolve(String reference, CollectionContext context, ReferenceFieldNode node, boolean sequential);
}
