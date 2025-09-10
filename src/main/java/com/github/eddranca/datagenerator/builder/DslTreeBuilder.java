package com.github.eddranca.datagenerator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.ValidationError;
import com.github.eddranca.datagenerator.generator.GeneratorRegistry;
import com.github.eddranca.datagenerator.node.ArrayFieldNode;
import com.github.eddranca.datagenerator.node.ChoiceFieldNode;
import com.github.eddranca.datagenerator.node.CollectionNode;
import com.github.eddranca.datagenerator.node.DslNode;
import com.github.eddranca.datagenerator.node.FilterNode;
import com.github.eddranca.datagenerator.node.GeneratedFieldNode;
import com.github.eddranca.datagenerator.node.ItemNode;
import com.github.eddranca.datagenerator.node.LiteralFieldNode;
import com.github.eddranca.datagenerator.node.ObjectFieldNode;
import com.github.eddranca.datagenerator.node.ReferenceFieldNode;
import com.github.eddranca.datagenerator.node.ReferenceSpreadFieldNode;
import com.github.eddranca.datagenerator.node.RootNode;
import com.github.eddranca.datagenerator.node.SpreadFieldNode;
import com.github.eddranca.datagenerator.validation.DslTreeBuildResult;
import com.github.eddranca.datagenerator.validation.ReferenceValidationVisitor;
import com.github.eddranca.datagenerator.validation.ValidationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.github.eddranca.datagenerator.builder.KeyWords.ARRAY;
import static com.github.eddranca.datagenerator.builder.KeyWords.COUNT;
import static com.github.eddranca.datagenerator.builder.KeyWords.ELLIPSIS;
import static com.github.eddranca.datagenerator.builder.KeyWords.FIELDS;
import static com.github.eddranca.datagenerator.builder.KeyWords.FILTER;
import static com.github.eddranca.datagenerator.builder.KeyWords.GENERATOR;
import static com.github.eddranca.datagenerator.builder.KeyWords.ITEM;
import static com.github.eddranca.datagenerator.builder.KeyWords.MAX_SIZE;
import static com.github.eddranca.datagenerator.builder.KeyWords.MIN_SIZE;
import static com.github.eddranca.datagenerator.builder.KeyWords.NAME;
import static com.github.eddranca.datagenerator.builder.KeyWords.OPTIONS;
import static com.github.eddranca.datagenerator.builder.KeyWords.PICK;
import static com.github.eddranca.datagenerator.builder.KeyWords.REFERENCE;
import static com.github.eddranca.datagenerator.builder.KeyWords.SEED;
import static com.github.eddranca.datagenerator.builder.KeyWords.SEQUENTIAL;
import static com.github.eddranca.datagenerator.builder.KeyWords.SIZE;
import static com.github.eddranca.datagenerator.builder.KeyWords.TAGS;

/**
 * Builder that parses JSON DSL and creates a validated node tree.
 * Performs validation during building and collects errors.
 */
public class DslTreeBuilder {
    private final ValidationContext context;
    private final List<ValidationError> errors;

    public DslTreeBuilder(GeneratorRegistry generatorRegistry) {
        this.context = new ValidationContext(generatorRegistry.getRegisteredGeneratorNames());
        this.errors = new ArrayList<>();
    }

    public DslTreeBuildResult build(JsonNode dslJson) {
        RootNode root = buildRoot(dslJson);

        // Run reference validation visitor on the built tree
        ReferenceValidationVisitor referenceValidator = new ReferenceValidationVisitor();
        root.accept(referenceValidator);
        errors.addAll(referenceValidator.getErrors());

        return new DslTreeBuildResult(root, errors);
    }

