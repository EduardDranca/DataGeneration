package com.github.eddranca.datagenerator;

import com.github.eddranca.datagenerator.util.SqlProjection;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.insert.Insert;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SqlProjectionIntegrationTest extends ParameterizedGenerationTest {

    private Insert parseInsert(String sql) {
        try {
            return (Insert) CCJSqlParserUtil.parse(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse SQL: " + sql, e);
        }
    }

    @BothImplementationsTest
    void shouldGenerateSqlWithFieldProjection(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
                "users": {
                    "count": 2,
                    "item": {
                        "id": {"gen": "sequence"},
                        "name": {"gen": "name.firstName"},
                        "email": {"gen": "internet.emailAddress"},
                        "tempHelper": "temporary-value"
                    }
                }
            }
            """;

        SqlProjection projection = SqlProjection.builder()
            .includeFields(Set.of("id", "name", "email"))
            .build();

        Generation generation = createGenerator(memoryOptimized)
            .fromJsonString(dsl)
            .generate();

        List<String> inserts = generation.streamSqlInsertsWithProjection("users", projection)
            .toList();

        assertThat(inserts)
            .hasSize(2)
            .allSatisfy(sql -> {
                Insert insert = parseInsert(sql);
                assertThat(insert.getTable().getName()).isEqualTo("users");

                List<String> columns = insert.getColumns().stream()
                    .map(Column::getColumnName)
                    .toList();

                assertThat(columns)
                    .containsExactlyInAnyOrder("id", "name", "email")
                    .doesNotContain("tempHelper");

                var values = insert.getValues().getExpressions();
                assertThat(values).hasSize(columns.size());

                // Verify data types
                int idIndex = columns.indexOf("id");
                int nameIndex = columns.indexOf("name");
                int emailIndex = columns.indexOf("email");

                assertThat(values.get(idIndex)).isInstanceOf(LongValue.class);
                assertThat(values.get(nameIndex)).isInstanceOf(StringValue.class);
                assertThat(values.get(emailIndex)).isInstanceOf(StringValue.class);
            });
    }

    @BothImplementationsTest
    void shouldGenerateSqlWithDataTypes(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
                "orders": {
                    "count": 2,
                    "item": {
                        "id": {"gen": "sequence"},
                        "total": {"gen": "number", "min": 10, "max": 100},
                        "active": {"gen": "boolean"},
                        "created_at": {"gen": "date", "format": "iso_datetime"}
                    }
                }
            }
            """;

        SqlProjection projection = SqlProjection.builder()
            .withFieldType("id", "BIGINT")
            .withFieldType("total", "DECIMAL(10,2)")
            .withFieldType("active", "TINYINT")
            .withFieldType("created_at", "TIMESTAMP")
            .build();

        Generation generation = createGenerator(memoryOptimized)
            .fromJsonString(dsl)
            .generate();

        List<String> inserts = generation.streamSqlInsertsWithProjection("orders", projection)
            .toList();

        assertThat(inserts)
            .hasSize(2)
            .allSatisfy(sql -> {
                Insert insert = parseInsert(sql);
                assertThat(insert.getTable().getName()).isEqualTo("orders");

                List<String> columns = insert.getColumns().stream()
                    .map(Column::getColumnName)
                    .toList();
                assertThat(columns).contains("id", "total", "active", "created_at");

                var values = insert.getValues().getExpressions();
                int idIndex = columns.indexOf("id");
                int totalIndex = columns.indexOf("total");
                int activeIndex = columns.indexOf("active");
                int createdAtIndex = columns.indexOf("created_at");

                // Verify data types
                assertThat(values.get(idIndex)).isInstanceOf(LongValue.class);
                assertThat(values.get(totalIndex)).isInstanceOf(LongValue.class);
                assertThat(values.get(createdAtIndex)).isInstanceOf(StringValue.class);

                // Verify boolean is formatted as 0 or 1 (TINYINT)
                assertThat(values.get(activeIndex)).isInstanceOf(LongValue.class);
                assertThat(((LongValue) values.get(activeIndex)).getValue()).isIn(0L, 1L);
            });
    }

    @BothImplementationsTest
    void shouldGenerateSqlFromCreateTableSchema(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
                "products": {
                    "count": 3,
                    "item": {
                        "id": {"gen": "sequence"},
                        "name": {"gen": "company.name"},
                        "price": {"gen": "number", "min": 10, "max": 1000},
                        "in_stock": {"gen": "boolean"},
                        "created_at": {"gen": "date", "format": "iso_datetime"}
                    }
                }
            }
            """;

        String createTableSql = """
            CREATE TABLE products (
                id BIGINT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                price DECIMAL(10,2),
                in_stock TINYINT,
                created_at TIMESTAMP
            )
            """;

        List<String> inserts = createGenerator(memoryOptimized)
            .fromJsonString(dsl)
            .streamSqlInsertsFromSchema("products", createTableSql)
            .toList();

        assertThat(inserts)
            .hasSize(3)
            .allSatisfy(sql -> {
                Insert insert = parseInsert(sql);
                assertThat(insert.getTable().getName()).isEqualTo("products");

                List<String> columns = insert.getColumns().stream()
                    .map(Column::getColumnName)
                    .toList();
                assertThat(columns).containsExactlyInAnyOrder(
                    "id", "name", "price", "in_stock", "created_at"
                );

                var values = insert.getValues().getExpressions();
                int idIndex = columns.indexOf("id");
                int nameIndex = columns.indexOf("name");
                int priceIndex = columns.indexOf("price");
                int inStockIndex = columns.indexOf("in_stock");
                int createdAtIndex = columns.indexOf("created_at");

                // Verify data types
                assertThat(values.get(idIndex)).isInstanceOf(LongValue.class);
                assertThat(values.get(nameIndex)).isInstanceOf(StringValue.class);
                assertThat(values.get(priceIndex)).isInstanceOf(LongValue.class);
                assertThat(values.get(createdAtIndex)).isInstanceOf(StringValue.class);

                // Verify boolean is formatted as 0 or 1 (TINYINT)
                assertThat(values.get(inStockIndex)).isInstanceOf(LongValue.class);
                assertThat(((LongValue) values.get(inStockIndex)).getValue()).isIn(0L, 1L);
            });
    }

    @BothImplementationsTest
    void shouldGenerateSqlFromMultipleSchemas(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
                "users": {
                    "count": 2,
                    "item": {
                        "id": {"gen": "sequence"},
                        "name": {"gen": "name.firstName"},
                        "active": {"gen": "boolean"}
                    }
                },
                "orders": {
                    "count": 3,
                    "item": {
                        "id": {"gen": "sequence"},
                        "user_id": {"ref": "users[*].id"},
                        "total": {"gen": "number", "min": 10, "max": 100}
                    }
                }
            }
            """;

        Map<String, String> schemas = Map.of(
            "users", "CREATE TABLE users (id INT, name VARCHAR(255), active TINYINT)",
            "orders", "CREATE TABLE orders (id INT, user_id INT, total DECIMAL(10,2))"
        );

        Map<String, Stream<String>> sqlStreams = createGenerator(memoryOptimized)
            .fromJsonString(dsl)
            .generateAsSqlFromSchemas(schemas);

        assertThat(sqlStreams).containsKeys("users", "orders");

        List<String> userInserts = sqlStreams.get("users").toList();
        List<String> orderInserts = sqlStreams.get("orders").toList();

        assertThat(userInserts)
            .hasSize(2)
            .allSatisfy(sql -> {
                Insert insert = parseInsert(sql);
                assertThat(insert.getTable().getName()).isEqualTo("users");

                List<String> columns = insert.getColumns().stream()
                    .map(Column::getColumnName)
                    .toList();
                assertThat(columns).contains("id", "name", "active");

                var values = insert.getValues().getExpressions();
                int idIndex = columns.indexOf("id");
                int nameIndex = columns.indexOf("name");
                int activeIndex = columns.indexOf("active");

                // Verify data types
                assertThat(values.get(idIndex)).isInstanceOf(LongValue.class);
                assertThat(values.get(nameIndex)).isInstanceOf(StringValue.class);

                // Verify boolean formatting
                assertThat(values.get(activeIndex)).isInstanceOf(LongValue.class);
                assertThat(((LongValue) values.get(activeIndex)).getValue()).isIn(0L, 1L);
            });

        assertThat(orderInserts)
            .hasSize(3)
            .allSatisfy(sql -> {
                Insert insert = parseInsert(sql);
                assertThat(insert.getTable().getName()).isEqualTo("orders");

                List<String> columns = insert.getColumns().stream()
                    .map(Column::getColumnName)
                    .toList();
                assertThat(columns).contains("id", "user_id", "total");

                var values = insert.getValues().getExpressions();
                int idIndex = columns.indexOf("id");
                int userIdIndex = columns.indexOf("user_id");
                int totalIndex = columns.indexOf("total");

                // Verify data types - all should be numeric
                assertThat(values.get(idIndex)).isInstanceOf(LongValue.class);
                assertThat(values.get(userIdIndex)).isInstanceOf(LongValue.class);
                assertThat(values.get(totalIndex)).isInstanceOf(LongValue.class);
            });
    }

    @BothImplementationsTest
    void shouldExcludeHelperFieldsInComplexScenario(boolean memoryOptimized) throws Exception {
        String dsl = """
            {
                "categories": {
                    "count": 2,
                    "item": {
                        "id": {"gen": "sequence"},
                        "name": {"gen": "company.industry"}
                    }
                },
                "products": {
                    "count": 5,
                    "item": {
                        "id": {"gen": "sequence"},
                        "name": {"gen": "company.name"},
                        "category_id": {"ref": "categories[*].id", "sequential": true},
                        "price": {"gen": "number", "min": 10, "max": 1000},
                        "_tempCategoryName": {"ref": "categories[*].name", "sequential": true}
                    }
                }
            }
            """;

        SqlProjection productsProjection = SqlProjection.builder()
            .includeFields(Set.of("id", "name", "category_id", "price"))
            .withFieldType("id", "BIGINT")
            .withFieldType("category_id", "BIGINT")
            .withFieldType("price", "DECIMAL(10,2)")
            .build();

        Map<String, SqlProjection> projections = Map.of(
            "categories", SqlProjection.builder().build(),
            "products", productsProjection
        );

        Generation generation = createGenerator(memoryOptimized)
            .fromJsonString(dsl)
            .generate();

        Map<String, Stream<String>> sqlStreams = generation.asSqlInsertsWithProjections(projections);

        List<String> productInserts = sqlStreams.get("products").toList();

        assertThat(productInserts)
            .hasSize(5)
            .allSatisfy(sql -> {
                Insert insert = parseInsert(sql);
                assertThat(insert.getTable().getName()).isEqualTo("products");

                List<String> columns = insert.getColumns().stream()
                    .map(Column::getColumnName)
                    .toList();

                assertThat(columns)
                    .containsExactlyInAnyOrder("id", "name", "category_id", "price")
                    .doesNotContain("_tempCategoryName");

                var values = insert.getValues().getExpressions();
                assertThat(values).hasSize(columns.size());

                int idIndex = columns.indexOf("id");
                int nameIndex = columns.indexOf("name");
                int categoryIdIndex = columns.indexOf("category_id");
                int priceIndex = columns.indexOf("price");

                // Verify data types
                assertThat(values.get(idIndex)).isInstanceOf(LongValue.class);
                assertThat(values.get(nameIndex)).isInstanceOf(StringValue.class);
                assertThat(values.get(categoryIdIndex)).isInstanceOf(LongValue.class);
                assertThat(values.get(priceIndex)).isInstanceOf(LongValue.class);
            });
    }
}
