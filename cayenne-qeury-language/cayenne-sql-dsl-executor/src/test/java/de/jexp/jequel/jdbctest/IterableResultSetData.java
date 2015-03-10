package de.jexp.jequel.jdbctest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IterableResultSetData extends AbstractResultSetData {
    private final Iterable data;
    private Iterator dataIterator;
    private Object row;

    public IterableResultSetData(Iterable data, String[] columnNames) {
        super(columnNames);
        this.data = data;
    }

    public Object getValue(int columnIndex) {
        if (row instanceof Iterable) {
            int column = 1;
            for (Object value : (Iterable) row) {
                if (columnIndex == column++) {
                    return value;
                }
            }
        } else {
            if (columnIndex == 1) {
                return row;
            }
        }
        return null;
    }

    public boolean next() {
        if (dataIterator == null) {
            dataIterator = data.iterator();
        }
        if (dataIterator.hasNext()) {
            row = dataIterator.next();
            return true;
        } else {
            row = null;
            return false;
        }
    }

    // todo for null values iterate over all data to find the fitting class ?
    protected List<Class<?>> loadColumnTypes() {
        List<Class<?>> columnTypes = new ArrayList<Class<?>>();
        Object firstRow = data.iterator().next();
        if (firstRow instanceof Iterable) {
            for (Object firstRowValue : (Iterable) firstRow) {
                columnTypes.add(getClass(firstRowValue));
            }
        } else {
            columnTypes.add(getClass(firstRow));
        }
        return columnTypes;
    }

}
