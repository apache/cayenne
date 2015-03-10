package de.jexp.jequel.jdbctest;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;

public class ResultSetStub extends ResultSetWrapper implements ResultSet {
    public ResultSetStub(ResultSetData resultSetData) {
        super(resultSetData);
    }

    public ResultSetStub(Object[] data, String... columnNames) {
        this(new ArrayResultSetData(data, columnNames));
    }

    public void close() throws SQLException {
    }

    public boolean wasNull() throws SQLException {
        return false;
    }

    public String getString(int i) throws SQLException {
        return get(String.class, i);
    }

    public boolean getBoolean(int i) throws SQLException {
        return get(Boolean.class, i);
    }

    public byte getByte(int i) throws SQLException {
        return get(Byte.class, i);
    }

    public short getShort(int i) throws SQLException {
        return get(Short.class, i);
    }

    public int getInt(int i) throws SQLException {
        return get(Integer.class, i);
    }

    public long getLong(int i) throws SQLException {
        return get(Long.class, i);
    }

    public float getFloat(int i) throws SQLException {
        return get(Float.class, i);
    }

    public double getDouble(int i) throws SQLException {
        return get(Double.class, i);
    }

    @Deprecated
    public BigDecimal getBigDecimal(int i, int i1) throws SQLException {
        return get(BigDecimal.class, i);
    }

    public byte[] getBytes(int i) throws SQLException {
        return get(byte[].class, i);
    }

    public Date getDate(int i) throws SQLException {
        return get(Date.class, i);
    }

    public Time getTime(int i) throws SQLException {
        return get(Time.class, i);
    }

    public Timestamp getTimestamp(int i) throws SQLException {
        return get(Timestamp.class, i);
    }

    public InputStream getAsciiStream(int i) throws SQLException {
        return get(InputStream.class, i);
    }

    @Deprecated
    public InputStream getUnicodeStream(int i) throws SQLException {
        return get(InputStream.class, i);
    }

    public InputStream getBinaryStream(int i) throws SQLException {
        return get(InputStream.class, i);
    }

    public String getString(String i) throws SQLException {
        return get(String.class, i);
    }

    public boolean getBoolean(String i) throws SQLException {
        return get(Boolean.class, i);
    }

    public byte getByte(String i) throws SQLException {
        return get(Byte.class, i);
    }

    public short getShort(String i) throws SQLException {
        return get(Short.class, i);
    }

    public int getInt(String i) throws SQLException {
        return get(Integer.class, i);
    }

    public long getLong(String i) throws SQLException {
        return get(Long.class, i);
    }

    public float getFloat(String i) throws SQLException {
        return get(Float.class, i);
    }

    public double getDouble(String i) throws SQLException {
        return get(Double.class, i);
    }

    @Deprecated
    public BigDecimal getBigDecimal(String i, int i1) throws SQLException {
        return get(BigDecimal.class, i);
    }

    public byte[] getBytes(String i) throws SQLException {
        return get(byte[].class, i);
    }

    public Date getDate(String i) throws SQLException {
        return get(Date.class, i);
    }

    public Time getTime(String i) throws SQLException {
        return get(Time.class, i);
    }

    public Timestamp getTimestamp(String i) throws SQLException {
        return get(Timestamp.class, i);
    }

    public InputStream getAsciiStream(String i) throws SQLException {
        return get(InputStream.class, i);
    }

    @Deprecated
    public InputStream getUnicodeStream(String i) throws SQLException {
        return get(InputStream.class, i);
    }

    public InputStream getBinaryStream(String i) throws SQLException {
        return get(InputStream.class, i);
    }

    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    public void clearWarnings() throws SQLException {
    }

    public String getCursorName() throws SQLException {
        return null;
    }

    public Object getObject(int i) throws SQLException {
        return get(Object.class, i);
    }

    public Object getObject(String s) throws SQLException {
        return get(Object.class, s);
    }

    public int findColumn(String s) throws SQLException {
        return 0; // TODO
    }

