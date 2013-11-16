/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import java.sql.Connection;
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
import java.sql.Types;
import java.util.Calendar;
import java.util.Map;

import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.RowDescriptorBuilder;
import org.apache.cayenne.access.jdbc.SQLStatement;
import org.apache.cayenne.access.jdbc.SQLTemplateAction;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.SQLTemplate;

/**
 * @since 3.0
 */
class OracleSQLTemplateAction extends SQLTemplateAction {

    protected DbEntity dbEntity;

    OracleSQLTemplateAction(SQLTemplate query, JdbcAdapter adapter,
            EntityResolver entityResolver) {
        super(query, adapter, entityResolver);
        this.dbEntity = query.getMetaData(entityResolver).getDbEntity();
    }

    @Override
    protected void processSelectResult(
            SQLStatement compiled,
            Connection connection,
            Statement statement,
            ResultSet resultSet,
            OperationObserver callback,
            long startTime) throws Exception {

        // wrap ResultSet to distinguish between Integer and BigDecimal for Oracle NUMBER
        // columns...

        if (compiled.getResultColumns().length == 0) {
            resultSet = new OracleResultSetWrapper(resultSet);
        }

        super.processSelectResult(
                compiled,
                connection,
                statement,
                resultSet,
                callback,
                startTime);
    }

    /**
     * @since 3.0
     */
    @Override
    protected RowDescriptorBuilder configureRowDescriptorBuilder(
            SQLStatement compiled,
            ResultSet resultSet) throws SQLException {

        RowDescriptorBuilder builder = super.configureRowDescriptorBuilder(
                compiled,
                resultSet);

        return builder;
    }

    final class OracleResultSetWrapper implements ResultSet {

        private ResultSet delegate;

        public ResultSetMetaData getMetaData() throws SQLException {
            return new OracleResultSetMetadata(delegate.getMetaData());
        }

        OracleResultSetWrapper(ResultSet delegate) {
            this.delegate = delegate;
        }

        public boolean absolute(int row) throws SQLException {
            return delegate.absolute(row);
        }

        public void afterLast() throws SQLException {
            delegate.afterLast();
        }

        public void beforeFirst() throws SQLException {
            delegate.beforeFirst();
        }

        public void cancelRowUpdates() throws SQLException {
            delegate.cancelRowUpdates();
        }

        public void clearWarnings() throws SQLException {
            delegate.clearWarnings();
        }

        public void close() throws SQLException {
            delegate.close();
        }

        public void deleteRow() throws SQLException {
            delegate.deleteRow();
        }

        public int findColumn(String columnName) throws SQLException {
            return delegate.findColumn(columnName);
        }

        public boolean first() throws SQLException {
            return delegate.first();
        }

        public Array getArray(int i) throws SQLException {
            return delegate.getArray(i);
        }

        public Array getArray(String colName) throws SQLException {
            return delegate.getArray(colName);
        }

        public InputStream getAsciiStream(int columnIndex) throws SQLException {
            return delegate.getAsciiStream(columnIndex);
        }

        public InputStream getAsciiStream(String columnName) throws SQLException {
            return delegate.getAsciiStream(columnName);
        }

        /**
         * @deprecated to mirror deprecation in the ResultSet interface
         */
        @Deprecated
        public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
            return delegate.getBigDecimal(columnIndex, scale);
        }

