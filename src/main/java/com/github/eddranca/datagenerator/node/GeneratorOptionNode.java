package com.github.eddranca.datagenerator.node;

/**
 * Represents a runtime-computed option value that uses a generator.
 * <p>
 * This enables dynamic option values that are generated at runtime, allowing for
 * more flexible and contextually appropriate data generation.
 * <p>
 * Example:
 * <pre>
 * {"gen": "choice", "options": [10, 23, 1, 29]}
 * </pre>
 * <p>
 * The generator will be executed during data generation to produce the actual option value.
 *
 * @see GeneratorOptions
 */
public class GeneratorOptionNode implements DslNode {
    private final GeneratedFieldNode generatorField;
    private final ChoiceFieldNode choiceField;

    public GeneratorOptionNode(GeneratedFieldNode generatorField) {
        this.generatorField = generatorField;
        this.choiceField = null;
    }

    public GeneratorOptionNode(ChoiceFieldNode choiceField) {
        this.choiceField = choiceField;
        this.generatorField = null;
    }

    public GeneratedFieldNode getGeneratorField() {
        return generatorField;
    }

    public ChoiceFieldNode getChoiceField() {
        return choiceField;
    }

    public boolean isChoiceField() {
        return choiceField != null;
    }

    @Override
    public <T> T accept(DslNodeVisitor<T> visitor) {
        return visitor.visitGeneratorOption(this);
    }
}
