package sql;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import core.Column;
import core.DataType;
import core.ForeignKey;
import core.Row;
import core.Table;


// разбор SQL запросов
// определяет тип команды, извлечение названия таблиц и столбцов, разбор WHERE, зн и группы зн
// обработка комады создания таблиц и индексов
public class SQLparser {

    private static final int CI = Pattern.CASE_INSENSITIVE;
    private static final Pattern WHERE = Pattern.compile("\\bWHERE\\b", CI);
    private static final Pattern SET = Pattern.compile("\\bSET\\b", CI);
    private static final Pattern VALUES = Pattern.compile("\\bVALUES\\b", CI);
    private static final Pattern SELECT_COLUMNS =
            Pattern.compile("^\\s*SELECT\\s+(.+?)\\s+FROM\\b", CI | Pattern.DOTALL);

    private static final Pattern CONDITION =
            Pattern.compile("^(\\w+)\\s*(>=|<=|!=|=|>|<)\\s*(.+?)\\s*;?\\s*$", Pattern.DOTALL);
    private static final Pattern CREATE_TABLE =
            Pattern.compile("^\\s*CREATE\\s+TABLE\\s+(\\w+)\\s*\\((.*)\\)\\s*;?\\s*$", CI | Pattern.DOTALL);
    private static final Pattern COLUMN_DEF =
            Pattern.compile("^(\\w+)\\s+(\\w+)(\\s+PRIMARY\\s+KEY)?$", CI);
    private static final Pattern FOREIGN_KEY_DEF =
            Pattern.compile("^FOREIGN\\s+KEY\\s*\\(\\s*(\\w+)\\s*\\)\\s*REFERENCES\\s+(\\w+)\\s*\\(\\s*(\\w+)\\s*\\)$", CI);
    private static final Pattern CREATE_INDEX =
            Pattern.compile("^\\s*CREATE\\s+INDEX\\s+(\\w+)\\s+ON\\s+(\\w+)\\s*\\(\\s*(\\w+)\\s*\\)\\s*;?\\s*$", CI);
    private static final Pattern DROP_INDEX =
            Pattern.compile("^\\s*DROP\\s+INDEX\\s+(\\w+)\\s+ON\\s+(\\w+)\\s*;?\\s*$", CI);


    public String getCommandType(String query){
        String[] parts = query.trim().toUpperCase().split("\\s+");
        String command = parts[0];

        if (command.equals("SELECT") || command.equals("INSERT")
                || command.equals("UPDATE") || command.equals("DELETE"))
            return command;

        if (command.equals("CREATE") || command.equals("DROP")) {
            if (parts.length > 1 && (parts[1].equals("TABLE") || parts[1].equals("INDEX")))
                return command + " " + parts[1];
            throw new IllegalArgumentException("Incorrect " + command + " format");
        }

        throw new IllegalArgumentException("Unknown SQL command");
    }


    public String getTableName(String query) {
        String[] parts = query.trim().split("\\s+");
        String command = parts[0].toUpperCase();
        String name;

        if (command.equals("SELECT")) {
            int i = 0;
            while (i < parts.length && !parts[i].equalsIgnoreCase("FROM"))
                i++;
            if (i + 1 >= parts.length)
                throw new IllegalArgumentException("FROM not found");
            name = parts[i + 1];
        }
        else if (command.equals("INSERT")) {
            if (parts.length < 3)
                throw new IllegalArgumentException("Incorrect INSERT format");
            name = parts[2];
        }
        else if (command.equals("UPDATE")) {
            if (parts.length < 2)
                throw new IllegalArgumentException("Incorrect UPDATE format");
            name = parts[1];
        }
        else if (command.equals("DELETE")) {
            if (parts.length < 3)
                throw new IllegalArgumentException("Incorrect DELETE format");
            name = parts[2];
        }
        else if (command.equals("CREATE") || command.equals("DROP")) {
            if (parts.length < 3)
                throw new IllegalArgumentException("Incorrect " + command + " format");
            name = parts[2];
        }
        else
            throw new IllegalArgumentException("Unknown SQL command");

        int paren = name.indexOf('(');
        if (paren != -1)
            name = name.substring(0, paren);

        return name.replace(";", "").trim();
    }


