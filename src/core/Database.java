package core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// управление таблицами
public class Database implements Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String, Table> tables;

    public Database(){
        this.tables = new HashMap<>();
    }

    public void addTable(Table table){
        tables.put(table.getName(), table);
    }

    public void addRow(String tableName, Row row){
        Table table = getTable(tableName);
        if (table == null)
            throw new IllegalArgumentException("Table does not exists: " + tableName);

        if (!isForeignKeyValid(table, row))
            throw new IllegalArgumentException("Foreign key does not exists");
        table.addRow(row);
        
    }

    public Table getTable(String name){
        return tables.get(name);
    }

    public boolean hasTable(String name){
        return tables.containsKey(name);
    }

    public Map<String, Table> getTables(){
        return tables;
    }

    public boolean isForeignKeyValid(Table table, Row row){
        for (ForeignKey foreignKey : table.getForeignKeys()){
            if (!isValueValid(foreignKey, row.getValue(foreignKey.getColumnName())))
                return false;
        }
        return true;
    }

    public boolean isValueValid(ForeignKey foreignKey, Object value){
        Table refTable = getTable(foreignKey.getRefTable());
        if (refTable == null)
            throw new IllegalArgumentException("Referenced table does not exist");

        if (value == null)
            return true;

        return !refTable.findByColumn(foreignKey.getRefColumn(), value).isEmpty();
    }

    public boolean isReferenced(Table table, Row row){
        for (Table other : tables.values()){
            for (ForeignKey foreignKey : other.getForeignKeys()){
                if (!foreignKey.getRefTable().equals(table.getName()))
                    continue;

                Object refValue = row.getValue(foreignKey.getRefColumn());
                if (refValue == null)
                    continue;

                List<Row> children = other.findByColumn(foreignKey.getColumnName(), refValue);
                if (!children.isEmpty())
                    return true;
            }
        }
        return false;
    }

}
