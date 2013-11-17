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

import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * JDBC utility class for setting up and analyzing the DB data sets for a single table.
 * TableHelper intentionally bypasses Cayenne stack.
 */
public class TableHelper {

    protected String tableName;
    protected DBHelper dbHelper;
    protected String[] columns;
    protected int[] columnTypes;

    public TableHelper(DBHelper dbHelper, String tableName) {
        this.dbHelper = dbHelper;
        this.tableName = tableName;
    }

    public TableHelper(DBHelper dbHelper, String tableName, String... columns) {
        this(dbHelper, tableName);
        setColumns(columns);
    }

    public UpdateBuilder update() throws SQLException {
        return dbHelper.update(tableName);
    }

    public DeleteBuilder delete() {
        return dbHelper.delete(tableName);
    }

    public int deleteAll() throws SQLException {
        return dbHelper.deleteAll(tableName);
    }

    public String getTableName() {
        return tableName;
    }

    /**
     * Sets columns that will be implicitly used in subsequent inserts and selects.
     */
    public TableHelper setColumns(String... columns) {
        this.columns = columns;
        return this;
    }

    /**
     * Sets JDBC types of the table columns. Setting column types may be needed when
     * inserting NULL values in certain DB's (Oracle) that can't figure out parameter
     * types from PreparedStatement metadata.
     */
    public TableHelper setColumnTypes(int... columnTypes) {
        this.columnTypes = columnTypes;
        return this;
    }

    public TableHelper insert(Object... values) throws SQLException {
        if (this.columns == null) {
            throw new IllegalStateException("Call 'setColumns' to prepare insert");
        }

        if (this.columns.length != values.length) {
            throw new IllegalArgumentException(
                    "Columns and values arrays are of different size");
        }

        if (columnTypes != null && columns.length != columnTypes.length) {
            throw new IllegalArgumentException(
                    "Columns and columnTypes arrays are of different size");
        }

        dbHelper.insert(tableName, columns, values, columnTypes);

        return this;
    }

    /**
     * Selects a single row from the mapped table.
     */
    public Object[] select() throws SQLException {
        return dbHelper.select(tableName, columns);
    }

    public int getRowCount() throws SQLException {
        return dbHelper.getRowCount(tableName);
    }

    public Object getObject(String column) throws SQLException {
        return dbHelper.getObject(tableName, column);
    }

    public byte getByte(String column) throws SQLException {
        return dbHelper.getByte(tableName, column);
    }

    public byte[] getBytes(String column) throws SQLException {
        return dbHelper.getBytes(tableName, column);
    }

    public int getInt(String column) throws SQLException {
        return dbHelper.getInt(tableName, column);
    }

    public long getLong(String column) throws SQLException {
        return dbHelper.getLong(tableName, column);
    }

    public double getDouble(String column) throws SQLException {
        return dbHelper.getDouble(tableName, column);
    }

    public boolean getBoolean(String column) throws SQLException {
        return dbHelper.getBoolean(tableName, column);
    }

    public String getString(String column) throws SQLException {
        return dbHelper.getString(tableName, column);
    }

    public java.util.Date getUtilDate(String column) throws SQLException {
        return dbHelper.getUtilDate(tableName, column);
    }

    public java.sql.Date getSqlDate(String column) throws SQLException {
        return dbHelper.getSqlDate(tableName, column);
    }

    public Time getTime(String column) throws SQLException {
        return dbHelper.getTime(tableName, column);
    }

    public Timestamp getTimestamp(String column) throws SQLException {
        return dbHelper.getTimestamp(tableName, column);
    }
}
