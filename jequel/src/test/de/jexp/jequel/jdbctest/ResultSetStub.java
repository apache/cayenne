package de.jexp.jequel.jdbctest;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;

/**
 * @author mh14 @ jexp.de
 * @since 03.11.2007 14:21:04 (c) 2007 jexp.de
 */
public class ResultSetStub extends TestResultSet implements ResultSet {
    public ResultSetStub(final ResultSetData resultSetData) {
        super(resultSetData);
    }

    public ResultSetStub(final Object[] data, final String... columnNames) {
        this(new ArrayResultSetData(data, columnNames));
    }

    public void close() throws SQLException {
    }

    public boolean wasNull() throws SQLException {
        return false;
    }

    public String getString(final int i) throws SQLException {
        return get(String.class, i);
    }

    public boolean getBoolean(final int i) throws SQLException {
        return get(Boolean.class, i);
    }

    public byte getByte(final int i) throws SQLException {
        return get(Byte.class, i);
    }

    public short getShort(final int i) throws SQLException {
        return get(Short.class, i);
    }

    public int getInt(final int i) throws SQLException {
        return get(Integer.class, i);
    }

    public long getLong(final int i) throws SQLException {
        return get(Long.class, i);
    }

    public float getFloat(final int i) throws SQLException {
        return get(Float.class, i);
    }

    public double getDouble(final int i) throws SQLException {
        return get(Double.class, i);
    }

    @Deprecated
    public BigDecimal getBigDecimal(final int i, final int i1) throws SQLException {
        return get(BigDecimal.class, i);
    }

    public byte[] getBytes(final int i) throws SQLException {
        return get(byte[].class, i);
    }

    public Date getDate(final int i) throws SQLException {
        return get(Date.class, i);
    }

    public Time getTime(final int i) throws SQLException {
        return get(Time.class, i);
    }

    public Timestamp getTimestamp(final int i) throws SQLException {
        return get(Timestamp.class, i);
    }

    public InputStream getAsciiStream(final int i) throws SQLException {
        return get(InputStream.class, i);
    }

    @Deprecated
    public InputStream getUnicodeStream(final int i) throws SQLException {
        return get(InputStream.class, i);
    }

    public InputStream getBinaryStream(final int i) throws SQLException {
        return get(InputStream.class, i);
    }

    public String getString(final String i) throws SQLException {
        return get(String.class, i);
    }

    public boolean getBoolean(final String i) throws SQLException {
        return get(Boolean.class, i);
    }

    public byte getByte(final String i) throws SQLException {
        return get(Byte.class, i);
    }

    public short getShort(final String i) throws SQLException {
        return get(Short.class, i);
    }

    public int getInt(final String i) throws SQLException {
        return get(Integer.class, i);
    }

    public long getLong(final String i) throws SQLException {
        return get(Long.class, i);
    }

    public float getFloat(final String i) throws SQLException {
        return get(Float.class, i);
    }

    public double getDouble(final String i) throws SQLException {
        return get(Double.class, i);
    }

    @Deprecated
    public BigDecimal getBigDecimal(final String i, final int i1) throws SQLException {
        return get(BigDecimal.class, i);
    }

    public byte[] getBytes(final String i) throws SQLException {
        return get(byte[].class, i);
    }

    public Date getDate(final String i) throws SQLException {
        return get(Date.class, i);
    }

    public Time getTime(final String i) throws SQLException {
        return get(Time.class, i);
    }

    public Timestamp getTimestamp(final String i) throws SQLException {
        return get(Timestamp.class, i);
    }

    public InputStream getAsciiStream(final String i) throws SQLException {
        return get(InputStream.class, i);
    }

    @Deprecated
    public InputStream getUnicodeStream(final String i) throws SQLException {
        return get(InputStream.class, i);
    }

