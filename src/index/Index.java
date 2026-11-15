package index;

import core.Row;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;


// индекс одного столбца таблицы
// позволяет выполнять поиск по точному значению и диапазону значений, добавлять/удалять записи из индекса
public class Index implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String column;
    private final boolean primary;
    private final TreeMap<Object, List<Row>> tree = new TreeMap<>();

    public Index(String name, String column, boolean primary) {
        this.name = name;
        this.column = column;
        this.primary = primary;
    }

    public String getName() {
        return name;
    }

    public String getColumn() {
        return column;
    }

    public boolean isPrimary() {
        return primary;
    }

    public int size() {
        return tree.size();
    }

    public void add(Object key, Row row) {
        if (key == null)
            return;

        List<Row> rows = tree.get(key);
        if (rows == null) {
            rows = new ArrayList<>(1);
            tree.put(key, rows);
        }
        rows.add(row);
    }

    public void remove(Object key, Row row) {
        if (key == null)
            return;

        List<Row> rows = tree.get(key);
        if (rows == null)
            return;

        Iterator<Row> iterator = rows.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == row) {
                iterator.remove();
                break;
            }
        }

        if (rows.isEmpty())
            tree.remove(key);
    }

    public boolean contains(Object key) {
        return key != null && tree.containsKey(key);
    }

    public List<Row> find(Object key) {
        if (key == null)
            return new ArrayList<>();

        List<Row> rows = tree.get(key);
        if (rows == null)
            return new ArrayList<>();

        return new ArrayList<>(rows);
    }

    public List<Row> range(Object from, boolean fromInclusive, Object to, boolean toInclusive) {
        NavigableMap<Object, List<Row>> view;

        if (from != null && to != null) {
            @SuppressWarnings("unchecked")
            int order = ((Comparable<Object>) from).compareTo(to);
            if (order > 0)
                return new ArrayList<>();
            view = tree.subMap(from, fromInclusive, to, toInclusive);
        } else if (from != null) {
            view = tree.tailMap(from, fromInclusive);
        } else if (to != null) {
            view = tree.headMap(to, toInclusive);
        } else {
            view = tree;
        }

        List<Row> result = new ArrayList<>();
        for (List<Row> rows : view.values())
            result.addAll(rows);
        return result;
    }
}
