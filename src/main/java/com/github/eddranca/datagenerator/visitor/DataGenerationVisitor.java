package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import com.github.eddranca.datagenerator.generator.defaults.ChoiceGenerator;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Visitor that generates actual data from the DSL node tree.
 * Maintains generation context and produces JSON output.
 */
public class DataGenerationVisitor<T> implements DslNodeVisitor<JsonNode> {
    private final AbstractGenerationContext<T> context;
    private ObjectNode currentItem; // Track current item for "this" references
    private String currentCollectionName; // Track current collection for lazy generation

    public DataGenerationVisitor(AbstractGenerationContext<T> context) {
        this.context = context;
    }

    @Override
    public JsonNode visitRoot(RootNode node) {
        // Dependency analysis is now handled at the DslDataGenerator level

        ObjectNode result = context.getMapper().createObjectNode();

        for (Map.Entry<String, CollectionNode> entry : node.getCollections().entrySet()) {
            ArrayNode collection = (ArrayNode) entry.getValue().accept(this);
            result.set(entry.getKey(), collection);
        }

        return result;
    }

    @Override
    public JsonNode visitCollection(CollectionNode node) {
        // Set collection context for lazy generation
        String previousCollectionName = this.currentCollectionName;
        this.currentCollectionName = node.getCollectionName();

        try {
            // Let the context handle collection creation and registration
            JsonNode collection = context.createAndRegisterCollection(node, this);

            // Handle picks
            for (Map.Entry<String, Integer> pick : node.getPicks().entrySet()) {
                String alias = pick.getKey();
                int index = pick.getValue();
                context.registerPickFromCollection(alias, index, node.getCollectionName());
            }

            return collection;
        } finally {
            // Restore previous collection context
            this.currentCollectionName = previousCollectionName;
        }
    }

    @Override
    public JsonNode visitItem(ItemNode node) {
        ObjectNode previousItem = this.currentItem;

        try {
            // Standard item generation - lazy generation is now handled at collection level
            ObjectNode item = context.getMapper().createObjectNode();
            this.currentItem = item;
            return visitObjectLikeNode(node.getFields(), item);
        } finally {
            this.currentItem = previousItem; // Restore previous item context
        }
    }


    @Override
    public JsonNode visitGeneratedField(GeneratedFieldNode node) {
        Generator generator = context.getGeneratorRegistry().get(node.getGeneratorName());
        if (generator == null) {
            throw new IllegalArgumentException("Unknown generator: " + node.getGeneratorName());
        }

        if (node.hasFilters()) {
            List<JsonNode> filterValues = computeFilteredValues(node.getFilters());
            return context.generateWithFilter(generator, node.getOptions(), node.getPath(), filterValues);
        }

        if (node.hasPath()) {
            GeneratorContext generatorContext = context.getGeneratorRegistry().createContext(node.getOptions(), context.getMapper());
            return generator.generateAtPath(generatorContext, node.getPath());
        } else {
            GeneratorContext generatorContext = context.getGeneratorRegistry().createContext(node.getOptions(), context.getMapper());
            return generator.generate(generatorContext);
        }
    }


    @Override
    public JsonNode visitIndexedReference(IndexedReferenceNode node) {
        List<JsonNode> filterValues = computeFilteredValues(node.getFilters());
        return node.resolve(context, currentItem, filterValues.isEmpty() ? null : filterValues);
    }

    @Override
    public JsonNode visitArrayFieldReference(ArrayFieldReferenceNode node) {
        List<JsonNode> filterValues = computeFilteredValues(node.getFilters());
        return node.resolve(context, currentItem, filterValues.isEmpty() ? null : filterValues);
    }

    @Override
    public JsonNode visitSelfReference(SelfReferenceNode node) {
        List<JsonNode> filterValues = computeFilteredValues(node.getFilters());
        return node.resolve(context, currentItem, filterValues.isEmpty() ? null : filterValues);
    }

    @Override
    public JsonNode visitSimpleReference(SimpleReferenceNode node) {
        List<JsonNode> filterValues = computeFilteredValues(node.getFilters());
        return node.resolve(context, currentItem, filterValues.isEmpty() ? null : filterValues);
    }

    @Override
    public JsonNode visitPickReference(PickReferenceNode node) {
        List<JsonNode> filterValues = computeFilteredValues(node.getFilters());
        return node.resolve(context, currentItem, filterValues.isEmpty() ? null : filterValues);
    }

