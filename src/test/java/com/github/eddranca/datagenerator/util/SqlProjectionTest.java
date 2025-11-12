package com.github.eddranca.datagenerator.util;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SqlProjectionTest {

    @Test
    void shouldIncludeAllFieldsWhenNoProjectionSpecified() {
        SqlProjection projection = SqlProjection.builder().build();

        assertThat(projection.shouldIncludeField("id")).isTrue();
        assertThat(projection.shouldIncludeField("name")).isTrue();
        assertThat(projection.shouldIncludeField("anyField")).isTrue();
    }

    @Test
    void shouldIncludeOnlySpecifiedFields() {
        SqlProjection projection = SqlProjection.builder()
            .includeFields(Set.of("id", "name"))
            .build();

        assertThat(projection.shouldIncludeField("id")).isTrue();
        assertThat(projection.shouldIncludeField("name")).isTrue();
        assertThat(projection.shouldIncludeField("email")).isFalse();
        assertThat(projection.shouldIncludeField("tempField")).isFalse();
    }

    @Test
    void shouldReturnFieldTypes() {
        SqlProjection projection = SqlProjection.builder()
            .withFieldType("id", "INT")
            .withFieldType("created_at", "TIMESTAMP")
            .withFieldType("name", "VARCHAR(255)")
            .build();

        assertThat(projection.getFieldType("id")).isEqualTo("INT");
        assertThat(projection.getFieldType("created_at")).isEqualTo("TIMESTAMP");
        assertThat(projection.getFieldType("name")).isEqualTo("VARCHAR(255)");
        assertThat(projection.getFieldType("unknown")).isNull();
    }

    @Test
    void shouldSupportBulkFieldTypes() {
        Map<String, String> types = Map.of(
            "id", "BIGINT",
            "price", "DECIMAL(10,2)",
            "active", "BOOLEAN"
        );

        SqlProjection projection = SqlProjection.builder()
            .withFieldTypes(types)
            .build();

        assertThat(projection.getFieldType("id")).isEqualTo("BIGINT");
        assertThat(projection.getFieldType("price")).isEqualTo("DECIMAL(10,2)");
        assertThat(projection.getFieldType("active")).isEqualTo("BOOLEAN");
        assertThat(projection.hasFieldTypes()).isTrue();
    }

    @Test
    void shouldCombineFieldsAndTypes() {
        SqlProjection projection = SqlProjection.builder()
            .includeFields(Set.of("id", "name", "created_at"))
            .withFieldType("id", "INT")
            .withFieldType("created_at", "TIMESTAMP")
            .build();

        assertThat(projection.shouldIncludeField("id")).isTrue();
        assertThat(projection.shouldIncludeField("name")).isTrue();
        assertThat(projection.shouldIncludeField("email")).isFalse();
        
        assertThat(projection.getFieldType("id")).isEqualTo("INT");
        assertThat(projection.getFieldType("created_at")).isEqualTo("TIMESTAMP");
        assertThat(projection.getFieldType("name")).isNull();
    }
}
