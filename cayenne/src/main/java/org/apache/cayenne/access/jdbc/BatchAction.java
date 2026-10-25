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
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.UpdateResult;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OptimisticLockException;
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.translator.TranslatedBatch;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.UpdateBatchQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @since 1.2
 */
public class BatchAction extends BaseSQLAction {

    protected boolean runningAsBatch;
    protected BatchQuery query;
    protected RSColumn[] keyColumns;

    /**
     * @since 4.0
     */
    public BatchAction(BatchQuery query, DataNode dataNode, boolean runningAsBatch) {
        super(dataNode);
        this.query = query;
        this.runningAsBatch = runningAsBatch;
    }

    /**
     * @return Query which originated this action
     */
    public BatchQuery getQuery() {
        return query;
    }

    @Override
    public void performAction(Connection connection, StatementContext context) throws Exception {
        DbAdapter adapter = dataNode.getAdapter();
        TranslatedBatch translated = switch (query) {
            case InsertBatchQuery insert -> dataNode.getInsertBatchTranslator().translate(insert, adapter);
            case UpdateBatchQuery update -> dataNode.getUpdateBatchTranslator().translate(update, adapter);
            case DeleteBatchQuery delete -> dataNode.getDeleteBatchTranslator().translate(delete, adapter);
            case null, default -> throw new CayenneRuntimeException("Unsupported batch query: %s", query);
        };

        context.setTranslated(translated);

        boolean isBatch = canRunAsBatch();
        boolean generatesKeys = hasGeneratedKeys() && supportsGeneratedKeys(isBatch);

        if (isBatch) {
            runAsBatch(connection, context, generatesKeys);
        } else {
            runAsIndividualQueries(connection, context, generatesKeys);
        }
    }

    protected boolean canRunAsBatch() {
        if (!runningAsBatch || query.getRows().size() <= 1) {
            return false;
        }

        if (hasGeneratedKeys()) {
            // turn off batch mode if we generate keys but can't do so in a batch
            return supportsGeneratedKeys(true)
                    && query.getDbEntity().getRelationships().stream().noneMatch(DbRelationship::isReflexive);
        }

        return true;
    }

    protected void runAsBatch(Connection con, StatementContext context, boolean generatesKeys) throws Exception {

        TranslatedBatch translated = (TranslatedBatch) context.getTranslated();
        String sql = translated.sql();
        DbAdapter adapter = dataNode.getAdapter();

        try (PreparedStatement statement = prepareStatement(con, sql, adapter, generatesKeys)) {
            for (int row = 0; row < query.getRows().size(); row++) {

                PSParameter<?>[] bindings = rowBindings(translated, row, adapter.getExtendedTypes());
                for (PSParameter<?> b : bindings) {
                    adapter.bindParameter(statement, b);
                }

                statement.addBatch();
            }

            // execute the whole batch
            int[] results = statement.executeBatch();
            context.addResult(new UpdateResult(results));

            if (generatesKeys) {
                processGeneratedKeys(statement, context, query.getRows());
            }
        }
    }

    /**
     * Executes batch as individual queries over the same prepared statement.
     */
    protected void runAsIndividualQueries(Connection connection, StatementContext context, boolean generatesKeys)
            throws Exception {

        if (query.getRows().isEmpty()) {
            return;
        }

        boolean useOptimisticLock = query.isUsingOptimisticLocking();

        TranslatedBatch translated = (TranslatedBatch) context.getTranslated();
        String queryStr = translated.sql();
        DbAdapter adapter = dataNode.getAdapter();

        try (PreparedStatement statement = prepareStatement(connection, queryStr, adapter, generatesKeys)) {
            List<BatchQueryRow> rows = query.getRows();
            for (int i = 0; i < rows.size(); i++) {
                BatchQueryRow row = rows.get(i);

                PSParameter<?>[] bindings = rowBindings(translated, i, adapter.getExtendedTypes());
                for (PSParameter<?> b : bindings) {
                    adapter.bindParameter(statement, b);
                }

                int updated = statement.executeUpdate();
                if (useOptimisticLock && updated != 1) {
                    throw new OptimisticLockException(row.getObjectId(), query.getDbEntity(), row.getQualifier(), translated);
                }

                context.addResult(new UpdateResult(new int[]{updated}));

                if (generatesKeys) {
                    processGeneratedKeys(statement, context, row);
                }
            }
        }
    }

    /**
     * Resolves the given batch row against the translated batch parameters, returning per-row bindings.
     */
    protected PSParameter<?>[] rowBindings(TranslatedBatch translated, int row, ExtendedTypeMap extendedTypes) {
        PSBatchParameter[] template = translated.bindings();
        PSParameter<?>[] bindings = new PSParameter[template.length];
        for (int j = 0; j < template.length; j++) {
            bindings[j] = bind(template[j], row, extendedTypes);
        }
        return bindings;
    }

    @SuppressWarnings("unchecked")
    private static <T> PSParameter<T> bind(PSBatchParameter parameter, int row, ExtendedTypeMap extendedTypes) {
        Object value = parameter.getValue(row);

        DbAttribute attribute = parameter.attribute();
        if (value == null && attribute.isPrimaryKey()) {
            String entity = attribute.getEntity() != null ? attribute.getEntity().getName() : "<null>";
            throw new CayenneRuntimeException("Failed to generate PK: %s.%s", entity, attribute.getName());
        }

        ExtendedType<T> extendedType = value != null
                ? extendedTypes.getRegisteredType(value.getClass())
                : extendedTypes.getDefaultType();
        return new PSParameter<>((T) value, parameter.psPosition(), parameter.psType(), parameter.psScale(),
                extendedType, parameter.attribute());
    }


