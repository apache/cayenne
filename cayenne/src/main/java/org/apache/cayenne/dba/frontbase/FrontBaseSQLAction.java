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
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.translator.TranslatedSQL;
import org.apache.cayenne.access.jdbc.DataNodeSQLAction;

import java.sql.*;
import java.util.Collection;

/**
 * @since 4.2
 */
public class FrontBaseSQLAction extends DataNodeSQLAction {
    public FrontBaseSQLAction(TranslatedSQL translated, DataNode dataNode) {
        super(translated, dataNode);
    }

    @Override
    protected void executeStatement(Connection connection, StatementContext context, Collection<Number> updateCounts)
            throws Exception {

        long t1 = System.currentTimeMillis();
        boolean iteratedResult = context.isIteratedResult();
        PreparedStatement statement = connection.prepareStatement(translated.sql());

        try {
            bind(statement, translated.bindings());

            // process a mix of results
            boolean isResultSet = statement.execute();

            if(query.returnGeneratedKeys()) {
                ResultSet generatedKeysResultSet = statement.getGeneratedKeys();
                if (generatedKeysResultSet != null) {
                    processSelectResult(connection, statement, generatedKeysResultSet, context, t1);
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
                            processSelectResult(connection, statement, resultSet, context, t1);
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
                }
            }
        } finally {
            if (!iteratedResult) {
                statement.close();
            }
        }
    }
}