    public List<String> parseSelectColumns(String query){
        Matcher matcher = SELECT_COLUMNS.matcher(query);
        if (!matcher.find())
            throw new IllegalArgumentException("Incorrect SELECT format");

        String part = matcher.group(1).trim();
        if (part.equals("*"))
            return null;

        List<String> columns = new ArrayList<>();
        for (String column : part.split(","))
            columns.add(column.trim());
        return columns;
    }

    public String getSetClause(String query){
        Matcher set = SET.matcher(query);
        if (!set.find())
            throw new IllegalArgumentException("Incorrect UPDATE format");

        int start = set.end();
        Matcher where = WHERE.matcher(query);
        int end = where.find(start) ? where.start() : query.length();
        return query.substring(start, end).trim();
    }

    public String getCondition(String query){
        Matcher where = WHERE.matcher(query);
        if (!where.find())
            return null;
        return query.substring(where.end()).trim();
    }

    public Condition parseCondition(String query){
        String condition = getCondition(query);
        if (condition == null)
            return null;

        Matcher matcher = CONDITION.matcher(condition);
        if (!matcher.matches())
            throw new IllegalArgumentException("Incorrect WHERE condition");

        return new Condition(matcher.group(1), matcher.group(2), matcher.group(3));
    }

    public Object parseValue(String value, DataType type){
        value = value.trim();

        switch(type){
            case INTEGER:
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Incorrect integer value: " + value);
                }
            case DOUBLE:
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Incorrect number value: " + value);
                }
            case STRING:
                return value.replaceAll("^'|'$", "");
            case DATE:
                try {
                    return LocalDate.parse(value.replaceAll("^'|'$", ""));
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Incorrect date (use 'YYYY-MM-DD'): " + value);
                }
            case BOOLEAN:
                if (value.equalsIgnoreCase("true"))
                    return true;
                if (value.equalsIgnoreCase("false"))
                    return false;
                throw new IllegalArgumentException("Incorrect boolean value: " + value);
            default:
                throw new IllegalArgumentException("Unknown data type");
        }
    }

    private List<String> splitValues(String text){
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : text.toCharArray()) {
            if (c == '\'')
                inQuotes = !inQuotes;

            if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (inQuotes)
            throw new IllegalArgumentException("Unclosed quote in VALUES");

        result.add(current.toString());
        return result;
    }

    public List<List<String>> parseValueGroups(String query) {
        Matcher values = VALUES.matcher(query);
        if (!values.find())
            throw new IllegalArgumentException("VALUES not found");

        String rest = query.substring(values.end());
        List<List<String>> groups = new ArrayList<>();
        StringBuilder current = null;
        boolean inQuotes = false;

        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (c == '\'')
                inQuotes = !inQuotes;

            if (current == null) {
                if (c == '(' && !inQuotes)
                    current = new StringBuilder();
                else if (!Character.isWhitespace(c) && c != ',' && c != ';')
                    throw new IllegalArgumentException("Incorrect VALUES format");
            } else if (c == ')' && !inQuotes) {
                groups.add(splitValues(current.toString()));
                current = null;
            } else {
                current.append(c);
            }
        }

        if (current != null || inQuotes || groups.isEmpty())
            throw new IllegalArgumentException("Incorrect VALUES format");

        return groups;
    }

    public List<String> parseColumns(String query) {
        Matcher values = VALUES.matcher(query);
        if (!values.find())
            throw new IllegalArgumentException("VALUES not found");

        String head = query.substring(0, values.start());
        int start = head.indexOf('(');
        if (start == -1)
            return null;

        int end = head.lastIndexOf(')');
        if (end < start)
            throw new IllegalArgumentException("Incorrect columns format");

        List<String> columns = new ArrayList<>();
        for (String column : head.substring(start + 1, end).split(","))
            columns.add(column.trim());

        return columns;
    }

    public List<Row> parseInsert(String query, Table table) {
        List<String> columns = parseColumns(query);
        List<List<String>> groups = parseValueGroups(query);
        if (columns == null) {
            columns = new ArrayList<>();
            for (Column column : table.getColumn()) {
                columns.add(column.getName());
            }
        }

        List<Row> rows = new ArrayList<>();
        for (List<String> values : groups) {
            if (columns.size() != values.size())
                throw new IllegalArgumentException("Number of columns and values does not match");

            Row row = new Row();
            for (int i = 0; i < columns.size(); i++) {
                String columnName = columns.get(i).trim();
                Column column = null;
                for (Column c : table.getColumn()) {
                    if (c.getName().equals(columnName)) {
                        column = c;
                        break;
                    }
                }
                if (column == null)
                    throw new IllegalArgumentException("Unknown column: " + columnName);
                Object value = parseValue(values.get(i), column.getType());
                row.setValue(columnName, value);
            }
            rows.add(row);
        }

        return rows;
    }

    public Table parseCreateTable(String query){
        Matcher matcher = CREATE_TABLE.matcher(query);
        if (!matcher.matches())
            throw new IllegalArgumentException("Incorrect CREATE TABLE format");

        Table table = new Table(matcher.group(1));
        List<ForeignKey> foreignKeys = new ArrayList<>();
        int primaryKeys = 0;

        for (String definition : matcher.group(2).split(",")) {
            definition = definition.trim();

            Matcher fk = FOREIGN_KEY_DEF.matcher(definition);
            if (fk.matches()) {
                foreignKeys.add(new ForeignKey(fk.group(1), fk.group(2), fk.group(3)));
                continue;
            }

            Matcher col = COLUMN_DEF.matcher(definition);
            if (!col.matches())
                throw new IllegalArgumentException("Incorrect column definition: " + definition);

            String columnName = col.group(1);
            if (table.hasColumn(columnName))
                throw new IllegalArgumentException("Duplicate column: " + columnName);

            boolean primary = col.group(3) != null;
            if (primary)
                primaryKeys++;

            table.addColumn(new Column(columnName, parseType(col.group(2)), primary));
        }

        if (primaryKeys != 1)
            throw new IllegalArgumentException("Table must have exactly one PRIMARY KEY column");

        for (ForeignKey foreignKey : foreignKeys) {
            if (!table.hasColumn(foreignKey.getColumnName()))
                throw new IllegalArgumentException("Column does not exist: " + foreignKey.getColumnName());
            table.addForeignKey(foreignKey);
        }

        return table;
    }

    private DataType parseType(String type){
        switch (type.toUpperCase()) {
            case "INTEGER":
            case "INT":
                return DataType.INTEGER;
            case "DOUBLE":
            case "FLOAT":
            case "REAL":
                return DataType.DOUBLE;
            case "STRING":
            case "VARCHAR":
            case "TEXT":
                return DataType.STRING;
            case "DATE":
                return DataType.DATE;
            case "BOOLEAN":
            case "BOOL":
                return DataType.BOOLEAN;
            default:
                throw new IllegalArgumentException("Unknown data type: " + type);
        }
    }


    public String[] parseCreateIndex(String query){
        Matcher matcher = CREATE_INDEX.matcher(query);
        if (!matcher.matches())
            throw new IllegalArgumentException("Incorrect CREATE INDEX format");
        return new String[]{matcher.group(1), matcher.group(2), matcher.group(3)};
    }

    public String[] parseDropIndex(String query){
        Matcher matcher = DROP_INDEX.matcher(query);
        if (!matcher.matches())
            throw new IllegalArgumentException("Incorrect DROP INDEX format");
        return new String[]{matcher.group(1), matcher.group(2)};
    }

}