    private RootNode buildRoot(JsonNode dslJson) {
        Long seed = null;
        if (dslJson.has(SEED)) {
            seed = dslJson.get(SEED).asLong();
        }

        RootNode root = new RootNode(seed);

        // First pass: declare all collections for reference validation
        for (Iterator<Map.Entry<String, JsonNode>> it = dslJson.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            if (!SEED.equals(entry.getKey())) {
                JsonNode collectionDef = entry.getValue();

                // Declare both the DSL key name and the final collection name
                String dslKeyName = entry.getKey();
                String finalCollectionName = collectionDef.has(NAME) ?
                        collectionDef.get(NAME).asText() : dslKeyName;

                context.declareCollection(dslKeyName);
                if (!dslKeyName.equals(finalCollectionName)) {
                    context.declareCollection(finalCollectionName);
                }

                // Also declare tags if present
                if (collectionDef.has(TAGS)) {
                    JsonNode tagsNode = collectionDef.get(TAGS);
                    if (tagsNode.isArray()) {
                        for (JsonNode tagNode : tagsNode) {
                            String tag = tagNode.asText();
                            if (!context.declareTagForCollection(tag, finalCollectionName)) {
                                String existingCollection = context.getTagCollection(tag);
                                addError("Tag '" + tag + "' is already declared by collection '" +
                                        existingCollection + "' and cannot be redeclared by collection '" +
                                        finalCollectionName + "'. Tags can only be shared by collections with the same final name.");
                            }
                        }
                    }
                }
            }
        }

        // Second pass: build collection nodes
        for (Iterator<Map.Entry<String, JsonNode>> it = dslJson.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            if (!"seed".equals(entry.getKey())) {
                context.setCurrentCollection(entry.getKey());
                CollectionNode collection = buildCollection(entry.getKey(), entry.getValue());
                if (collection != null) {
                    root.addCollection(entry.getKey(), collection);
                }
            }
        }

        return root;
    }

    private CollectionNode buildCollection(String name, JsonNode def) {
        if (!validateCollectionStructure(name, def)) {
            return null;
        }

        int count = validateAndGetCount(name, def);
        ItemNode item = buildItem(def.get(ITEM));
        if (item == null) {
            return null;
        }

        List<String> tags = buildCollectionTags(name, def);
        Map<String, Integer> picks = buildCollectionPicks(name, def, count);
        String collectionName = def.has(NAME) ? def.get(NAME).asText() : null;

        return new CollectionNode(name, count, item, tags, picks, collectionName);
    }

    private boolean validateCollectionStructure(String name, JsonNode def) {
        if (!def.isObject()) {
            addError("Collection '" + name + "' must be an object");
            return false;
        }
        if (!def.has(ITEM)) {
            addError("Collection '" + name + "' is missing required 'item' field");
            return false;
        }
        return true;
    }

    private int validateAndGetCount(String name, JsonNode def) {
        int count = def.path(COUNT).asInt(1);
        if (count < 0) {
            addError("Collection '" + name + "' count must be non-negative, got: " + count);
            return 1; // Use default for recovery
        }
        return count;
    }

    private List<String> buildCollectionTags(String name, JsonNode def) {
        List<String> tags = new ArrayList<>();
        if (def.has(TAGS)) {
            JsonNode tagsNode = def.get(TAGS);
            if (tagsNode.isArray()) {
                for (JsonNode tagNode : tagsNode) {
                    tags.add(tagNode.asText());
                }
            } else {
                addError("Collection '" + name + "' tags must be an array");
            }
        }
        return tags;
    }

    private Map<String, Integer> buildCollectionPicks(String name, JsonNode def, int count) {
        Map<String, Integer> picks = new HashMap<>();
        if (def.has(PICK)) {
            JsonNode pickNode = def.get(PICK);
            if (pickNode.isObject()) {
                for (Iterator<Map.Entry<String, JsonNode>> it = pickNode.fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    int index = entry.getValue().asInt();
                    if (index >= count) {
                        addError("Collection '" + name + "' pick alias '" + entry.getKey() +
                                "' index " + index + " is out of bounds (count: " + count + ")");
                    } else {
                        context.declarePick(entry.getKey());
                        picks.put(entry.getKey(), index);
                    }
                }
            } else {
                addError("Collection '" + name + "' pick must be an object");
            }
        }
        return picks;
    }

