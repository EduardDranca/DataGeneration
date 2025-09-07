package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Field node that spreads fields from a generator into the current object.
 * Supports field mapping (e.g., "mappedName:originalName").
 */
public class SpreadFieldNode implements DslNode {
    private final String generatorName;
    private final JsonNode options;
    private final List<String> fields; // fields to extract, may include mappings

    public SpreadFieldNode(String generatorName, JsonNode options, List<String> fields) {
        this.generatorName = generatorName;
        this.options = options;
        this.fields = new ArrayList<>(fields);
    }

    public String getGeneratorName() {
        return generatorName;
    }

    public JsonNode getOptions() {
        return options;
    }

    public List<String> getFields() {
        return fields;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitSpreadField(this);
    }
}

