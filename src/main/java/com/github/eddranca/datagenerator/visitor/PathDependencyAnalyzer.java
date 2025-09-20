package com.github.eddranca.datagenerator.visitor;

import com.github.eddranca.datagenerator.node.ArrayFieldNode;
import com.github.eddranca.datagenerator.node.ArrayFieldReferenceNode;
import com.github.eddranca.datagenerator.node.ChoiceFieldNode;
import com.github.eddranca.datagenerator.node.CollectionNode;
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
import com.github.eddranca.datagenerator.node.TagReferenceNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes DSL nodes to determine which paths (fields) are referenced by other
 * <p>
 * <p>
 * <p>
 * <p>
 * <p>
 * <p>
 * <p>
 * <p>
 * <p>
 * collections.
 * <p>
 * <p>
 * This enables memory optimization by only generating referenced fields during
 * initial generation.
 * <p>
 * <p>
 * Supports nested path analysis like "users.address.street" to enable selective
 * generation
 * at any depth in the object hierarchy.
 */
public class PathDependencyAnalyzer implements DslNodeVisitor<Void> {
    // Pattern to extract collection and nested field path from reference strings
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("^([^\\[.]+)(?:\\[[^\\]]*\\])?(?:\\.(.+))?$");
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
        analyzeReference(node.getReferenceString());
        return null;
    }

    @Override
    public Void visitArrayFieldReference(ArrayFieldReferenceNode node) {
        analyzeReference(node.getReferenceString());
        return null;
    }

    @Override
    public Void visitIndexedReference(IndexedReferenceNode node) {
        // IndexedReference doesn't have getReferenceString, skip for now
        return null;
    }

    @Override
    public Void visitTagReference(TagReferenceNode node) {
        analyzeReference(node.getReferenceString());
        return null;
    }

    @Override
    public Void visitPickReference(PickReferenceNode node) {
        // Pick references don't reference collection fields directly
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
        // Analyze the base reference
        String baseRef = node.getReferenceNode().getReferenceString();
        String collection = extractCollection(baseRef);

        if (collection != null) {
            // For spread fields, we need specific fields
            for (String field : node.getFields()) {
                // Handle field mappings like "name:firstName" -> we want "firstName"
                String actualField = field.contains(":") ? field.split(":", 2)[1] : field;
                addReferencedPath(collection, actualField);
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
     * Analyzes a reference string and extracts collection and field information.
     * Supports nested paths for deep field access.
     * <p>
     * Examples:
     * - "users" -> collection="users", path=null (entire object)
     * - "users.name" -> collection="users", path="name"
     * - "users.address.street" -> collection="users", path="address.street"
     * - "users[*].profile.bio" -> collection="users", path="profile.bio"
     * - "byTag[premium].name" -> collection="byTag[premium]", path="name"
     */
    private void analyzeReference(String referenceString) {
        if (referenceString == null || referenceString.isEmpty()) {
            return;
        }

        Matcher matcher = REFERENCE_PATTERN.matcher(referenceString);
        if (matcher.matches()) {
            String collection = matcher.group(1);
            String path = matcher.group(2);

            if (path != null && !path.isEmpty()) {
                addReferencedPath(collection, path);
            } else {
                // Entire object referenced
                addReferencedPath(collection, "*");
            }
        }
    }

    /**
     * Extracts just the collection name from a reference string.
     */
    private String extractCollection(String referenceString) {
        if (referenceString == null || referenceString.isEmpty()) {
            return null;
        }

        Matcher matcher = REFERENCE_PATTERN.matcher(referenceString);
        return matcher.matches() ? matcher.group(1) : null;
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