    private ItemNode buildItem(JsonNode itemDef) {
        if (!itemDef.isObject()) {
            addError("Item definition must be an object");
            return null;
        }

        Map<String, DslNode> fields = new LinkedHashMap<>();

        for (Iterator<Map.Entry<String, JsonNode>> it = itemDef.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            String fieldName = entry.getKey();
            JsonNode fieldDef = entry.getValue();

            DslNode field = buildField(fieldName, fieldDef);
            if (field != null) {
                fields.put(fieldName, field);
            }
        }

        return new ItemNode(fields);
    }

    private DslNode buildField(String fieldName, JsonNode fieldDef) {
        // Check for count field first - this creates arrays using the shorthand syntax
        if (fieldDef.isObject() && fieldDef.has(COUNT)) {
            return buildFieldWithCount(fieldName, fieldDef);
        }

        if (fieldDef.has(GENERATOR)) {
            return buildGeneratorBasedField(fieldName, fieldDef);
        }
        
        if (fieldDef.has(REFERENCE)) {
            return buildReferenceBasedField(fieldName, fieldDef);
        }
        
        if (fieldDef.has(ARRAY)) {
            return buildArrayField(fieldName, fieldDef);
        }
        
        if (fieldDef.isObject()) {
            return buildObjectField(fieldDef);
        }
        
        return new LiteralFieldNode(fieldDef);
    }

    private DslNode buildGeneratorBasedField(String fieldName, JsonNode fieldDef) {
        if (fieldName.startsWith(ELLIPSIS)) {
            return buildSpreadField(fieldName, fieldDef);
        }
        return buildGeneratedField(fieldName, fieldDef);
    }

    private DslNode buildReferenceBasedField(String fieldName, JsonNode fieldDef) {
        if (fieldName.startsWith(ELLIPSIS)) {
            return buildReferenceSpreadField(fieldName, fieldDef);
        }
        return buildReferenceField(fieldName, fieldDef);
    }

