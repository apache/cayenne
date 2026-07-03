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

package org.apache.cayenne.access.jdbc;

import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.translator.select.TranslatedSelect;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.query.PrefetchProcessor;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.Select;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

/**
 * A SQLAction that handles SelectQuery execution.
 *
 * @since 1.2
 */
public class SelectAction extends BaseSQLAction {

    protected Select<?> query;
    protected QueryMetadata queryMetadata;

    /**
     * @since 4.0
     */
    public SelectAction(Select<?> query, DataNode dataNode) {
        super(dataNode);
        this.query = query;
        this.queryMetadata = query.getMetaData(dataNode.getEntityResolver());
    }

    @Override
    public void performAction(Connection connection, OperationObserver observer) throws Exception {
        TranslatedSelect translated = dataNode.getSelectTranslator().translate(query, dataNode.getAdapter(), dataNode.getEntityResolver());
        performAction(connection, observer, translated);
    }

    protected void performAction(Connection connection, OperationObserver observer, TranslatedSelect translated) throws Exception {

        long t1 = System.currentTimeMillis();

        JdbcEventLogger logger = dataNode.getJdbcEventLogger();

        logger.logQuery(translated.sql(), translated.bindings());

        DbAdapter adapter = dataNode.getAdapter();
        PreparedStatement statement = connection.prepareStatement(translated.sql());

        for (PSParameter<?> b : translated.bindings()) {

            // null DbAttributes are a result of inferior qualifier
            // processing (qualifier can't map parameters to DbAttributes
            // and therefore only supports standard java types now) hence, a
            // special moronic case here:
            if (b.attribute() == null) {
                statement.setObject(b.psPosition(), b.value());
            } else {
                adapter.bindParameter(statement, b);
            }
        }
        
        int fetchSize = queryMetadata.getStatementFetchSize();
        if (fetchSize != 0) {
            statement.setFetchSize(fetchSize);
        }

        int queryTimeout = queryMetadata.getQueryTimeout();
        if (queryTimeout != QueryMetadata.QUERY_TIMEOUT_DEFAULT) {
            statement.setQueryTimeout(queryTimeout);
        }

        ResultSet rs;

        // need to run in try-catch block to close statement properly if
        // exception happens
        try {
            rs = statement.executeQuery();
        } catch (Exception ex) {
            statement.close();
            throw ex;
        }

        RowReader<?> rowReader = dataNode.getRowReaderFactory().rowReader(translated.resultColumns(), queryMetadata, dataNode.getAdapter());

        ResultIterator<?> it = new RSIterator<>(statement, rs, rowReader);
        it = forIteratedResult(it, observer, connection, t1, translated.sql());
        it = forSuppressedDistinct(it, translated);
        it = forFetchLimit(it, translated);

        // TODO: Should do something about closing ResultSet and
        // PreparedStatement in this method, instead of relying on
        // DefaultResultIterator to do that later

        if (observer.isIteratedResult()) {
            try {
                observer.nextRows(query, it);
            } catch (Exception ex) {
                it.close();
                throw ex;
            }
        } else {
            List<?> resultRows;
            try {
                resultRows = it.allRows();
            } finally {
                it.close();
            }

            dataNode.getJdbcEventLogger().logSelectCount(resultRows.size(), System.currentTimeMillis() - t1, translated.sql());

            observer.nextRows(query, resultRows);
        }
    }

    private <T> ResultIterator<T> forIteratedResult(ResultIterator<T> iterator, OperationObserver observer,
                                                    Connection connection, final long queryStartedAt, final String sql) {
        if (!observer.isIteratedResult()) {
            return iterator;
        }

        return new ConnectionAwareResultIterator<>(iterator, connection) {
            @Override
            protected void doClose() {
                dataNode.getJdbcEventLogger().logSelectCount(rowCounter, System.currentTimeMillis() - queryStartedAt, sql);
                super.doClose();
            }
        };
    }

    private <T> ResultIterator<T> forFetchLimit(ResultIterator<T> iterator, TranslatedSelect translated) {
        // wrap iterator in a fetch limit checker ... there are a few cases when
        // in-memory fetch limit is a noop, however in a general case this is
        // needed, as the SQL result count does not directly correspond to the
        // number of objects returned from Cayenne.

        int fetchLimit = queryMetadata.getFetchLimit();
        int offset = translated.suppressingDistinct()
                ? queryMetadata.getFetchOffset()
                : getInMemoryOffset(queryMetadata.getFetchOffset());

        if (fetchLimit > 0 || offset > 0) {
            return new LimitResultIterator<>(iterator, offset, fetchLimit);
        } else {
            return iterator;
        }
    }

    private <T> ResultIterator<T> forSuppressedDistinct(ResultIterator<T> iterator, TranslatedSelect translated) {
        if (!translated.suppressingDistinct() ||
                queryMetadata.isSuppressingDistinct()) {
            return iterator;
        }

        // wrap result iterator if distinct has to be suppressed

        // a joint prefetch warrants full row compare
        final boolean[] compareFullRows = new boolean[1];
        compareFullRows[0] = translated.hasJoins();

        final PrefetchTreeNode rootPrefetch = queryMetadata.getPrefetchTree();
        if (!compareFullRows[0] && rootPrefetch != null) {
            rootPrefetch.traverse(new PrefetchProcessor() {

                @Override
                public void finishPrefetch(PrefetchTreeNode node) {
                }

                @Override
                public boolean startDisjointPrefetch(PrefetchTreeNode node) {
                    // continue to children only if we are at root
                    return rootPrefetch == node;
                }

                @Override
                public boolean startDisjointByIdPrefetch(PrefetchTreeNode node) {
                    // continue to children only if we are at root
                    return rootPrefetch == node;
                }

                @Override
                public boolean startUnknownPrefetch(PrefetchTreeNode node) {
                    // continue to children only if we are at root
                    return rootPrefetch == node;
                }

                @Override
                public boolean startJointPrefetch(PrefetchTreeNode node) {
                    if (rootPrefetch != node) {
                        compareFullRows[0] = true;
                        return false;
                    }

                    return true;
                }

                @Override
                public boolean startPhantomPrefetch(PrefetchTreeNode node) {
                    return true;
                }
            });
        }

        return new DistinctResultIterator<>(iterator, queryMetadata.getDbEntity(), compareFullRows[0]);
    }

}
