package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.exception.DataGenerationException;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import net.datafaker.service.RandomService;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class CsvGenerator implements Generator {
    // IdentityHashMap for JsonNode keys - object identity matters for per-field counters
    private final Map<JsonNode, Integer> sequentialCounters = new IdentityHashMap<>();
    // Regular HashMap for String keys - file paths should use value equality
    private final Map<String, List<String[]>> csvCache = new HashMap<>();

    @Override
    public JsonNode generate(GeneratorContext context) {
        JsonNode options = context.options();
        String file = context.getStringOption("file");
        boolean sequential = context.getBooleanOption("sequential", true);
        ObjectMapper mapper = context.mapper();

        List<String[]> records = csvCache.computeIfAbsent(file, f -> {
            try (CSVReader reader = new CSVReader(new FileReader(f))) {
                return reader.readAll();
            } catch (IOException | CsvException e) {
                throw new DataGenerationException("Error reading CSV file: " + f, e);
            }
        });

        if (records.size() < 2) { // Header + at least one data row
            return mapper.nullNode();
        }

        String[] header = records.get(0);
        int recordIndex;

        if (sequential) {
            int currentIndex = sequentialCounters.getOrDefault(options, 0);
            recordIndex = 1 + (currentIndex % (records.size() - 1));
            sequentialCounters.put(options, currentIndex + 1);
        } else {
            // Use the Faker's random instance for consistency
            RandomService contextRandom = context.faker().random();
            recordIndex = 1 + contextRandom.nextInt(records.size() - 1);
        }

        String[] values = records.get(recordIndex);
        ObjectNode result = mapper.createObjectNode();
        for (int i = 0; i < header.length; i++) {
            result.put(header[i], values[i]);
        }

        return result;
    }
}