    public Reader getCharacterStream(int i) throws SQLException {
        return get(Reader.class, i);
    }

    public Reader getCharacterStream(String s) throws SQLException {
        return get(Reader.class, s);
    }

    public BigDecimal getBigDecimal(int i) throws SQLException {
        return get(BigDecimal.class, i);
    }

    public BigDecimal getBigDecimal(String s) throws SQLException {
        return get(BigDecimal.class, s);
    }

    public boolean isBeforeFirst() throws SQLException {
        return false;
    }

    public boolean isAfterLast() throws SQLException {
        return false;
    }

    public boolean isFirst() throws SQLException {
        return false;
    }

    public boolean isLast() throws SQLException {
        return false;
    }

    public void beforeFirst() throws SQLException {

    }

    public void afterLast() throws SQLException {

    }

    public boolean first() throws SQLException {
        return false;
    }

    public boolean last() throws SQLException {
        return false;
    }

    public int getRow() throws SQLException {
        return 0;
    }

    public boolean absolute(int i) throws SQLException {
        return false;
    }

    public boolean relative(int i) throws SQLException {
        return false;
    }

    public boolean previous() throws SQLException {
        return false;
    }

    public void setFetchDirection(int i) throws SQLException {
    }

    public int getFetchDirection() throws SQLException {
        return 0;
    }

    public void setFetchSize(int i) throws SQLException {
    }

    public int getFetchSize() throws SQLException {
        return 0;
    }

    public int getType() throws SQLException {
        return 0;
    }

    public int getConcurrency() throws SQLException {
        return 0;
    }

    public boolean rowUpdated() throws SQLException {
        return false;
    }

    public boolean rowInserted() throws SQLException {
        return false;
    }

    public boolean rowDeleted() throws SQLException {
        return false;
    }

    public void updateNull(int i) throws SQLException {
    }

    public void updateBoolean(int i, boolean b) throws SQLException {
    }

    public void updateByte(int i, byte b) throws SQLException {
    }

    public void updateShort(int i, short i1) throws SQLException {
    }

    public void updateInt(int i, int i1) throws SQLException {
    }

    public void updateLong(int i, long l) throws SQLException {
    }

    public void updateFloat(int i, float v) throws SQLException {
    }

    public void updateDouble(int i, double v) throws SQLException {
    }

    public void updateBigDecimal(int i, BigDecimal bigDecimal) throws SQLException {
    }

    public void updateString(int i, String s) throws SQLException {
    }

    public void updateBytes(int i, byte[] bytes) throws SQLException {
    }

    public void updateDate(int i, Date date) throws SQLException {
    }

    public void updateTime(int i, Time time) throws SQLException {
    }

    public void updateTimestamp(int i, Timestamp timestamp) throws SQLException {
    }

    public void updateAsciiStream(int i, InputStream inputStream, int i1) throws SQLException {
    }

    public void updateBinaryStream(int i, InputStream inputStream, int i1) throws SQLException {
    }

    public void updateCharacterStream(int i, Reader reader, int i1) throws SQLException {
    }

    public void updateObject(int i, Object o, int i1) throws SQLException {
    }

    public void updateObject(int i, Object o) throws SQLException {
    }

    public void updateNull(String s) throws SQLException {
    }

    public void updateBoolean(String s, boolean b) throws SQLException {
    }

    public void updateByte(String s, byte b) throws SQLException {
    }

    public void updateShort(String s, short i) throws SQLException {
    }

    public void updateInt(String s, int i) throws SQLException {
    }

    public void updateLong(String s, long l) throws SQLException {
    }

    public void updateFloat(String s, float v) throws SQLException {
    }

    public void updateDouble(String s, double v) throws SQLException {
    }

    public void updateBigDecimal(String s, BigDecimal bigDecimal) throws SQLException {
    }

    public void updateString(String s, String s1) throws SQLException {
    }

    public void updateBytes(String s, byte[] bytes) throws SQLException {
    }

