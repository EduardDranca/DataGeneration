package com.github.eddranca.datagenerator.generator;

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

    public static GeneratorRegistry withDefaultGenerators(Faker faker) {
        GeneratorRegistry registry = new GeneratorRegistry();

        // Register all default generators
        registry.register("uuid", new UuidGenerator(faker));
        registry.register("name", new NameGenerator(faker));
        registry.register("company", new CompanyGenerator(faker));
        registry.register("address", new AddressGenerator(faker));
        registry.register("internet", new InternetGenerator(faker));
        registry.register("country", new CountryGenerator(faker));
        registry.register("book", new BookGenerator(faker));
        registry.register("finance", new FinanceGenerator(faker));
        registry.register("number", new NumberGenerator(faker));
        registry.register("float", new FloatGenerator(faker));
        registry.register("string", new StringGenerator(faker));
        registry.register("sequence", new SequenceGenerator());
        registry.register("csv", new CsvGenerator());
        registry.register("date", new DateGenerator(faker));
        registry.register("boolean", new BooleanGenerator(faker));
        registry.register("lorem", new LoremGenerator(faker));
        registry.register("phone", new PhoneGenerator(faker));
        return registry;
    }

    public void register(String name, Generator generator) {
        generators.put(name, generator);
    }

    public Generator get(String name) {
        return generators.get(name);
    }

    public Set<String> getRegisteredGeneratorNames() {
        return generators.keySet();
    }
}
