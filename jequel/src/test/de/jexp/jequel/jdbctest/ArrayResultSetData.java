package de.jexp.jequel.jdbctest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mh14 @ jexp.de
 * @since 03.11.2007 13:38:12 (c) 2007 jexp.de
 */
public class ArrayResultSetData extends AbstractResultSetData {
    protected final Object[] data;
    int rowIndex = -1;

    public ArrayResultSetData(final Object[] data, final String[] columnNames) {
        super(columnNames);
        this.data = data;
    }

    public ArrayResultSetData(final Object[] data, final String[] columnNames, final Class<?>[] columnTypes) {
        super(columnNames, columnTypes);
        this.data = data;
    }

    public Object getValue(final int columnIndex) {
        final Object row = data[rowIndex];
        if (row instanceof Object[]) {
            final Object[] rowArray = (Object[]) row;
            if (columnIndex < rowArray.length) {
                return rowArray[columnIndex - 1];
            }
        } else {
            if (columnIndex == 1) return row;
        }
        return null;
    }

    public boolean next() {
        if (rowIndex < data.length - 1) {
            rowIndex++;
            return true;
        }
        return false;
    }

    protected List<Class<?>> loadColumnTypes() {
        final List<Class<?>> columnTypes = new ArrayList<Class<?>>();
        for (int col = 1; col <= getColumnCount(); col++) {
            columnTypes.add(getClass(getValue(col)));

        }
        return columnTypes;
    }

}