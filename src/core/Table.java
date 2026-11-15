package core;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

// управление одной таблицей
public class Table implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private List<Column> columns;
    private List<Row> rows;
    private List<ForeignKey> foreignKeys;

    public Table(String name){
        this.name = name;
        this.columns = new ArrayList<>();
        this.rows = new ArrayList<>();
        this.foreignKeys = new ArrayList<>();
    }

    public String getName(){
        return name;
    }

    public List<Column> getColumn(){
        return columns;
    }

    public List<Row> getRow(){
        return rows;
    }

            // страховка на случай, если таблица загружена из старого файла
    public List<ForeignKey> getForeignKeys(){
        if (foreignKeys == null)
            foreignKeys = new ArrayList<>();
        return foreignKeys;
    }

    public void addColumn(Column column){
        columns.add(column);
    }

    public void addForeignKey(ForeignKey foreign_key){
        foreignKeys.add(foreign_key);
    }

    public void isCorrectType(Column column, Object value){
        if (value == null){
            if (column.getKey())
                throw new IllegalArgumentException("Primary key cannot be empty: " + column.getName());
            return;
        }

        boolean error = true;
        switch (column.getType()) {
            case DATE -> error = !(value instanceof LocalDate);
            case INTEGER -> error = !(value instanceof Integer);
            case STRING -> error = !(value instanceof String);
            case DOUBLE -> error = !(value instanceof Double);
            case BOOLEAN -> error = !(value instanceof Boolean);
        }
        if (error){
            throw new IllegalArgumentException("Incorrect type of value for column: " + column.getName());
        }
    }

    public void addRow(Row row){
        for (String nameColumn : row.getValues().keySet()){
            boolean exist = false;
            for (Column column : columns){
                if (Objects.equals(column.getName(), nameColumn)){
                    exist = true;
                    break;
                }
            }
            if (!exist){
                throw new IllegalArgumentException("Column does not exists: " + nameColumn);
            }
        }

        for (Column column : columns){
            Object value = row.getValue(column.getName());
            isCorrectType(column, value);

        }
        for (Column column : columns){
            if(column.getKey()){
                Object newID = row.getValue(column.getName());
                for (Row exist_row : rows){
                    Object exist_id = exist_row.getValue(column.getName());
                    if (Objects.equals(exist_id, newID)){
                        throw new IllegalArgumentException("This ID already exists: " + newID);
                    }
                }
            }
        }
        rows.add(row);
    }

    public Row findByPrimaryKey(Object key){
        for (Column column : columns){
            if (column.getKey()){
                for (Row row : rows){
                    if (Objects.equals(row.getValue(column.getName()),key))
                        return row;
                }
            }
        }
        return null;
    }

    public List<Row> findByColumn(String column_name, Object value){
        List <Row> result = new ArrayList<>();
        for (Row row : rows){
            if (Objects.equals(row.getValue(column_name), value))
                result.add(row);
        }
        return result;
    }

    public boolean deleteByPrimaryKey(Object key){
        for (Column column : columns){
            if (column.getKey()){
                Iterator <Row> iterator = rows.iterator();
                while (iterator.hasNext()){
                    Row row = iterator.next();
                    if (Objects.equals(row.getValue(column.getName()), key)){
                        iterator.remove();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public int deleteByColumn(String column_name, Object value){
        int count = 0;
        Iterator <Row> iterator = rows.iterator();
        while (iterator.hasNext()){
            Row row = iterator.next();
            if (Objects.equals(row.getValue(column_name), value)){
                iterator.remove();
                count += 1;
            }
        }
        return count;

    }

    public boolean updateByPrimaryKey(Object key, String column_name, Object new_value){
        Column prKeyColumn = null;
        Column targetColumn = null;

        for (Column column : columns){
            if (column.getKey())
                prKeyColumn = column;
            if (column.getName().equals(column_name))
                targetColumn = column;
        }

        if (prKeyColumn == null)
            throw new IllegalArgumentException("Primary key is not found");
        if (targetColumn == null)
            throw new IllegalArgumentException("Column does not exist");
        isCorrectType(targetColumn, new_value);

        for (Row row : rows){
            if (Objects.equals(row.getValue(prKeyColumn.getName()), key)){
                if (targetColumn.getKey()){
                    for (Row othRow : rows){
                        if (othRow != row && Objects.equals(othRow.getValue(column_name), new_value)){
                            throw new IllegalArgumentException("Primary key already exists");
                        }
                    }
                }
                row.setValue(column_name, new_value);
                return true;
            }
        }
        return false;
    }

}
