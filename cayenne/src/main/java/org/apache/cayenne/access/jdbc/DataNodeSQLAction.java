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
import org.apache.cayenne.SelectResult;
import org.apache.cayenne.UpdateResult;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.translator.TranslatedSQL;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ResultSegment;
import org.apache.cayenne.query.ResultSegments;
import org.apache.cayenne.query.ScalarResultSegment;
import org.apache.cayenne.query.resolved.ResolvedQuery;
import org.apache.cayenne.query.resolved.ResolvedSQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Runs a translated raw SQL statement, reporting its result sets and update counts.
 *
 * @since 6.0
 */
public class DataNodeSQLAction {

    protected final TranslatedSQL translated;
    protected final ResolvedSQL query;
    protected final DataNode dataNode;

    public DataNodeSQLAction(TranslatedSQL translated, DataNode dataNode) {
        this.translated = translated;
        this.query = translated.query();
        this.dataNode = dataNode;
    }

    public void execute(Connection connection, StatementContext context) throws Exception {
        List<Number> counts = new ArrayList<>();
        executeStatement(connection, context, counts);

        // report the combined counts of all statements inside the SQL
        int[] ints = new int[counts.size()];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = counts.get(i).intValue();
        }
        context.addResult(new UpdateResult(ints));
    }

    protected void executeStatement(Connection connection, StatementContext context, Collection<Number> updateCounts)
            throws Exception {

        long t1 = System.currentTimeMillis();
        boolean iteratedResult = context.isIteratedResult();
        int generatedKeys = query.returnGeneratedKeys() ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS;
        PreparedStatement statement = connection.prepareStatement(translated.sql(), generatedKeys);
        try {
            bind(statement, translated.bindings());

            // process a mix of results
            boolean isResultSet = statement.execute();

            // read the first update count before the generated keys, as some drivers (e.g. Derby) reset it once
            // the generated keys are read
            int firstUpdateCount = isResultSet ? -1 : statement.getUpdateCount();

            if (query.returnGeneratedKeys()) {
                ResultSet generatedKeysResultSet = statement.getGeneratedKeys();
                if (generatedKeysResultSet != null) {
                    processSelectResult(connection, statement, generatedKeysResultSet, context, t1);
                }
            }
            boolean firstIteration = true;
            while (true) {

                if (!firstIteration) {
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
                    int updateCount = firstIteration ? firstUpdateCount : statement.getUpdateCount();
                    if (updateCount == -1) {
                        break;
                    }
                    updateCounts.add(updateCount);
                }

                firstIteration = false;
            }
        } finally {
            if (!iteratedResult) {
                statement.close();
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void processSelectResult(
            Connection connection,
            Statement statement,
            ResultSet resultSet,
            StatementContext context,
            long startTime) throws Exception {

        boolean iteratedResult = context.isIteratedResult();

        RSColumn[] columns = rowBuilder(resultSet).build(dataNode.getAdapter().getExtendedTypes());
        RowReader<?> rowReader = dataNode.getRowReaderFactory()
                .rowReader(columns, resultSegments(resultSet), query, dataNode.getAdapter());

        ResultIterator<?> it = new RSIterator<>(statement, resultSet, rowReader);

        if (iteratedResult) {
            it = new ConnectionAwareResultIterator(it, connection);
        }

        it = new LimitResultIterator<>(it, query.fetchOffset(), query.fetchLimit());

        if (iteratedResult) {
            try {
                context.addResult(it);
            } catch (Exception ex) {
                it.close();
                throw ex;
            }
        } else {
            // note that we are not closing the iterator here, relying on caller
            // to close the underlying ResultSet on its own... this is a hack,
            // maybe a cleaner flow is due here.
            List<?> resultRows = it.allRows();
            context.addResult(new SelectResult<>(resultRows));
        }
    }

    /**
     * Returns the result mapping of the query. An empty mapping of a scalar query means "one scalar per ResultSet
     * column", and is resolved here against the actual ResultSet width.
     */
    private ResultSegments resultSegments(ResultSet resultSet) throws SQLException {
        ResultSegments declared = query.resultSegments();
        if (!query.useScalar() || declared == null || !declared.segments().isEmpty()) {
            return declared;
        }

        int width = resultSet.getMetaData().getColumnCount();
        List<ResultSegment> segments = new ArrayList<>(width);
        for (int i = 0; i < width; i++) {
            segments.add(new ScalarResultSegment(String.valueOf(i), null, i));
        }
        return new ResultSegments(segments, true);
    }

    /**
     * Returns the column descriptors declared explicitly, either by the template ("#result" directives) or by the
     * query's result column types, or null if neither is set.
     */
    private RSColumn[] createColumnDescriptors() {

        // template #result columns take precedence over other ways to determine the type
        if (translated.resultColumns().length > 0) {
            if (!query.resultColumnsTypes().isEmpty()) {
                throw new CayenneRuntimeException("Caused by setting return types by directives and by parameters in query.");
            } else {
                return translated.resultColumns();
            }
        }

        // check explicitly set column types
        if (query.resultColumnsTypes().isEmpty()) {
            return null;
        }

        ExtendedTypeMap extendedTypes = dataNode.getAdapter().getExtendedTypes();
        int size = query.resultColumnsTypes().size();
        RSColumn[] columns = new RSColumn[size];
        for (int i = 0; i < size; i++) {
            // only the Java class is known here; name and jdbcType are resolved later from ResultSet metadata
            ExtendedType type = extendedTypes.getRegisteredType(query.resultColumnsTypes().get(i));
            columns[i] = new RSColumn(null, 0, null, type, null);
        }
        return columns;
    }

    /**
     * Creates a builder of the result columns, configured with the declared column types and the column type
     * overrides derived from the mapping.
     */
    protected RSColumn.RowBuilder rowBuilder(ResultSet resultSet) throws SQLException {

        DbEntity dbEntity = query.dbEntity();
        RSColumn.RowBuilder builder = RSColumn.rowBuilder()
                .resultSet(resultSet)
                .columns(createColumnDescriptors())
                .validateDuplicateColumnNames()
                // resolve column DbAttributes so the row reader factory can tell e.g. PK columns apart, the same way
                // it can for ObjectSelect; lets pagination read the PK regardless of column order
                .dbEntity(dbEntity);

        boolean declaredTypes = !query.resultColumnsTypes().isEmpty();
        if (declaredTypes) {
            builder.mergeColumnsWithRsMetadata();
        }

        ObjEntity entity = query.objEntity();
        if (entity != null && !declaredTypes) {
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
        if (dbEntity != null && !declaredTypes) {
            for (DbAttribute attribute : dbEntity.getAttributes()) {
                if (TypesMapping.isNumeric(attribute.getType())) {
                    builder.overrideColumnType(attribute.getName(), TypesMapping.getJavaBySqlType(attribute));
                }
            }
        }

        switch (query.columnNameCaps()) {
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
     * Binds parameters and sets the fetch size and timeout of the prepared statement.
     */
    protected void bind(PreparedStatement preparedStatement, PSParameter[] bindings) throws Exception {
        // bind parameters
        for (PSParameter binding : bindings) {
            dataNode.getAdapter().bindParameter(preparedStatement, binding);
        }

        if (query.statementFetchSize() != 0) {
            preparedStatement.setFetchSize(query.statementFetchSize());
        }

        int queryTimeout = query.queryTimeout();
        if (queryTimeout != ResolvedQuery.QUERY_TIMEOUT_DEFAULT) {
            preparedStatement.setQueryTimeout(queryTimeout);
        }
    }
}
