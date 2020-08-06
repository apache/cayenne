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
package org.apache.cayenne.dba.oracle;

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
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

final class OracleResultSetWrapper implements ResultSet {

	private ResultSet delegate;

	OracleResultSetWrapper(ResultSet delegate) {
		this.delegate = delegate;
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return new OracleResultSetMetadata(delegate.getMetaData());
	}

	@Override
	public boolean absolute(int row) throws SQLException {
		return delegate.absolute(row);
	}

	@Override
	public void afterLast() throws SQLException {
		delegate.afterLast();
	}

	@Override
	public void beforeFirst() throws SQLException {
		delegate.beforeFirst();
	}

	@Override
	public void cancelRowUpdates() throws SQLException {
		delegate.cancelRowUpdates();
	}

	@Override
	public void clearWarnings() throws SQLException {
		delegate.clearWarnings();
	}

	@Override
	public void close() throws SQLException {
		delegate.close();
	}

	@Override
	public void deleteRow() throws SQLException {
		delegate.deleteRow();
	}

	@Override
	public int findColumn(String columnName) throws SQLException {
		return delegate.findColumn(columnName);
	}

	@Override
	public boolean first() throws SQLException {
		return delegate.first();
	}

	@Override
	public Array getArray(int i) throws SQLException {
		return delegate.getArray(i);
	}

	@Override
	public Array getArray(String colName) throws SQLException {
		return delegate.getArray(colName);
	}

