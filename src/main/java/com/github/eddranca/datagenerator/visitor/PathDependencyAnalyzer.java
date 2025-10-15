package com.github.eddranca.datagenerator.visitor;

import com.github.eddranca.datagenerator.node.ArrayFieldNode;
import com.github.eddranca.datagenerator.node.ArrayFieldReferenceNode;
import com.github.eddranca.datagenerator.node.ChoiceFieldNode;
import com.github.eddranca.datagenerator.node.CollectionNode;
import com.github.eddranca.datagenerator.node.Condition;
import com.github.eddranca.datagenerator.node.ConditionalReferenceNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.DslNodeVisitor;
import com.github.eddranca.datagenerator.node.FilterNode;
import com.github.eddranca.datagenerator.node.GeneratedFieldNode;
import com.github.eddranca.datagenerator.node.IndexedReferenceNode;
import com.github.eddranca.datagenerator.node.ItemNode;
import com.github.eddranca.datagenerator.node.LiteralFieldNode;
import com.github.eddranca.datagenerator.node.ObjectFieldNode;
import com.github.eddranca.datagenerator.node.PickReferenceNode;
import com.github.eddranca.datagenerator.node.ReferenceSpreadFieldNode;
import com.github.eddranca.datagenerator.node.RootNode;
import com.github.eddranca.datagenerator.node.SelfReferenceNode;
import com.github.eddranca.datagenerator.node.SimpleReferenceNode;
import com.github.eddranca.datagenerator.node.SpreadFieldNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Analyzes DSL nodes to determine which paths (fields) are referenced by other collections.
 * <p>
 * This enables memory optimization by only generating referenced fields during the initial generation.
 * <p>
 * Supports nested path analysis like "users.address.street" to enable selective
 * generation
 * at any depth in the object hierarchy.
 */
public class PathDependencyAnalyzer implements DslNodeVisitor<Void> {
    // Clean implementation using proper node getters - no regex parsing needed!
    private final Map<String, Set<String>> referencedPaths = new HashMap<>();

    @Override
    public Void visitRoot(RootNode node) {
        for (CollectionNode collection : node.getCollections().values()) {
            collection.accept(this);
        }
        return null;
    }

    @Override
    public Void visitCollection(CollectionNode node) {
        node.getItem().accept(this);
        return null;
    }

    @Override
    public Void visitItem(ItemNode node) {
        for (DslNode field : node.getFields().values()) {
            field.accept(this);
        }
        return null;
    }

    @Override
    public Void visitSimpleReference(SimpleReferenceNode node) {
        String collectionName = node.getCollectionName();
        String fieldName = node.getFieldName();

        if (fieldName != null && !fieldName.isEmpty()) {
            addReferencedPath(collectionName, fieldName);
        } else {
            // Entire object referenced
            addReferencedPath(collectionName, "*");
        }
        return null;
    }

    @Override
    public Void visitArrayFieldReference(ArrayFieldReferenceNode node) {
        addReferencedPath(node.getCollectionName(), node.getFieldName());
        return null;
    }

    @Override
    public Void visitIndexedReference(IndexedReferenceNode node) {
        String collectionName = node.getCollectionName();
        String fieldName = node.getFieldName();

        if (fieldName != null && !fieldName.isEmpty()) {
            addReferencedPath(collectionName, fieldName);
        } else {
            // Entire object referenced
            addReferencedPath(collectionName, "*");
        }
        return null;
    }


    @Override
    public Void visitPickReference(PickReferenceNode node) {
        // Pick references don't reference collection fields directly
        return null;
    }

    @Override
    public Void visitConditionalReference(ConditionalReferenceNode node) {
        String collectionName = node.getCollectionName();
        String fieldName = node.getFieldName();

        // Add all paths referenced by conditions
        for (Condition condition : node.getConditions()) {
            for (String path : condition.getReferencedPaths()) {
                addReferencedPath(collectionName, path);
            }
        }

        // Add the extracted field if specified
        if (fieldName != null && !fieldName.isEmpty()) {
            addReferencedPath(collectionName, fieldName);
        } else {
            // Entire object referenced
            addReferencedPath(collectionName, "*");
        }
        return null;
    }

    @Override
    public Void visitSelfReference(SelfReferenceNode node) {
        // Self references don't reference other collections
        return null;
    }

    @Override
    public Void visitObjectField(ObjectFieldNode node) {
        for (DslNode field : node.getFields().values()) {
            field.accept(this);
        }
        return null;
    }

    @Override
    public Void visitReferenceSpreadField(ReferenceSpreadFieldNode node) {
        // Get collection name from the reference node using proper getters
        String collectionName = getCollectionNameFromReferenceNode(node.getReferenceNode());

        if (collectionName != null) {
            // For spread fields, we need specific fields
            for (String field : node.getFields()) {
                // Handle field mappings like "name:firstName" -> we want "firstName"
                String actualField = field.contains(":") ? field.split(":", 2)[1] : field;
                addReferencedPath(collectionName, actualField);
            }
        }
        return null;
    }

    @Override
    public Void visitGeneratedField(GeneratedFieldNode node) {
        // Generated fields don't reference collections
        return null;
    }

    @Override
    public Void visitChoiceField(ChoiceFieldNode node) {
        for (DslNode option : node.getOptions()) {
            option.accept(this);
        }
        return null;
    }

    @Override
    public Void visitSpreadField(SpreadFieldNode node) {
        // Spread fields don't reference collections
        return null;
    }

    @Override
    public Void visitLiteralField(LiteralFieldNode node) {
        // Literal fields don't reference collections
        return null;
    }

    @Override
    public Void visitArrayField(ArrayFieldNode node) {
        node.getItemNode().accept(this);
        return null;
    }

    @Override
    public Void visitFilter(FilterNode node) {
        node.getFilterExpression().accept(this);
        return null;
    }

    /**
     * Helper method to get collection name from any reference node type.
     * Now uses proper getters instead of string parsing - no more regex needed!
     */
    private String getCollectionNameFromReferenceNode(Object referenceNode) {
        if (referenceNode instanceof SimpleReferenceNode simpleReferenceNode) {
            return simpleReferenceNode.getCollectionName();
        } else if (referenceNode instanceof ArrayFieldReferenceNode arrayFieldReferenceNode) {
            return arrayFieldReferenceNode.getCollectionName();
        } else if (referenceNode instanceof IndexedReferenceNode indexedReferenceNode) {
            return indexedReferenceNode.getCollectionName();
        }
        return null;
    }

    private void addReferencedPath(String collection, String path) {
        referencedPaths.computeIfAbsent(collection, k -> new HashSet<>()).add(path);
    }

    /**
     * Returns the map of referenced paths for each collection.
     * Key: collection name
     * Value: set of referenced field paths ("*" means entire object)
     */
    public Map<String, Set<String>> getReferencedPaths() {
        return new HashMap<>(referencedPaths);
    }

    /**
     * Analyzes a root node to find all referenced paths.
     * This is the main entry point for dependency analysis.
     */
    public Map<String, Set<String>> analyzeRoot(RootNode rootNode) {
        // Clear previous analysis
        referencedPaths.clear();

        // Visit the root node to analyze all references
        rootNode.accept(this);

        return getReferencedPaths();
    }
}
