package core;

import java.io.Serializable;

// описывает один столбец таблицы бд
// хранит название столбца, его тип данных и признак первичного ключа
public class Column implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private DataType type;
    private boolean primaryKey;

    public Column(String name, DataType type, boolean primaryKey){
        this.name = name;
        this.type = type;
        this.primaryKey = primaryKey;
    }

    public String getName(){
        return name;
    }

    public DataType getType(){
        return type;
    }

    public boolean getKey(){
        return primaryKey;
    }

}
