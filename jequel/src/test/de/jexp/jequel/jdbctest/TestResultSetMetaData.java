package de.jexp.jequel.jdbctest;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * @author mh14 @ jexp.de
 * @since 03.11.2007 10:21:52 (c) 2007 jexp.de
 */
public class TestResultSetMetaData implements ResultSetMetaData {
    private final ResultSetData resultSetData;

    public TestResultSetMetaData(final ResultSetData resultSetData) {
        this.resultSetData = resultSetData;
    }

    public int getColumnCount() throws SQLException {
        return resultSetData.getColumnCount();
    }

    public boolean isAutoIncrement(final int i) throws SQLException {
        return false;
    }

    public boolean isCaseSensitive(final int i) throws SQLException {
        return false;
    }

    public boolean isSearchable(final int i) throws SQLException {
        return false;
    }

    public boolean isCurrency(final int i) throws SQLException {
        return false;
    }

    public int isNullable(final int i) throws SQLException {
        return 0;
    }

    public boolean isSigned(final int i) throws SQLException {
        return false;
    }

    public int getColumnDisplaySize(final int i) throws SQLException {
        return getColumnName(i).length();
    }

    public String getColumnLabel(final int i) throws SQLException {
        return getColumnName(i);
    }

    public String getColumnName(final int i) throws SQLException {
        return resultSetData.getColumnName(i);
    }

    public String getSchemaName(final int i) throws SQLException {
        return null;
    }

    public int getPrecision(final int i) throws SQLException {
        return 0;
    }

    public int getScale(final int i) throws SQLException {
        return 0;
    }

    public String getTableName(final int i) throws SQLException {
        return null;
    }

    public String getCatalogName(final int i) throws SQLException {
        return null;
    }

    public int getColumnType(final int i) throws SQLException {
        return resultSetData.getColumnType(i);
    }

    public String getColumnTypeName(final int i) throws SQLException {
        return TypeNames.getTypeName(getColumnType(i));
    }

    public boolean isReadOnly(final int i) throws SQLException {
        return true;
    }

    public boolean isWritable(final int i) throws SQLException {
        return false;
    }

    public boolean isDefinitelyWritable(final int i) throws SQLException {
        return false;
    }

    public String getColumnClassName(final int i) throws SQLException {
        return resultSetData.getColumnClass(i).getName();
    }
}
