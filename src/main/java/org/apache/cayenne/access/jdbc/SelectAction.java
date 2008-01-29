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

package org.apache.cayenne.access.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.access.ResultIterator;
import org.apache.cayenne.access.trans.SelectTranslator;
import org.apache.cayenne.access.util.DistinctResultIterator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.PrefetchProcessor;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.DataRow;

/**
 * A SQLAction that handles SelectQuery execution.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class SelectAction extends BaseSQLAction {

    protected SelectQuery query;

    public SelectAction(SelectQuery query, DbAdapter adapter,
            EntityResolver entityResolver) {
        super(adapter, entityResolver);
        this.query = query;
    }

    protected SelectTranslator createTranslator(Connection connection) {
        SelectTranslator translator = new SelectTranslator();
        translator.setQuery(query);
        translator.setAdapter(adapter);
        translator.setEntityResolver(getEntityResolver());
        translator.setConnection(connection);
        return translator;
    }

    public void performAction(Connection connection, OperationObserver observer)
            throws SQLException, Exception {

        long t1 = System.currentTimeMillis();

        SelectTranslator translator = createTranslator(connection);
        PreparedStatement prepStmt = translator.createStatement();
        ResultSet rs = prepStmt.executeQuery();

        RowDescriptor descriptor = new RowDescriptor(
                translator.getResultColumns(),
                getAdapter().getExtendedTypes());
        JDBCResultIterator workerIterator = new JDBCResultIterator(
                connection,
                prepStmt,
                rs,
                descriptor,
                query.getFetchLimit());

        workerIterator.setPostProcessor(DataRowPostProcessor
                .createPostProcessor(translator));

        ResultIterator it = workerIterator;

        // wrap result iterator if distinct has to be suppressed
        if (translator.isSuppressingDistinct()) {

            // a joint prefetch warrants full row compare

            final boolean[] compareFullRows = new boolean[1];

            QueryMetadata md = query.getMetaData(getEntityResolver());
            final PrefetchTreeNode rootPrefetch = md.getPrefetchTree();

            if (rootPrefetch != null) {
                rootPrefetch.traverse(new PrefetchProcessor() {

                    public void finishPrefetch(PrefetchTreeNode node) {
                    }

                    public boolean startDisjointPrefetch(PrefetchTreeNode node) {
                        // continue to children only if we are at root
                        return rootPrefetch == node;
                    }

                    public boolean startUnknownPrefetch(PrefetchTreeNode node) {
                        // continue to children only if we are at root
                        return rootPrefetch == node;
                    }

                    public boolean startJointPrefetch(PrefetchTreeNode node) {
                        if (rootPrefetch != node) {
                            compareFullRows[0] = true;
                            return false;
                        }

                        return true;
                    }

                    public boolean startPhantomPrefetch(PrefetchTreeNode node) {
                        return true;
                    }
                });
            }

            it = new DistinctResultIterator(
                    workerIterator,
                    translator.getRootDbEntity(),
                    compareFullRows[0]);
        }

        // TODO: Should do something about closing ResultSet and PreparedStatement in this
        // method, instead of relying on DefaultResultIterator to do that later

        if (!observer.isIteratedResult()) {
            // note that we don't need to close ResultIterator
            // since "dataRows" will do it internally
            List<DataRow> resultRows = it.dataRows(true);
            QueryLogger
                    .logSelectCount(resultRows.size(), System.currentTimeMillis() - t1);

            observer.nextDataRows(query, resultRows);
        }
        else {
            try {
                workerIterator.setClosingConnection(true);
                observer.nextDataRows(translator.getQuery(), it);
            }
            catch (Exception ex) {
                it.close();
                throw ex;
            }
        }
    }
}
