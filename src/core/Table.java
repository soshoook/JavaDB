package core;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// управление одной таблицей
public class Table implements Serializable {
    private String name;
    private List<Column> columns;
    private List<Row> rows;

    public Table(String name){
        this.name = name;
        this.columns = new ArrayList<>();
        this.rows = new ArrayList<>();
    }

    public void addColumn(Column column){
        columns.add(column);
    }

    public void addRow(Row row){
        for (String nameColumn : row.getValues().keySet()){
            boolean exist = false;
            for (Column column : columns){
                if (column.getName() == nameColumn){
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
            boolean error = false;
            switch (column.getType()){
                case DATE:
                    if(!(value instanceof LocalDate)){
                        error = true;
                        break;
                    }
                case INTEGER:
                    if(!(value instanceof Integer)){
                        error = true;
                        break;
                    }
                case STRING:
                    if(!(value instanceof String)){
                        error = true;
                        break;
                    }
                case DOUBLE:
                    if(!(value instanceof Double)) {
                        error = true;
                        break;
                    }
                case BOOLEAN:
                    if(!(value instanceof Boolean)){
                        error = true;
                        break;
                    }
            }
            if (!error){
                throw new IllegalArgumentException("Incorrect type of values for column: " + column.getName());
            }

        }
        for (Column column : columns){
            if(column.getKey()){
                Object newID = row.getValue(column.getName());
                for (Row exist_row : rows){
                    Object exist_id = exist_row.getValue(column.getName());
                    if(exist_id == newID){
                        throw new IllegalArgumentException("This ID already exists: " + newID);
                    }
                }
            }
        }
        rows.add(row);
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

}