    protected PreparedStatement prepareStatement(Connection connection, String queryStr,
                                                 DbAdapter adapter, boolean generatedKeys) throws SQLException {
        return (generatedKeys)
                ? connection.prepareStatement(queryStr, Statement.RETURN_GENERATED_KEYS)
                : connection.prepareStatement(queryStr);
    }

    protected boolean supportsGeneratedKeys(boolean isBatch) {
        // see if we are configured to support generated keys
        return isBatch
                ? dataNode.getAdapter().supportsGeneratedKeysForBatchInserts()
                : dataNode.getAdapter().supportsGeneratedKeys();
    }

    /**
     * Returns whether BatchQuery generates any keys.
     */
    protected boolean hasGeneratedKeys() {
        // see if the query needs them
        if (query instanceof InsertBatchQuery) {

            // see if any of the generated attributes is PK
            for (final DbAttribute attr : query.getDbEntity().getGeneratedAttributes()) {
                if (attr.isPrimaryKey()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Implements generated keys extraction supported in JDBC 3.0 specification.
     *
     * @since 4.0
     */
    protected void processGeneratedKeys(Statement statement, StatementContext context, BatchQueryRow row)
            throws SQLException {
        processGeneratedKeys(statement, context, Collections.singletonList(row));
    }

    /**
     * Reads the keys generated for the given rows, stores the generated primary key in the replacement id map of
     * each row's temporary {@link ObjectId}, and reports the keys to the statement context. The ids are updated right
     * away, as the rows inserted next may reference them via their foreign keys.
     *
     * @since 4.2
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void processGeneratedKeys(Statement statement, StatementContext context, List<BatchQueryRow> rows)
            throws SQLException {

        ResultSet keysRS = statement.getGeneratedKeys();

        // TODO: andrus, 7/4/2007 - use a different form of Statement.execute -
        //  "execute(String,String[])" to be able to map generated column names
        // (this way we can support multiple columns..
        // although need to check how well this works with most common drivers)

        RSColumn.RowBuilder rowBuilder = RSColumn.rowBuilder();

        if (this.keyColumns == null) {
            // attempt to figure out the right descriptor from the mapping...
            Collection<DbAttribute> generated = query.getDbEntity().getGeneratedAttributes();
            if (generated.size() == 1 && keysRS.getMetaData().getColumnCount() == 1) {
                DbAttribute key = generated.iterator().next();

                // use column name from result set, but type and Java class from DB attribute
                ResultSetMetaData md = keysRS.getMetaData();
                String columnName = md.getColumnLabel(1);
                if (columnName == null || columnName.isEmpty()) {
                    columnName = md.getColumnName(1);
                    if (columnName == null || columnName.isEmpty()) {
                        columnName = "column_1";
                    }
                }
                ExtendedType type = dataNode.getAdapter().getExtendedTypes()
                        .getRegisteredType(typeForGeneratedPK(context, key));
                rowBuilder.columns(new RSColumn(columnName, key.getType(), columnName, type, null));
            } else {
                rowBuilder.resultSet(keysRS);
            }

            this.keyColumns = rowBuilder.build(dataNode.getAdapter().getExtendedTypes());
        }

        QueryMetadata metadata = context.getMetadata();
        RowReader<?> rowReader = dataNode.getRowReaderFactory()
                .rowReader(keyColumns, metadata.getResultSegments(), metadata, dataNode.getAdapter());

        // generated keys are small (one row per inserted row), so materialize them here rather than keeping a live,
        // single-use iterator
        List<DataRow> keys;
        try (ResultIterator<?> iterator = new RSIterator(null, keysRS, rowReader)) {
            keys = (List<DataRow>) iterator.allRows();
        }

        applyGeneratedKeys(keys, rows);
        context.addGeneratedKeys(keys);
    }

    /**
     * Stores the generated primary key of each row in the replacement id map of the row's temporary ObjectId.
     *
     * @since 6.0
     */
    protected void applyGeneratedKeys(List<DataRow> keys, List<BatchQueryRow> rows) {

        if (keys.size() != rows.size()) {
            throw new CayenneRuntimeException("Mismatching number of generated PKs: expected %d, instead got %d",
                    rows.size(), keys.size());
        }

        for (int i = 0; i < keys.size(); i++) {
            DataRow key = keys.get(i);

            if (key.isEmpty()) {
                throw new CayenneRuntimeException("Empty key generated.");
            }

            ObjectId idToUpdate = rows.get(i).getObjectId();
            if (idToUpdate == null || !idToUpdate.isTemporary()) {
                // why would this happen?
                return;
            }

            for (DbAttribute attribute : query.getDbEntity().getGeneratedAttributes()) {

                // batch can have generated attributes that are not PKs, e.g. columns with DB DEFAULT values. Ignore
                // those.
                if (attribute.isPrimaryKey()) {

                    Object value = key.get(attribute.getName());

                    // As of now (01/2005) many tested drivers don't provide decent descriptors of identity result
                    // sets, so a data row may contain garbage labels.
                    if (value == null) {
                        value = key.values().iterator().next();
                    }

                    // I guess we should override any existing value, as generated key is the latest thing that
                    // exists in the DB.
                    idToUpdate.getReplacementIdMap().put(attribute.getName(), value);
                    break;
                }
            }
        }
    }

    private String typeForGeneratedPK(StatementContext context, DbAttribute key) {
        String entityName = getQuery().getRows().getFirst().getObjectId().getEntityName();
        ObjEntity objEntity = context.getResolver().getObjEntity(entityName);
        if (objEntity != null) {
            ObjAttribute attributeForDbAttribute = objEntity.getAttributeForDbAttribute(key);
            if (attributeForDbAttribute != null) {
                return attributeForDbAttribute.getType();
            }
        }
        return key.getJavaClassName();
    }
}
