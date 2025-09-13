package com.github.eddranca.datagenerator.node;

import java.util.ArrayList;
import java.util.List;

/**
 * Field node that spreads multiple fields from a referenced item into the current item.
 * Similar to SpreadFieldNode but for references instead of generators.
 * <p>
 * Example: "...userInfo": {"ref": "users[*]", "fields": ["id", "name"]}
 * This would spread the id and name fields from a referenced user into the current item.
 *
 * Now uses typed AbstractReferenceNode for type safety and better performance.
 */
public class ReferenceSpreadFieldNode implements DslNode, SequentialTrackable {
    private final AbstractReferenceNode referenceNode;
    private final List<String> fields;
    private final List<FilterNode> filters;
    private final boolean sequential;

    /**
     * Constructor for typed reference nodes (preferred).
     */
    public ReferenceSpreadFieldNode(AbstractReferenceNode referenceNode, List<String> fields) {
        this.referenceNode = referenceNode;
        this.fields = new ArrayList<>(fields);
        this.filters = referenceNode != null ? referenceNode.getFilters() : new ArrayList<>();
        this.sequential = referenceNode != null && referenceNode.isSequential();
    }

    public AbstractReferenceNode getReferenceNode() {
        return referenceNode;
    }

    public List<String> getFields() {
        return fields;
    }

    public List<FilterNode> getFilters() {
        return filters;
    }

    public boolean isSequential() {
        return sequential;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitReferenceSpreadField(this);
    }
}
