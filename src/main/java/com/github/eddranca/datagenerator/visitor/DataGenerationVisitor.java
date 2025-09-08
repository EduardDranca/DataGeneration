package com.github.eddranca.datagenerator.visitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.defaults.ChoiceGenerator;
import com.github.eddranca.datagenerator.node.ArrayFieldNode;
import com.github.eddranca.datagenerator.node.ChoiceFieldNode;
import com.github.eddranca.datagenerator.node.CollectionNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.DslNodeVisitor;
import com.github.eddranca.datagenerator.node.FilterNode;
import com.github.eddranca.datagenerator.node.GeneratedFieldNode;
import com.github.eddranca.datagenerator.node.ItemNode;
import com.github.eddranca.datagenerator.node.LiteralFieldNode;
import com.github.eddranca.datagenerator.node.ObjectFieldNode;
import com.github.eddranca.datagenerator.node.ReferenceFieldNode;
import com.github.eddranca.datagenerator.node.ReferenceSpreadFieldNode;
import com.github.eddranca.datagenerator.node.RootNode;
import com.github.eddranca.datagenerator.node.SpreadFieldNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Visitor that generates actual data from the DSL node tree.
 * Maintains generation context and produces JSON output.
 */
public class DataGenerationVisitor implements DslNodeVisitor<JsonNode> {
    private final GenerationContext context;
    private ObjectNode currentItem; // Track current item for "this" references

    public DataGenerationVisitor(GenerationContext context) {
        this.context = context;
    }

    @Override
    public JsonNode visitRoot(RootNode node) {
        context.clearFilteredCollectionCache();

        ObjectNode result = context.getMapper().createObjectNode();

        for (Map.Entry<String, CollectionNode> entry : node.getCollections().entrySet()) {
            ArrayNode collection = (ArrayNode) entry.getValue().accept(this);
            result.set(entry.getKey(), collection);
        }

        return result;
    }

    @Override
    public JsonNode visitCollection(CollectionNode node) {
        List<JsonNode> items = new ArrayList<>();

        for (int i = 0; i < node.getCount(); i++) {
            JsonNode item = node.getItem().accept(this);
            items.add(item);
        }

        context.registerCollection(node.getCollectionName(), items);

        if (!node.getName().equals(node.getCollectionName())) {
            context.registerReferenceCollection(node.getName(), items);
        }

        for (String tag : node.getTags()) {
            context.registerTaggedCollection(tag, items);
        }

        for (Map.Entry<String, Integer> pick : node.getPicks().entrySet()) {
            String alias = pick.getKey();
            int index = pick.getValue();
            if (index < items.size()) {
                context.registerPick(alias, items.get(index));
            }
        }

        context.clearFilteredCollectionCache();

        return context.getMapper().valueToTree(items);
    }

    @Override
    public JsonNode visitItem(ItemNode node) {
        ObjectNode item = context.getMapper().createObjectNode();
        ObjectNode previousItem = this.currentItem;
        this.currentItem = item;
        try {
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
            Map<String, Supplier<JsonNode>> fieldSuppliers = generator.getFieldSuppliers(node.getOptions());
            if (fieldSuppliers != null && fieldSuppliers.containsKey(node.getPath())) {
                return fieldSuppliers.get(node.getPath()).get();
            } else {
                return generator.generateAtPath(node.getOptions(), node.getPath());
            }
        } else {
            return generator.generate(node.getOptions());
        }
    }

    @Override
    public JsonNode visitReferenceField(ReferenceFieldNode node) {
        List<JsonNode> filterValues = computeFilteredValues(node.getFilters());

        return context.resolveReferenceWithFiltering(
                node.getReference(),
                currentItem,
                filterValues.isEmpty() ? null : filterValues,
                node,
                node.isSequential()
        );
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
        ChoiceGenerator choiceGenerator = new ChoiceGenerator(context.getRandom());

        if (node.hasFilters()) {
            List<JsonNode> filterValues = computeFilteredValues(node.getFilters());
            return context.generateWithFilter(choiceGenerator, choiceOptions, null, filterValues);
        }

        return choiceGenerator.generate(choiceOptions);
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
                            fn -> newObject.set(fn, spreadObj.get(fn))
                    );
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

        JsonNode generated = generator.generate(node.getOptions());
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

        JsonNode referencedItem = context.resolveReferenceWithFiltering(
                node.getReference(),
                currentItem,
                filterValues.isEmpty() ? null : filterValues,
                node,
                node.isSequential()
        );

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

    // ------------------------
    // Private helpers
    // ------------------------

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
}
