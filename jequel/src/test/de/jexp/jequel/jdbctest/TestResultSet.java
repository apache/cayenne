package de.jexp.jequel.jdbctest;

import java.sql.ResultSetMetaData;

/**
 * @author mh14 @ jexp.de
 * @since 03.11.2007 14:20:13 (c) 2007 jexp.de
 */
public abstract class TestResultSet {
    protected final ResultSetData resultSetData;

    public TestResultSet(final ResultSetData resultSetData) {
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
