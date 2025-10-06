package com.github.eddranca.datagenerator.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.defaults.AddressGenerator;
import com.github.eddranca.datagenerator.generator.defaults.BookGenerator;
import com.github.eddranca.datagenerator.generator.defaults.BooleanGenerator;
import com.github.eddranca.datagenerator.generator.defaults.CompanyGenerator;
import com.github.eddranca.datagenerator.generator.defaults.CountryGenerator;
import com.github.eddranca.datagenerator.generator.defaults.CsvGenerator;
import com.github.eddranca.datagenerator.generator.defaults.DateGenerator;
import com.github.eddranca.datagenerator.generator.defaults.FinanceGenerator;
import com.github.eddranca.datagenerator.generator.defaults.FloatGenerator;
import com.github.eddranca.datagenerator.generator.defaults.InternetGenerator;
import com.github.eddranca.datagenerator.generator.defaults.LoremGenerator;
import com.github.eddranca.datagenerator.generator.defaults.NameGenerator;
import com.github.eddranca.datagenerator.generator.defaults.NumberGenerator;
import com.github.eddranca.datagenerator.generator.defaults.PhoneGenerator;
import com.github.eddranca.datagenerator.generator.defaults.SequenceGenerator;
import com.github.eddranca.datagenerator.generator.defaults.StringGenerator;
import com.github.eddranca.datagenerator.generator.defaults.UuidGenerator;
import net.datafaker.Faker;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GeneratorRegistry {
    private final Map<String, Generator> generators = new HashMap<>();
    private final Faker faker;

    private GeneratorRegistry(Faker faker) {
        this.faker = faker;
    }

    public static GeneratorRegistry withDefaultGenerators(Faker faker) {
        GeneratorRegistry registry = new GeneratorRegistry(faker);

        // Register all default generators
        registry.register("uuid", new UuidGenerator());
        registry.register("name", new NameGenerator());
        registry.register("company", new CompanyGenerator());
        registry.register("address", new AddressGenerator());
        registry.register("internet", new InternetGenerator());
        registry.register("country", new CountryGenerator());
        registry.register("book", new BookGenerator());
        registry.register("finance", new FinanceGenerator());
        registry.register("number", new NumberGenerator());
        registry.register("float", new FloatGenerator());
        registry.register("string", new StringGenerator());
        registry.register("sequence", new SequenceGenerator());
        registry.register("csv", new CsvGenerator());
        registry.register("date", new DateGenerator());
        registry.register("boolean", new BooleanGenerator());
        registry.register("lorem", new LoremGenerator());
        registry.register("phone", new PhoneGenerator());
        return registry;
    }

    public void register(String name, Generator generator) {
        generators.put(name, generator);
    }

    /**
     * Creates a GeneratorContext for the given options.
     * This provides generators with access to the shared Faker instance.
     *
     * @param options the generation options
     * @param mapper  the ObjectMapper to use for JSON operations
     * @return a GeneratorContext containing the Faker and options
     */
    public GeneratorContext createContext(JsonNode options, ObjectMapper mapper) {
        return new GeneratorContext(faker, options, mapper);
    }

    public Generator get(String name) {
        return generators.get(name);
    }

    public Set<String> getRegisteredGeneratorNames() {
        return generators.keySet();
    }
}
