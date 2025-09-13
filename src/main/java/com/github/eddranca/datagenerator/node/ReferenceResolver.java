package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.visitor.GenerationContext;

import java.util.List;

/**
 * Interface for reference nodes that can resolve themselves.
 * This allows each specialized reference node to implement its own resolution logic.
 */
interface ReferenceResolver {
    /**
     * Resolves this reference to a JsonNode value.
     *
     * @param context      the generation context
     * @param currentItem  the current item being generated (for "this" references)
     * @param filterValues values to exclude from the result (null if no filtering)
     * @return the resolved value
     */
    JsonNode resolve(GenerationContext context, JsonNode currentItem, List<JsonNode> filterValues);
}
