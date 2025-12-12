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
import com.github.eddranca.datagenerator.node.Condition;
import com.github.eddranca.datagenerator.node.ConditionalReferenceNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.DslNodeVisitor;
import com.github.eddranca.datagenerator.node.FilterNode;
import com.github.eddranca.datagenerator.node.GeneratedFieldNode;
import com.github.eddranca.datagenerator.node.GeneratorOptionNode;
import com.github.eddranca.datagenerator.node.GeneratorOptions;
import com.github.eddranca.datagenerator.node.IndexedReferenceNode;
import com.github.eddranca.datagenerator.node.ItemNode;
import com.github.eddranca.datagenerator.node.LiteralFieldNode;
import com.github.eddranca.datagenerator.node.ObjectFieldNode;
import com.github.eddranca.datagenerator.node.OptionReferenceNode;
import com.github.eddranca.datagenerator.node.PickReferenceNode;
import com.github.eddranca.datagenerator.node.ReferenceSpreadFieldNode;
import com.github.eddranca.datagenerator.node.RootNode;
import com.github.eddranca.datagenerator.node.SelfReferenceNode;
import com.github.eddranca.datagenerator.node.ShadowBindingFieldNode;
import com.github.eddranca.datagenerator.node.ShadowBindingNode;
import com.github.eddranca.datagenerator.node.SimpleReferenceNode;
import com.github.eddranca.datagenerator.node.SpreadFieldNode;
import com.github.eddranca.datagenerator.util.FieldApplicationUtil;

