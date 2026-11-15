package core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

//одна запись (строка) таблицы
// хранит значения столбцов в формате "название ст - значение"
public class Row implements Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String, Object> values;

    public Row(){
        this.values = new HashMap<>();
    }

    public void setValue(String column, Object value){
        values.put(column, value);
    }

    public Object getValue(String column){
        return values.get(column);
    }

    public Map<String, Object> getValues(){
        return values;
    }

}