    public InputStream getBinaryStream(final String i) throws SQLException {
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

    public Object getObject(final int i) throws SQLException {
        return get(Object.class, i);
    }

    public Object getObject(final String s) throws SQLException {
        return get(Object.class, s);
    }

    public int findColumn(final String s) throws SQLException {
        return 0; // TODO
    }

    public Reader getCharacterStream(final int i) throws SQLException {
        return get(Reader.class, i);
    }

    public Reader getCharacterStream(final String s) throws SQLException {
        return get(Reader.class, s);
    }

    public BigDecimal getBigDecimal(final int i) throws SQLException {
        return get(BigDecimal.class, i);
    }

    public BigDecimal getBigDecimal(final String s) throws SQLException {
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

    public boolean absolute(final int i) throws SQLException {
        return false;
    }

    public boolean relative(final int i) throws SQLException {
        return false;
    }

    public boolean previous() throws SQLException {
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

    public void updateNull(final int i) throws SQLException {
    }

    public void updateBoolean(final int i, final boolean b) throws SQLException {
    }

    public void updateByte(final int i, final byte b) throws SQLException {
    }

    public void updateShort(final int i, final short i1) throws SQLException {
    }

    public void updateInt(final int i, final int i1) throws SQLException {
    }

    public void updateLong(final int i, final long l) throws SQLException {
    }

    public void updateFloat(final int i, final float v) throws SQLException {
    }

    public void updateDouble(final int i, final double v) throws SQLException {
    }

    public void updateBigDecimal(final int i, final BigDecimal bigDecimal) throws SQLException {
    }

    public void updateString(final int i, final String s) throws SQLException {
    }

    public void updateBytes(final int i, final byte[] bytes) throws SQLException {
    }

    public void updateDate(final int i, final Date date) throws SQLException {
    }

    public void updateTime(final int i, final Time time) throws SQLException {
    }

    public void updateTimestamp(final int i, final Timestamp timestamp) throws SQLException {
    }

    public void updateAsciiStream(final int i, final InputStream inputStream, final int i1) throws SQLException {
    }

    public void updateBinaryStream(final int i, final InputStream inputStream, final int i1) throws SQLException {
    }

    public void updateCharacterStream(final int i, final Reader reader, final int i1) throws SQLException {
    }

    public void updateObject(final int i, final Object o, final int i1) throws SQLException {
    }

    public void updateObject(final int i, final Object o) throws SQLException {
    }

    public void updateNull(final String s) throws SQLException {
    }

    public void updateBoolean(final String s, final boolean b) throws SQLException {
    }

    public void updateByte(final String s, final byte b) throws SQLException {
    }

    public void updateShort(final String s, final short i) throws SQLException {
    }

    public void updateInt(final String s, final int i) throws SQLException {
    }

    public void updateLong(final String s, final long l) throws SQLException {
    }

    public void updateFloat(final String s, final float v) throws SQLException {
    }

    public void updateDouble(final String s, final double v) throws SQLException {
    }

    public void updateBigDecimal(final String s, final BigDecimal bigDecimal) throws SQLException {
    }

    public void updateString(final String s, final String s1) throws SQLException {
    }

    public void updateBytes(final String s, final byte[] bytes) throws SQLException {
    }

    public void updateDate(final String s, final Date date) throws SQLException {
    }

    public void updateTime(final String s, final Time time) throws SQLException {
    }

    public void updateTimestamp(final String s, final Timestamp timestamp) throws SQLException {
    }

    public void updateAsciiStream(final String s, final InputStream inputStream, final int i) throws SQLException {
    }

    public void updateBinaryStream(final String s, final InputStream inputStream, final int i) throws SQLException {
    }

    public void updateCharacterStream(final String s, final Reader reader, final int i) throws SQLException {
    }

    public void updateObject(final String s, final Object o, final int i) throws SQLException {
    }

    public void updateObject(final String s, final Object o) throws SQLException {
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

    public Object getObject(final int i, final Map<String, Class<?>> stringClassMap) throws SQLException {
        return get(Object.class, i);  // TODO
    }

    public Ref getRef(final int i) throws SQLException {
        return get(Ref.class, i);
    }

    public Blob getBlob(final int i) throws SQLException {
        return get(Blob.class, i);
    }

    public Clob getClob(final int i) throws SQLException {
        return get(Clob.class, i);
    }

    public Array getArray(final int i) throws SQLException {
        return get(Array.class, i);
    }

    public Object getObject(final String s, final Map<String, Class<?>> stringClassMap) throws SQLException {
        return get(Object.class, s); // TODO
    }

    public Ref getRef(final String s) throws SQLException {
        return get(Ref.class, s);
    }

    public Blob getBlob(final String s) throws SQLException {
        return get(Blob.class, s);
    }

    public Clob getClob(final String s) throws SQLException {
        return get(Clob.class, s);
    }

    public Array getArray(final String s) throws SQLException {
        return get(Array.class, s);
    }

    public Date getDate(final int i, final Calendar calendar) throws SQLException {
        return getDate(i);
    }

    public Date getDate(final String s, final Calendar calendar) throws SQLException {
        return getDate(s);
    }

    public Time getTime(final int i, final Calendar calendar) throws SQLException {
        return getTime(i);
    }

    public Time getTime(final String s, final Calendar calendar) throws SQLException {
        return getTime(s);
    }

    public Timestamp getTimestamp(final int i, final Calendar calendar) throws SQLException {
        return getTimestamp(i);
    }

    public Timestamp getTimestamp(final String s, final Calendar calendar) throws SQLException {
        return getTimestamp(s);
    }

    public URL getURL(final int i) throws SQLException {
        return get(URL.class, i);
    }

    public URL getURL(final String s) throws SQLException {
        return get(URL.class, s);
    }

    public void updateRef(final int i, final Ref ref) throws SQLException {
    }

    public void updateRef(final String s, final Ref ref) throws SQLException {
    }

    public void updateBlob(final int i, final Blob blob) throws SQLException {
    }

    public void updateBlob(final String s, final Blob blob) throws SQLException {
    }

    public void updateClob(final int i, final Clob clob) throws SQLException {
    }

    public void updateClob(final String s, final Clob clob) throws SQLException {
    }

    public void updateArray(final int i, final Array array) throws SQLException {
    }

    public void updateArray(final String s, final Array array) throws SQLException {
    }
}
