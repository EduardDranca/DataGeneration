package com.github.eddranca.datagenerator.node;

/**
 * Visitor interface for traversing and operating on DSL nodes.
 * Implements the Visitor pattern to separate operations from the node structure.
 *
 * @param <T> the return type of visitor operations
 */
public interface DslNodeVisitor<T> {

    T visitRoot(RootNode node);

    T visitCollection(CollectionNode node);

    T visitItem(ItemNode node);

    T visitGeneratedField(GeneratedFieldNode node);


    T visitIndexedReference(IndexedReferenceNode node);

    T visitArrayFieldReference(ArrayFieldReferenceNode node);

    T visitSelfReference(SelfReferenceNode node);

    T visitSimpleReference(SimpleReferenceNode node);

    T visitPickReference(PickReferenceNode node);

    T visitConditionalReference(ConditionalReferenceNode node);

    T visitChoiceField(ChoiceFieldNode node);

    T visitObjectField(ObjectFieldNode node);

    T visitSpreadField(SpreadFieldNode node);

    T visitReferenceSpreadField(ReferenceSpreadFieldNode node);

    T visitLiteralField(LiteralFieldNode node);

    T visitArrayField(ArrayFieldNode node);

    T visitFilter(FilterNode node);
}
