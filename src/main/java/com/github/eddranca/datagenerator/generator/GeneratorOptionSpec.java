package com.github.eddranca.datagenerator.generator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Describes the option schema for a generator, including which options are required,
 * which are optional, and whether the generator is strict about unknown options.
 * <p>
 * Strict generators reject any options not declared in their spec at DSL parse time.
 * Non-strict generators (the default for custom generators) allow arbitrary options.
 */
public class GeneratorOptionSpec {
    private final Set<String> requiredOptions;
    private final Set<String> optionalOptions;
    private final boolean strict;

    private GeneratorOptionSpec(Set<String> requiredOptions, Set<String> optionalOptions, boolean strict) {
        this.requiredOptions = Collections.unmodifiableSet(new HashSet<>(requiredOptions));
        this.optionalOptions = Collections.unmodifiableSet(new HashSet<>(optionalOptions));
        this.strict = strict;
    }

    /**
     * Creates a non-strict spec that accepts any options. This is the default for custom generators.
     */
    public static GeneratorOptionSpec nonStrict() {
        return new GeneratorOptionSpec(Set.of(), Set.of(), false);
    }

    /**
     * Creates a strict spec with no options allowed.
     */
    public static GeneratorOptionSpec strict() {
        return new GeneratorOptionSpec(Set.of(), Set.of(), true);
    }

    /**
     * Builder for creating option specs with required and optional params.
     */
    public static Builder builder() {
        return new Builder();
    }

    public Set<String> getRequiredOptions() {
        return requiredOptions;
    }

    public Set<String> getOptionalOptions() {
        return optionalOptions;
    }

    public boolean isStrict() {
        return strict;
    }

    /**
     * Returns all known option names (required + optional).
     */
    public Set<String> getAllKnownOptions() {
        Set<String> all = new HashSet<>(requiredOptions);
        all.addAll(optionalOptions);
        return all;
    }

    public static class Builder {
        private final Set<String> required = new HashSet<>();
        private final Set<String> optional = new HashSet<>();
        private boolean strict = true;

        private Builder() {
        }

        public Builder required(String... names) {
            Collections.addAll(required, names);
            return this;
        }

        public Builder optional(String... names) {
            Collections.addAll(optional, names);
            return this;
        }

        public Builder strict(boolean strict) {
            this.strict = strict;
            return this;
        }

        public GeneratorOptionSpec build() {
            return new GeneratorOptionSpec(required, optional, strict);
        }
    }
}
