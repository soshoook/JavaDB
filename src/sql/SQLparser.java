package sql;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import core.Column;
import core.DataType;
import core.Row;
import core.Table;

public class SQLparser {

    private static final Pattern WHERE = Pattern.compile("\\bWHERE\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SET = Pattern.compile("\\bSET\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SELECT_COLUMNS = Pattern.compile("^\\s*SELECT\\s+(.+?)\\s+FROM\\b", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern CONDITION = Pattern.compile("^(\\w+)\\s*(>=|<=|!=|=|>|<)\\s*(.+?)\\s*;?\\s*$", Pattern.DOTALL);

    public String getCommandType(String query){
        query = query.trim().toUpperCase();
        String command = query.split("\\s+")[0];
        if (command.equals("SELECT"))
            return "SELECT";
        else if (command.equals("INSERT"))
            return "INSERT";
        else if (command.equals("UPDATE") )
            return "UPDATE";
        else if (command.equals("DELETE"))
            return "DELETE";
        else
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

    public List<String> parseValues(String query) {
        String upQuery = query.toUpperCase();
        int valuesIndex = upQuery.indexOf("VALUES");
        if (valuesIndex == -1)
            throw new IllegalArgumentException("VALUES not found");

        int start = query.indexOf('(', valuesIndex);
        int end = query.lastIndexOf(')');
        if (start == -1 || end == -1 || end < start)
            throw new IllegalArgumentException("Incorrect VALUES format");

        String valuesPart = query.substring(start + 1, end);
        return splitValues(valuesPart);
    }

    public List<String> parseColumns(String query) {
        String upQuery = query.toUpperCase();
        int intoIndex = upQuery.indexOf("INTO");
        int valuesIndex = upQuery.indexOf("VALUES");
        int start = query.indexOf('(', intoIndex);
        if (start == -1 || start > valuesIndex)
            return null;
        int end = query.indexOf(')', start);
        if (end == -1)
            throw new IllegalArgumentException("Incorrect columns format");
        String columnsPart = query.substring(start + 1, end);
        List<String> columns = new ArrayList<>();
        for (String column : columnsPart.split(","))
            columns.add(column.trim());

        return columns;
    }

    public Row parseInsert(String query, Table table) {
        List<String> columns = parseColumns(query);
        List<String> values = parseValues(query);
        if (columns == null) {
            columns = new ArrayList<>();
            for (Column column : table.getColumn()) {
                columns.add(column.getName());
            }
        }

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

        return row;
    }

}
