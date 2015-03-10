package de.jexp.jequel.jdbctest;

import java.sql.ResultSetMetaData;

public abstract class ResultSetWrapper {
    protected ResultSetData resultSetData;

    public ResultSetWrapper(ResultSetData resultSetData) {
        this.resultSetData = resultSetData;
    }

    public ResultSetData getResultSetData() {
        return resultSetData;
    }

    public <T> T get(Class<T> returnType, int columnIndex) {
        return resultSetData.get(returnType, columnIndex);
    }

    public <T> T get(Class<T> returnType, String columnName) {
        return resultSetData.get(returnType, columnName);
    }

    public boolean next() {
        return resultSetData.next();
    }

    public ResultSetMetaData getMetaData() {
        return resultSetData.getMetaData();
    }
}
