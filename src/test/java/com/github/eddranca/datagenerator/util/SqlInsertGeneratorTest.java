package com.github.eddranca.datagenerator.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.insert.Insert;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SqlInsertGeneratorTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private Insert parseInsert(String sql) {
        try {
            return (Insert) CCJSqlParserUtil.parse(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse SQL: " + sql, e);
        }
    }

    @Test
    void shouldGenerateBasicInsert() throws Exception {
        String json = """
            {
                "id": 1,
                "name": "John Doe",
                "active": true
            }
            """;
        JsonNode item = mapper.readTree(json);

        String sql = SqlInsertGenerator.generateSqlInsert("users", item);

        Insert insert = parseInsert(sql);
        assertThat(insert.getTable().getName()).isEqualTo("users");

        List<String> columns = insert.getColumns().stream()
            .map(Column::getColumnName)
            .toList();
        assertThat(columns).containsExactlyInAnyOrder("id", "name", "active");

        var values = insert.getValues().getExpressions();
        int idIndex = columns.indexOf("id");
        int nameIndex = columns.indexOf("name");
        int activeIndex = columns.indexOf("active");

        assertThat(values.get(idIndex)).isInstanceOf(LongValue.class);
        assertThat(((LongValue) values.get(idIndex)).getValue()).isEqualTo(1L);
        assertThat(values.get(nameIndex)).isInstanceOf(StringValue.class);
        assertThat(((StringValue) values.get(nameIndex)).getValue()).isEqualTo("John Doe");
        // Boolean without type specification is rendered as "true"/"false" which JSqlParser treats as column reference
        assertThat(values.get(activeIndex)).hasToString("true");
    }

    @Test
    void shouldHandleNullValues() throws Exception {
        String json = """
            {
                "id": 1,
                "middle_name": null,
                "email": "test@example.com"
            }
            """;
        JsonNode item = mapper.readTree(json);

        String sql = SqlInsertGenerator.generateSqlInsert("users", item);

        Insert insert = parseInsert(sql);
        List<String> columns = insert.getColumns().stream()
            .map(Column::getColumnName)
            .toList();

        var values = insert.getValues().getExpressions();
        int middleNameIndex = columns.indexOf("middle_name");
        int emailIndex = columns.indexOf("email");

        assertThat(values.get(middleNameIndex)).isInstanceOf(NullValue.class);
        assertThat(values.get(emailIndex)).isInstanceOf(StringValue.class);
        assertThat(((StringValue) values.get(emailIndex)).getValue()).isEqualTo("test@example.com");
    }

    @Test
    void shouldEscapeSingleQuotes() throws Exception {
        String json = """
            {
                "name": "O'Brien",
                "comment": "It's a test"
            }
            """;
        JsonNode item = mapper.readTree(json);

        String sql = SqlInsertGenerator.generateSqlInsert("users", item);

        Insert insert = parseInsert(sql);
        List<String> columns = insert.getColumns().stream()
            .map(Column::getColumnName)
            .toList();

        var values = insert.getValues().getExpressions();
        int nameIndex = columns.indexOf("name");
        int commentIndex = columns.indexOf("comment");

        // JSqlParser returns escaped values as-is (with double quotes for SQL)
        assertThat(values.get(nameIndex)).isInstanceOf(StringValue.class);
        assertThat(((StringValue) values.get(nameIndex)).getValue()).isEqualTo("O''Brien");
        assertThat(values.get(commentIndex)).isInstanceOf(StringValue.class);
        assertThat(((StringValue) values.get(commentIndex)).getValue()).isEqualTo("It''s a test");
    }

    @Test
    void shouldFilterFieldsWithProjection() throws Exception {
        String json = """
            {
                "id": 1,
                "name": "John",
                "tempField": "should be excluded",
                "email": "john@example.com"
            }
            """;
        JsonNode item = mapper.readTree(json);

        SqlProjection projection = SqlProjection.builder()
            .includeFields(Set.of("id", "name", "email"))
            .build();

        String sql = SqlInsertGenerator.generateSqlInsert("users", item, projection);

        Insert insert = parseInsert(sql);
        List<String> columns = insert.getColumns().stream()
            .map(Column::getColumnName)
            .toList();

        assertThat(columns)
            .containsExactlyInAnyOrder("id", "name", "email")
            .doesNotContain("tempField");
    }

    @Test
    void shouldFormatBooleanAsIntegerWhenTypeSpecified() throws Exception {
        String json = """
            {
                "id": 1,
                "active": true,
                "deleted": false
            }
            """;
        JsonNode item = mapper.readTree(json);

        SqlProjection projection = SqlProjection.builder()
            .withFieldType("active", "TINYINT")
            .withFieldType("deleted", "INT")
            .build();

        String sql = SqlInsertGenerator.generateSqlInsert("users", item, projection);

        Insert insert = parseInsert(sql);
        List<String> columns = insert.getColumns().stream()
            .map(Column::getColumnName)
            .toList();

        var values = insert.getValues().getExpressions();
        int activeIndex = columns.indexOf("active");
        int deletedIndex = columns.indexOf("deleted");

        // Verify booleans are formatted as 0/1 for INT/TINYINT types
        assertThat(values.get(activeIndex)).isInstanceOf(LongValue.class);
        assertThat(((LongValue) values.get(activeIndex)).getValue()).isEqualTo(1L);
        assertThat(values.get(deletedIndex)).isInstanceOf(LongValue.class);
        assertThat(((LongValue) values.get(deletedIndex)).getValue()).isZero();
    }

    @Test
    void shouldHandleComplexObjects() throws Exception {
        String json = """
            {
                "id": 1,
                "metadata": {
                    "key": "value",
                    "nested": {
                        "deep": "data"
                    }
                }
            }
            """;
        JsonNode item = mapper.readTree(json);

        String sql = SqlInsertGenerator.generateSqlInsert("users", item);

        Insert insert = parseInsert(sql);
        List<String> columns = insert.getColumns().stream()
            .map(Column::getColumnName)
            .toList();

        assertThat(columns).contains("metadata");

        // Complex objects are serialized as JSON strings
        var values = insert.getValues().getExpressions();
        int metadataIndex = columns.indexOf("metadata");
        assertThat(values.get(metadataIndex)).isInstanceOf(StringValue.class);
        String metadataJson = ((StringValue) values.get(metadataIndex)).getValue();
        assertThat(metadataJson).contains("key", "value", "nested", "deep", "data");
    }

    @Test
    void shouldHandleArrays() throws Exception {
        String json = """
            {
                "id": 1,
                "tags": ["java", "testing", "sql"]
            }
            """;
        JsonNode item = mapper.readTree(json);

        String sql = SqlInsertGenerator.generateSqlInsert("users", item);

        Insert insert = parseInsert(sql);
        List<String> columns = insert.getColumns().stream()
            .map(Column::getColumnName)
            .toList();

        assertThat(columns).contains("tags");

        // Arrays are serialized as JSON strings
        var values = insert.getValues().getExpressions();
        int tagsIndex = columns.indexOf("tags");
        assertThat(values.get(tagsIndex)).isInstanceOf(StringValue.class);
        String tagsJson = ((StringValue) values.get(tagsIndex)).getValue();
        assertThat(tagsJson).contains("java", "testing", "sql");
    }

    @Test
    void shouldCombineProjectionAndTypes() throws Exception {
        String json = """
            {
                "id": 1,
                "name": "John",
                "created_at": "2024-01-15T10:30:00",
                "tempHelper": "exclude me",
                "active": true
            }
            """;
        JsonNode item = mapper.readTree(json);

        SqlProjection projection = SqlProjection.builder()
            .includeFields(Set.of("id", "name", "created_at", "active"))
            .withFieldType("id", "BIGINT")
            .withFieldType("created_at", "TIMESTAMP")
            .withFieldType("active", "TINYINT")
            .build();

        String sql = SqlInsertGenerator.generateSqlInsert("users", item, projection);

        Insert insert = parseInsert(sql);
        List<String> columns = insert.getColumns().stream()
            .map(Column::getColumnName)
            .toList();

        assertThat(columns)
            .containsExactlyInAnyOrder("id", "name", "created_at", "active")
            .doesNotContain("tempHelper");

        var values = insert.getValues().getExpressions();
        int activeIndex = columns.indexOf("active");

        // Boolean formatted as 1 for TINYINT type
        assertThat(values.get(activeIndex)).isInstanceOf(LongValue.class);
        assertThat(((LongValue) values.get(activeIndex)).getValue()).isEqualTo(1L);
    }
}
