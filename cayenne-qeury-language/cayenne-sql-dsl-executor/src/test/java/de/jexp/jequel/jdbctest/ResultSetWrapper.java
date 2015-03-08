package de.jexp.jequel.jdbctest;

import java.sql.ResultSetMetaData;

public abstract class ResultSetWrapper {
    protected final ResultSetData resultSetData;

    public ResultSetWrapper(final ResultSetData resultSetData) {
        this.resultSetData = resultSetData;
    }

    public ResultSetData getResultSetData() {
        return resultSetData;
    }

    public <T> T get(final Class<T> returnType, final int columnIndex) {
        return resultSetData.get(returnType, columnIndex);
    }

    public <T> T get(final Class<T> returnType, final String columnName) {
        return resultSetData.get(returnType, columnName);
    }

    public boolean next() {
        return resultSetData.next();
    }

    public ResultSetMetaData getMetaData() {
        return resultSetData.getMetaData();
    }
}
