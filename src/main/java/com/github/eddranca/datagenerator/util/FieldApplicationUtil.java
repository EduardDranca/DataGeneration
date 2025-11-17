package com.github.eddranca.datagenerator.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.ReferenceSpreadFieldNode;
import com.github.eddranca.datagenerator.node.SpreadFieldNode;

/**
 * Utility class for applying field values to objects, handling special cases like spread fields.
 * This eliminates code duplication between DataGenerationVisitor and AbstractLazyProxy.
 */
public class FieldApplicationUtil {

    /**
     * Applies a field value to an object, handling spread fields appropriately.
     * Spread fields merge their object properties into the target object instead of
     * setting a single field value.
     *
     * @param target the object node to apply the field to
     * @param fieldName the name of the field being applied
     * @param fieldNode the DSL node representing the field
     * @param value the generated value for the field
     */
    public static void applyFieldToObject(ObjectNode target, String fieldName, DslNode fieldNode, JsonNode value) {
        if (fieldNode instanceof SpreadFieldNode || fieldNode instanceof ReferenceSpreadFieldNode) {
            // Spread fields return an object to merge
            if (value != null && value.isObject()) {
                ObjectNode spreadObj = (ObjectNode) value;
                spreadObj.fieldNames().forEachRemaining(
                    fn -> target.set(fn, spreadObj.get(fn)));
            }
        } else {
            target.set(fieldName, value);
        }
    }

    private FieldApplicationUtil() {
        // Utility class - prevent instantiation
    }
}
