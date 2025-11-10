package com.github.eddranca.datagenerator.util;

import com.github.eddranca.datagenerator.exception.SerializationException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses SQL CREATE TABLE statements to extract table schemas and column types.
 * Uses JSqlParser to handle various SQL dialects.
 */
public class SqlSchemaParser {
    private static final Logger logger = Logger.getLogger(SqlSchemaParser.class.getName());

    private SqlSchemaParser() {
        // Utility class
    }

    /**
     * Parses a CREATE TABLE statement and returns a SqlProjection with field type mappings.
     *
     * @param createTableSql the CREATE TABLE SQL statement
     * @return SqlProjection configured with the table's column types
     * @throws SerializationException if parsing fails
     */
    public static SqlProjection parseCreateTable(String createTableSql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(createTableSql);
            
            if (!(statement instanceof CreateTable)) {
                IllegalArgumentException cause = new IllegalArgumentException("Got: " + statement.getClass().getSimpleName());
                throw new SerializationException("Expected CREATE TABLE statement", cause);
            }

            CreateTable createTable = (CreateTable) statement;
            List<ColumnDefinition> columns = createTable.getColumnDefinitions();
            
            Map<String, String> fieldTypes = new HashMap<>();
            for (ColumnDefinition column : columns) {
                String columnName = column.getColumnName();
                String dataType = column.getColDataType().toString();
                fieldTypes.put(columnName, dataType);
            }

            logger.log(Level.FINE, "Parsed CREATE TABLE for {0} with {1} columns", 
                new Object[]{createTable.getTable().getName(), fieldTypes.size()});

            return SqlProjection.builder()
                .withFieldTypes(fieldTypes)
                .build();

        } catch (Exception e) {
            throw new SerializationException("Failed to parse CREATE TABLE statement: " + e.getMessage(), e);
        }
    }

    /**
     * Parses multiple CREATE TABLE statements and returns a map of table names to SqlProjections.
     *
     * @param createTableStatements map of table names to CREATE TABLE SQL
     * @return map of table names to SqlProjection configurations
     * @throws SerializationException if any parsing fails
     */
    public static Map<String, SqlProjection> parseCreateTables(Map<String, String> createTableStatements) {
        Map<String, SqlProjection> projections = new HashMap<>();
        
        for (Map.Entry<String, String> entry : createTableStatements.entrySet()) {
            String tableName = entry.getKey();
            String createTableSql = entry.getValue();
            
            SqlProjection projection = parseCreateTable(createTableSql);
            projections.put(tableName, projection);
        }
        
        return projections;
    }

    /**
     * Extracts the table name from a CREATE TABLE statement.
     *
     * @param createTableSql the CREATE TABLE SQL statement
     * @return the table name
     * @throws SerializationException if parsing fails
     */
    public static String extractTableName(String createTableSql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(createTableSql);
            
            if (!(statement instanceof CreateTable)) {
                IllegalArgumentException cause = new IllegalArgumentException("Got: " + statement.getClass().getSimpleName());
                throw new SerializationException("Expected CREATE TABLE statement", cause);
            }

            CreateTable createTable = (CreateTable) statement;
            return createTable.getTable().getName();

        } catch (Exception e) {
            throw new SerializationException("Failed to extract table name: " + e.getMessage(), e);
        }
    }
}
