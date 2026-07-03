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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.translator.TranslatedSQL;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DefaultScalarResultSegment;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.util.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implements a strategy for execution of SQLTemplates.
 *
 * @since 1.2 replaces SQLTemplateExecutionPlan
 */
public class SQLTemplateAction implements SQLAction {

    protected SQLTemplate query;
    protected QueryMetadata queryMetadata;

    protected DbEntity dbEntity;
    protected DataNode dataNode;
    protected DbAdapter dbAdapter;

    /**
     * @since 4.0
     */
    public SQLTemplateAction(SQLTemplate query, DataNode dataNode) {
        this.query = query;
        this.dataNode = dataNode;
        this.queryMetadata = query.getMetaData(dataNode.getEntityResolver());
        this.dbEntity = queryMetadata.getDbEntity();

        // using unwrapped adapter to check for the right SQL flavor...
        this.dbAdapter = dataNode.getAdapter().unwrap();
    }

    /**
     * Returns unwrapped DbAdapter used to find correct SQL for a given DB.
     */
    public DbAdapter getAdapter() {
        return dbAdapter;
    }

    /**
     * Runs a SQLTemplate query, collecting all results. If a callback expects
     * an iterated result, result processing is stopped after the first
     * ResultSet is encountered.
     */
    @Override
    public void performAction(Connection connection, OperationObserver callback) throws Exception {

        String template = extractTemplateString();

        // sanity check - misconfigured templates
        if (template == null) {
            throw new CayenneRuntimeException("No template string configured for adapter " + dbAdapter.getClass().getName());
        }

        List<Number> counts = new ArrayList<>();

        // bind either positional or named parameters;
        // for legacy reasons named parameters are processed as a batch.. this
        // should go away after 4.0; newer positional parameter only support a
        // single set of values.
        if (query.getPositionalParams().isEmpty()) {
            runWithNamedParameters(connection, callback, template, counts);
        } else {
            runWithPositionalParameters(connection, callback, template, counts);
        }

        // notify of combined counts of all queries inside SQLTemplate
        // multiplied by the number of parameter sets...
        int[] ints = new int[counts.size()];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = counts.get(i).intValue();
        }

