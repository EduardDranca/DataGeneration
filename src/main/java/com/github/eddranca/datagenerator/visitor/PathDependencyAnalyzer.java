package com.github.eddranca.datagenerator.visitor;

import com.github.eddranca.datagenerator.node.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes DSL nodes to determine which paths (fields) are referenced by other collections.
 * This enables memory optimization by only generating referenced fields during initial generation.
 * 
 * Supports nested path analysis like "users.address.street" to enable selective generation
 * at any depth in the object hierarchy.
 */
public class PathDependencyAnalyzer implements DslNodeVisitor<Void> {
    private final Map<String, Set<String>> referencedPaths = new HashMap<>();
    
    // Pattern to extract collection and nested field path from reference strings
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("^([^\\[.]+)(?:\\[[^\\]]*\\])?(?:\\.(.+))?$");
    
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
    public Void visitObjectField(ObjectFieldNode node) {
        for (DslNode field : node.getFields().values()) {
            field.accept(this);
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
     * 
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
     * Returns the referenced paths for a specific collection.
     */
    public Set<String> getReferencedPaths(String collection) {
        return referencedPaths.getOrDefault(collection, Set.of());
    }
    
    /**
     * Checks if a specific path is referenced for a collection.
     * Also checks if any parent path is referenced (for nested paths).
     */
    public boolean isPathReferenced(String collection, String path) {
        Set<String> paths = referencedPaths.get(collection);
        if (paths == null) {
            return false;
        }
        
        // Check for exact match or wildcard
        if (paths.contains(path) || paths.contains("*")) {
            return true;
        }
        
        // Check if any referenced path is a parent of this path
        // e.g., if "address" is referenced, then "address.street" should be considered referenced
        for (String referencedPath : paths) {
            if (path.startsWith(referencedPath + ".")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a path or any of its nested paths are referenced.
     * This is useful for determining if we need to generate an object field.
     */
    public boolean isPathOrChildReferenced(String collection, String path) {
        Set<String> paths = referencedPaths.get(collection);
        if (paths == null) {
            return false;
        }
        
        // Check for wildcard
        if (paths.contains("*")) {
            return true;
        }
        
        // Check if this path or any child path is referenced
        for (String referencedPath : paths) {
            if (referencedPath.equals(path) || referencedPath.startsWith(path + ".")) {
                return true;
            }
        }
        
        return false;
    }
}