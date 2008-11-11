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
package org.apache.cayenne.itest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;

import javax.sql.DataSource;

/**
 * JDBC utilities for integration testing that bypass Cayenne for DB access.
 * 
 */
public class ItestDBUtils {

    protected DataSource dataSource;

    public ItestDBUtils(DataSource dataSource) {
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
            try {
                for (int i = 0; i < values.length; i++) {
                    st.setObject(i + 1, values[i]);
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
        
        final int[] result = new int[1];
        RowTemplate template = new RowTemplate(this) {

            @Override
            void readRow(ResultSet rs, String sql) throws SQLException {
                result[0] = rs.getInt(1);
            }
        };

        template.execute(sql);
        return result[0];
    }

    public Object getObject(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        final Object[] result = new Object[1];
        RowTemplate template = new RowTemplate(this) {

            @Override
            void readRow(ResultSet rs, String sql) throws SQLException {
                result[0] = rs.getObject(1);
            }
        };

        template.execute(sql);
        return result[0];
    }

    public byte getByte(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        final byte[] result = new byte[1];

        RowTemplate template = new RowTemplate(this) {

            @Override
            void readRow(ResultSet rs, String sql) throws SQLException {
                result[0] = rs.getByte(1);
            }
        };

        template.execute(sql);
        return result[0];
    }

    public byte[] getBytes(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        final byte[][] result = new byte[1][];

        RowTemplate template = new RowTemplate(this) {

            @Override
            void readRow(ResultSet rs, String sql) throws SQLException {
                result[0] = rs.getBytes(1);
            }
        };

        template.execute(sql);
        return result[0];
    }

    public int getInt(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        final int[] result = new int[1];

        RowTemplate template = new RowTemplate(this) {

            @Override
            void readRow(ResultSet rs, String sql) throws SQLException {
                result[0] = rs.getInt(1);
            }
        };

        template.execute(sql);
        return result[0];
    }

    public long getLong(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        final long[] result = new long[1];

        RowTemplate template = new RowTemplate(this) {

            @Override
            void readRow(ResultSet rs, String sql) throws SQLException {
                result[0] = rs.getLong(1);
            }
        };

        template.execute(sql);
        return result[0];
    }

    public double getDouble(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        final double[] result = new double[1];

        RowTemplate template = new RowTemplate(this) {

            @Override
            void readRow(ResultSet rs, String sql) throws SQLException {
                result[0] = rs.getDouble(1);
            }
        };

        template.execute(sql);
        return result[0];
    }

    public boolean getBoolean(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        final boolean[] result = new boolean[1];

        RowTemplate template = new RowTemplate(this) {

            @Override
            void readRow(ResultSet rs, String sql) throws SQLException {
                result[0] = rs.getBoolean(1);
            }
        };

        template.execute(sql);
        return result[0];
    }

    public java.util.Date getUtilDate(String table, String column) throws SQLException {
        Timestamp ts = getTimestamp(table, column);
        return ts != null ? new java.util.Date(ts.getTime()) : null;
    }

    public java.sql.Date getSqlDate(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        final java.sql.Date[] result = new java.sql.Date[1];

        RowTemplate template = new RowTemplate(this) {

            @Override
            void readRow(ResultSet rs, String sql) throws SQLException {
                result[0] = rs.getDate(1);
            }
        };

        template.execute(sql);
        return result[0];
    }

    public Time getTime(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        final Time[] result = new Time[1];

        RowTemplate template = new RowTemplate(this) {

            @Override
            void readRow(ResultSet rs, String sql) throws SQLException {
                result[0] = rs.getTime(1);
            }
        };

        template.execute(sql);
        return result[0];
    }

    public Timestamp getTimestamp(String table, String column) throws SQLException {
        final String sql = "select " + column + " from " + table;

        final Timestamp[] result = new Timestamp[1];

        RowTemplate template = new RowTemplate(this) {

            @Override
            void readRow(ResultSet rs, String sql) throws SQLException {
                result[0] = rs.getTimestamp(1);
            }
        };

        template.execute(sql);
        return result[0];
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
