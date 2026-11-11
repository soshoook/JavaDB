package core;

import java.io.Serializable;

public class ForeignKey implements Serializable {
    private static final long serialVersionUID = 1L;
    private String columnName;
    private String refTable;
    private String refColumn;

    public ForeignKey(String columnName, String refTable, String refColumn){
        this.columnName = columnName;
        this.refTable = refTable;
        this.refColumn = refColumn;
    }

    public String getColumnName(){
        return columnName;
    }

    public String getRefColumn(){
        return refColumn;
    }

    public String getRefTable(){
        return refTable;
    }

}