        public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
            return delegate.getBigDecimal(columnIndex);
        }

        /**
         * @deprecated to mirror deprecation in the ResultSet interface
         */
        @Deprecated
        public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException {
            return delegate.getBigDecimal(columnName, scale);
        }

        public BigDecimal getBigDecimal(String columnName) throws SQLException {
            return delegate.getBigDecimal(columnName);
        }

        public InputStream getBinaryStream(int columnIndex) throws SQLException {
            return delegate.getBinaryStream(columnIndex);
        }

        public InputStream getBinaryStream(String columnName) throws SQLException {
            return delegate.getBinaryStream(columnName);
        }

        public Blob getBlob(int i) throws SQLException {
            return delegate.getBlob(i);
        }

        public Blob getBlob(String colName) throws SQLException {
            return delegate.getBlob(colName);
        }

        public boolean getBoolean(int columnIndex) throws SQLException {
            return delegate.getBoolean(columnIndex);
        }

        public boolean getBoolean(String columnName) throws SQLException {
            return delegate.getBoolean(columnName);
        }

        public byte getByte(int columnIndex) throws SQLException {
            return delegate.getByte(columnIndex);
        }

        public byte getByte(String columnName) throws SQLException {
            return delegate.getByte(columnName);
        }

        public byte[] getBytes(int columnIndex) throws SQLException {
            return delegate.getBytes(columnIndex);
        }

        public byte[] getBytes(String columnName) throws SQLException {
            return delegate.getBytes(columnName);
        }

        public Reader getCharacterStream(int columnIndex) throws SQLException {
            return delegate.getCharacterStream(columnIndex);
        }

        public Reader getCharacterStream(String columnName) throws SQLException {
            return delegate.getCharacterStream(columnName);
        }

        public Clob getClob(int i) throws SQLException {
            return delegate.getClob(i);
        }

        public Clob getClob(String colName) throws SQLException {
            return delegate.getClob(colName);
        }

        public int getConcurrency() throws SQLException {
            return delegate.getConcurrency();
        }

        public String getCursorName() throws SQLException {
            return delegate.getCursorName();
        }

        public Date getDate(int columnIndex, Calendar cal) throws SQLException {
            return delegate.getDate(columnIndex, cal);
        }

        public Date getDate(int columnIndex) throws SQLException {
            return delegate.getDate(columnIndex);
        }

        public Date getDate(String columnName, Calendar cal) throws SQLException {
            return delegate.getDate(columnName, cal);
        }

        public Date getDate(String columnName) throws SQLException {
            return delegate.getDate(columnName);
        }

        public double getDouble(int columnIndex) throws SQLException {
            return delegate.getDouble(columnIndex);
        }

        public double getDouble(String columnName) throws SQLException {
            return delegate.getDouble(columnName);
        }

        public int getFetchDirection() throws SQLException {
            return delegate.getFetchDirection();
        }

        public int getFetchSize() throws SQLException {
            return delegate.getFetchSize();
        }

        public float getFloat(int columnIndex) throws SQLException {
            return delegate.getFloat(columnIndex);
        }

        public float getFloat(String columnName) throws SQLException {
            return delegate.getFloat(columnName);
        }

        public int getInt(int columnIndex) throws SQLException {
            return delegate.getInt(columnIndex);
        }

        public int getInt(String columnName) throws SQLException {
            return delegate.getInt(columnName);
        }

        public long getLong(int columnIndex) throws SQLException {
            return delegate.getLong(columnIndex);
        }

        public long getLong(String columnName) throws SQLException {
            return delegate.getLong(columnName);
        }

        public Object getObject(int columnIndex, Map<String, Class<?>> map)
                throws SQLException {
            return delegate.getObject(columnIndex, map);
        }

        public Object getObject(int columnIndex) throws SQLException {
            return delegate.getObject(columnIndex);
        }

        public Object getObject(String columnLabel, Map<String, Class<?>> map)
                throws SQLException {
            return delegate.getObject(columnLabel, map);
        }

        public Object getObject(String columnName) throws SQLException {
            return delegate.getObject(columnName);
        }

        public Ref getRef(int i) throws SQLException {
            return delegate.getRef(i);
        }

        public Ref getRef(String colName) throws SQLException {
            return delegate.getRef(colName);
        }

        public int getRow() throws SQLException {
            return delegate.getRow();
        }

        public short getShort(int columnIndex) throws SQLException {
            return delegate.getShort(columnIndex);
        }

        public short getShort(String columnName) throws SQLException {
            return delegate.getShort(columnName);
        }

        public Statement getStatement() throws SQLException {
            return delegate.getStatement();
        }

        public String getString(int columnIndex) throws SQLException {
            return delegate.getString(columnIndex);
        }

        public String getString(String columnName) throws SQLException {
            return delegate.getString(columnName);
        }

        public Time getTime(int columnIndex, Calendar cal) throws SQLException {
            return delegate.getTime(columnIndex, cal);
        }

        public Time getTime(int columnIndex) throws SQLException {
            return delegate.getTime(columnIndex);
        }

        public Time getTime(String columnName, Calendar cal) throws SQLException {
            return delegate.getTime(columnName, cal);
        }

        public Time getTime(String columnName) throws SQLException {
            return delegate.getTime(columnName);
        }

        public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
            return delegate.getTimestamp(columnIndex, cal);
        }

        public Timestamp getTimestamp(int columnIndex) throws SQLException {
            return delegate.getTimestamp(columnIndex);
        }

        public Timestamp getTimestamp(String columnName, Calendar cal)
                throws SQLException {
            return delegate.getTimestamp(columnName, cal);
        }

        public Timestamp getTimestamp(String columnName) throws SQLException {
            return delegate.getTimestamp(columnName);
        }

        public int getType() throws SQLException {
            return delegate.getType();
        }

        /**
         * @deprecated
         */
        @Deprecated
        public InputStream getUnicodeStream(int columnIndex) throws SQLException {
            return delegate.getUnicodeStream(columnIndex);
        }

        /**
         * @deprecated
         */
        @Deprecated
        public InputStream getUnicodeStream(String columnName) throws SQLException {
            return delegate.getUnicodeStream(columnName);
        }

        public URL getURL(int columnIndex) throws SQLException {
            return delegate.getURL(columnIndex);
        }

        public URL getURL(String columnName) throws SQLException {
            return delegate.getURL(columnName);
        }

        public SQLWarning getWarnings() throws SQLException {
            return delegate.getWarnings();
        }

        public void insertRow() throws SQLException {
            delegate.insertRow();
        }

        public boolean isAfterLast() throws SQLException {
            return delegate.isAfterLast();
        }

        public boolean isBeforeFirst() throws SQLException {
            return delegate.isBeforeFirst();
        }

        public boolean isFirst() throws SQLException {
            return delegate.isFirst();
        }

        public boolean isLast() throws SQLException {
            return delegate.isLast();
        }

        public boolean last() throws SQLException {
            return delegate.last();
        }

        public void moveToCurrentRow() throws SQLException {
            delegate.moveToCurrentRow();
        }

        public void moveToInsertRow() throws SQLException {
            delegate.moveToInsertRow();
        }

        public boolean next() throws SQLException {
            return delegate.next();
        }

        public boolean previous() throws SQLException {
            return delegate.previous();
        }

        public void refreshRow() throws SQLException {
            delegate.refreshRow();
        }

        public boolean relative(int rows) throws SQLException {
            return delegate.relative(rows);
        }

        public boolean rowDeleted() throws SQLException {
            return delegate.rowDeleted();
        }

        public boolean rowInserted() throws SQLException {
            return delegate.rowInserted();
        }

        public boolean rowUpdated() throws SQLException {
            return delegate.rowUpdated();
        }

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

        public void updateAsciiStream(int columnIndex, InputStream x, int length)
                throws SQLException {
            delegate.updateAsciiStream(columnIndex, x, length);
        }

        public void updateAsciiStream(String columnName, InputStream x, int length)
                throws SQLException {
            delegate.updateAsciiStream(columnName, x, length);
        }

        public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
            delegate.updateBigDecimal(columnIndex, x);
        }

        public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException {
            delegate.updateBigDecimal(columnName, x);
        }

        public void updateBinaryStream(int columnIndex, InputStream x, int length)
                throws SQLException {
            delegate.updateBinaryStream(columnIndex, x, length);
        }

        public void updateBinaryStream(String columnName, InputStream x, int length)
                throws SQLException {
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

        public void updateCharacterStream(int columnIndex, Reader x, int length)
                throws SQLException {
            delegate.updateCharacterStream(columnIndex, x, length);
        }

        public void updateCharacterStream(String columnName, Reader reader, int length)
                throws SQLException {
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

        public void updateObject(int columnIndex, Object x, int scale)
                throws SQLException {
            delegate.updateObject(columnIndex, x, scale);
        }

        public void updateObject(int columnIndex, Object x) throws SQLException {
            delegate.updateObject(columnIndex, x);
        }

        public void updateObject(String columnName, Object x, int scale)
                throws SQLException {
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

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public int getHoldability() throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public Reader getNCharacterStream(int arg0) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public Reader getNCharacterStream(String arg0) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public String getNString(int columnIndex) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public String getNString(String arg0) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public boolean isClosed() throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateAsciiStream(int arg0, InputStream arg1) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateAsciiStream(String arg0, InputStream arg1) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateAsciiStream(int arg0, InputStream arg1, long arg2)
                throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateAsciiStream(String arg0, InputStream arg1, long arg2)
                throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateBinaryStream(int arg0, InputStream arg1) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateBinaryStream(String arg0, InputStream arg1) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateBinaryStream(int arg0, InputStream arg1, long arg2)
                throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateBinaryStream(String arg0, InputStream arg1, long arg2)
                throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateBlob(int arg0, InputStream arg1) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateBlob(String arg0, InputStream arg1) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateBlob(int arg0, InputStream arg1, long arg2) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateBlob(String arg0, InputStream arg1, long arg2)
                throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateCharacterStream(int arg0, Reader arg1) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateCharacterStream(String arg0, Reader arg1) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateCharacterStream(int arg0, Reader arg1, long arg2)
                throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateCharacterStream(String arg0, Reader arg1, long arg2)
                throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateClob(int arg0, Reader arg1) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateClob(String arg0, Reader arg1) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateClob(int arg0, Reader arg1, long arg2) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateClob(String arg0, Reader arg1, long arg2) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateNCharacterStream(int arg0, Reader arg1) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateNCharacterStream(String arg0, Reader arg1) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateNCharacterStream(int arg0, Reader arg1, long arg2)
                throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateNCharacterStream(String arg0, Reader arg1, long arg2)
                throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateNClob(int arg0, Reader arg1) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateNClob(String arg0, Reader arg1) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateNClob(int arg0, Reader arg1, long arg2) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateNClob(String arg0, Reader arg1, long arg2) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateNString(int arg0, String arg1) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateNString(String arg0, String arg1) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public NClob getNClob(int arg0) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public NClob getNClob(String columnLabel) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public RowId getRowId(int columnIndex) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public RowId getRowId(String columnLabel) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public SQLXML getSQLXML(int columnIndex) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public SQLXML getSQLXML(String columnLabel) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateNClob(int columnIndex, NClob clob) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateNClob(String columnLabel, NClob clob) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateRowId(int columnIndex, RowId x) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateRowId(String columnLabel, RowId x) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public void updateSQLXML(String columnLabel, SQLXML xmlObject)
                throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.1 JDBC 4.1 compatibility under Java 1.5
         */
        public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.1 JDBC 4.1 compatibility under Java 1.5
         */
        public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
            throw new UnsupportedOperationException();
        }
    }

    final class OracleResultSetMetadata implements ResultSetMetaData {

        private ResultSetMetaData delegate;

        OracleResultSetMetadata(ResultSetMetaData delegate) {
            this.delegate = delegate;
        }

        public String getCatalogName(int column) throws SQLException {
            return delegate.getCatalogName(column);
        }

        public String getColumnClassName(int column) throws SQLException {
            String className = delegate.getColumnClassName(column);

            if (BigDecimal.class.getName().equals(className)
                    && getColumnType(column) == Types.INTEGER) {
                className = Integer.class.getName();
            }

            return className;
        }

        public int getColumnCount() throws SQLException {
            return delegate.getColumnCount();
        }

        public int getColumnDisplaySize(int column) throws SQLException {
            return delegate.getColumnDisplaySize(column);
        }

        public String getColumnLabel(int column) throws SQLException {
            return delegate.getColumnLabel(column);
        }

        public String getColumnName(int column) throws SQLException {
            return delegate.getColumnName(column);
        }

        public int getColumnType(int column) throws SQLException {
            int type = delegate.getColumnType(column);

            // this only detects INTEGER but not BIGINT...
            if (type == Types.NUMERIC) {
                int precision = delegate.getPrecision(column);
                if ((precision == 10 || precision == 38)
                        && delegate.getScale(column) == 0) {
                    type = Types.INTEGER;
                }
            }

            return type;
        }

        public String getColumnTypeName(int column) throws SQLException {
            return delegate.getColumnTypeName(column);
        }

        public int getPrecision(int column) throws SQLException {
            return delegate.getPrecision(column);
        }

        public int getScale(int column) throws SQLException {
            return delegate.getScale(column);
        }

        public String getSchemaName(int column) throws SQLException {
            return delegate.getSchemaName(column);
        }

        public String getTableName(int column) throws SQLException {
            return delegate.getTableName(column);
        }

        public boolean isAutoIncrement(int column) throws SQLException {
            return delegate.isAutoIncrement(column);
        }

        public boolean isCaseSensitive(int column) throws SQLException {
            return delegate.isCaseSensitive(column);
        }

        public boolean isCurrency(int column) throws SQLException {
            return delegate.isCurrency(column);
        }

        public boolean isDefinitelyWritable(int column) throws SQLException {
            return delegate.isDefinitelyWritable(column);
        }

        public int isNullable(int column) throws SQLException {
            return delegate.isNullable(column);
        }

        public boolean isReadOnly(int column) throws SQLException {
            return delegate.isReadOnly(column);
        }

        public boolean isSearchable(int column) throws SQLException {
            return delegate.isSearchable(column);
        }

        public boolean isSigned(int column) throws SQLException {
            return delegate.isSigned(column);
        }

        public boolean isWritable(int column) throws SQLException {
            return delegate.isWritable(column);
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new UnsupportedOperationException();
        }
    }
}
