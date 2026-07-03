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
package org.apache.cayenne.dba.sqlite;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.translator.TranslatedSQL;
import org.apache.cayenne.access.jdbc.SQLTemplateAction;
import org.apache.cayenne.query.SQLTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;

/**
 * @since 3.0
 */
class SQLiteSQLTemplateAction extends SQLTemplateAction {

    public SQLiteSQLTemplateAction(SQLTemplate query, DataNode dataNode) {
        super(query, dataNode);
    }

    /**
     * Overrides super implementation to guess whether the query is selecting or not and
     * execute it appropriately. Super implementation relied on generic JDBC mechanism,
     * common for selecting and updating statements that does not work in SQLite drivers.
     */
    @Override
    protected void execute(
            Connection connection,
            OperationObserver callback,
            TranslatedSQL compiled,
            Collection<Number> updateCounts) throws Exception {

        String sql = compiled.sql().trim();
        boolean select = sql.length() > "SELECT".length()
                && sql.substring(0, "SELECT".length()).equalsIgnoreCase("SELECT");

        long t1 = System.currentTimeMillis();
        boolean iteratedResult = callback.isIteratedResult();
        PreparedStatement statement = connection.prepareStatement(compiled.sql());
        try {
            bind(statement, compiled.bindings());

            // start - code different from super
            if (select) {

                ResultSet resultSet = statement.executeQuery();
                try {
                    processSelectResult(
                            compiled,
                            connection,
                            statement,
                            resultSet,
                            callback,
                            t1);
                }
                finally {
                    if (!iteratedResult) {
                        resultSet.close();
                    }
                }
            }
            else {
                int updateCount = statement.executeUpdate();
                updateCounts.add(updateCount);
            }

            // end - code different from super
        }
        finally {
            if (!iteratedResult) {
                statement.close();
            }
        }
    }

}
