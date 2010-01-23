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
package org.apache.cayenne.test;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;

import javax.sql.DataSource;

/**
 * JDBC utilities class for setting up and analyzing the DB data sets. DBHelper
 * intentionally bypasses Cayenne stack.
 */
public class DBHelper {

    protected DataSource dataSource;

    public DBHelper(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Inserts a single row.
     */
    public void insert(String table, String[] columns, Object[] values)
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
        sql.append(table).append(" (").append(columns[0]);
        for (int i = 1; i < columns.length; i++) {
            sql.append(", ").append(columns[i]);
        }

        sql.append(") VALUES (?");
        for (int i = 1; i < values.length; i++) {
            sql.append(", ?");
        }
        sql.append(")");

        Connection c = getConnection();
        try {

            PreparedStatement st = c.prepareStatement(sql.toString());
            ParameterMetaData parameters = null;
            try {
                for (int i = 0; i < values.length; i++) {

                    if (values[i] == null) {

                        // check for the right NULL type
                        if (parameters == null) {
                            parameters = st.getParameterMetaData();
                        }

                        st.setNull(i + 1, parameters.getParameterType(i + 1));
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

    public int deleteAll(String table) throws SQLException {
        String sql = "delete from " + table;

        Connection c = getConnection();
        try {

            Statement st = c.createStatement();
            int count = st.executeUpdate(sql);
            st.close();
            c.commit();

            return count;
        }
        finally {
            c.close();
        }
    }

    public int getRowCount(String table) throws SQLException {
        String sql = "select count(*) from " + table;

        return new RowTemplate<Integer>(this) {

            @Override
            Integer readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getInt(1);
            }

        }.execute(sql);
    }

    public Object getObject(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        return new RowTemplate<Object>(this) {

            @Override
            Object readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getObject(1);
            }

        }.execute(sql);
    }

    public byte getByte(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        return new RowTemplate<Byte>(this) {

            @Override
            Byte readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getByte(1);
            }

        }.execute(sql);
    }

    public byte[] getBytes(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        return new RowTemplate<byte[]>(this) {

            @Override
            byte[] readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getBytes(1);
            }

        }.execute(sql);
    }

    public int getInt(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        return new RowTemplate<Integer>(this) {

            @Override
            Integer readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getInt(1);
            }

        }.execute(sql);
    }

    public long getLong(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        return new RowTemplate<Long>(this) {

            @Override
            Long readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getLong(1);
            }

        }.execute(sql);
    }

    public double getDouble(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        return new RowTemplate<Double>(this) {

            @Override
            Double readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getDouble(1);
            }

        }.execute(sql);
    }

    public boolean getBoolean(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

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
        final String sql = "select " + column + " from " + table;

        return new RowTemplate<java.sql.Date>(this) {

            @Override
            java.sql.Date readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getDate(1);
            }

        }.execute(sql);
    }

    public Time getTime(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        return new RowTemplate<Time>(this) {

            @Override
            Time readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getTime(1);
            }

        }.execute(sql);
    }

    public Timestamp getTimestamp(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        return new RowTemplate<Timestamp>(this) {

            @Override
            Timestamp readRow(ResultSet rs, String sql) throws SQLException {
                return rs.getTimestamp(1);
            }

        }.execute(sql);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
