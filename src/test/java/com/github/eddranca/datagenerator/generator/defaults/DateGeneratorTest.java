package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eddranca.datagenerator.generator.GeneratorContext;
import net.datafaker.Faker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateGeneratorTest {

    private final DateGenerator dateGenerator = new DateGenerator();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Faker faker = new Faker();

    @Test
    @DisplayName("Should generate date with default range (epoch to next year)")
    void shouldGenerateDateWithDefaultRange() throws Exception {
        // Given
        JsonNode options = objectMapper.readTree("{}");

        // When
        JsonNode result = dateGenerator.generate(new GeneratorContext(faker, options, objectMapper));

        // Then
        assertThat(result.isTextual()).isTrue();
        LocalDate generatedDate = LocalDate.parse(result.asText());
        LocalDate epochDate = LocalDate.of(1970, 1, 1);
        LocalDate nextYear = LocalDate.now().plusYears(1);
        assertThat(generatedDate)
            .isAfterOrEqualTo(epochDate)
            .isBeforeOrEqualTo(nextYear);
    }

    @Test
    @DisplayName("Should generate date between specified dates")
    void shouldGenerateDateBetweenSpecifiedDates() throws Exception {
        // Given
        JsonNode options = objectMapper.readTree("{\"from\": \"2023-01-01\", \"to\": \"2023-12-31\"}");

        // When
        JsonNode result = dateGenerator.generate(new GeneratorContext(faker, options, objectMapper));

        // Then
        assertThat(result.isTextual()).isTrue();
        LocalDate generatedDate = LocalDate.parse(result.asText());
        assertThat(generatedDate)
            .isAfterOrEqualTo(LocalDate.of(2023, 1, 1))
            .isBeforeOrEqualTo(LocalDate.of(2023, 12, 31));
    }

    @Test
    @DisplayName("Should throw exception when from date is after to date")
    void shouldThrowExceptionWhenFromDateIsAfterToDate() throws Exception {
        // Given
        JsonNode options = objectMapper.readTree("{\"from\": \"2023-12-31\", \"to\": \"2023-01-01\"}");

        // When & Then
        assertThatThrownBy(() -> dateGenerator.generate(new GeneratorContext(faker, options, objectMapper)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'from' date must be before 'to' date");
    }

    @Test
    @DisplayName("Should format date as ISO by default")
    void shouldFormatDateAsIsoByDefault() throws Exception {
        // Given
        JsonNode options = objectMapper.readTree("{\"from\": \"2023-06-15\", \"to\": \"2023-06-15\"}");

        // When
        JsonNode result = dateGenerator.generate(new GeneratorContext(faker, options, objectMapper));

        // Then
        assertThat(result.asText()).isEqualTo("2023-06-15");
    }

    @ParameterizedTest
    @CsvSource({
        "dd/MM/yyyy, 15/06/2023",
        "iso_datetime, 2023-06-15T00:00",
        "timestamp, 1686787200000",
        "epoch, 1686787200",
        "yyyy-MM-dd, 2023-06-15"
    })
    @DisplayName("Should format date with various formats")
    void shouldFormatDateWithVariousFormats(String format, String expected) throws Exception {
        // Given
        JsonNode options = objectMapper
            .readTree("{\"from\": \"2023-06-15\", \"to\": \"2023-06-15\", \"format\": \"" + format + "\"}");

        // When
        JsonNode result = dateGenerator.generate(new GeneratorContext(faker, options, objectMapper));

        // Then
        assertThat(result.asText()).isEqualTo(expected);
    }

    @ParameterizedTest
    @DisplayName("Should handle various custom date formats")
    @CsvSource({
        "yyyy-MM-dd, 2023-06-15",
        "dd-MM-yyyy, 15-06-2023",
        "MM/dd/yyyy, 06/15/2023",
        "yyyy, 2023",
        "'MMM dd, yyyy', 'Jun 15, 2023'"
    })
    void shouldHandleVariousCustomDateFormats(String format, String expected) throws Exception {
        // Given
        LocalDate testDate = LocalDate.of(2023, 6, 15);
        String dateStr = testDate.toString();

        JsonNode options = objectMapper.readTree(
            String.format("{\"from\": \"%s\", \"to\": \"%s\", \"format\": \"%s\"}",
                dateStr, dateStr, format));

        // When
        JsonNode result = dateGenerator.generate(new GeneratorContext(faker, options, objectMapper));

        // Then
        assertThat(result.asText()).isEqualTo(expected);
    }
}
