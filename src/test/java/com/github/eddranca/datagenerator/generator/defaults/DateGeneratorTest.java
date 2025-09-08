package com.github.eddranca.datagenerator.generator.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DateGenerator Tests")
class DateGeneratorTest {

    private DateGenerator dateGenerator;
    private ObjectMapper objectMapper;
    private Faker faker;

    @BeforeEach
    void setUp() {
        faker = new Faker();
        dateGenerator = new DateGenerator(faker);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should generate date with default range (epoch to next year)")
    void shouldGenerateDateWithDefaultRange() throws Exception {
        // Given
        JsonNode config = objectMapper.readTree("{}");

        // When
        JsonNode result = dateGenerator.generate(config);

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
        JsonNode config = objectMapper.readTree("{\"from\": \"2023-01-01\", \"to\": \"2023-12-31\"}");

        // When
        JsonNode result = dateGenerator.generate(config);

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
        JsonNode config = objectMapper.readTree("{\"from\": \"2023-12-31\", \"to\": \"2023-01-01\"}");

        // When & Then
        assertThatThrownBy(() -> dateGenerator.generate(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'from' date must be before 'to' date");
    }

    @Test
    @DisplayName("Should format date as ISO by default")
    void shouldFormatDateAsIsoByDefault() throws Exception {
        // Given
        JsonNode config = objectMapper.readTree("{\"from\": \"2023-06-15\", \"to\": \"2023-06-15\"}");

        // When
        JsonNode result = dateGenerator.generate(config);

        // Then
        assertThat(result.asText()).isEqualTo("2023-06-15");
    }

    @Test
    @DisplayName("Should format date with custom format")
    void shouldFormatDateWithCustomFormat() throws Exception {
        // Given
        JsonNode config = objectMapper
            .readTree("{\"from\": \"2023-06-15\", \"to\": \"2023-06-15\", \"format\": \"dd/MM/yyyy\"}");

        // When
        JsonNode result = dateGenerator.generate(config);

        // Then
        assertThat(result.asText()).isEqualTo("15/06/2023");
    }

    @Test
    @DisplayName("Should format date as ISO datetime")
    void shouldFormatDateAsIsoDatetime() throws Exception {
        // Given
        JsonNode config = objectMapper
            .readTree("{\"from\": \"2023-06-15\", \"to\": \"2023-06-15\", \"format\": \"iso_datetime\"}");

        // When
        JsonNode result = dateGenerator.generate(config);

        // Then
        assertThat(result.asText()).isEqualTo("2023-06-15T00:00");
    }

    @Test
    @DisplayName("Should format date as timestamp")
    void shouldFormatDateAsTimestamp() throws Exception {
        // Given
        JsonNode config = objectMapper
            .readTree("{\"from\": \"2023-06-15\", \"to\": \"2023-06-15\", \"format\": \"timestamp\"}");

        // When
        JsonNode result = dateGenerator.generate(config);

        // Then
        assertThat(result.asText()).isEqualTo("1686787200000"); // 2023-06-15 00:00:00 UTC in milliseconds
    }

    @Test
    @DisplayName("Should format date as epoch seconds")
    void shouldFormatDateAsEpochSeconds() throws Exception {
        // Given
        JsonNode config = objectMapper
            .readTree("{\"from\": \"2023-06-15\", \"to\": \"2023-06-15\", \"format\": \"epoch\"}");

        // When
        JsonNode result = dateGenerator.generate(config);

        // Then
        assertThat(result.asText()).isEqualTo("1686787200"); // 2023-06-15 00:00:00 UTC in seconds
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

        JsonNode config = objectMapper.readTree(
            String.format("{\"from\": \"%s\", \"to\": \"%s\", \"format\": \"%s\"}",
                dateStr, dateStr, format));

        // When
        JsonNode result = dateGenerator.generate(config);

        // Then
        assertThat(result.asText()).isEqualTo(expected);
    }
}