import java.util.ArrayList;
import java.util.HashMap;
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
    private Map<String, JsonNode> shadowBindings = new HashMap<>(); // Track shadow bindings for current item

    public DataGenerationVisitor(AbstractGenerationContext<T> context) {
        this.context = context;
    }

    /**
     * Gets the current shadow bindings map.
     * Used by condition evaluation to resolve $binding.field references.
     */
    public Map<String, JsonNode> getShadowBindings() {
        return shadowBindings;
    }

    public ObjectNode getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(ObjectNode currentItem) {
        this.currentItem = currentItem;
    }

    @Override
    public JsonNode visitRoot(RootNode node) {
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
        Map<String, JsonNode> previousShadowBindings = this.shadowBindings;

        try {
            // Standard item generation - lazy generation is now handled at collection level
            ObjectNode item = context.getMapper().createObjectNode();
            this.currentItem = item;
            this.shadowBindings = new HashMap<>(); // Fresh shadow bindings for each item
            return visitObjectLikeNode(node.getFields(), item);
        } finally {
            this.currentItem = previousItem; // Restore previous item context
            this.shadowBindings = previousShadowBindings; // Restore previous shadow bindings
        }
    }


    @Override
    public JsonNode visitGeneratedField(GeneratedFieldNode node) {
        Generator generator = context.getGeneratorRegistry().get(node.getGeneratorName());
        if (generator == null) {
            throw new IllegalArgumentException("Unknown generator: " + node.getGeneratorName());
        }

        // Resolve runtime options if present
        JsonNode resolvedOptions = resolveGeneratorOptions(node.getOptions());

        if (node.hasFilters()) {
            List<JsonNode> filterValues = computeFilteredValues(node.getFilters());
            return context.generateWithFilter(generator, resolvedOptions, node.getPath(), filterValues);
        }

        if (node.hasPath()) {
            GeneratorContext generatorContext = context.getGeneratorRegistry().createContext(resolvedOptions, context.getMapper());
            return generator.generateAtPath(generatorContext, node.getPath());
        } else {
            GeneratorContext generatorContext = context.getGeneratorRegistry().createContext(resolvedOptions, context.getMapper());
            return generator.generate(generatorContext);
        }
    }

    @Override
    public JsonNode visitGeneratorOption(GeneratorOptionNode node) {
        // Generate the value using the embedded generator or choice field
        if (node.isChoiceField()) {
            return node.getChoiceField().accept(this);
        } else {
            return node.getGeneratorField().accept(this);
        }
    }

    /**
     * Resolves generator options, replacing runtime references with actual values.
     * <p>
     * This method handles both simple references ({"ref": "this.field"}) and
     * mapped references ({"ref": "this.field", "map": {...}}).
     *
     * @param options the generator options that may contain runtime references
     * @return resolved options with all references replaced by actual values
     * @throws IllegalArgumentException if a mapped value is not found
     */
    private JsonNode resolveGeneratorOptions(GeneratorOptions options) {
        if (!options.hasRuntimeOptions()) {
            return options.getStaticOptions();
        }

        ObjectNode resolved = options.getStaticOptions().deepCopy();
        resolveReferenceBasedOptions(options, resolved);
        resolveGeneratorBasedOptions(options, resolved);
        return resolved;
    }

    private void resolveReferenceBasedOptions(GeneratorOptions options, ObjectNode resolved) {
        for (Map.Entry<String, OptionReferenceNode> entry : options.getRuntimeOptions().entrySet()) {
            String optionKey = entry.getKey();
            OptionReferenceNode optionRef = entry.getValue();

            JsonNode referencedValue = optionRef.getReference().resolve(context, currentItem, null);

            if (referencedValue == null || referencedValue.isNull()) {
                continue;
            }

            if (optionRef.hasMapping()) {
                applyValueMapping(optionKey, optionRef, referencedValue, resolved);
            } else {
                resolved.set(optionKey, referencedValue);
            }
        }
    }

    private void applyValueMapping(String optionKey, OptionReferenceNode optionRef, JsonNode referencedValue, ObjectNode resolved) {
        JsonNode mappedValue = optionRef.getValueMap().get(referencedValue.asText());
        if (mappedValue != null) {
            resolved.set(optionKey, mappedValue);
        } else {
            throw new IllegalArgumentException(
                "No mapping found for value '" + referencedValue.asText() +
                "' in option '" + optionKey + "'"
            );
        }
    }

    private void resolveGeneratorBasedOptions(GeneratorOptions options, ObjectNode resolved) {
        for (Map.Entry<String, GeneratorOptionNode> entry : options.getGeneratorOptions().entrySet()) {
            String optionKey = entry.getKey();
            GeneratorOptionNode generatorOption = entry.getValue();

            JsonNode generatedValue = generatorOption.accept(this);
            if (generatedValue != null && !generatedValue.isNull()) {
                resolved.set(optionKey, generatedValue);
            }
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
    public JsonNode visitConditionalReference(ConditionalReferenceNode node) {
        List<JsonNode> filterValues = computeFilteredValues(node.getFilters());

        // Resolve shadow bindings in the condition if present
        if (node.getCondition().hasShadowBindingReferences()) {
            Condition resolvedCondition = node.getCondition().resolveShadowBindings(shadowBindings);
            return resolveConditionalReference(node, resolvedCondition, filterValues);
        }

        return node.resolve(context, currentItem, filterValues.isEmpty() ? null : filterValues);
    }

    private JsonNode resolveConditionalReference(ConditionalReferenceNode node, Condition resolvedCondition, List<JsonNode> filterValues) {
        // Use the context's filtered collection with the resolved condition
        List<JsonNode> filteredCollection = context.getFilteredCollection(
            node.getCollectionNameString(),
            resolvedCondition,
            filterValues.isEmpty() ? null : filterValues,
            node.hasFieldName() ? node.getFieldName() : ""
        );

        if (filteredCollection.isEmpty()) {
            if (!filterValues.isEmpty()) {
                return context.handleFilteringFailure("Conditional reference '" + node.getReferenceString() + "' has no valid values after filtering");
            } else {
                return context.handleFilteringFailure("Conditional reference '" + node.getReferenceString() + "' matched no items");
            }
        }

        // Select an element
        JsonNode selected = context.getElementFromCollection(filteredCollection, node, node.isSequential());

        // Extract field if specified
        return node.hasFieldName() ? extractNestedField(selected, node.getFieldName()) : selected;
    }

    private JsonNode extractNestedField(JsonNode item, String fieldPath) {
        if (item == null || item.isNull()) {
            return context.getMapper().nullNode();
        }

        String[] parts = fieldPath.split("\\.");
        JsonNode current = item;
        for (String part : parts) {
            if (current == null || current.isNull() || current.isMissingNode()) {
                return context.getMapper().nullNode();
            }
            current = current.get(part);
        }
        return current != null ? current : context.getMapper().nullNode();
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

            // Skip shadow binding fields from output (they start with $)
            if (fieldName.startsWith("$")) {
                continue;
            }

            FieldApplicationUtil.applyFieldToObject(newObject, fieldName, fieldNode, value);
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

    @Override
    public JsonNode visitShadowBinding(ShadowBindingNode node) {
        // Resolve the reference to get the bound value
        JsonNode boundValue = node.getReferenceNode().accept(this);

        // Store in shadow bindings map for later use in conditions
        shadowBindings.put(node.getBindingName(), boundValue);

        // Return the bound value (but it won't be added to output due to $ prefix handling)
        return boundValue;
    }

    @Override
    public JsonNode visitShadowBindingField(ShadowBindingFieldNode node) {
        String bindingName = node.getBindingName();
        JsonNode boundValue = shadowBindings.get(bindingName);

        if (boundValue == null) {
            throw new IllegalArgumentException(
                "Shadow binding '" + bindingName + "' not found. " +
                "Make sure it's defined before use in the item."
            );
        }

        // Extract the field from the bound value
        return extractNestedField(boundValue, node.getFieldPath());
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

}
