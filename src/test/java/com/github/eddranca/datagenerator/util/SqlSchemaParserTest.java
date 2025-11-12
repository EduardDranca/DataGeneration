package com.github.eddranca.datagenerator.util;

import com.github.eddranca.datagenerator.exception.SerializationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlSchemaParserTest {

    @Test
    void shouldParseSimpleCreateTable() {
        String sql = "CREATE TABLE users (id INT, name VARCHAR(255), email VARCHAR(255))";

        SqlProjection projection = SqlSchemaParser.parseCreateTable(sql);

        assertThat(projection.getFieldType("id")).isEqualTo("INT");
        assertThat(projection.getFieldType("name")).isEqualTo("VARCHAR (255)");
        assertThat(projection.getFieldType("email")).isEqualTo("VARCHAR (255)");
    }

    @Test
    void shouldParseCreateTableWithConstraints() {
        String sql = """
            CREATE TABLE orders (
                id BIGINT PRIMARY KEY,
                user_id INT NOT NULL,
                total DECIMAL(10,2),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        SqlProjection projection = SqlSchemaParser.parseCreateTable(sql);

        assertThat(projection.getFieldType("id")).contains("BIGINT");
        assertThat(projection.getFieldType("user_id")).contains("INT");
        assertThat(projection.getFieldType("total")).contains("DECIMAL");
        assertThat(projection.getFieldType("created_at")).contains("TIMESTAMP");
    }

    @Test
    void shouldParseCreateTableWithVariousTypes() {
        String sql = """
            CREATE TABLE products (
                id UUID,
                name TEXT,
                price NUMERIC(10,2),
                in_stock BOOLEAN,
                created_date DATE,
                updated_at DATETIME
            )
            """;

        SqlProjection projection = SqlSchemaParser.parseCreateTable(sql);

        assertThat(projection.getFieldType("id")).isEqualTo("UUID");
        assertThat(projection.getFieldType("name")).isEqualTo("TEXT");
        assertThat(projection.getFieldType("price")).contains("NUMERIC");
        assertThat(projection.getFieldType("in_stock")).isEqualTo("BOOLEAN");
        assertThat(projection.getFieldType("created_date")).isEqualTo("DATE");
        assertThat(projection.getFieldType("updated_at")).isEqualTo("DATETIME");
    }

    @Test
    void shouldExtractTableName() {
        String sql = "CREATE TABLE my_table (id INT, name VARCHAR(100))";

        String tableName = SqlSchemaParser.extractTableName(sql);

        assertThat(tableName).isEqualTo("my_table");
    }

    @Test
    void shouldParseMultipleCreateTables() {
        Map<String, String> schemas = Map.of(
            "users", "CREATE TABLE users (id INT, name VARCHAR(255))",
            "orders", "CREATE TABLE orders (id INT, user_id INT, total DECIMAL(10,2))"
        );

        Map<String, SqlProjection> projections = SqlSchemaParser.parseCreateTables(schemas);

        assertThat(projections).hasSize(2);
        assertThat(projections.get("users").getFieldType("id")).isEqualTo("INT");
        assertThat(projections.get("orders").getFieldType("total")).contains("DECIMAL");
    }

    @Test
    void shouldThrowExceptionForInvalidSql() {
        String invalidSql = "SELECT * FROM users";

        assertThatThrownBy(() -> SqlSchemaParser.parseCreateTable(invalidSql))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("Expected CREATE TABLE statement");
    }

    @Test
    void shouldThrowExceptionForMalformedSql() {
        String malformedSql = "CREATE TABLE users (id INT name VARCHAR";

        assertThatThrownBy(() -> SqlSchemaParser.parseCreateTable(malformedSql))
            .isInstanceOf(SerializationException.class);
    }
}
