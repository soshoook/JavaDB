package sql;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import core.Column;
import core.Database;
import core.ForeignKey;
import core.Row;
import core.Table;
import index.Index;

// выполнение SQL команд
// использует парсер для разбора запросов + взаимодействует с таблицей и бд для выполнения операций

public class SQLExecutor {

    private final Database database;
    private final SQLparser parser;
    private final PrintStream out;
    private String lastIndexUsed;


    public SQLExecutor(Database database) {
        this(database, System.out);
    }

    public SQLExecutor(Database database, PrintStream out) {
        this.database = database;
        this.parser = new SQLparser();
        this.out = out;
    }

    public String getLastIndexUsed() {
        return lastIndexUsed;
    }

    public void execute(String query) {
        String command = parser.getCommandType(query);

        switch (command) {
            case "INSERT":
                executeInsert(query);
                break;
            case "SELECT":
                executeSelect(query);
                break;
            case "UPDATE":
                executeUpdate(query);
                break;
            case "DELETE":
                executeDelete(query);
                break;
            case "CREATE TABLE":
                executeCreateTable(query);
                break;
            case "DROP TABLE":
                executeDropTable(query);
                break;
            case "CREATE INDEX":
                executeCreateIndex(query);
                break;
            case "DROP INDEX":
                executeDropIndex(query);
                break;
            default:
                throw new IllegalArgumentException("Unknown SQL command");
        }
    }

    private void executeInsert(String query) {
        String tableName = parser.getTableName(query);
        Table table = requireTable(tableName);

        List<Row> rows = parser.parseInsert(query, table);
        List<Row> inserted = new ArrayList<>();

        try {
            for (Row row : rows) {
                database.addRow(tableName, row);
                inserted.add(row);
            }
        } catch (RuntimeException e) {
            table.deleteRows(inserted);
            throw e;
        }

        out.println("Inserted rows: " + inserted.size());
    }

    private void executeSelect(String query) {
        Table table = requireTable(parser.getTableName(query));

        List<String> names = parser.parseSelectColumns(query);
        List<Column> shown = new ArrayList<>();
        if (names == null) {
            shown.addAll(table.getColumn());
        } else {
            for (String name : names)
                shown.add(findColumn(table, name));
        }

        Filter filter = buildFilter(table, parser.parseCondition(query));
        List<Row> result = new ArrayList<>();
        for (Row row : candidateRows(table, filter)) {
            if (filter == null || filter.matches(row))
                result.add(row);
        }

        printTable(shown, result);
        out.println("(" + result.size() + (result.size() == 1 ? " row" : " rows")
                + (lastIndexUsed != null ? ", index: " + lastIndexUsed : "") + ")");
    }

    private void executeUpdate(String query) {
        Table table = requireTable(parser.getTableName(query));

        String[] parts = parser.getSetClause(query).split("=", 2);
        if (parts.length != 2)
            throw new IllegalArgumentException("Incorrect SET format");

        String columnName = parts[0].trim();
        Column column = findColumn(table, columnName);

        if (column.getKey())
            throw new IllegalArgumentException("Cannot update primary key");

        Object newValue = parser.parseValue(removeSemicolon(parts[1]), column.getType());

        for (ForeignKey foreignKey : table.getForeignKeys()) {
            if (foreignKey.getColumnName().equals(columnName)
                    && !database.isValueValid(foreignKey, newValue))
                throw new IllegalArgumentException("Foreign key does not exists");
        }

        Filter filter = buildFilter(table, parser.parseCondition(query));
        int updated = 0;

        for (Row row : candidateRows(table, filter)) {
            if (filter == null || filter.matches(row)) {
                table.updateValue(row, columnName, newValue);
                updated++;
            }
        }

        out.println("Updated rows: " + updated);
    }

    private void executeDelete(String query) {
        Table table = requireTable(parser.getTableName(query));

        Condition condition = parser.parseCondition(query);
        if (condition == null)
            throw new IllegalArgumentException("DELETE requires WHERE");

        Filter filter = buildFilter(table, condition);
        List<Row> toDelete = new ArrayList<>();
        for (Row row : candidateRows(table, filter)) {
            if (filter.matches(row))
                toDelete.add(row);
        }

        for (Row row : toDelete) {
            if (database.isReferenced(table, row))
                throw new IllegalArgumentException("Cannot delete: row is referenced by another table");
        }

        table.deleteRows(toDelete);
        out.println("Deleted rows: " + toDelete.size());
    }

