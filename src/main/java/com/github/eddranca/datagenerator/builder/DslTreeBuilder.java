package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.eddranca.datagenerator.ValidationError;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.node.CollectionNode;
import com.github.eddranca.datagenerator.node.RootNode;
import com.github.eddranca.datagenerator.validation.DslTreeBuildResult;
import com.github.eddranca.datagenerator.validation.ReferenceValidationVisitor;
import com.github.eddranca.datagenerator.validation.ValidationContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.github.eddranca.datagenerator.builder.KeyWords.NAME;
import static com.github.eddranca.datagenerator.builder.KeyWords.SEED;


/**
 * Builder that parses JSON DSL and creates a validated node tree.
 * Performs validation during building and collects errors.
 * <p>
 * This class coordinates the building process and delegates to specialized builders.
 */
public class DslTreeBuilder {
    private final ValidationContext context;
    private final List<ValidationError> errors;
    private final CollectionNodeBuilder collectionBuilder;

    public DslTreeBuilder(GeneratorRegistry generatorRegistry) {
        this.context = new ValidationContext(generatorRegistry.getRegisteredGeneratorNames());
        this.errors = new ArrayList<>();
        this.collectionBuilder = initCollectionBuilder();
    }

    public DslTreeBuildResult build(JsonNode dslJson) {
        RootNode root = buildRoot(dslJson);

        ReferenceValidationVisitor referenceValidator = new ReferenceValidationVisitor();
        root.accept(referenceValidator);
        errors.addAll(referenceValidator.getErrors());

        return new DslTreeBuildResult(root, errors);
    }

    private RootNode buildRoot(JsonNode dslJson) {
        Long seed = extractSeed(dslJson);
        RootNode root = new RootNode(seed);

        // First pass: declare all collections for reference validation
        declareCollectionsAndTags(dslJson);

        // Second pass: build collection nodes
        buildCollectionNodes(dslJson, root);

        return root;
    }

    private Long extractSeed(JsonNode dslJson) {
        return dslJson.has(SEED) ? dslJson.get(SEED).asLong() : null;
    }

    private void declareCollectionsAndTags(JsonNode dslJson) {
        for (Iterator<Map.Entry<String, JsonNode>> it = dslJson.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            if (!SEED.equals(entry.getKey())) {
                declareCollectionAndTags(entry.getKey(), entry.getValue());
            }
        }
    }

    private void declareCollectionAndTags(String dslKeyName, JsonNode collectionDef) {
        // Declare both the DSL key name and the final collection name
        String finalCollectionName = collectionDef.has(NAME) ?
            collectionDef.get(NAME).asText() : dslKeyName;

        context.declareCollection(dslKeyName);
        if (!dslKeyName.equals(finalCollectionName)) {
            context.declareCollection(finalCollectionName);
        }


    }


    private void buildCollectionNodes(JsonNode dslJson, RootNode root) {
        for (Iterator<Map.Entry<String, JsonNode>> it = dslJson.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            if (!"seed".equals(entry.getKey())) {
                context.setCurrentCollection(entry.getKey());
                CollectionNode collection = collectionBuilder.buildCollection(entry.getKey(), entry.getValue());
                if (collection != null) {
                    root.addCollection(entry.getKey(), collection);
                }
            }
        }
    }

    private CollectionNodeBuilder initCollectionBuilder() {
        NodeBuilderContext builderContext = new NodeBuilderContext(context, errors);
        return new CollectionNodeBuilder(builderContext, new FieldNodeBuilder(builderContext));
    }
}
