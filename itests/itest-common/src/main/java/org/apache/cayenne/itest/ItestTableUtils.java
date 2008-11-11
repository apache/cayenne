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

import java.sql.SQLException;

/**
 * JDBC utilities for integration testing that bypass Cayenne for DB access.
 * 
 */
public class ItestTableUtils {

    protected String tableName;
    protected ItestDBUtils dbUtils;
    protected String[] columns;

    public ItestTableUtils(ItestDBUtils dbUtils, String tableName) {
        this.dbUtils = dbUtils;
        this.tableName = tableName;
    }

    public ItestTableUtils deleteAll() throws SQLException {
        dbUtils.deleteAll(tableName);
        return this;
    }

    public ItestTableUtils setColumns(String... columns) {
        this.columns = columns;
        return this;
    }

    public ItestTableUtils insert(Object... values) throws SQLException {
        if (this.columns == null) {
            throw new IllegalStateException("Call 'setColumns' to prepare insert");
        }

        if (this.columns.length != values.length) {
            throw new IllegalArgumentException(
                    "Columns and values arrays are of different size");
        }

        dbUtils.insert(tableName, columns, values);
        return this;
    }

}