    private void executeCreateTable(String query) {
        Table table = parser.parseCreateTable(query);

        if (database.hasTable(table.getName()))
            throw new IllegalArgumentException("Table already exists: " + table.getName());

        for (ForeignKey foreignKey : table.getForeignKeys()) {
            Table parent = database.getTable(foreignKey.getRefTable());
            if (parent == null)
                throw new IllegalArgumentException("Referenced table does not exist: " + foreignKey.getRefTable());

            Column parentColumn = findColumn(parent, foreignKey.getRefColumn());
            if (!parentColumn.getKey())
                throw new IllegalArgumentException("Referenced column must be a PRIMARY KEY: " + foreignKey.getRefColumn());

            Column ownColumn = findColumn(table, foreignKey.getColumnName());
            if (ownColumn.getType() != parentColumn.getType())
                throw new IllegalArgumentException("Foreign key column type does not match: " + ownColumn.getName());
        }

        database.addTable(table);
        out.println("Table created: " + table.getName());
    }

    private void executeDropTable(String query) {
        String tableName = parser.getTableName(query);
        database.dropTable(tableName);
        out.println("Table dropped: " + tableName);
    }

    private void executeCreateIndex(String query) {
        String[] parts = parser.parseCreateIndex(query);
        Table table = requireTable(parts[1]);
        table.createIndex(parts[0], parts[2]);
        out.println("Index created: " + parts[0]);
    }

    private void executeDropIndex(String query) {
        String[] parts = parser.parseDropIndex(query);
        Table table = requireTable(parts[1]);
        table.dropIndex(parts[0]);
        out.println("Index dropped: " + parts[0]);
    }


    private List<Row> candidateRows(Table table, Filter filter) {
        lastIndexUsed = null;

        if (filter != null && !filter.operator.equals("!=")) {
            Index index = table.getIndexOnColumn(filter.column.getName());
            if (index != null) {
                lastIndexUsed = index.getName();
                switch (filter.operator) {
                    case "=":
                        return index.find(filter.expected);
                    case ">":
                        return index.range(filter.expected, false, null, false);
                    case ">=":
                        return index.range(filter.expected, true, null, false);
                    case "<":
                        return index.range(null, false, filter.expected, false);
                    case "<=":
                        return index.range(null, false, filter.expected, true);
                    default:
                        break;
                }
            }
        }

        lastIndexUsed = null;
        return table.getRow();
    }

    private Filter buildFilter(Table table, Condition condition) {
        if (condition == null)
            return null;

        Column column = findColumn(table, condition.getColumn());
        String operator = condition.getOperator();

        if (!(operator.equals("=") || operator.equals("!=") || operator.equals(">")
                || operator.equals("<") || operator.equals(">=") || operator.equals("<=")))
            throw new IllegalArgumentException("Unknown operator: " + operator);

        Object expected = parser.parseValue(removeSemicolon(condition.getValue()), column.getType());
        return new Filter(column, operator, expected);
    }

    private static class Filter {
        final Column column;
        final String operator;
        final Object expected;

        Filter(Column column, String operator, Object expected) {
            this.column = column;
            this.operator = operator;
            this.expected = expected;
        }

        boolean matches(Row row) {
            Object actual = row.getValue(column.getName());
            if (actual == null)
                return false;

            if (operator.equals("="))
                return actual.equals(expected);
            if (operator.equals("!="))
                return !actual.equals(expected);

            @SuppressWarnings("unchecked")
            int comparison = ((Comparable<Object>) actual).compareTo(expected);

            switch (operator) {
                case ">":
                    return comparison > 0;
                case "<":
                    return comparison < 0;
                case ">=":
                    return comparison >= 0;
                default:
                    return comparison <= 0;
            }
        }
    }

    private void printTable(List<Column> columns, List<Row> rows) {
        int n = columns.size();
        int[] width = new int[n];
        String[][] cells = new String[rows.size()][n];

        for (int i = 0; i < n; i++)
            width[i] = columns.get(i).getName().length();

        for (int r = 0; r < rows.size(); r++) {
            for (int i = 0; i < n; i++) {
                Object value = rows.get(r).getValue(columns.get(i).getName());
                cells[r][i] = value == null ? "NULL" : String.valueOf(value);
                width[i] = Math.max(width[i], cells[r][i].length());
            }
        }

        String[] header = new String[n];
        for (int i = 0; i < n; i++)
            header[i] = columns.get(i).getName();

        out.println(formatLine(header, width));

        StringBuilder separator = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0)
                separator.append('+');
            separator.append(repeat('-', width[i] + 2));
        }
        out.println(separator);

        for (String[] row : cells)
            out.println(formatLine(row, width));
    }

    private String formatLine(String[] cells, int[] width) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0)
                line.append('|');
            line.append(' ').append(cells[i]).append(repeat(' ', width[i] - cells[i].length())).append(' ');
        }
        return line.toString();
    }

    private String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++)
            sb.append(c);
        return sb.toString();
    }

    private Table requireTable(String name) {
        Table table = database.getTable(name);
        if (table == null)
            throw new IllegalArgumentException("Table does not exist: " + name);
        return table;
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
