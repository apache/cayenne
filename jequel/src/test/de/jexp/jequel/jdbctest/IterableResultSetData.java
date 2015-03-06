package de.jexp.jequel.jdbctest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author mh14 @ jexp.de
 * @since 03.11.2007 13:38:12 (c) 2007 jexp.de
 */
public class IterableResultSetData extends AbstractResultSetData {
    protected final Iterable data;
    private Iterator dataIterator;
    private Object row;

    public IterableResultSetData(final Iterable data, final String[] columnNames) {
        super(columnNames);
        this.data = data;
    }

    public Object getValue(final int columnIndex) {
        if (row instanceof Iterable) {
            int column = 1;
            for (final Object value : (Iterable) row) {
                if (columnIndex == column++) {
                    return value;
                }
            }
        } else {
            if (columnIndex == 1) return row;
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
        final List<Class<?>> columnTypes = new ArrayList<Class<?>>();
        final Object firstRow = data.iterator().next();
        if (firstRow instanceof Iterable) {
            for (final Object firstRowValue : (Iterable) firstRow) {
                columnTypes.add(getClass(firstRowValue));
            }
        } else {
            columnTypes.add(getClass(firstRow));
        }
        return columnTypes;
    }

}
