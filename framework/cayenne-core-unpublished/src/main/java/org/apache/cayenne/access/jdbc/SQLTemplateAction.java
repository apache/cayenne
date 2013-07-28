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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.util.Util;
import org.apache.commons.collections.IteratorUtils;

/**
 * Implements a strategy for execution of SQLTemplates.
 * 
 * @since 1.2 replaces SQLTemplateExecutionPlan
 */
public class SQLTemplateAction implements SQLAction {

    protected JdbcAdapter adapter;
    protected SQLTemplate query;
    protected QueryMetadata queryMetadata;

    protected DbEntity dbEntity;

    /**
     * @since 3.0
     */
    public SQLTemplateAction(SQLTemplate query, JdbcAdapter adapter, EntityResolver entityResolver) {
        this.query = query;
        this.adapter = adapter;
        this.queryMetadata = query.getMetaData(entityResolver);
        this.dbEntity = query.getMetaData(entityResolver).getDbEntity();
    }

    /**
     * Returns DbAdapter associated with this execution plan object.
     */
    public DbAdapter getAdapter() {
        return adapter;
    }

    /**
     * Runs a SQLTemplate query, collecting all results. If a callback expects
     * an iterated result, result processing is stopped after the first
     * ResultSet is encountered.
     */
    public void performAction(Connection connection, OperationObserver callback) throws SQLException, Exception {

        String template = extractTemplateString();

        // sanity check - misconfigured templates
        if (template == null) {
            throw new CayenneException("No template string configured for adapter " + getAdapter().getClass().getName());
        }

        boolean loggable = adapter.getJdbcEventLogger().isLoggable();
        int size = query.parametersSize();

        SQLTemplateProcessor templateProcessor = new SQLTemplateProcessor();

        // zero size indicates a one-shot query with no parameters
        // so fake a single entry batch...
        int batchSize = (size > 0) ? size : 1;

        List<Number> counts = new ArrayList<Number>(batchSize);
        Iterator<?> it = (size > 0) ? query.parametersIterator() : IteratorUtils
                .singletonIterator(Collections.EMPTY_MAP);
        for (int i = 0; i < batchSize; i++) {
            Map nextParameters = (Map) it.next();

            SQLStatement compiled = templateProcessor.processTemplate(template, nextParameters);

            if (loggable) {
                adapter.getJdbcEventLogger().logQuery(compiled.getSql(), Arrays.asList(compiled.getBindings()));
            }

            execute(connection, callback, compiled, counts);
        }

        // notify of combined counts of all queries inside SQLTemplate
        // multiplied by the
        // number of parameter sets...
        int[] ints = new int[counts.size()];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = counts.get(i).intValue();
        }

