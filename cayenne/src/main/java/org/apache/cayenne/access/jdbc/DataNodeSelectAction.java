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
import org.apache.cayenne.SelectResult;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.translator.TranslatedSelect;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.query.PrefetchProcessor;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.resolved.ResolvedQuery;
import org.apache.cayenne.query.resolved.ResolvedSelect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Runs a translated select and reads its rows.
 *
 * @since 6.0
 */
public class DataNodeSelectAction {

    protected final DataNode dataNode;
    protected final TranslatedSelect translated;
    protected final ResolvedSelect query;

    public DataNodeSelectAction(TranslatedSelect translated, DataNode dataNode) {
        this.dataNode = dataNode;
        this.translated = translated;
        this.query = translated.query();
    }

    public void execute(Connection connection, StatementContext context) throws Exception {
        DbAdapter adapter = dataNode.getAdapter();
        PreparedStatement statement = connection.prepareStatement(translated.sql());
        for (PSParameter<?> p : translated.bindings()) {
            adapter.bindParameter(statement, p);
        }

        int fetchSize = statementFetchSize(context);
        if (fetchSize != 0) {
            statement.setFetchSize(fetchSize);
        }

        int queryTimeout = query.queryTimeout();
        if (queryTimeout != ResolvedQuery.QUERY_TIMEOUT_DEFAULT) {
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

        RSColumn[] columns = resultColumns(rs);
        RowReader<?> rowReader = dataNode.getRowReaderFactory()
                .rowReader(columns, translated.resultSetMapping(), query, adapter);

        ResultIterator<?> it = new RSIterator<>(statement, rs, rowReader);
        it = forIteratedResult(it, context, connection);
        it = forSuppressedDistinct(it, translated);
        it = forFetchLimit(it);

        // TODO: Should do something about closing ResultSet and
        // PreparedStatement in this method, instead of relying on
        // DefaultResultIterator to do that later
        if (context.isIteratedResult()) {
            try {
                context.addResult(it);
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
            context.addResult(new SelectResult<>(resultRows));
        }
    }

    protected RSColumn[] resultColumns(ResultSet rs) throws SQLException {
        return translated.resultColumns();
    }

    /**
     * Returns the fetch size to set on the PreparedStatement, 0 meaning "leave the driver default". The default
     * implementation returns the query's statement fetch size, adapters may override.
     */
    protected int statementFetchSize(StatementContext context) {
        return query.statementFetchSize();
    }

    private <T> ResultIterator<T> forIteratedResult(ResultIterator<T> iterator, StatementContext context,
                                                    Connection connection) {
        if (!context.isIteratedResult()) {
            return iterator;
        }
        return new ConnectionAwareResultIterator<>(iterator, connection);
    }

    /**
     * Applies the fetch limit, and the offset where the SQL did not, to the rows read. The limit is always checked in
     * memory, as the SQL row count does not directly correspond to the number of objects returned. The offset is in the
     * SQL, unless distinct is done in memory, which leaves both out of the SQL. An adapter that rewrites the offset
     * into the limit ("TOP (offset + limit)") overrides this to skip the offset rows here.
     */
    protected <T> ResultIterator<T> forFetchLimit(ResultIterator<T> iterator) {
        int offset = translated.suppressingDistinct() ? query.fetchOffset() : 0;
        int limit = query.fetchLimit();
        return limit > 0 || offset > 0 ? new LimitResultIterator<>(iterator, offset, limit) : iterator;
    }

    private <T> ResultIterator<T> forSuppressedDistinct(ResultIterator<T> iterator, TranslatedSelect translated) {
        if (!translated.suppressingDistinct() || query.suppressingDistinct()) {
            return iterator;
        }

        // wrap result iterator if distinct has to be suppressed
        // a joint prefetch warrants full row compare, and so does a result that is not made of
        // DataRows - a column select yields scalars or Object[], neither of which has an ObjectId
        final boolean[] compareFullRows = new boolean[1];
        compareFullRows[0] = translated.hasJoins() || query.resultSegments() != null;

        final PrefetchTreeNode rootPrefetch = query.prefetchTree();

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

        return new DistinctResultIterator<>(iterator, query.table(), compareFullRows[0]);
    }
}