        callback.nextBatchCount(query, ints);
    }

    private void runWithPositionalParameters(Connection connection, OperationObserver callback, String template,
                                             Collection<Number> counts) throws Exception {

        TranslatedSQL compiled = dataNode.getSqlTemplateTranslator().translate(template,
                query.getPositionalParams(), getAdapter());

        callback.nextStatement(query, compiled);

        execute(connection, callback, compiled, counts);
    }

    private void runWithNamedParameters(
            Connection connection,
            OperationObserver callback,
            String template,
            Collection<Number> counts) throws Exception {

        if (query.parametersSize() == 0) {
            runParametersBatch(connection, callback, template, counts, Map.of());
        } else {
            Iterator<Map<String, ?>> it = query.parametersIterator();
            while (it.hasNext()) {
                runParametersBatch(connection, callback, template, counts, it.next());
            }
        }
    }

    private void runParametersBatch(
            Connection connection,
            OperationObserver callback,
            String template,
            Collection<Number> counts,
            Map<String, ?> nextParameters) throws Exception {

        TranslatedSQL compiled = dataNode.getSqlTemplateTranslator().translate(template, nextParameters, getAdapter());
        callback.nextStatement(query, compiled);

        execute(connection, callback, compiled, counts);
    }

    protected void execute(Connection connection, OperationObserver callback, TranslatedSQL compiled,
                           Collection<Number> updateCounts) throws Exception {

        long t1 = System.currentTimeMillis();
        boolean iteratedResult = callback.isIteratedResult();
        int generatedKeys = query.isReturnGeneratedKeys() ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS;
        PreparedStatement statement = connection.prepareStatement(compiled.sql(), generatedKeys);

        try {
            bind(statement, compiled.bindings());

            // process a mix of results
            boolean isResultSet = statement.execute();

            if (query.isReturnGeneratedKeys()) {
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
                }
            }
        } finally {
            if (!iteratedResult) {
                statement.close();
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void processSelectResult(
            TranslatedSQL compiled,
            Connection connection, Statement statement,
            ResultSet resultSet,
            OperationObserver callback,
            long startTime) throws Exception {

        recreateQueryMetadata(resultSet);
        boolean iteratedResult = callback.isIteratedResult();
        RSColumn[] columns = rowBuilder(compiled, resultSet).build(dataNode.getAdapter().getExtendedTypes());

        RowReader<?> rowReader = dataNode.getRowReaderFactory().rowReader(columns, queryMetadata, dataNode.getAdapter());
        ResultIterator<?> it = new RSIterator<>(statement, resultSet, rowReader);

        if (iteratedResult) {
            it = new ConnectionAwareResultIterator(it, connection);
        }

        it = new LimitResultIterator<>(it, getFetchOffset(), query.getFetchLimit());

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
            List<?> resultRows = it.allRows();

            callback.nextRows(query, resultRows);
        }
    }

    private void recreateQueryMetadata(ResultSet resultSet) throws SQLException {
        if (query.isUseScalar() && queryMetadata.getResultSetMapping() != null && queryMetadata.getResultSetMapping().isEmpty()) {
            for (int i = 0; i < resultSet.getMetaData().getColumnCount(); i++) {
                queryMetadata.getResultSetMapping().add(new DefaultScalarResultSegment(String.valueOf(i), i));
            }
        }
    }

    /**
     * Creates column descriptors based on compiled statement and query metadata
     */
    private RSColumn[] createColumnDescriptors(TranslatedSQL compiled) {
        // SQLTemplate #result columns take precedence over other ways to determine the type
        if (compiled.resultColumns().length > 0) {
            if (query.getResultColumnsTypes() != null) {
                throw new CayenneRuntimeException("Caused by setting return types by directives and by parameters in query.");
            } else {
                return compiled.resultColumns();
            }
        }

        // check explicitly set column types
        if (query.getResultColumnsTypes() == null) {
            return null;
        }

        ExtendedTypeMap extendedTypes = dataNode.getAdapter().getExtendedTypes();
        int size = query.getResultColumnsTypes().size();
        RSColumn[] columns = new RSColumn[size];
        for (int i = 0; i < size; i++) {
            // only the Java class is known here; name and jdbcType are resolved later from ResultSet metadata
            ExtendedType type = extendedTypes.getRegisteredType(query.getResultColumnsTypes().get(i));
            columns[i] = new RSColumn(null, 0, null, type, null);
        }
        return columns;
    }

    /**
     * @since 3.0
     */
    protected RSColumn.RowBuilder rowBuilder(TranslatedSQL compiled, ResultSet resultSet) throws SQLException {
        RSColumn.RowBuilder builder = RSColumn.rowBuilder()
                .resultSet(resultSet)
                .columns(createColumnDescriptors(compiled))
                .validateDuplicateColumnNames()
                // resolve column DbAttributes so the row reader factory can tell e.g. PK columns apart, the same way
                // it can for ObjectSelect; lets pagination read the PK regardless of column order
                .dbEntity(dbEntity);

        if (query.getResultColumnsTypes() != null) {
            builder.mergeColumnsWithRsMetadata();
        }

        ObjEntity entity = queryMetadata.getObjEntity();
        if (entity != null && isResultColumnTypesEmpty()) {
            // TODO: andrus 2008/03/28 support flattened attributes with aliases...
            for (ObjAttribute attribute : entity.getAttributes()) {
                CayennePath column = attribute.getDbAttributePath();
                if (column == null || column.length() > 1) {
                    continue;
                }
                builder.overrideColumnType(column.value(), attribute.getType());
            }
        }

        // override numeric Java types based on JDBC defaults for DbAttributes, as Oracle
        // ResultSetMetadata is not very precise about NUMERIC distinctions...
        // (BigDecimal vs Long vs. Integer). These are fallbacks: the ObjAttribute overrides
        // registered above take precedence, as the builder keeps the first override per column.
        if (dbEntity != null && isResultColumnTypesEmpty()) {
            for (DbAttribute attribute : dbEntity.getAttributes()) {
                if (TypesMapping.isNumeric(attribute.getType())) {
                    builder.overrideColumnType(attribute.getName(), TypesMapping.getJavaBySqlType(attribute));
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

    private boolean isResultColumnTypesEmpty() {
        return query.getResultColumnsTypes() == null || query.getResultColumnsTypes().isEmpty();
    }

    /**
     * Extracts a template string from a SQLTemplate query. Exists mainly for
     * the benefit of subclasses that can customize returned template.
     *
     * @since 1.2
     */
    protected String extractTemplateString() {
        String sql = query.getTemplate(dbAdapter.getClass().getName());

        // note that we MUST convert line breaks to spaces. On some databases (DB2)
        // queries with breaks simply won't run; the rest are affected by CAY-726.
        return Util.stripLineBreaks(sql, ' ');
    }

    /**
     * Binds parameters to the PreparedStatement.
     */
    protected void bind(PreparedStatement preparedStatement, PSParameter[] bindings) throws Exception {
        // bind parameters
        for (PSParameter binding : bindings) {
            dataNode.getAdapter().bindParameter(preparedStatement, binding);
        }

        if (queryMetadata.getStatementFetchSize() != 0) {
            preparedStatement.setFetchSize(queryMetadata.getStatementFetchSize());
        }

        int queryTimeout = queryMetadata.getQueryTimeout();
        if (queryTimeout != QueryMetadata.QUERY_TIMEOUT_DEFAULT) {
            preparedStatement.setQueryTimeout(queryTimeout);
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
