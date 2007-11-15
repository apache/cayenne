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
package org.apache.cayenne.merge;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.validation.SimpleValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

public class ExecutingMergerContext implements MergerContext {

    private DataMap map;
    private DbAdapter adapter;
    private DataSource dataSource;
    private ValidationResult result = new ValidationResult();

    public ExecutingMergerContext(DataMap map, DataNode node) {
        this.map = map;
        this.dataSource = node.getDataSource();
        this.adapter = node.getAdapter();
    }

    public ExecutingMergerContext(DataMap map, DataSource dataSource, DbAdapter adapter) {
        this.map = map;
        this.dataSource = dataSource;
        this.adapter = adapter;
    }

    public void executeSql(String sql) {
        Connection conn = null;
        Statement st = null;
        try {
            QueryLogger.log(sql);
            conn = dataSource.getConnection();
            st = conn.createStatement();
            st.execute(sql);
        }
        catch (SQLException e) {
            result.addFailure(new SimpleValidationFailure(sql, e.getMessage()));
            QueryLogger.logQueryError(e);
        }
        finally {
            if (st != null) {
                try {
                    st.close();
                }
                catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                }
                catch (SQLException e) {
                }
            }
        }
    }

    public DbAdapter getAdapter() {
        return adapter;
    }

    public DataMap getDataMap() {
        return map;
    }

    public ValidationResult getValidationResult() {
        return result;
    }

}
