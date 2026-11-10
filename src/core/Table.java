package core;

import java.io.Serializable;
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