        callback.nextBatchCount(query, ints);
    }

    protected void execute(Connection connection, OperationObserver callback, SQLStatement compiled,
            Collection<Number> updateCounts) throws SQLException, Exception {

        long t1 = System.currentTimeMillis();
        boolean iteratedResult = callback.isIteratedResult();
        PreparedStatement statement = connection.prepareStatement(compiled.getSql());
        try {
            bind(statement, compiled.getBindings());

            // process a mix of results
            boolean isResultSet = statement.execute();
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

                        // ignore possible following update counts and bail
                        // early on
                        // iterated results
                        if (iteratedResult) {
                            break;
                        }
                    }
                } else {
                    int updateCount = statement.getUpdateCount();
                    if (updateCount == -1) {
                        break;
                    }

                    updateCounts.add(Integer.valueOf(updateCount));
                    adapter.getJdbcEventLogger().logUpdateCount(updateCount);
                }
            }
        } finally {
            if (!iteratedResult) {
                statement.close();
            }
        }
    }

    protected void processSelectResult(SQLStatement compiled, Connection connection, Statement statement,
            ResultSet resultSet, OperationObserver callback, final long startTime) throws Exception {

        boolean iteratedResult = callback.isIteratedResult();

        ExtendedTypeMap types = getAdapter().getExtendedTypes();
        RowDescriptorBuilder builder = configureRowDescriptorBuilder(compiled, resultSet);

        JDBCResultIterator result = new JDBCResultIterator(statement, resultSet, builder.getDescriptor(types),
                queryMetadata);

        ResultIterator it = result;

        if (iteratedResult) {

            it = new ConnectionAwareResultIterator(it, connection) {
                @Override
                protected void doClose() {
                    adapter.getJdbcEventLogger().logSelectCount(rowCounter, System.currentTimeMillis() - startTime);
                    super.doClose();
                }
            };
        }

        it = new LimitResultIterator(it, getFetchOffset(), query.getFetchLimit());

        if (iteratedResult) {
            try {
                callback.nextRows(query, it);
            } catch (Exception ex) {
                it.close();
                throw ex;
            }
        } else {
            // note that we are not closing the iterator here, relying on caller
            // to close the underlying ResultSet on its own... this is a hack,
            // maybe a cleaner flow is due here.
            List<DataRow> resultRows = (List<DataRow>) it.allRows();

            adapter.getJdbcEventLogger().logSelectCount(resultRows.size(), System.currentTimeMillis() - startTime);

            callback.nextRows(query, resultRows);
        }
    }

    /**
     * @since 3.0
     */
    protected RowDescriptorBuilder configureRowDescriptorBuilder(SQLStatement compiled, ResultSet resultSet)
            throws SQLException {
        RowDescriptorBuilder builder = new RowDescriptorBuilder();
        builder.setResultSet(resultSet);

        // SQLTemplate #result columns take precedence over other ways to
        // determine the
        // type
        if (compiled.getResultColumns().length > 0) {
            builder.setColumns(compiled.getResultColumns());
        }

        ObjEntity entity = queryMetadata.getObjEntity();
        if (entity != null) {
            // TODO: andrus 2008/03/28 support flattened attributes with
            // aliases...
            for (ObjAttribute attribute : entity.getAttributes()) {
                String column = attribute.getDbAttributePath();
                if (column == null || column.indexOf('.') > 0) {
                    continue;
                }
                builder.overrideColumnType(column, attribute.getType());
            }
        }

        // override numeric Java types based on JDBC defaults for DbAttributes,
        // as
        // Oracle
        // ResultSetMetadata is not very precise about NUMERIC distinctions...
        // (BigDecimal vs Long vs. Integer)
        if (dbEntity != null) {
            for (DbAttribute attribute : dbEntity.getAttributes()) {

                if (!builder.isOverriden(attribute.getName()) && TypesMapping.isNumeric(attribute.getType())) {

                    builder.overrideColumnType(attribute.getName(), TypesMapping.getJavaBySqlType(attribute.getType()));
                }
            }
        }

        switch (query.getColumnNamesCapitalization()) {
        case LOWER:
            builder.useLowercaseColumnNames();
            break;
        case UPPER:
            builder.useUppercaseColumnNames();
            break;
        }

        return builder;
    }

    /**
     * Extracts a template string from a SQLTemplate query. Exists mainly for
     * the benefit of subclasses that can customize returned template.
     * 
     * @since 1.2
     */
    protected String extractTemplateString() {
        String sql = query.getTemplate(getAdapter().getClass().getName());

        // note that we MUST convert line breaks to spaces. On some databases
        // (DB2)
        // queries with breaks simply won't run; the rest are affected by
        // CAY-726.
        return Util.stripLineBreaks(sql, ' ');
    }

    /**
     * Binds parameters to the PreparedStatement.
     */
    protected void bind(PreparedStatement preparedStatement, ParameterBinding[] bindings) throws SQLException,
            Exception {
        // bind parameters
        if (bindings.length > 0) {
            int len = bindings.length;
            for (int i = 0; i < len; i++) {
                adapter.bindParameter(preparedStatement, bindings[i].getValue(), i + 1, bindings[i].getJdbcType(),
                        bindings[i].getScale());
            }
        }

        if (queryMetadata.getStatementFetchSize() != 0) {
            preparedStatement.setFetchSize(queryMetadata.getStatementFetchSize());
        }
    }

    /**
     * Returns a SQLTemplate for this action.
     */
    public SQLTemplate getQuery() {
        return query;
    }

    /**
     * @since 3.0
     */
    protected int getFetchOffset() {
        return query.getFetchOffset();
    }
}
