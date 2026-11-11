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
            Table refTable = getTable(foreignKey.getRefTable());
            if (refTable == null)
                throw new IllegalArgumentException("Referenced table does not exist");
        
            Object value = row.getValue(foreignKey.getColumnName());
            List <Row> match = refTable.findByColumn(foreignKey.getRefColumn(), value);
            if (match.isEmpty())
                return false;

        }
        return true;
    }

}
