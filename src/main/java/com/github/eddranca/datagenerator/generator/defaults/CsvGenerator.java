package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.eddranca.datagenerator.exception.DataGenerationException;
import com.github.eddranca.datagenerator.generator.Generator;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CsvGenerator implements Generator {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Random random = new Random();
    private final Map<JsonNode, Integer> sequentialCounters = new IdentityHashMap<>();
    private final Map<String, List<String[]>> csvCache = new IdentityHashMap<>();

    @Override
    public JsonNode generate(JsonNode options) {
        String file = options.path("file").asText();
        boolean sequential = options.path("sequential").asBoolean(true);
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
            recordIndex = 1 + random.nextInt(records.size() - 1);
        }

        String[] values = records.get(recordIndex);
        ObjectNode result = mapper.createObjectNode();
        for (int i = 0; i < header.length; i++) {
            result.put(header[i], values[i]);
        }

        return result;
    }
}
