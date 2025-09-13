package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.node.ReferenceFieldNode;
import com.github.eddranca.datagenerator.node.SequentialTrackable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Registry for reference resolvers that handles different types of references.
 */
public class ReferenceResolverRegistry {
    
    private final List<ResolverEntry> resolvers = new ArrayList<>();
    
    /**
     * Registers a resolver for references matching the given predicate.
     */
    public void register(Predicate<String> matcher, ReferenceResolver resolver) {
        resolvers.add(new ResolverEntry(matcher, resolver));
    }
    
    /**
     * Resolves a reference using the first matching resolver.
     */
    public JsonNode resolve(String reference, CollectionContext context, SequentialTrackable node, boolean sequential) {
        for (ResolverEntry entry : resolvers) {
            if (entry.matcher.test(reference)) {
                return entry.resolver.resolve(reference, context, node, sequential);
            }
        }
        throw new com.github.eddranca.datagenerator.exception.InvalidReferenceException(reference);
    }
    
    private static class ResolverEntry {
        final Predicate<String> matcher;
        final ReferenceResolver resolver;
        
        ResolverEntry(Predicate<String> matcher, ReferenceResolver resolver) {
            this.matcher = matcher;
            this.resolver = resolver;
        }
    }
    
    @FunctionalInterface
    public interface ReferenceResolver {
        JsonNode resolve(String reference, CollectionContext context, SequentialTrackable node, boolean sequential);
    }
}