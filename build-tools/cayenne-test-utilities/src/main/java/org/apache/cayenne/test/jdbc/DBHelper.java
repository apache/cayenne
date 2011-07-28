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
package org.apache.cayenne.test.jdbc;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

import javax.sql.DataSource;

/**
 * JDBC utility class for setting up and analyzing the DB data sets. DBHelper
 * intentionally bypasses Cayenne stack.
 */
public class DBHelper {

    protected DataSource dataSource;

    public DBHelper(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Quotes a SQL identifier as appropriate for the given DB. This implementation
     * returns the identifier unchanged, while subclasses can implement a custom quoting
     * strategy.
     */
    protected String quote(String sqlIdentifier) {
        return sqlIdentifier;
    }

    /**
     * Selects a single row.
     */
    public Object[] select(String table, final String[] columns) throws SQLException {

        if (columns.length == 0) {
            throw new IllegalArgumentException("No columns");
        }

        StringBuilder sql = new StringBuilder("select ");
        sql.append(quote(columns[0]));
        for (int i = 1; i < columns.length; i++) {
            sql.append(", ").append(quote(columns[i]));
        }
        sql.append(" from ").append(quote(table));

        return new RowTemplate<Object[]>(this) {

            @Override
            Object[] readRow(ResultSet rs, String sql) throws SQLException {

                Object[] result = new Object[columns.length];
                for (int i = 1; i <= result.length; i++) {
                    result[i - 1] = rs.getObject(i);
                }

                return result;
            }
        }.execute(sql.toString());
    }

    /**
     * Inserts a single row. Columns types can be null and will be determined from
     * ParameterMetaData in this case. The later scenario will not work if values contains
     * nulls and the DB is Oracle.
     */
    public void insert(String table, String[] columns, Object[] values, int[] columnTypes)
            throws SQLException {

        if (columns.length != values.length) {
            throw new IllegalArgumentException(
                    "Columns and values arrays have different sizes: "
                            + columns.length
                            + " and "
                            + values.length);
        }

        if (columns.length == 0) {
            throw new IllegalArgumentException("No columns");
        }

        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(quote(table)).append(" (").append(quote(columns[0]));
        for (int i = 1; i < columns.length; i++) {
            sql.append(", ").append(quote(columns[i]));
        }

        sql.append(") VALUES (?");
        for (int i = 1; i < values.length; i++) {
            sql.append(", ?");
        }
        sql.append(")");

        Connection c = getConnection();
        try {

            String sqlString = sql.toString();
            UtilityLogger.log(sqlString);
            PreparedStatement st = c.prepareStatement(sqlString);
            ParameterMetaData parameters = null;
            try {
                for (int i = 0; i < values.length; i++) {

                    if (values[i] == null) {

                        int type;

                        if (columnTypes == null) {

                            // check for the right NULL type
                            if (parameters == null) {
                                parameters = st.getParameterMetaData();
                            }

                            type = parameters.getParameterType(i + 1);
                        }
                        else {
                            type = columnTypes[i];
                        }

                        st.setNull(i + 1, type);
                    }
                    else {
                        st.setObject(i + 1, values[i]);
                    }
                }

                st.executeUpdate();
            }
            finally {
                st.close();
            }
            c.commit();
        }
        finally {
            c.close();
        }

    }

    public int deleteAll(String tableName) throws SQLException {
        return delete(tableName).execute();
    }

    public UpdateBuilder update(String tableName) throws SQLException {
        return new UpdateBuilder(this, tableName);
    }

    public DeleteBuilder delete(String tableName) {
        return new DeleteBuilder(this, tableName);
    }

    public int getRowCount(String table) throws SQLException {
        String sql = "select count(*) from " + quote(table);

        return new RowTemplate<Integer>(this) {

            @Override
            Integer readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getInt(1);
            }

        }.execute(sql);
    }

    public String getString(String table, String column) throws SQLException {
        final String sql = "select " + quote(column) + " from " + quote(table);

        return new RowTemplate<String>(this) {

            @Override
            String readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getString(1);
            }

        }.execute(sql);
    }

    public Object getObject(String table, String column) throws SQLException {
        final String sql = "select " + quote(column) + " from " + quote(table);

        return new RowTemplate<Object>(this) {

            @Override
            Object readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getObject(1);
            }

        }.execute(sql);
    }

    public byte getByte(String table, String column) throws SQLException {
        final String sql = "select " + quote(column) + " from " + quote(table);

        return new RowTemplate<Byte>(this) {

            @Override
            Byte readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getByte(1);
            }

        }.execute(sql);
    }

    public byte[] getBytes(String table, String column) throws SQLException {
        final String sql = "select " + quote(column) + " from " + quote(table);

        return new RowTemplate<byte[]>(this) {

            @Override
            byte[] readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getBytes(1);
            }

        }.execute(sql);
    }

    public int getInt(String table, String column) throws SQLException {
        final String sql = "select " + quote(column) + " from " + quote(table);

        return new RowTemplate<Integer>(this) {

            @Override
            Integer readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getInt(1);
            }

        }.execute(sql);
    }

    public long getLong(String table, String column) throws SQLException {
        final String sql = "select " + quote(column) + " from " + quote(table);

        return new RowTemplate<Long>(this) {

            @Override
            Long readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getLong(1);
            }

        }.execute(sql);
    }

    public double getDouble(String table, String column) throws SQLException {
        final String sql = "select " + quote(column) + " from " + quote(table);

        return new RowTemplate<Double>(this) {

            @Override
            Double readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getDouble(1);
            }

        }.execute(sql);
    }

    public boolean getBoolean(String table, String column) throws SQLException {
        final String sql = "select " + quote(column) + " from " + quote(table);

        return new RowTemplate<Boolean>(this) {

            @Override
            Boolean readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getBoolean(1);
            }

        }.execute(sql);
    }

    public java.util.Date getUtilDate(String table, String column) throws SQLException {
        Timestamp ts = getTimestamp(table, column);
        return ts != null ? new java.util.Date(ts.getTime()) : null;
    }

    public java.sql.Date getSqlDate(String table, String column) throws SQLException {
        final String sql = "select " + quote(column) + " from " + quote(table);

        return new RowTemplate<java.sql.Date>(this) {

            @Override
            java.sql.Date readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getDate(1);
            }

        }.execute(sql);
    }

    public Time getTime(String table, String column) throws SQLException {
        final String sql = "select " + quote(column) + " from " + quote(table);

        return new RowTemplate<Time>(this) {

            @Override
            Time readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getTime(1);
            }

        }.execute(sql);
    }

    public Timestamp getTimestamp(String table, String column) throws SQLException {
        final String sql = "select " + quote(column) + " from " + quote(table);

        return new RowTemplate<Timestamp>(this) {

            @Override
            Timestamp readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getTimestamp(1);
            }

        }.execute(sql);
    }

    public Connection getConnection() throws SQLException {
        Connection connection = dataSource.getConnection();

        try {
            connection.setAutoCommit(false);
        }
        catch (SQLException e) {

            try {
                connection.close();
            }
            catch (SQLException ignored) {
            }
        }
        return connection;
    }
}
