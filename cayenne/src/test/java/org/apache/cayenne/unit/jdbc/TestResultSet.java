/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.unit.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Minimal in-memory JDBC {@link ResultSet} for tests. Replaces the parts of
 * {@code com.mockrunner.mock.jdbc.MockResultSet} that Cayenne tests rely on.
 *
 * <p>Configure by calling {@link #addColumn(String)} or {@link #addColumn(String, Object[])}
 * before reading. Then call {@link #next()} to advance the cursor.
 */
public class TestResultSet implements ResultSet {

    private final String name;
    private final List<String> columnNames = new ArrayList<>();
    private final List<List<Object>> rows = new ArrayList<>();
    private int cursor = -1;
    private boolean closed;
    private boolean lastWasNull;

    public TestResultSet(String name) {
        this.name = name;
    }

    public void addColumn(String columnName) {
        columnNames.add(columnName);
        // pad existing rows so their length matches column count
        for (List<Object> row : rows) {
            while (row.size() < columnNames.size()) {
                row.add(null);
            }
        }
    }

    public void addColumn(String columnName, Object[] values) {
        int colIdx = columnNames.size();
        columnNames.add(columnName);
        for (int i = 0; i < values.length; i++) {
            ensureRow(i);
            List<Object> row = rows.get(i);
            while (row.size() <= colIdx) {
                row.add(null);
            }
            row.set(colIdx, values[i]);
        }
    }

    public void addRow(Object[] values) {
        List<Object> row = new ArrayList<>(columnNames.size());
        for (int i = 0; i < columnNames.size(); i++) {
            row.add(i < values.length ? values[i] : null);
        }
        rows.add(row);
    }

    private void ensureRow(int index) {
        while (rows.size() <= index) {
            List<Object> row = new ArrayList<>(columnNames.size());
            for (int i = 0; i < columnNames.size(); i++) {
                row.add(null);
            }
            rows.add(row);
        }
    }

    private Object value(int columnIndex) throws SQLException {
        if (cursor < 0 || cursor >= rows.size()) {
            throw new SQLException("No current row");
        }
        if (columnIndex < 1 || columnIndex > columnNames.size()) {
            throw new SQLException("Invalid column index: " + columnIndex);
        }
        Object v = rows.get(cursor).get(columnIndex - 1);
        lastWasNull = (v == null);
        return v;
    }

    private int columnIndex(String columnLabel) throws SQLException {
        for (int i = 0; i < columnNames.size(); i++) {
            if (columnNames.get(i).equalsIgnoreCase(columnLabel)) {
                return i + 1;
            }
        }
        throw new SQLException("Unknown column: " + columnLabel);
    }

    @Override
    public boolean next() {
        cursor++;
        return cursor < rows.size();
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean wasNull() {
        return lastWasNull;
    }

    @Override
    public ResultSetMetaData getMetaData() {
        return new TestResultSetMetaData(columnNames);
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        return value(columnIndex);
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        return getObject(columnIndex(columnLabel));
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        Object v = value(columnIndex);
        return v == null ? null : v.toString();
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        return getString(columnIndex(columnLabel));
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        Object v = value(columnIndex);
        if (v == null) {
            return false;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof Number n) {
            return n.intValue() != 0;
        }
        return Boolean.parseBoolean(v.toString());
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        return getBoolean(columnIndex(columnLabel));
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        Object v = value(columnIndex);
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(v.toString());
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        return getInt(columnIndex(columnLabel));
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        Object v = value(columnIndex);
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(v.toString());
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        return getLong(columnIndex(columnLabel));
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        Object v = value(columnIndex);
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(v.toString());
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        return getDouble(columnIndex(columnLabel));
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        return (float) getDouble(columnIndex);
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        return getFloat(columnIndex(columnLabel));
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        return (short) getInt(columnIndex);
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        return getShort(columnIndex(columnLabel));
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        return (byte) getInt(columnIndex);
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        return getByte(columnIndex(columnLabel));
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        Object v = value(columnIndex);
        return v == null ? null : (byte[]) v;
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        return getBytes(columnIndex(columnLabel));
    }

    @Override public SQLWarning getWarnings() { return null; }
    @Override public void clearWarnings() {}
    @Override public String getCursorName() { return name; }
    @Override public int findColumn(String columnLabel) throws SQLException { return columnIndex(columnLabel); }
    @Override public boolean isBeforeFirst() { return cursor < 0; }
    @Override public boolean isAfterLast() { return cursor >= rows.size(); }
    @Override public boolean isFirst() { return cursor == 0; }
    @Override public boolean isLast() { return cursor == rows.size() - 1; }
    @Override public int getRow() { return cursor + 1; }
    @Override public int getType() { return TYPE_FORWARD_ONLY; }
    @Override public int getConcurrency() { return CONCUR_READ_ONLY; }
    @Override public Statement getStatement() { return null; }
    @Override public int getFetchDirection() { return FETCH_FORWARD; }
    @Override public int getFetchSize() { return 0; }
    @Override public int getHoldability() { return HOLD_CURSORS_OVER_COMMIT; }

    // --- everything below is unused by tests; throw to flag accidental reliance ---

    private static UnsupportedOperationException uoe() {
        return new UnsupportedOperationException("not implemented in TestResultSet");
    }

    @Override public BigDecimal getBigDecimal(int columnIndex) { throw uoe(); }
    @Override public BigDecimal getBigDecimal(String columnLabel) { throw uoe(); }
    @Override public BigDecimal getBigDecimal(int columnIndex, int scale) { throw uoe(); }
    @Override public BigDecimal getBigDecimal(String columnLabel, int scale) { throw uoe(); }
    @Override public Date getDate(int columnIndex) { throw uoe(); }
    @Override public Date getDate(String columnLabel) { throw uoe(); }
    @Override public Date getDate(int columnIndex, Calendar cal) { throw uoe(); }
    @Override public Date getDate(String columnLabel, Calendar cal) { throw uoe(); }
    @Override public Time getTime(int columnIndex) { throw uoe(); }
    @Override public Time getTime(String columnLabel) { throw uoe(); }
    @Override public Time getTime(int columnIndex, Calendar cal) { throw uoe(); }
    @Override public Time getTime(String columnLabel, Calendar cal) { throw uoe(); }
    @Override public Timestamp getTimestamp(int columnIndex) { throw uoe(); }
    @Override public Timestamp getTimestamp(String columnLabel) { throw uoe(); }
    @Override public Timestamp getTimestamp(int columnIndex, Calendar cal) { throw uoe(); }
    @Override public Timestamp getTimestamp(String columnLabel, Calendar cal) { throw uoe(); }
    @Override public InputStream getAsciiStream(int columnIndex) { throw uoe(); }
    @Override public InputStream getAsciiStream(String columnLabel) { throw uoe(); }
    @Override public InputStream getUnicodeStream(int columnIndex) { throw uoe(); }
    @Override public InputStream getUnicodeStream(String columnLabel) { throw uoe(); }
    @Override public InputStream getBinaryStream(int columnIndex) { throw uoe(); }
    @Override public InputStream getBinaryStream(String columnLabel) { throw uoe(); }
    @Override public Reader getCharacterStream(int columnIndex) { throw uoe(); }
    @Override public Reader getCharacterStream(String columnLabel) { throw uoe(); }
    @Override public Reader getNCharacterStream(int columnIndex) { throw uoe(); }
    @Override public Reader getNCharacterStream(String columnLabel) { throw uoe(); }
    @Override public String getNString(int columnIndex) { throw uoe(); }
    @Override public String getNString(String columnLabel) { throw uoe(); }
    @Override public Object getObject(int columnIndex, Map<String, Class<?>> map) { throw uoe(); }
    @Override public Object getObject(String columnLabel, Map<String, Class<?>> map) { throw uoe(); }
    @Override public <T> T getObject(int columnIndex, Class<T> type) { throw uoe(); }
    @Override public <T> T getObject(String columnLabel, Class<T> type) { throw uoe(); }
    @Override public Ref getRef(int columnIndex) { throw uoe(); }
    @Override public Ref getRef(String columnLabel) { throw uoe(); }
    @Override public Blob getBlob(int columnIndex) { throw uoe(); }
    @Override public Blob getBlob(String columnLabel) { throw uoe(); }
    @Override public Clob getClob(int columnIndex) { throw uoe(); }
    @Override public Clob getClob(String columnLabel) { throw uoe(); }
    @Override public NClob getNClob(int columnIndex) { throw uoe(); }
    @Override public NClob getNClob(String columnLabel) { throw uoe(); }
    @Override public Array getArray(int columnIndex) { throw uoe(); }
    @Override public Array getArray(String columnLabel) { throw uoe(); }
    @Override public URL getURL(int columnIndex) { throw uoe(); }
    @Override public URL getURL(String columnLabel) { throw uoe(); }
    @Override public RowId getRowId(int columnIndex) { throw uoe(); }
    @Override public RowId getRowId(String columnLabel) { throw uoe(); }
    @Override public SQLXML getSQLXML(int columnIndex) { throw uoe(); }
    @Override public SQLXML getSQLXML(String columnLabel) { throw uoe(); }
    @Override public void beforeFirst() { throw uoe(); }
    @Override public void afterLast() { throw uoe(); }
    @Override public boolean first() { throw uoe(); }
    @Override public boolean last() { throw uoe(); }
    @Override public boolean absolute(int row) { throw uoe(); }
    @Override public boolean relative(int rows) { throw uoe(); }
    @Override public boolean previous() { throw uoe(); }
    @Override public void setFetchDirection(int direction) { throw uoe(); }
    @Override public void setFetchSize(int rows) { throw uoe(); }
    @Override public boolean rowUpdated() { throw uoe(); }
    @Override public boolean rowInserted() { throw uoe(); }
    @Override public boolean rowDeleted() { throw uoe(); }
    @Override public void insertRow() { throw uoe(); }
    @Override public void updateRow() { throw uoe(); }
    @Override public void deleteRow() { throw uoe(); }
    @Override public void refreshRow() { throw uoe(); }
    @Override public void cancelRowUpdates() { throw uoe(); }
    @Override public void moveToInsertRow() { throw uoe(); }
    @Override public void moveToCurrentRow() { throw uoe(); }
    @Override public void updateNull(int columnIndex) { throw uoe(); }
    @Override public void updateNull(String columnLabel) { throw uoe(); }
    @Override public void updateBoolean(int columnIndex, boolean x) { throw uoe(); }
    @Override public void updateBoolean(String columnLabel, boolean x) { throw uoe(); }
    @Override public void updateByte(int columnIndex, byte x) { throw uoe(); }
    @Override public void updateByte(String columnLabel, byte x) { throw uoe(); }
    @Override public void updateShort(int columnIndex, short x) { throw uoe(); }
    @Override public void updateShort(String columnLabel, short x) { throw uoe(); }
    @Override public void updateInt(int columnIndex, int x) { throw uoe(); }
    @Override public void updateInt(String columnLabel, int x) { throw uoe(); }
    @Override public void updateLong(int columnIndex, long x) { throw uoe(); }
    @Override public void updateLong(String columnLabel, long x) { throw uoe(); }
    @Override public void updateFloat(int columnIndex, float x) { throw uoe(); }
    @Override public void updateFloat(String columnLabel, float x) { throw uoe(); }
    @Override public void updateDouble(int columnIndex, double x) { throw uoe(); }
    @Override public void updateDouble(String columnLabel, double x) { throw uoe(); }
    @Override public void updateBigDecimal(int columnIndex, BigDecimal x) { throw uoe(); }
    @Override public void updateBigDecimal(String columnLabel, BigDecimal x) { throw uoe(); }
    @Override public void updateString(int columnIndex, String x) { throw uoe(); }
    @Override public void updateString(String columnLabel, String x) { throw uoe(); }
    @Override public void updateNString(int columnIndex, String x) { throw uoe(); }
    @Override public void updateNString(String columnLabel, String x) { throw uoe(); }
    @Override public void updateBytes(int columnIndex, byte[] x) { throw uoe(); }
    @Override public void updateBytes(String columnLabel, byte[] x) { throw uoe(); }
    @Override public void updateDate(int columnIndex, Date x) { throw uoe(); }
    @Override public void updateDate(String columnLabel, Date x) { throw uoe(); }
    @Override public void updateTime(int columnIndex, Time x) { throw uoe(); }
    @Override public void updateTime(String columnLabel, Time x) { throw uoe(); }
    @Override public void updateTimestamp(int columnIndex, Timestamp x) { throw uoe(); }
    @Override public void updateTimestamp(String columnLabel, Timestamp x) { throw uoe(); }
    @Override public void updateAsciiStream(int columnIndex, InputStream x, int length) { throw uoe(); }
    @Override public void updateAsciiStream(String columnLabel, InputStream x, int length) { throw uoe(); }
    @Override public void updateAsciiStream(int columnIndex, InputStream x, long length) { throw uoe(); }
    @Override public void updateAsciiStream(String columnLabel, InputStream x, long length) { throw uoe(); }
    @Override public void updateAsciiStream(int columnIndex, InputStream x) { throw uoe(); }
    @Override public void updateAsciiStream(String columnLabel, InputStream x) { throw uoe(); }
    @Override public void updateBinaryStream(int columnIndex, InputStream x, int length) { throw uoe(); }
    @Override public void updateBinaryStream(String columnLabel, InputStream x, int length) { throw uoe(); }
    @Override public void updateBinaryStream(int columnIndex, InputStream x, long length) { throw uoe(); }
    @Override public void updateBinaryStream(String columnLabel, InputStream x, long length) { throw uoe(); }
    @Override public void updateBinaryStream(int columnIndex, InputStream x) { throw uoe(); }
    @Override public void updateBinaryStream(String columnLabel, InputStream x) { throw uoe(); }
    @Override public void updateCharacterStream(int columnIndex, Reader x, int length) { throw uoe(); }
    @Override public void updateCharacterStream(String columnLabel, Reader x, int length) { throw uoe(); }
    @Override public void updateCharacterStream(int columnIndex, Reader x, long length) { throw uoe(); }
    @Override public void updateCharacterStream(String columnLabel, Reader x, long length) { throw uoe(); }
    @Override public void updateCharacterStream(int columnIndex, Reader x) { throw uoe(); }
    @Override public void updateCharacterStream(String columnLabel, Reader x) { throw uoe(); }
    @Override public void updateNCharacterStream(int columnIndex, Reader x, long length) { throw uoe(); }
    @Override public void updateNCharacterStream(String columnLabel, Reader x, long length) { throw uoe(); }
    @Override public void updateNCharacterStream(int columnIndex, Reader x) { throw uoe(); }
    @Override public void updateNCharacterStream(String columnLabel, Reader x) { throw uoe(); }
    @Override public void updateObject(int columnIndex, Object x, int scaleOrLength) { throw uoe(); }
    @Override public void updateObject(String columnLabel, Object x, int scaleOrLength) { throw uoe(); }
    @Override public void updateObject(int columnIndex, Object x) { throw uoe(); }
    @Override public void updateObject(String columnLabel, Object x) { throw uoe(); }
    @Override public void updateRef(int columnIndex, Ref x) { throw uoe(); }
    @Override public void updateRef(String columnLabel, Ref x) { throw uoe(); }
    @Override public void updateBlob(int columnIndex, Blob x) { throw uoe(); }
    @Override public void updateBlob(String columnLabel, Blob x) { throw uoe(); }
    @Override public void updateBlob(int columnIndex, InputStream inputStream, long length) { throw uoe(); }
    @Override public void updateBlob(String columnLabel, InputStream inputStream, long length) { throw uoe(); }
    @Override public void updateBlob(int columnIndex, InputStream inputStream) { throw uoe(); }
    @Override public void updateBlob(String columnLabel, InputStream inputStream) { throw uoe(); }
    @Override public void updateClob(int columnIndex, Clob x) { throw uoe(); }
    @Override public void updateClob(String columnLabel, Clob x) { throw uoe(); }
    @Override public void updateClob(int columnIndex, Reader reader, long length) { throw uoe(); }
    @Override public void updateClob(String columnLabel, Reader reader, long length) { throw uoe(); }
    @Override public void updateClob(int columnIndex, Reader reader) { throw uoe(); }
    @Override public void updateClob(String columnLabel, Reader reader) { throw uoe(); }
    @Override public void updateNClob(int columnIndex, NClob nClob) { throw uoe(); }
    @Override public void updateNClob(String columnLabel, NClob nClob) { throw uoe(); }
    @Override public void updateNClob(int columnIndex, Reader reader, long length) { throw uoe(); }
    @Override public void updateNClob(String columnLabel, Reader reader, long length) { throw uoe(); }
    @Override public void updateNClob(int columnIndex, Reader reader) { throw uoe(); }
    @Override public void updateNClob(String columnLabel, Reader reader) { throw uoe(); }
    @Override public void updateArray(int columnIndex, Array x) { throw uoe(); }
    @Override public void updateArray(String columnLabel, Array x) { throw uoe(); }
    @Override public void updateRowId(int columnIndex, RowId x) { throw uoe(); }
    @Override public void updateRowId(String columnLabel, RowId x) { throw uoe(); }
    @Override public void updateSQLXML(int columnIndex, SQLXML xmlObject) { throw uoe(); }
    @Override public void updateSQLXML(String columnLabel, SQLXML xmlObject) { throw uoe(); }
    @Override public <T> T unwrap(Class<T> iface) { throw uoe(); }
    @Override public boolean isWrapperFor(Class<?> iface) { return false; }

    private static final class TestResultSetMetaData implements ResultSetMetaData {
        private final List<String> names;

        TestResultSetMetaData(List<String> names) {
            this.names = names;
        }

        @Override public int getColumnCount() { return names.size(); }
        @Override public String getColumnLabel(int column) { return names.get(column - 1); }
        @Override public String getColumnName(int column) { return names.get(column - 1); }
        @Override public int getColumnType(int column) { return Types.OTHER; }
        @Override public String getColumnTypeName(int column) { return "OTHER"; }
        @Override public String getColumnClassName(int column) { return Object.class.getName(); }
        @Override public int getPrecision(int column) { return 0; }
        @Override public int getScale(int column) { return 0; }
        @Override public int isNullable(int column) { return columnNullableUnknown; }
        @Override public boolean isAutoIncrement(int column) { return false; }
        @Override public boolean isCaseSensitive(int column) { return false; }
        @Override public boolean isSearchable(int column) { return false; }
        @Override public boolean isCurrency(int column) { return false; }
        @Override public boolean isSigned(int column) { return false; }
        @Override public int getColumnDisplaySize(int column) { return 0; }
        @Override public String getSchemaName(int column) { return ""; }
        @Override public String getTableName(int column) { return ""; }
        @Override public String getCatalogName(int column) { return ""; }
        @Override public boolean isReadOnly(int column) { return true; }
        @Override public boolean isWritable(int column) { return false; }
        @Override public boolean isDefinitelyWritable(int column) { return false; }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLFeatureNotSupportedException(); }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
