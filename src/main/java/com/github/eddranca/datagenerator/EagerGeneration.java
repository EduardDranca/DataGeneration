package com.github.eddranca.datagenerator;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public class EagerGeneration extends AbstractGeneration<JsonNode> {

    EagerGeneration(Map<String, List<JsonNode>> collectionsMap) {
        super(collectionsMap);
    }

    @Override
    protected JsonNode toJsonNode(JsonNode item) {
        // For eager generation, items are already JsonNodes
        return item;
    }


}
