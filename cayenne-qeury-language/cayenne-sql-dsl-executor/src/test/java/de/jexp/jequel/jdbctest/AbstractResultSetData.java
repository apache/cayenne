package de.jexp.jequel.jdbctest;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author mh14 @ jexp.de
 * @since 03.11.2007 14:03:28 (c) 2007 jexp.de
 */
public abstract class AbstractResultSetData implements ResultSetData {
    protected List<String> columnNames;
    private List<Class<?>> columnTypes = new ArrayList<Class<?>>();

    public AbstractResultSetData(String[] columnNames) {
        this.columnNames = Arrays.asList(columnNames);
    }

    public AbstractResultSetData(String[] columnNames, Class<?>[] columnTypes) {
        this.columnNames = Arrays.asList(columnNames);
        this.columnTypes.addAll(Arrays.asList(columnTypes));
    }

    public ResultSetMetaData getMetaData() {
        return new ResultSetMetaDataStub(this);
    }

    public String getColumnName(int col) {
        return columnNames.get(col - 1);
    }

    public int getColumnType(int col) {
        return TypeNames.getTypeForClass(getColumnClass(col));
    }

    public Class getColumnClass(int col) {
        return getColumnTypes().get(col - 1);
    }

    public int getColumnCount() {
        return getColumnNames().size();
    }

    protected List<Class<?>> getColumnTypes() {
        if (columnTypes.isEmpty()) {
            columnTypes.addAll(loadColumnTypes());
        }
        return columnTypes;
    }

    protected abstract List<Class<?>> loadColumnTypes();

    protected Class<?> getClass(Object value) {
        return value != null ? value.getClass() : Object.class;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public <T> T get(Class<T> returnType, String columnName) {
        return get(returnType, columnNames.indexOf(columnName) + 1);
    }

    public <T> T get(Class<T> returnType, int columnIndex) {
        return (T) getValue(columnIndex);
    }

    public abstract Object getValue(int columnIndex);
}
