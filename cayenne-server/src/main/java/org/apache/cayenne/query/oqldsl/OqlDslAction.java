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
package org.apache.cayenne.query.oqldsl;

import de.jexp.jequel.sql.Sql;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.BaseSQLAction;
import org.apache.cayenne.query.sqldsl.SqlDslAction;
import org.apache.cayenne.query.sqldsl.SqlQuery;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @since 4.0
 */
public class OqlDslAction extends BaseSQLAction {

    protected OqlQuery<?> query;

    /**
     * @since 4.0
     */
    public OqlDslAction(OqlQuery<?> query, DataNode dataNode) {
        super(dataNode);
        this.query = query;
    }

    @Override
    public void performAction(Connection connection, OperationObserver observer) throws SQLException, Exception {

        Sql sql = new OqlToSqlTransformer().transform(query.getSelect(), query.getDataMap());

        // TODO do it trought visitor
        new SqlDslAction(new SqlQuery(sql), dataNode).performAction(connection, observer);



        /*PreparedStatement prepStmt = translator.createStatement();

        ResultSet rs;

        // need to run in try-catch block to close statement properly if
        // exception happens
        try {
            rs = prepStmt.executeQuery();
        } catch (Exception ex) {
            prepStmt.close();
            throw ex;
        }

        QueryMetadata md = query.getMetaData(dataNode.getEntityResolver());
        RowDescriptor descriptor = new RowDescriptorBuilder()
                .setColumns(translator.getResultColumns())
                .getDescriptor(dataNode.getAdapter().getExtendedTypes());

        RowReader<?> rowReader = dataNode.rowReader(descriptor, md, Collections.<ObjAttribute, ColumnDescriptor>emptyMap());

        JDBCResultIterator workerIterator = new JDBCResultIterator(prepStmt, rs, rowReader);
        ResultIterator it = workerIterator;

        if (observer.isIteratedResult()) {
            it = new ConnectionAwareResultIterator(it, connection) {
                @Override
                protected void doClose() {
                    super.doClose();
                }
            };
        }

        // TODO: Should do something about closing ResultSet and
        // PreparedStatement in this
        // method, instead of relying on DefaultResultIterator to do that later
        if (observer.isIteratedResult()) {
            try {
                observer.nextRows(query, it);
            } catch (Exception ex) {
                it.close();
                throw ex;
            }
        } else {
            List<DataRow> resultRows;
            try {
                resultRows = it.allRows();
            } finally {
                it.close();
            }

            observer.nextRows(query, resultRows);
        }*/
    }
}
