package de.jexp.jequel.jdbctest;

import java.util.ArrayList;
import java.util.List;

public class ArrayResultSetData extends AbstractResultSetData {
    private Object[] data;
    private int rowIndex = -1;

    public ArrayResultSetData(Object[] data, String[] columnNames) {
        super(columnNames);
        this.data = data;
    }

    public ArrayResultSetData(Object[] data, String[] columnNames, Class<?>[] columnTypes) {
        super(columnNames, columnTypes);
        this.data = data;
    }

    public Object getValue(int columnIndex) {
        Object row = data[rowIndex];
        if (row instanceof Object[]) {
            Object[] rowArray = (Object[]) row;
            if (columnIndex < rowArray.length) {
                return rowArray[columnIndex - 1];
            }
        } else {
            if (columnIndex == 1) {
                return row;
            }
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
        List<Class<?>> columnTypes = new ArrayList<Class<?>>();
        for (int col = 1; col <= getColumnCount(); col++) {
            columnTypes.add(getClass(getValue(col)));

        }
        return columnTypes;
    }

}