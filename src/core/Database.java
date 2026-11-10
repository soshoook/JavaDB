package core;

import java.io.Serializable;
import java.util.HashMap;
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

}
