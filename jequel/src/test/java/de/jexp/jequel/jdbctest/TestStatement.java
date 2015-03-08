package de.jexp.jequel.jdbctest;

import java.sql.*;

/**
 * @author mh14 @ jexp.de
 * @since 03.11.2007 08:12:38 (c) 2007 jexp.de
 */
public class TestStatement implements Statement {
    private Connection connection;
    private ResultSet resultSet;
    private int changeCount;

    public TestStatement(final Connection connection, final ResultSet resultSet, final int changeCount) {
        this.connection = connection;
        this.resultSet = resultSet;
        this.changeCount = changeCount;
    }

    public ResultSet executeQuery(final String s) throws SQLException {
        return resultSet;
    }

    public int executeUpdate(final String s) throws SQLException {
        return changeCount;
    }

    public void close() throws SQLException {
    }

    public int getMaxFieldSize() throws SQLException {
        return 0;
    }

    public void setMaxFieldSize(final int i) throws SQLException {
    }

    public int getMaxRows() throws SQLException {
        return 0;
    }

    public void setMaxRows(final int i) throws SQLException {
    }

    public void setEscapeProcessing(final boolean b) throws SQLException {
    }

    public int getQueryTimeout() throws SQLException {
        return 0;
    }

    public void setQueryTimeout(final int i) throws SQLException {
    }

    public void cancel() throws SQLException {
    }

    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    public void clearWarnings() throws SQLException {
    }

    public void setCursorName(final String s) throws SQLException {
    }

    public boolean execute(final String s) throws SQLException {
        return false;
    }

    public ResultSet getResultSet() throws SQLException {
        return resultSet;
    }

    public int getUpdateCount() throws SQLException {
        return changeCount;
    }

    public boolean getMoreResults() throws SQLException {
        return false;
    }

    public void setFetchDirection(final int i) throws SQLException {
    }

    public int getFetchDirection() throws SQLException {
        return 0;
    }

    public void setFetchSize(final int i) throws SQLException {
    }

    public int getFetchSize() throws SQLException {
        return 0;
    }

    public int getResultSetConcurrency() throws SQLException {
        return 0;
    }

    public int getResultSetType() throws SQLException {
        return 0;
    }

    public void addBatch(final String s) throws SQLException {
    }

    public void clearBatch() throws SQLException {
    }

    public int[] executeBatch() throws SQLException {
        return new int[0];
    }

    public Connection getConnection() throws SQLException {
        return connection;
    }

    public boolean getMoreResults(final int i) throws SQLException {
        return false;
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        return null;
    }

    public int executeUpdate(final String s, final int i) throws SQLException {
        return changeCount;
    }

    public int executeUpdate(final String s, final int[] ints) throws SQLException {
        return changeCount;
    }

    public int executeUpdate(final String s, final String[] strings) throws SQLException {
        return changeCount;
    }

    public boolean execute(final String s, final int i) throws SQLException {
        return false;
    }

    public boolean execute(final String s, final int[] ints) throws SQLException {
        return false;
    }

    public boolean execute(final String s, final String[] strings) throws SQLException {
        return false;
    }

    public int getResultSetHoldability() throws SQLException {
        return 0;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return false;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {

    }

    @Override
    public boolean isPoolable() throws SQLException {
        return false;
    }

    @Override
    public void closeOnCompletion() throws SQLException {

    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