    public void updateDate(String s, Date date) throws SQLException {
    }

    public void updateTime(String s, Time time) throws SQLException {
    }

    public void updateTimestamp(String s, Timestamp timestamp) throws SQLException {
    }

    public void updateAsciiStream(String s, InputStream inputStream, int i) throws SQLException {
    }

    public void updateBinaryStream(String s, InputStream inputStream, int i) throws SQLException {
    }

    public void updateCharacterStream(String s, Reader reader, int i) throws SQLException {
    }

    public void updateObject(String s, Object o, int i) throws SQLException {
    }

    public void updateObject(String s, Object o) throws SQLException {
    }

    public void insertRow() throws SQLException {
    }

    public void updateRow() throws SQLException {
    }

    public void deleteRow() throws SQLException {
    }

    public void refreshRow() throws SQLException {
    }

    public void cancelRowUpdates() throws SQLException {
    }

    public void moveToInsertRow() throws SQLException {
    }

    public void moveToCurrentRow() throws SQLException {
    }

    public Statement getStatement() throws SQLException {
        return null; // TODO
    }

    public Object getObject(int i, Map<String, Class<?>> stringClassMap) throws SQLException {
        return get(Object.class, i);  // TODO
    }

    public Ref getRef(int i) throws SQLException {
        return get(Ref.class, i);
    }

    public Blob getBlob(int i) throws SQLException {
        return get(Blob.class, i);
    }

    public Clob getClob(int i) throws SQLException {
        return get(Clob.class, i);
    }

    public Array getArray(int i) throws SQLException {
        return get(Array.class, i);
    }

    public Object getObject(String s, Map<String, Class<?>> stringClassMap) throws SQLException {
        return get(Object.class, s); // TODO
    }

    public Ref getRef(String s) throws SQLException {
        return get(Ref.class, s);
    }

    public Blob getBlob(String s) throws SQLException {
        return get(Blob.class, s);
    }

    public Clob getClob(String s) throws SQLException {
        return get(Clob.class, s);
    }

    public Array getArray(String s) throws SQLException {
        return get(Array.class, s);
    }

    public Date getDate(int i, Calendar calendar) throws SQLException {
        return getDate(i);
    }

    public Date getDate(String s, Calendar calendar) throws SQLException {
        return getDate(s);
    }

    public Time getTime(int i, Calendar calendar) throws SQLException {
        return getTime(i);
    }

    public Time getTime(String s, Calendar calendar) throws SQLException {
        return getTime(s);
    }

    public Timestamp getTimestamp(int i, Calendar calendar) throws SQLException {
        return getTimestamp(i);
    }

    public Timestamp getTimestamp(String s, Calendar calendar) throws SQLException {
        return getTimestamp(s);
    }

    public URL getURL(int i) throws SQLException {
        return get(URL.class, i);
    }

    public URL getURL(String s) throws SQLException {
        return get(URL.class, s);
    }

    public void updateRef(int i, Ref ref) throws SQLException {
    }

    public void updateRef(String s, Ref ref) throws SQLException {
    }

    public void updateBlob(int i, Blob blob) throws SQLException {
    }

    public void updateBlob(String s, Blob blob) throws SQLException {
    }

    public void updateClob(int i, Clob clob) throws SQLException {
    }

    public void updateClob(String s, Clob clob) throws SQLException {
    }

    public void updateArray(int i, Array array) throws SQLException {
    }

    public void updateArray(String s, Array array) throws SQLException {
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {

    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {

    }

    @Override
    public int getHoldability() throws SQLException {
        return 0;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return false;
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {

    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {

    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {

    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {

    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {

    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {

    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {

    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {

    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {

    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {

    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {

    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {

    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {

    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {

    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {

    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {

    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {

    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {

    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {

    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {

    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {

    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {

    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {

    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {

    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {

    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {

    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {

    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {

    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {

    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {

    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {

    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {

    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {

    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {

    }

    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        return null;
    }

    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        return null;
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
