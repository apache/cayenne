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

package org.apache.cayenne.dba.sqlserver;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.BatchAction;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;

/**
 * @since 1.2
 */
public class SQLServerBatchAction extends BatchAction {

    public SQLServerBatchAction(BatchQuery batchQuery, DataNode dataNode) {
        super(batchQuery, dataNode);
    }

    @Override
    public void performAction(Connection connection, OperationObserver observer)
            throws SQLException, Exception {

        // this condition checks if identity columns are present in the query and adapter
        // is not ready to process them... e.g. if we are using a MS driver...
        boolean identityOverride = expectsToOverrideIdentityColumns();
        if (identityOverride) {
            setIdentityInsert(connection, true);
        }

        try {
            super.performAction(connection, observer);
        }
        finally {

            // important: turn off IDENTITY_INSERT as SQL Server won't be able to process
            // other identity columns in the same transaction

            // TODO: if an error happens here this would mask the parent error
            if (identityOverride) {
                setIdentityInsert(connection, false);
            }
        }
    }

    protected void setIdentityInsert(Connection connection, boolean on)
            throws SQLException {

        String flag = on ? " ON" : " OFF";
        String configSQL = "SET IDENTITY_INSERT "
                + query.getDbEntity().getFullyQualifiedName()
                + flag;

        dataNode.getJdbcEventLogger().logQuery(configSQL, Collections.EMPTY_LIST);

        Statement statement = connection.createStatement();
        try {
            statement.execute(configSQL);
        }
        finally {
            try {
                statement.close();
            }
            catch (Exception e) {
            }
        }
    }

    /**
     * Returns whether a table has identity columns.
     */
    protected boolean expectsToOverrideIdentityColumns() {
        // jTDS driver supports identity columns, no need for tricks...
        if (dataNode.getAdapter().supportsGeneratedKeys()) {
            return false;
        }

        if (!(query instanceof InsertBatchQuery) || query.getDbEntity() == null) {
            return false;
        }

        // find identity attributes
        for (DbAttribute attribute : query.getDbEntity().getAttributes()) {
            if (attribute.isGenerated()) {
                return true;
            }
        }

        return false;
    }
}
