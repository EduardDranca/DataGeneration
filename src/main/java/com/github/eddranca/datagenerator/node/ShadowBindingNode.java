package com.github.eddranca.datagenerator.node;

/**
 * Node representing a shadow binding definition.
 * Shadow bindings are fields prefixed with $ that are resolved during generation
 * but excluded from the output.
 * 
 * Example: "$user": {"ref": "users[*]"}
 * 
 * The binding makes the referenced item available via $user.fieldName
 * in subsequent field definitions, but $user itself won't appear in output.
 */
public class ShadowBindingNode implements DslNode {
    private final String bindingName;
    private final DslNode referenceNode;

    public ShadowBindingNode(String bindingName, DslNode referenceNode) {
        this.bindingName = bindingName;
        this.referenceNode = referenceNode;
    }

    public String getBindingName() {
        return bindingName;
    }

    public DslNode getReferenceNode() {
        return referenceNode;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitShadowBinding(this);
    }
}
