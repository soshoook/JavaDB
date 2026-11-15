package sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import core.Column;
import core.Database;
import core.ForeignKey;
import core.Row;
import core.Table;

public class SQLExecutor {

    private Database database;
    private SQLparser parser;

    public SQLExecutor(Database database) {
        this.database = database;
        this.parser = new SQLparser();
    }

    public void execute(String query) {
        String command = parser.getCommandType(query);

        if (command.equals("INSERT")) {
            String tableName = parser.getTableName(query);
            Table table = database.getTable(tableName);

            if (table == null)
                throw new IllegalArgumentException("Table does not exist: " + tableName);

            Row row = parser.parseInsert(query, table);
            database.addRow(tableName, row);
        }

        else if (command.equals("SELECT")) {
            String tableName = parser.getTableName(query);
            Table table = database.getTable(tableName);

            if (table == null)
                throw new IllegalArgumentException("Table does not exist: " + tableName);

            List<String> names = parser.parseSelectColumns(query);
            List<Column> shown = new ArrayList<>();

            if (names == null) {
                shown.addAll(table.getColumn());
            } else {
                for (String name : names)
                    shown.add(findColumn(table, name));
            }

            Condition condition = parser.parseCondition(query);

            for (Column column : shown) {
                System.out.print(column.getName() + "\t");
            }

            System.out.println();

            for (Row row : table.getRow()) {
                if (matches(table, row, condition)) {
                    for (Column column : shown) {
                        System.out.print(row.getValue(column.getName()) + "\t");
                    }
                    System.out.println();
                }
            }
        }

        else if (command.equals("UPDATE")) {
            String tableName = parser.getTableName(query);
            Table table = database.getTable(tableName);
            if (table == null)
                throw new IllegalArgumentException("Table does not exist: " + tableName);

            String[] parts = parser.getSetClause(query).split("=", 2);

            if (parts.length != 2)
                throw new IllegalArgumentException("Incorrect SET format");

            String columnName = parts[0].trim();
            Column column = findColumn(table, columnName);

            if (column.getKey())
                throw new IllegalArgumentException("Cannot update primary key");

            Object newValue = parser.parseValue(
                removeSemicolon(parts[1]), column.getType()
            );

            for (ForeignKey foreignKey : table.getForeignKeys()) {
                if (foreignKey.getColumnName().equals(columnName)
                        && !database.isValueValid(foreignKey, newValue))
                    throw new IllegalArgumentException("Foreign key does not exists");
            }

            Condition condition = parser.parseCondition(query);
            int updated = 0;

            for (Row row : table.getRow()) {
                if (matches(table, row, condition)) {
                    row.setValue(columnName, newValue);
                    updated++;
                }
            }

            System.out.println("Updated rows: " + updated);
        }

        else if (command.equals("DELETE")) {
            String tableName = parser.getTableName(query);
            Table table = database.getTable(tableName);

            if (table == null)
                throw new IllegalArgumentException("Table does not exist: " + tableName);

            Condition condition = parser.parseCondition(query);

            if (condition == null)
                throw new IllegalArgumentException("DELETE requires WHERE");

            List<Row> toDelete = new ArrayList<>();
            for (Row row : table.getRow()) {
                if (matches(table, row, condition))
                    toDelete.add(row);
            }

            for (Row row : toDelete) {
                if (database.isReferenced(table, row))
                    throw new IllegalArgumentException("Cannot delete: row is referenced by another table");
            }

            table.getRow().removeAll(toDelete);

            System.out.println("Deleted rows: " + toDelete.size());
        }

        else {
            throw new IllegalArgumentException("Unknown SQL command");
        }
    }

    private boolean matches(Table table, Row row, Condition condition) {
        if (condition == null)
            return true;

        Column column = findColumn(table, condition.getColumn());

        Object actualValue = row.getValue(column.getName());
        Object expectedValue = parser.parseValue(
            removeSemicolon(condition.getValue()), column.getType()
        );

        if (actualValue == null)
            return false;

        String operator = condition.getOperator();

        if (operator.equals("="))
            return Objects.equals(actualValue, expectedValue);

        if (operator.equals("!="))
            return !Objects.equals(actualValue, expectedValue);

        if (!(actualValue instanceof Comparable))
            throw new IllegalArgumentException("Values cannot be compared");

        @SuppressWarnings("unchecked")
        int comparison = ((Comparable<Object>) actualValue).compareTo(expectedValue);

        if (operator.equals(">"))
            return comparison > 0;
        else if (operator.equals("<"))
            return comparison < 0;
        else if (operator.equals(">="))
            return comparison >= 0;
        else if (operator.equals("<="))
            return comparison <= 0;

        throw new IllegalArgumentException("Unknown operator: " + operator);
    }

    private Column findColumn(Table table, String name) {
        for (Column column : table.getColumn()) {
            if (column.getName().equals(name))
                return column;
        }

        throw new IllegalArgumentException("Column does not exist: " + name);
    }

    private String removeSemicolon(String value) {
        value = value.trim();

        if (value.endsWith(";"))
            value = value.substring(0, value.length() - 1).trim();

        return value;
    }
}
