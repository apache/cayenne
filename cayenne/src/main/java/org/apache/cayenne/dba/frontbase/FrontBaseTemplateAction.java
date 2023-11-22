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

package org.apache.cayenne.dba.frontbase;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.SQLStatement;
import org.apache.cayenne.access.jdbc.SQLTemplateAction;
import org.apache.cayenne.query.SQLTemplate;

import java.sql.*;
import java.util.Collection;

/**
 * @since 4.2
 */
public class FrontBaseTemplateAction extends SQLTemplateAction {
    /**
     * @param query
     * @param dataNode
     * @since 4.2
     */
    public FrontBaseTemplateAction(SQLTemplate query, DataNode dataNode) {
        super(query, dataNode);
    }

    @Override
    protected void execute(Connection connection, OperationObserver callback, SQLStatement compiled,
                 Collection<Number> updateCounts) throws SQLException, Exception {

        long t1 = System.currentTimeMillis();
        boolean iteratedResult = callback.isIteratedResult();
        PreparedStatement statement = connection.prepareStatement(compiled.getSql());

        try {
            bind(statement, compiled.getBindings());

            // process a mix of results
            boolean isResultSet = statement.execute();

            if(query.isReturnGeneratedKeys()) {
                ResultSet generatedKeysResultSet = statement.getGeneratedKeys();
                if (generatedKeysResultSet != null) {
                    processSelectResult(compiled, connection, statement, generatedKeysResultSet, callback, t1);
                }
            }

            boolean firstIteration = true;
            while (true) {
                if (firstIteration) {
                    firstIteration = false;
                } else {
                    isResultSet = statement.getMoreResults();
                }

                if (isResultSet) {

                    ResultSet resultSet = statement.getResultSet();
                    if (resultSet != null) {

                        try {
                            processSelectResult(compiled, connection, statement, resultSet, callback, t1);
                        } finally {
                            if (!iteratedResult) {
                                resultSet.close();
                            }
                        }

                        // ignore possible following update counts and bail early on iterated results
                        if (iteratedResult) {
                            break;
                        }
                    }
                } else {
                    int updateCount = statement.getUpdateCount();
                    if (updateCount == -1) {
                        break;
                    }

                    updateCounts.add(updateCount);
                    dataNode.getJdbcEventLogger().logUpdateCount(updateCount);
                }
            }
        } finally {
            if (!iteratedResult) {
                statement.close();
            }
        }
    }
}
