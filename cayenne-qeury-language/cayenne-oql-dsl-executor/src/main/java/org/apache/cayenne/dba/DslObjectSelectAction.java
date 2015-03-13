/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cayenne.dba;

import de.jexp.jequel.sql.Sql;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.BaseSQLAction;
import org.apache.cayenne.access.jdbc.ConnectionAwareResultIterator;
import org.apache.cayenne.access.jdbc.JDBCResultIterator;
import org.apache.cayenne.access.jdbc.RowDescriptor;
import org.apache.cayenne.access.jdbc.RowDescriptorBuilder;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.query.DslObjectSelect;
import org.apache.cayenne.query.QueryMetadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * @since 4.0
 */
public class DslObjectSelectAction<T> extends BaseSQLAction {
    private final DslObjectSelect<T> query;

    public DslObjectSelectAction(DslObjectSelect<T> query, DataNode dataNode) {
        super(dataNode);
        this.query = query;
    }

    @Override
    public void performAction(Connection connection, OperationObserver observer) throws SQLException, Exception {

        final long t1 = System.currentTimeMillis();

        Sql sql = new OqlToSqlTransformer().transform(query.getSelect());

        PreparedStatement prepStmt = translator.createStatement();

        // TODO: ugly... 'createSqlString' is already called inside
        // 'createStatement', but calling resultIterator here again to store for logging purposes
        final String sqlString = translator.createSqlString();

        ResultSet rs;

        // need to run in try-catch block to close statement properly if
        // exception happens
        try {
            rs = prepStmt.executeQuery();
        } catch (Exception ex) {
            prepStmt.close();
            throw ex;
        }
        QueryMetadata queryMetaData = query.getMetaData(dataNode.getEntityResolver());
        RowDescriptor descriptor = new RowDescriptorBuilder().setColumns(translator.getResultColumns()).getDescriptor(
                dataNode.getAdapter().getExtendedTypes());

        RowReader<?> rowReader = dataNode.rowReader(descriptor, queryMetaData, translator.getAttributeOverrides());

        ResultIterator resultIterator = new JDBCResultIterator(prepStmt, rs, rowReader);

        // TODO: Should do something about closing ResultSet and PreparedStatement in this
        // method, instead of relying on DefaultResultIterator to do that later

        if (observer.isIteratedResult()) {
            resultIterator = new ConnectionAwareResultIterator(resultIterator, connection) {
                @Override
                protected void doClose() {
                    dataNode.getJdbcEventLogger().logSelectCount(rowCounter, System.currentTimeMillis() - t1, sqlString);
                    super.doClose();
                }
            };

            try {
                observer.nextRows(query, resultIterator);
            } catch (Exception ex) {
                resultIterator.close();
                throw ex;
            }
        } else {
            List<DataRow> resultRows;
            try {
                resultRows = resultIterator.allRows();
            } finally {
                resultIterator.close();
            }

            dataNode.getJdbcEventLogger().logSelectCount(resultRows.size(), System.currentTimeMillis() - t1, sqlString);

            observer.nextRows(query, resultRows);
        }
    }
}
