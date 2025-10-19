package com.github.eddranca.datagenerator.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.visitor.AbstractGenerationContext;

import java.util.List;
import java.util.Optional;

/**
 * Reference node for pick references.
 * Picks are named values that can be referenced directly or have fields extracted from them.
 */
public class PickReferenceNode extends AbstractReferenceNode {
    private final String pickName;
    private final String fieldName; // Optional field to extract from the pick

    public PickReferenceNode(String pickName, String fieldName,
                             List<FilterNode> filters, boolean sequential) {
        super(filters, sequential);
        this.pickName = pickName;
        this.fieldName = fieldName != null ? fieldName : "";
    }

    public boolean hasFieldName() {
        return !fieldName.isEmpty();
    }

    @Override
    public Optional<String> getCollectionName() {
        return Optional.empty(); // Pick references don't reference collections
    }

    @Override
    public String getReferenceString() {
        return hasFieldName() ? pickName + "." + fieldName : pickName;
    }

    @Override
    public JsonNode resolve(AbstractGenerationContext<?> context, JsonNode currentItem, List<JsonNode> filterValues) {
        JsonNode pick = context.getNamedPick(pickName);
        if (pick == null) {
            return context.getMapper().nullNode();
        }

        JsonNode value = hasFieldName() ? extractNestedField(pick, fieldName) : pick;

        // Check filtering if needed
        if (filterValues != null && !filterValues.isEmpty() && filterValues.contains(value)) {
            return context.handleFilteringFailure("Pick reference '" + getReferenceString() + "' value matches filter");
        }

        return value.isMissingNode() ? context.getMapper().nullNode() : value;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitPickReference(this);
    }
}
