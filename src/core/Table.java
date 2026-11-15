package core;

import index.Index;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


// управление одной таблицы бд
// добавление, удаление, поиск и обновление записей, проверка типов данных
// уникальность первичного ключа, создание и удаление индексов
public class Table implements Serializable, Iterable<Row> {
    private static final long serialVersionUID = 3L;

    private String name;
    private List<Column> columns;
    private List<Row> rows;
    private List<ForeignKey> foreignKeys;
    private Map<String, Index> indexes;

    public Table(String name){
        this.name = name;
        this.columns = new ArrayList<>();
        this.rows = new ArrayList<>();
        this.foreignKeys = new ArrayList<>();
        this.indexes = new LinkedHashMap<>();
    }

    public String getName(){
        return name;
    }

    public List<Column> getColumn(){
        return columns;
    }

    public List<Row> getRow(){
        return Collections.unmodifiableList(rows);
    }

    public int rowCount(){
        return rows.size();
    }

    public List<ForeignKey> getForeignKeys(){
        if (foreignKeys == null)
            foreignKeys = new ArrayList<>();
        return foreignKeys;
    }

    public Map<String, Index> getIndexes(){
        if (indexes == null)
            indexes = new LinkedHashMap<>();
        return indexes;
    }

    @Override
    public Iterator<Row> iterator(){
        return Collections.unmodifiableList(rows).iterator();
    }

    public void addColumn(Column column){
        columns.add(column);
        if (column.getKey())
            createIndex("pk_" + name, column.getName(), true);
    }

    public void addForeignKey(ForeignKey foreign_key){
        getForeignKeys().add(foreign_key);
    }

    public boolean hasColumn(String columnName){
        for (Column column : columns){
            if (column.getName().equals(columnName))
                return true;
        }
        return false;
    }

    public Index createIndex(String indexName, String columnName){
        return createIndex(indexName, columnName, false);
    }

    private Index createIndex(String indexName, String columnName, boolean primary){
        if (!hasColumn(columnName))
            throw new IllegalArgumentException("Column does not exist: " + columnName);
        if (getIndexes().containsKey(indexName))
            throw new IllegalArgumentException("Index already exists: " + indexName);

        Index existing = getIndexOnColumn(columnName);
        if (existing != null)
            throw new IllegalArgumentException(
                "Column " + columnName + " already has an index: " + existing.getName());

        Index index = new Index(indexName, columnName, primary);
        for (Row row : rows)
            index.add(row.getValue(columnName), row);

        getIndexes().put(indexName, index);
        return index;
    }

    public void dropIndex(String indexName){
        Index index = getIndexes().get(indexName);
        if (index == null)
            throw new IllegalArgumentException("Index does not exist: " + indexName);
        if (index.isPrimary())
            throw new IllegalArgumentException("Cannot drop primary key index: " + indexName);

        getIndexes().remove(indexName);
    }

    public Index getIndexOnColumn(String columnName){
        for (Index index : getIndexes().values()){
            if (index.getColumn().equals(columnName))
                return index;
        }
        return null;
    }

    public void isCorrectType(Column column, Object value){
        if (value == null){
            if (column.getKey())
                throw new IllegalArgumentException("Primary key cannot be empty: " + column.getName());
            return;
        }

        boolean error = true;
        switch (column.getType()) {
            case DATE:
                error = !(value instanceof LocalDate);
                break;
            case INTEGER:
                error = !(value instanceof Integer);
                break;
            case STRING:
                error = !(value instanceof String);
                break;
            case DOUBLE:
                error = !(value instanceof Double);
                break;
            case BOOLEAN:
                error = !(value instanceof Boolean);
                break;
        }
        if (error){
            throw new IllegalArgumentException("Incorrect type of value for column: " + column.getName());
        }
    }

    public void addRow(Row row){
        for (String nameColumn : row.getValues().keySet()){
            if (!hasColumn(nameColumn))
                throw new IllegalArgumentException("Column does not exists: " + nameColumn);
        }

        for (Column column : columns){
            Object value = row.getValue(column.getName());
            isCorrectType(column, value);
        }

        for (Column column : columns){
            if(column.getKey()){
                Object newID = row.getValue(column.getName());
                if (containsValue(column.getName(), newID))
                    throw new IllegalArgumentException("This ID already exists: " + newID);
            }
        }

        rows.add(row);
        for (Index index : getIndexes().values())
            index.add(row.getValue(index.getColumn()), row);
    }

    private boolean containsValue(String columnName, Object value){
        Index index = getIndexOnColumn(columnName);
        if (index != null)
            return index.contains(value);

        for (Row row : rows){
            if (Objects.equals(row.getValue(columnName), value))
                return true;
        }
        return false;
    }

    public Row findByPrimaryKey(Object key){
        for (Column column : columns){
            if (column.getKey()){
                List<Row> found = findByColumn(column.getName(), key);
                return found.isEmpty() ? null : found.get(0);
            }
        }
        return null;
    }

    public List<Row> findByColumn(String column_name, Object value){
        Index index = getIndexOnColumn(column_name);
        if (index != null)
            return index.find(value);

        List <Row> result = new ArrayList<>();
        for (Row row : rows){
            if (Objects.equals(row.getValue(column_name), value))
                result.add(row);
        }
        return result;
    }

    public void updateValue(Row row, String columnName, Object newValue){
        Index index = getIndexOnColumn(columnName);
        if (index != null)
            index.remove(row.getValue(columnName), row);

        row.setValue(columnName, newValue);

        if (index != null)
            index.add(newValue, row);
    }

    public void deleteRows(Collection<Row> toDelete){
        if (toDelete.isEmpty())
            return;

        for (Row row : toDelete){
            for (Index index : getIndexes().values())
                index.remove(row.getValue(index.getColumn()), row);
        }

        Set<Row> set = Collections.newSetFromMap(new IdentityHashMap<Row, Boolean>());
        set.addAll(toDelete);
        rows.removeAll(set);
    }

    public boolean deleteByPrimaryKey(Object key){
        Row row = findByPrimaryKey(key);
        if (row == null)
            return false;

        deleteRows(Collections.singletonList(row));
        return true;
    }

    public int deleteByColumn(String column_name, Object value){
        List<Row> found = findByColumn(column_name, value);
        deleteRows(found);
        return found.size();
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

        Row row = findByPrimaryKey(key);
        if (row == null)
            return false;

        if (targetColumn.getKey()){
            for (Row other : findByColumn(column_name, new_value)){
                if (other != row)
                    throw new IllegalArgumentException("Primary key already exists");
            }
        }

        updateValue(row, column_name, new_value);
        return true;
    }

}