	@Override
	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		return delegate.getAsciiStream(columnIndex);
	}

	@Override
	public InputStream getAsciiStream(String columnName) throws SQLException {
		return delegate.getAsciiStream(columnName);
	}

	/**
	 * @deprecated to mirror deprecation in the ResultSet interface
	 */
	@Deprecated
	@Override
	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		return delegate.getBigDecimal(columnIndex, scale);
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		return delegate.getBigDecimal(columnIndex);
	}

	/**
	 * @deprecated to mirror deprecation in the ResultSet interface
	 */
	@Deprecated
	@Override
	public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException {
		return delegate.getBigDecimal(columnName, scale);
	}

	@Override
	public BigDecimal getBigDecimal(String columnName) throws SQLException {
		return delegate.getBigDecimal(columnName);
	}

	@Override
	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		return delegate.getBinaryStream(columnIndex);
	}

	@Override
	public InputStream getBinaryStream(String columnName) throws SQLException {
		return delegate.getBinaryStream(columnName);
	}

	@Override
	public Blob getBlob(int i) throws SQLException {
		return delegate.getBlob(i);
	}

	@Override
	public Blob getBlob(String colName) throws SQLException {
		return delegate.getBlob(colName);
	}

	@Override
	public boolean getBoolean(int columnIndex) throws SQLException {
		return delegate.getBoolean(columnIndex);
	}

	@Override
	public boolean getBoolean(String columnName) throws SQLException {
		return delegate.getBoolean(columnName);
	}

	@Override
	public byte getByte(int columnIndex) throws SQLException {
		return delegate.getByte(columnIndex);
	}

	@Override
	public byte getByte(String columnName) throws SQLException {
		return delegate.getByte(columnName);
	}

	@Override
	public byte[] getBytes(int columnIndex) throws SQLException {
		return delegate.getBytes(columnIndex);
	}

	@Override
	public byte[] getBytes(String columnName) throws SQLException {
		return delegate.getBytes(columnName);
	}

	@Override
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		return delegate.getCharacterStream(columnIndex);
	}

	@Override
	public Reader getCharacterStream(String columnName) throws SQLException {
		return delegate.getCharacterStream(columnName);
	}

	@Override
	public Clob getClob(int i) throws SQLException {
		return delegate.getClob(i);
	}

	@Override
	public Clob getClob(String colName) throws SQLException {
		return delegate.getClob(colName);
	}

	@Override
	public int getConcurrency() throws SQLException {
		return delegate.getConcurrency();
	}

	@Override
	public String getCursorName() throws SQLException {
		return delegate.getCursorName();
	}

	@Override
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		return delegate.getDate(columnIndex, cal);
	}

	@Override
	public Date getDate(int columnIndex) throws SQLException {
		return delegate.getDate(columnIndex);
	}

	@Override
	public Date getDate(String columnName, Calendar cal) throws SQLException {
		return delegate.getDate(columnName, cal);
	}

	@Override
	public Date getDate(String columnName) throws SQLException {
		return delegate.getDate(columnName);
	}

	@Override
	public double getDouble(int columnIndex) throws SQLException {
		return delegate.getDouble(columnIndex);
	}

	@Override
	public double getDouble(String columnName) throws SQLException {
		return delegate.getDouble(columnName);
	}

	@Override
	public int getFetchDirection() throws SQLException {
		return delegate.getFetchDirection();
	}

	@Override
	public int getFetchSize() throws SQLException {
		return delegate.getFetchSize();
	}

	@Override
	public float getFloat(int columnIndex) throws SQLException {
		return delegate.getFloat(columnIndex);
	}

	@Override
	public float getFloat(String columnName) throws SQLException {
		return delegate.getFloat(columnName);
	}

	@Override
	public int getInt(int columnIndex) throws SQLException {
		return delegate.getInt(columnIndex);
	}

	@Override
	public int getInt(String columnName) throws SQLException {
		return delegate.getInt(columnName);
	}

	@Override
	public long getLong(int columnIndex) throws SQLException {
		return delegate.getLong(columnIndex);
	}

	@Override
	public long getLong(String columnName) throws SQLException {
		return delegate.getLong(columnName);
	}

	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		return delegate.getObject(columnIndex, map);
	}

	@Override
	public Object getObject(int columnIndex) throws SQLException {
		return delegate.getObject(columnIndex);
	}

	@Override
	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		return delegate.getObject(columnLabel, map);
	}

	@Override
	public Object getObject(String columnName) throws SQLException {
		return delegate.getObject(columnName);
	}

	@Override
	public Ref getRef(int i) throws SQLException {
		return delegate.getRef(i);
	}

	@Override
	public Ref getRef(String colName) throws SQLException {
		return delegate.getRef(colName);
	}

	@Override
	public int getRow() throws SQLException {
		return delegate.getRow();
	}

	@Override
	public short getShort(int columnIndex) throws SQLException {
		return delegate.getShort(columnIndex);
	}

	@Override
	public short getShort(String columnName) throws SQLException {
		return delegate.getShort(columnName);
	}

	@Override
	public Statement getStatement() throws SQLException {
		return delegate.getStatement();
	}

	@Override
	public String getString(int columnIndex) throws SQLException {
		return delegate.getString(columnIndex);
	}

	@Override
	public String getString(String columnName) throws SQLException {
		return delegate.getString(columnName);
	}

	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		return delegate.getTime(columnIndex, cal);
	}

	@Override
	public Time getTime(int columnIndex) throws SQLException {
		return delegate.getTime(columnIndex);
	}

	@Override
	public Time getTime(String columnName, Calendar cal) throws SQLException {
		return delegate.getTime(columnName, cal);
	}

	@Override
	public Time getTime(String columnName) throws SQLException {
		return delegate.getTime(columnName);
	}

	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		return delegate.getTimestamp(columnIndex, cal);
	}

	@Override
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		return delegate.getTimestamp(columnIndex);
	}

	@Override
	public Timestamp getTimestamp(String columnName, Calendar cal) throws SQLException {
		return delegate.getTimestamp(columnName, cal);
	}

	@Override
	public Timestamp getTimestamp(String columnName) throws SQLException {
		return delegate.getTimestamp(columnName);
	}

	@Override
	public int getType() throws SQLException {
		return delegate.getType();
	}

	@Deprecated
	@Override
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		return delegate.getUnicodeStream(columnIndex);
	}

	@Deprecated
	@Override
	public InputStream getUnicodeStream(String columnName) throws SQLException {
		return delegate.getUnicodeStream(columnName);
	}

	@Override
	public URL getURL(int columnIndex) throws SQLException {
		return delegate.getURL(columnIndex);
	}

	@Override
	public URL getURL(String columnName) throws SQLException {
		return delegate.getURL(columnName);
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return delegate.getWarnings();
	}

	@Override
	public void insertRow() throws SQLException {
		delegate.insertRow();
	}

	@Override
	public boolean isAfterLast() throws SQLException {
		return delegate.isAfterLast();
	}

	@Override
	public boolean isBeforeFirst() throws SQLException {
		return delegate.isBeforeFirst();
	}

	@Override
	public boolean isFirst() throws SQLException {
		return delegate.isFirst();
	}

	@Override
	public boolean isLast() throws SQLException {
		return delegate.isLast();
	}

	@Override
	public boolean last() throws SQLException {
		return delegate.last();
	}

	@Override
	public void moveToCurrentRow() throws SQLException {
		delegate.moveToCurrentRow();
	}

	@Override
	public void moveToInsertRow() throws SQLException {
		delegate.moveToInsertRow();
	}

	@Override
	public boolean next() throws SQLException {
		return delegate.next();
	}

	@Override
	public boolean previous() throws SQLException {
		return delegate.previous();
	}

	@Override
	public void refreshRow() throws SQLException {
		delegate.refreshRow();
	}

	@Override
	public boolean relative(int rows) throws SQLException {
		return delegate.relative(rows);
	}

	@Override
	public boolean rowDeleted() throws SQLException {
		return delegate.rowDeleted();
	}

	@Override
	public boolean rowInserted() throws SQLException {
		return delegate.rowInserted();
	}

	@Override
	public boolean rowUpdated() throws SQLException {
		return delegate.rowUpdated();
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		delegate.setFetchDirection(direction);
	}

	public void setFetchSize(int rows) throws SQLException {
		delegate.setFetchSize(rows);
	}

	public void updateArray(int columnIndex, Array x) throws SQLException {
		delegate.updateArray(columnIndex, x);
	}

	public void updateArray(String columnName, Array x) throws SQLException {
		delegate.updateArray(columnName, x);
	}

	public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
		delegate.updateAsciiStream(columnIndex, x, length);
	}

	public void updateAsciiStream(String columnName, InputStream x, int length) throws SQLException {
		delegate.updateAsciiStream(columnName, x, length);
	}

	public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
		delegate.updateBigDecimal(columnIndex, x);
	}

	public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException {
		delegate.updateBigDecimal(columnName, x);
	}

	public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
		delegate.updateBinaryStream(columnIndex, x, length);
	}

	public void updateBinaryStream(String columnName, InputStream x, int length) throws SQLException {
		delegate.updateBinaryStream(columnName, x, length);
	}

	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		delegate.updateBlob(columnIndex, x);
	}

	public void updateBlob(String columnName, Blob x) throws SQLException {
		delegate.updateBlob(columnName, x);
	}

	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		delegate.updateBoolean(columnIndex, x);
	}

	public void updateBoolean(String columnName, boolean x) throws SQLException {
		delegate.updateBoolean(columnName, x);
	}

	public void updateByte(int columnIndex, byte x) throws SQLException {
		delegate.updateByte(columnIndex, x);
	}

	public void updateByte(String columnName, byte x) throws SQLException {
		delegate.updateByte(columnName, x);
	}

	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		delegate.updateBytes(columnIndex, x);
	}

	public void updateBytes(String columnName, byte[] x) throws SQLException {
		delegate.updateBytes(columnName, x);
	}

	public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
		delegate.updateCharacterStream(columnIndex, x, length);
	}

	public void updateCharacterStream(String columnName, Reader reader, int length) throws SQLException {
		delegate.updateCharacterStream(columnName, reader, length);
	}

	public void updateClob(int columnIndex, Clob x) throws SQLException {
		delegate.updateClob(columnIndex, x);
	}

	public void updateClob(String columnName, Clob x) throws SQLException {
		delegate.updateClob(columnName, x);
	}

	public void updateDate(int columnIndex, Date x) throws SQLException {
		delegate.updateDate(columnIndex, x);
	}

	public void updateDate(String columnName, Date x) throws SQLException {
		delegate.updateDate(columnName, x);
	}

	public void updateDouble(int columnIndex, double x) throws SQLException {
		delegate.updateDouble(columnIndex, x);
	}

	public void updateDouble(String columnName, double x) throws SQLException {
		delegate.updateDouble(columnName, x);
	}

	public void updateFloat(int columnIndex, float x) throws SQLException {
		delegate.updateFloat(columnIndex, x);
	}

	public void updateFloat(String columnName, float x) throws SQLException {
		delegate.updateFloat(columnName, x);
	}

	public void updateInt(int columnIndex, int x) throws SQLException {
		delegate.updateInt(columnIndex, x);
	}

	public void updateInt(String columnName, int x) throws SQLException {
		delegate.updateInt(columnName, x);
	}

	public void updateLong(int columnIndex, long x) throws SQLException {
		delegate.updateLong(columnIndex, x);
	}

	public void updateLong(String columnName, long x) throws SQLException {
		delegate.updateLong(columnName, x);
	}

	public void updateNull(int columnIndex) throws SQLException {
		delegate.updateNull(columnIndex);
	}

	public void updateNull(String columnName) throws SQLException {
		delegate.updateNull(columnName);
	}

	public void updateObject(int columnIndex, Object x, int scale) throws SQLException {
		delegate.updateObject(columnIndex, x, scale);
	}

	public void updateObject(int columnIndex, Object x) throws SQLException {
		delegate.updateObject(columnIndex, x);
	}

	public void updateObject(String columnName, Object x, int scale) throws SQLException {
		delegate.updateObject(columnName, x, scale);
	}

	public void updateObject(String columnName, Object x) throws SQLException {
		delegate.updateObject(columnName, x);
	}

	public void updateRef(int columnIndex, Ref x) throws SQLException {
		delegate.updateRef(columnIndex, x);
	}

	public void updateRef(String columnName, Ref x) throws SQLException {
		delegate.updateRef(columnName, x);
	}

	public void updateRow() throws SQLException {
		delegate.updateRow();
	}

	public void updateShort(int columnIndex, short x) throws SQLException {
		delegate.updateShort(columnIndex, x);
	}

	public void updateShort(String columnName, short x) throws SQLException {
		delegate.updateShort(columnName, x);
	}

	public void updateString(int columnIndex, String x) throws SQLException {
		delegate.updateString(columnIndex, x);
	}

	public void updateString(String columnName, String x) throws SQLException {
		delegate.updateString(columnName, x);
	}

	public void updateTime(int columnIndex, Time x) throws SQLException {
		delegate.updateTime(columnIndex, x);
	}

	public void updateTime(String columnName, Time x) throws SQLException {
		delegate.updateTime(columnName, x);
	}

	public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
		delegate.updateTimestamp(columnIndex, x);
	}

	public void updateTimestamp(String columnName, Timestamp x) throws SQLException {
		delegate.updateTimestamp(columnName, x);
	}

	public boolean wasNull() throws SQLException {
		return delegate.wasNull();
	}

	@Override
	public int getHoldability() throws SQLException {
		return delegate.getHoldability();
	}

	@Override
	public Reader getNCharacterStream(int arg0) throws SQLException {
		return delegate.getNCharacterStream(arg0);
	}

	@Override
	public Reader getNCharacterStream(String arg0) throws SQLException {
		return delegate.getNCharacterStream(arg0);
	}

	@Override
	public String getNString(int columnIndex) throws SQLException {
		return delegate.getNString(columnIndex);
	}

	@Override
	public String getNString(String arg0) throws SQLException {
		return delegate.getNString(arg0);
	}

	@Override
	public boolean isClosed() throws SQLException {
		return delegate.isClosed();
	}

	@Override
	public void updateAsciiStream(int arg0, InputStream arg1) throws SQLException {
		delegate.updateAsciiStream(arg0, arg1);
	}

	@Override
	public void updateAsciiStream(String arg0, InputStream arg1) throws SQLException {
		delegate.updateAsciiStream(arg0, arg1);
	}

	@Override
	public void updateAsciiStream(int arg0, InputStream arg1, long arg2) throws SQLException {
		delegate.updateAsciiStream(arg0, arg1, arg2);
	}

	@Override
	public void updateAsciiStream(String arg0, InputStream arg1, long arg2) throws SQLException {
		delegate.updateAsciiStream(arg0, arg1, arg2);
	}

	@Override
	public void updateBinaryStream(int arg0, InputStream arg1) throws SQLException {
		delegate.updateBinaryStream(arg0, arg1);
	}

	@Override
	public void updateBinaryStream(String arg0, InputStream arg1) throws SQLException {
		delegate.updateBinaryStream(arg0, arg1);
	}

	@Override
	public void updateBinaryStream(int arg0, InputStream arg1, long arg2) throws SQLException {
		delegate.updateBinaryStream(arg0, arg1, arg2);
	}

	@Override
	public void updateBinaryStream(String arg0, InputStream arg1, long arg2) throws SQLException {
		delegate.updateBinaryStream(arg0, arg1, arg2);
	}

	@Override
	public void updateBlob(int arg0, InputStream arg1) throws SQLException {
		delegate.updateBlob(arg0, arg1);
	}

	@Override
	public void updateBlob(String arg0, InputStream arg1) throws SQLException {
		delegate.updateBlob(arg0, arg1);
	}

	@Override
	public void updateBlob(int arg0, InputStream arg1, long arg2) throws SQLException {
		delegate.updateBlob(arg0, arg1, arg2);
	}

	@Override
	public void updateBlob(String arg0, InputStream arg1, long arg2) throws SQLException {
		delegate.updateBlob(arg0, arg1, arg2);
	}

	@Override
	public void updateCharacterStream(int arg0, Reader arg1) throws SQLException {
		delegate.updateCharacterStream(arg0, arg1);
	}

	@Override
	public void updateCharacterStream(String arg0, Reader arg1) throws SQLException {
		delegate.updateCharacterStream(arg0, arg1);
	}

	@Override
	public void updateCharacterStream(int arg0, Reader arg1, long arg2) throws SQLException {
		delegate.updateCharacterStream(arg0, arg1, arg2);
	}

	@Override
	public void updateCharacterStream(String arg0, Reader arg1, long arg2) throws SQLException {
		delegate.updateCharacterStream(arg0, arg1, arg2);
	}

	@Override
	public void updateClob(int arg0, Reader arg1) throws SQLException {
		delegate.updateClob(arg0, arg1);
	}

	@Override
	public void updateClob(String arg0, Reader arg1) throws SQLException {
		delegate.updateClob(arg0, arg1);
	}

	@Override
	public void updateClob(int arg0, Reader arg1, long arg2) throws SQLException {
		delegate.updateClob(arg0, arg1, arg2);
	}

	@Override
	public void updateClob(String arg0, Reader arg1, long arg2) throws SQLException {
		delegate.updateClob(arg0, arg1, arg2);
	}

	@Override
	public void updateNCharacterStream(int arg0, Reader arg1) throws SQLException {
		delegate.updateNCharacterStream(arg0, arg1);
	}

	@Override
	public void updateNCharacterStream(String arg0, Reader arg1) throws SQLException {
		delegate.updateNCharacterStream(arg0, arg1);
	}

	@Override
	public void updateNCharacterStream(int arg0, Reader arg1, long arg2) throws SQLException {
		delegate.updateNCharacterStream(arg0, arg1, arg2);
	}

	@Override
	public void updateNCharacterStream(String arg0, Reader arg1, long arg2) throws SQLException {
		delegate.updateNCharacterStream(arg0, arg1, arg2);
	}

	@Override
	public void updateNClob(int arg0, Reader arg1) throws SQLException {
		delegate.updateNClob(arg0, arg1);
	}

	@Override
	public void updateNClob(String arg0, Reader arg1) throws SQLException {
		delegate.updateNClob(arg0, arg1);
	}

	@Override
	public void updateNClob(int arg0, Reader arg1, long arg2) throws SQLException {
		delegate.updateNClob(arg0, arg1, arg2);
	}

	@Override
	public void updateNClob(String arg0, Reader arg1, long arg2) throws SQLException {
		delegate.updateNClob(arg0, arg1, arg2);
	}

	@Override
	public void updateNString(int arg0, String arg1) throws SQLException {
		delegate.updateNString(arg0, arg1);
	}

	@Override
	public void updateNString(String arg0, String arg1) throws SQLException {
		delegate.updateNString(arg0, arg1);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		// TODO
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		// TODO
		throw new UnsupportedOperationException();
	}

	@Override
	public NClob getNClob(int arg0) throws SQLException {
		return delegate.getNClob(arg0);
	}

	@Override
	public NClob getNClob(String columnLabel) throws SQLException {
		return delegate.getNClob(columnLabel);
	}

	@Override
	public RowId getRowId(int columnIndex) throws SQLException {
		return delegate.getRowId(columnIndex);
	}

	@Override
	public RowId getRowId(String columnLabel) throws SQLException {
		return delegate.getRowId(columnLabel);
	}

	@Override
	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		return delegate.getSQLXML(columnIndex);
	}

	@Override
	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		return delegate.getSQLXML(columnLabel);
	}

	@Override
	public void updateNClob(int columnIndex, NClob clob) throws SQLException {
		delegate.updateNClob(columnIndex, clob);
	}

	@Override
	public void updateNClob(String columnLabel, NClob clob) throws SQLException {
		delegate.updateNClob(columnLabel, clob);
	}

	@Override
	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		delegate.updateRowId(columnIndex, x);
	}

	@Override
	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		delegate.updateRowId(columnLabel, x);
	}

	@Override
	public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
		delegate.updateSQLXML(columnIndex, xmlObject);
	}

	@Override
	public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
		delegate.updateSQLXML(columnLabel, xmlObject);
	}

	@Override
	public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
		return delegate.getObject(columnIndex, type);
	}

	@Override
	public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
		return delegate.getObject(columnLabel, type);

	}
}