    private DslNode buildObjectField(JsonNode fieldDef) {
        Map<String, DslNode> fields = new LinkedHashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = fieldDef.fields(); it.hasNext(); ) {
            var field = it.next();
            DslNode fieldNode = buildField(field.getKey(), field.getValue());
            if (fieldNode != null) {
                fields.put(field.getKey(), fieldNode);
            }
        }
        return new ObjectFieldNode(fields);
    }

    private DslNode buildGeneratedField(String fieldName, JsonNode fieldDef) {
        String generatorSpec = fieldDef.get(GENERATOR).asText();

        if ("choice".equals(generatorSpec)) {
            return buildChoiceField(fieldName, fieldDef);
        }

        // Handle dot notation (e.g., "name.firstName")
        String generatorName;
        String path = null;
        if (generatorSpec.contains(".")) {
            String[] parts = generatorSpec.split("\\.", 2);
            generatorName = parts[0];
            path = parts[1];
        } else {
            generatorName = generatorSpec;
        }

        // Validate generator exists
        if (!context.isGeneratorRegistered(generatorName)) {
            addError("Unknown generator: " + generatorName);
            return null;
        }

        // Build filters if present
        List<FilterNode> filters = new ArrayList<>();
        if (fieldDef.has(FILTER)) {
            JsonNode filtersNode = fieldDef.get(FILTER);
            if (filtersNode.isArray()) {
                for (JsonNode filterNode : filtersNode) {
                    DslNode filterExpression = buildField(fieldName + "[filter]", filterNode);
                    if (filterExpression != null) {
                        filters.add(new FilterNode(filterExpression));
                    }
                }
            } else {
                addError("Generated field '" + fieldName + "' filter must be an array");
            }
        }

        return new GeneratedFieldNode(generatorName, fieldDef, path, filters);
    }

    private DslNode buildChoiceField(String fieldName, JsonNode fieldDef) {
        if (!validateChoiceFieldStructure(fieldName, fieldDef)) {
            return null;
        }

        List<DslNode> options = buildChoiceOptions(fieldName, fieldDef);
        if (options.isEmpty()) {
            addError("Choice field '" + fieldName + "' must have at least one valid option");
            return null;
        }

        List<FilterNode> filters = buildChoiceFilters(fieldName, fieldDef);
        
        if (fieldDef.has("weights")) {
            List<Double> weights = buildChoiceWeights(fieldName, fieldDef, options.size());
            if (weights == null) {
                return null;
            }
            return ChoiceFieldNode.withWeightsAndFilters(options, weights, filters);
        }

        return filters.isEmpty() ? new ChoiceFieldNode(options) : ChoiceFieldNode.withFilters(options, filters);
    }

    private boolean validateChoiceFieldStructure(String fieldName, JsonNode fieldDef) {
        if (!fieldDef.has(OPTIONS)) {
            addError("Choice field '" + fieldName + "' is missing required 'options' array");
            return false;
        }
        if (!fieldDef.get(OPTIONS).isArray()) {
            addError("Choice field '" + fieldName + "' options must be an array");
            return false;
        }
        return true;
    }

    private List<DslNode> buildChoiceOptions(String fieldName, JsonNode fieldDef) {
        List<DslNode> options = new ArrayList<>();
        JsonNode optionsNode = fieldDef.get(OPTIONS);
        for (JsonNode optionNode : optionsNode) {
            DslNode option = buildField(fieldName + "[option]", optionNode);
            if (option != null) {
                options.add(option);
            }
        }
        return options;
    }

    private List<FilterNode> buildChoiceFilters(String fieldName, JsonNode fieldDef) {
        List<FilterNode> filters = new ArrayList<>();
        if (fieldDef.has(FILTER)) {
            JsonNode filtersNode = fieldDef.get(FILTER);
            if (filtersNode.isArray()) {
                for (JsonNode filterNode : filtersNode) {
                    DslNode filterExpression = buildField(fieldName + "[filter]", filterNode);
                    if (filterExpression != null) {
                        filters.add(new FilterNode(filterExpression));
                    }
                }
            } else {
                addError("Choice field '" + fieldName + "' filter must be an array");
            }
        }
        return filters;
    }

    private List<Double> buildChoiceWeights(String fieldName, JsonNode fieldDef, int optionsCount) {
        JsonNode weightsNode = fieldDef.get("weights");
        if (!weightsNode.isArray()) {
            addError("Choice field '" + fieldName + "' weights must be an array");
            return null;
        }

        if (weightsNode.size() != optionsCount) {
            addError("Choice field '" + fieldName + "' weights array must have the same size as options array");
            return null;
        }

        List<Double> weights = new ArrayList<>();
        for (int i = 0; i < weightsNode.size(); i++) {
            JsonNode weightNode = weightsNode.get(i);
            if (!weightNode.isNumber()) {
                addError("Choice field '" + fieldName + "' weight at index " + i + " must be a number");
                return null;
            }

            double weight = weightNode.asDouble();
            if (weight <= 0) {
                addError("Choice field '" + fieldName + "' weight at index " + i + " must be positive");
                return null;
            }

            weights.add(Math.round(weight * 100.0) / 100.0);
        }
        return weights;
    }

    private DslNode buildReferenceField(String fieldName, JsonNode fieldDef) {
        String reference = fieldDef.get(REFERENCE).asText();

        // Basic reference validation
        if (reference.isEmpty()) {
            addError("Reference field '" + fieldName + "' has empty reference");
            return null;
        }

        // Validate reference patterns
        validateReference(fieldName, reference);

        // Parse sequential flag
        boolean sequential = fieldDef.path(SEQUENTIAL).asBoolean(false);

        ReferenceFieldNode refField = new ReferenceFieldNode(reference, sequential);

        // Build filters if present
        if (fieldDef.has(FILTER)) {
            JsonNode filtersNode = fieldDef.get(FILTER);
            if (filtersNode.isArray()) {
                for (JsonNode filterNode : filtersNode) {
                    DslNode filterExpression = buildField(fieldName + "[filter]", filterNode);
                    if (filterExpression != null) {
                        refField.addFilter(new FilterNode(filterExpression));
                    }
                }
            } else {
                addError("Reference field '" + fieldName + "' filter must be an array");
            }
        }

        return refField;
    }

    private void validateReference(String fieldName, String reference) {
        if (reference.startsWith("byTag[")) {
            validateTagReference(fieldName, reference);
        } else if (reference.startsWith("this.")) {
            // Self-references will be validated by the validation visitor after tree is built
        } else {
            validateCollectionReference(fieldName, reference);
        }
    }

    private void validateTagReference(String fieldName, String reference) {
        int start = reference.indexOf('[') + 1;
        int end = reference.indexOf(']');
        if (start >= end) {
            addError("Reference field '" + fieldName + "' has malformed byTag reference: " + reference);
            return;
        }

        String tagExpr = reference.substring(start, end);

        if (!tagExpr.startsWith("this.") && !context.isTagDeclared(tagExpr)) {
            addError("Reference field '" + fieldName + "' references undeclared tag: " + tagExpr);
        }
    }

    private void validateCollectionReference(String fieldName, String reference) {
        String collectionName = extractCollectionName(fieldName, reference);
        if (collectionName == null) {
            return; // Error already reported or it's a valid pick reference
        }

        if (!context.isCollectionDeclared(collectionName)) {
            addError("Reference field '" + fieldName + "' references undeclared collection or pick: " + collectionName);
        }
    }

    private String extractCollectionName(String fieldName, String reference) {
        if (reference.contains("[*].")) {
            return reference.substring(0, reference.indexOf("[*]."));
        }
        
        if (reference.contains("[")) {
            return reference.substring(0, reference.indexOf("["));
        }
        
        if (reference.contains(".")) {
            String baseName = reference.substring(0, reference.indexOf("."));
            if (!context.isPickDeclared(baseName)) {
                addError("Reference field '" + fieldName + "' references field within collection: " + reference + " without index");
                return null;
            }
            return null; // It's a valid pick reference
        }
        
        if (!context.isPickDeclared(reference)) {
            return reference;
        }
        
        return null; // It's a valid pick reference
    }

    private DslNode buildSpreadField(String fieldName, JsonNode fieldDef) {
        String generatorName = fieldDef.get(GENERATOR).asText();

        // Validate generator exists
        if (!context.isGeneratorRegistered(generatorName)) {
            addError("Unknown generator: " + generatorName);
            return null;
        }

        // Fields array is optional - if not provided, all generator fields will be used
        List<String> fields = new ArrayList<>();
        if (fieldDef.has(FIELDS)) {
            JsonNode fieldsNode = fieldDef.get(FIELDS);
            if (!fieldsNode.isArray()) {
                addError("Spread field '" + fieldName + "' fields must be an array");
                return null;
            }

            for (JsonNode fieldNode : fieldsNode) {
                fields.add(fieldNode.asText());
            }

            if (fields.isEmpty()) {
                addError("Spread field '" + fieldName + "' must have at least one field when fields array is provided");
                return null;
            }
        }
        // If fields is empty, it means use all available fields from the generator

        return new SpreadFieldNode(generatorName, fieldDef, fields);
    }

    private DslNode buildReferenceSpreadField(String fieldName, JsonNode fieldDef) {
        String reference = fieldDef.get(REFERENCE).asText();

        // Basic reference validation
        if (reference.isEmpty()) {
            addError("Reference spread field '" + fieldName + "' has empty reference");
            return null;
        }

        // Validate reference patterns
        validateReference(fieldName, reference);

        // Parse sequential flag
        boolean sequential = fieldDef.path(SEQUENTIAL).asBoolean(false);

        // Fields array is optional - if not provided, all fields from referenced item will be used
        List<String> fields = new ArrayList<>();
        if (fieldDef.has(FIELDS)) {
            JsonNode fieldsNode = fieldDef.get(FIELDS);
            if (!fieldsNode.isArray()) {
                addError("Reference spread field '" + fieldName + "' fields must be an array");
                return null;
            }

            for (JsonNode fieldNode : fieldsNode) {
                fields.add(fieldNode.asText());
            }

            if (fields.isEmpty()) {
                addError("Reference spread field '" + fieldName + "' must have at least one field when fields array is provided");
                return null;
            }
        }
        // If fields is empty, it means use all available fields from the referenced item

        // Build filters if present
        List<FilterNode> filters = new ArrayList<>();
        if (fieldDef.has(FILTER)) {
            JsonNode filtersNode = fieldDef.get(FILTER);
            if (filtersNode.isArray()) {
                for (JsonNode filterNode : filtersNode) {
                    DslNode filterExpression = buildField(FILTER, filterNode);
                    if (filterExpression != null) {
                        filters.add(new FilterNode(filterExpression));
                    }
                }
            } else {
                addError("Reference spread field '" + fieldName + "' filter must be an array");
            }
        }


        return new ReferenceSpreadFieldNode(reference, fields, filters, sequential);
    }

    private DslNode buildArrayField(String fieldName, JsonNode fieldDef) {
        JsonNode arrayDef = fieldDef.get(ARRAY);

        if (!arrayDef.isObject()) {
            addError("Array field '" + fieldName + "' array definition must be an object");
            return null;
        }

        if (!arrayDef.has(ITEM)) {
            addError("Array field '" + fieldName + "' must have an 'item' definition");
            return null;
        }

        // Parse size configuration
        boolean hasSize = arrayDef.has(SIZE);
        boolean hasMinMax = arrayDef.has(MIN_SIZE) || arrayDef.has(MAX_SIZE);

        if (hasSize && hasMinMax) {
            addError("Array field '" + fieldName + "' cannot have both 'size' and 'minSize/maxSize'");
            return null;
        }

        if (!hasSize && !hasMinMax) {
            addError("Array field '" + fieldName + "' must have either 'size' or 'minSize/maxSize'");
            return null;
        }

        // Build the item node
        DslNode itemNode = buildField(fieldName + "[item]", arrayDef.get(ITEM));
        if (itemNode == null) {
            return null;
        }

        if (hasSize) {
            int size = arrayDef.get(SIZE).asInt();
            if (size < 0) {
                addError("Array field '" + fieldName + "' size must be non-negative");
                return null;
            }
            return new ArrayFieldNode(size, itemNode);
        } else {
            int minSize = arrayDef.path(MIN_SIZE).asInt(0);
            int maxSize = arrayDef.path(MAX_SIZE).asInt(10);

            if (minSize < 0) {
                addError("Array field '" + fieldName + "' minSize must be non-negative");
                return null;
            }

            if (maxSize < minSize) {
                addError("Array field '" + fieldName + "' maxSize must be >= minSize");
                return null;
            }

            return new ArrayFieldNode(minSize, maxSize, itemNode);
        }
    }

    private ArrayFieldNode buildFieldWithCount(String fieldName, JsonNode fieldDef) {
        int count = validateCountValue(fieldName, fieldDef);
        if (count < 0) {
            return null;
        }

        DslNode itemNode = buildItemNodeFromCountField(fieldName, fieldDef);
        if (itemNode == null) {
            return null;
        }

        return new ArrayFieldNode(count, itemNode);
    }

    private int validateCountValue(String fieldName, JsonNode fieldDef) {
        JsonNode countNode = fieldDef.get(COUNT);
        if (!countNode.isNumber()) {
            addError("Field '" + fieldName + "' count must be a number");
            return -1;
        }

        int count = countNode.asInt();
        if (count < 0) {
            addError("Field '" + fieldName + "' count must be non-negative");
            return -1;
        }
        return count;
    }

    private DslNode buildItemNodeFromCountField(String fieldName, JsonNode fieldDef) {
        ObjectNode itemDef = fieldDef.deepCopy();
        itemDef.remove(COUNT);

        if (itemDef.isEmpty()) {
            addError("Field '" + fieldName + "' with count must have additional field definition");
            return null;
        }
        
        if (itemDef.size() == 1 && itemDef.has("value")) {
            return new LiteralFieldNode(itemDef.get("value"));
        }
        
        return buildField("item", itemDef);
    }

    private void addError(String message) {
        String path = this.context.getCurrentCollection() != null ?
                this.context.getCurrentCollection() : "root";
        errors.add(new ValidationError(path, message));
    }
}
