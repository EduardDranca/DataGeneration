package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.Generator;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Simple date generator that creates random dates between two given dates with
 * optional formatting
 */
public class DateGenerator implements Generator {

    @Override
    public JsonNode generate(GeneratorContext context) {
        Faker faker = context.faker();
        ObjectMapper mapper = context.mapper();
        
        // Default date range: epoch time to next year
        LocalDate defaultFrom = LocalDate.of(1970, 1, 1); // Unix epoch
        LocalDate defaultTo = LocalDate.now().plusYears(1);

        // Parse from and to dates using syntactic sugar
        String fromStr = context.getStringOption("from");
        String toStr = context.getStringOption("to");
        
        LocalDate from = fromStr != null ? LocalDate.parse(fromStr) : defaultFrom;
        LocalDate to = toStr != null ? LocalDate.parse(toStr) : defaultTo;

        // Generate random date between from and to
        long daysBetween = ChronoUnit.DAYS.between(from, to);
        if (daysBetween < 0) {
            throw new IllegalArgumentException("'from' date must be before 'to' date");
        }

        LocalDate randomDate = from.plusDays(faker.random().nextLong(daysBetween + 1));

        // Format the date using syntactic sugar
        String format = context.getStringOption("format");
        String formattedDate = formatDate(randomDate, format);

        return mapper.valueToTree(formattedDate);
    }

    private String formatDate(LocalDate date, String format) {
        if (format == null) {
            // Default ISO date format
            return date.toString();
        }

        return switch (format.toLowerCase()) {
            case "iso" -> date.toString();
            case "iso_datetime" -> date.atStartOfDay().toString();
            case "timestamp" -> String.valueOf(date.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000);
            case "epoch" -> String.valueOf(date.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC));
            default -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                yield date.format(formatter);
            }
        };
    }
}