    @Override
    public JsonNode visitChoiceField(ChoiceFieldNode node) {
        if (node.getOptions().isEmpty()) {
            return context.getMapper().nullNode();
        }

        // Pre-generate all choice options
        List<JsonNode> generatedOptions = new ArrayList<>();
        for (DslNode option : node.getOptions()) {
            JsonNode generated = option.accept(this);
            generatedOptions.add(generated);
        }

        // Create options structure for ChoiceGenerator
        ObjectNode choiceOptions = context.getMapper().createObjectNode();
        ArrayNode optionsArray = context.getMapper().createArrayNode();
        for (JsonNode generated : generatedOptions) {
            optionsArray.add(generated);
        }
        choiceOptions.set("options", optionsArray);

        // Add weights if present
        if (node.hasWeights()) {
            ArrayNode weightsArray = context.getMapper().createArrayNode();
            for (Double weight : node.getWeights()) {
                weightsArray.add(weight);
            }
            choiceOptions.set("weights", weightsArray);
        }

        // Use ChoiceGenerator to make the selection
        ChoiceGenerator choiceGenerator = new ChoiceGenerator();

        if (node.hasFilters()) {
            List<JsonNode> filterValues = computeFilteredValues(node.getFilters());
            return context.generateWithFilter(choiceGenerator, choiceOptions, null, filterValues);
        }

        GeneratorContext choiceContext = context.getGeneratorRegistry().createContext(choiceOptions, context.getMapper());
        return choiceGenerator.generate(choiceContext);
    }

    private List<JsonNode> computeFilteredValues(List<FilterNode> filters) {
        List<JsonNode> filterValues = new ArrayList<>();
        for (FilterNode filter : filters) {
            JsonNode filterValue = filter.accept(this);
            if (filterValue != null && !filterValue.isNull()) {
                filterValues.add(filterValue);
            }
        }
        return filterValues;
    }

    @Override
    public JsonNode visitObjectField(ObjectFieldNode node) {
        ObjectNode object = context.getMapper().createObjectNode();
        return visitObjectLikeNode(node.getFields(), object);
    }

    private JsonNode visitObjectLikeNode(Map<String, DslNode> fields, ObjectNode newObject) {
        for (Map.Entry<String, DslNode> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            DslNode fieldNode = entry.getValue();

            JsonNode value = fieldNode.accept(this);

            if (fieldNode instanceof SpreadFieldNode || fieldNode instanceof ReferenceSpreadFieldNode) {
                // Spread fields return an object to merge
                if (value != null && value.isObject()) {
                    ObjectNode spreadObj = (ObjectNode) value;
                    spreadObj.fieldNames().forEachRemaining(
                        fn -> newObject.set(fn, spreadObj.get(fn)));
                }
            } else {
                newObject.set(fieldName, value);
            }
        }

        return newObject;
    }

    @Override
    public JsonNode visitSpreadField(SpreadFieldNode node) {
        Generator generator = context.getGeneratorRegistry().get(node.getGeneratorName());
        if (generator == null) {
            throw new IllegalArgumentException("Unknown generator: " + node.getGeneratorName());
        }

        GeneratorContext generatorContext = context.getGeneratorRegistry().createContext(node.getOptions(), context.getMapper());
        JsonNode generated = generator.generate(generatorContext);
        ObjectNode spreadObject = context.getMapper().createObjectNode();
        spreadInto(spreadObject, generated, node.getFields());
        return spreadObject;
    }

    @Override
    public JsonNode visitLiteralField(LiteralFieldNode node) {
        return node.getValue();
    }

    @Override
    public JsonNode visitArrayField(ArrayFieldNode node) {
        ArrayNode array = context.getMapper().createArrayNode();

        int arraySize;
        if (node.hasFixedSize()) {
            arraySize = node.getSize();
        } else {
            // Generate random size between min and max
            int minSize = node.getMinSize();
            int maxSize = node.getMaxSize();
            arraySize = minSize + context.getRandom().nextInt(maxSize - minSize + 1);
        }

        // Generate array items
        for (int i = 0; i < arraySize; i++) {
            JsonNode item = node.getItemNode().accept(this);
            array.add(item);
        }

        return array;
    }

    @Override
    public JsonNode visitReferenceSpreadField(ReferenceSpreadFieldNode node) {
        List<JsonNode> filterValues = computeFilteredValues(node.getFilters());

        // Use the typed reference node directly
        JsonNode referencedItem = node.getReferenceNode().resolve(context, currentItem, filterValues.isEmpty() ? null : filterValues);

        if (referencedItem == null || referencedItem.isNull()) {
            return context.getMapper().createObjectNode();
        }

        ObjectNode spreadObject = context.getMapper().createObjectNode();
        spreadInto(spreadObject, referencedItem, node.getFields());
        return spreadObject;
    }

    @Override
    public JsonNode visitFilter(FilterNode node) {
        return node.getFilterExpression().accept(this);
    }


    private void spreadInto(ObjectNode target, JsonNode source, List<String> fieldSpecs) {
        if (source == null || !source.isObject()) {
            return;
        }

        List<String> specs = new ArrayList<>(fieldSpecs);
        if (specs.isEmpty()) {
            source.fieldNames().forEachRemaining(specs::add);
        }

        for (String spec : specs) {
            String targetField;
            String sourceField;
            if (spec.contains(":")) {
                String[] parts = spec.split(":", 2);
                targetField = parts[0];
                sourceField = parts[1];
            } else {
                targetField = sourceField = spec;
            }

            JsonNode value = source.path(sourceField);
            if (!value.isMissingNode()) {
                target.set(targetField, value);
            }
        }
    }

    // Methods for managing current item context in lazy proxies
    public ObjectNode getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(ObjectNode currentItem) {
        this.currentItem = currentItem;
    }

}
