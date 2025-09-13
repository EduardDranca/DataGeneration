package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.node.DslNode;

/**
 * Interface for building fields from JSON definitions.
 * This breaks circular dependencies between specialized builders.
 */
interface FieldBuilder {
    /**
     * Builds a field node from a JSON definition.
     */
    DslNode buildField(String fieldName, JsonNode fieldDef);
}
