package com.github.eddranca.datagenerator.node;

import java.util.ArrayList;
import java.util.List;

/**
 * Field node that spreads multiple fields from a referenced item into the current item.
 * Similar to SpreadFieldNode but for references instead of generators.
 * <p>
 * Example: "...userInfo": {"ref": "users[*]", "fields": ["id", "name"]}
 * This would spread the id and name fields from a referenced user into the current item.
 */
public class ReferenceSpreadFieldNode extends ReferenceFieldNode {
    private final List<String> fields;

    public ReferenceSpreadFieldNode(String reference, List<String> fields, List<FilterNode> filters, boolean sequential) {
        super(reference, filters, sequential);
        this.fields = new ArrayList<>(fields);
    }

    public List<String> getFields() {
        return fields;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitReferenceSpreadField(this);
    }
}
