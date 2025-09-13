package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.visitor.GenerationContext;

import java.util.List;

import static com.github.eddranca.datagenerator.builder.KeyWords.THIS_PREFIX;

/**
 * Reference node for tag-based references like "byTag[tag]" or
 * "byTag[this.field]".
 * Handles both static tags and dynamic tags that reference fields in the
 * current item.
 */
public class TagReferenceNode extends AbstractReferenceNode {
    private final String tagExpression;
    private final String fieldName; // Optional field to extract from the referenced item
    private final boolean isDynamicTag; // true if tag expression starts with THIS_PREFIX

    public TagReferenceNode(String tagExpression, String fieldName, List<FilterNode> filters, boolean sequential) {
        super(filters, sequential);
        this.tagExpression = tagExpression;
        this.fieldName = fieldName != null ? fieldName : "";
        this.isDynamicTag = tagExpression.startsWith(THIS_PREFIX);
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean hasFieldName() {
        return !fieldName.isEmpty();
    }

    /**
     * Gets the local field name for dynamic tags (removes THIS_PREFIX).
     */
    public String getLocalFieldName() {
        return isDynamicTag ? tagExpression.substring(THIS_PREFIX.length()) : tagExpression;
    }

    @Override
    public String getReferenceString() {
        String base = "byTag[" + tagExpression + "]";
        return hasFieldName() ? base + "." + fieldName : base;
    }

    @Override
    public JsonNode resolve(GenerationContext context, JsonNode currentItem, List<JsonNode> filterValues) {
        // Resolve the tag value
        String tag = isDynamicTag ? resolveTagValue(currentItem) : tagExpression;

        if (tag == null) {
            return context.getMapper().nullNode();
        }

        // Get the tagged collection
        List<JsonNode> collection = context.getTaggedCollection(tag);

        // Apply filtering if needed
        if (filterValues != null && !filterValues.isEmpty()) {
            collection = context.applyFiltering(collection, hasFieldName() ? fieldName : "", filterValues);
            if (collection.isEmpty()) {
                return context.handleFilteringFailure(
                    "Tag reference '" + getReferenceString() + "' has no valid values after filtering");
            }
        }

        if (collection.isEmpty()) {
            return context.getMapper().nullNode();
        }

        // Select an element
        JsonNode selected = context.getElementFromCollection(collection, this, sequential);

        // Extract field if specified
        return hasFieldName() ? selected.path(fieldName) : selected;
    }

    private String resolveTagValue(JsonNode currentItem) {
        if (!isDynamicTag) {
            return tagExpression;
        }

        String localField = getLocalFieldName();
        JsonNode val = currentItem.path(localField);
        return (val == null || val.isNull()) ? null : val.asText();
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitTagReference(this);
    }
}
