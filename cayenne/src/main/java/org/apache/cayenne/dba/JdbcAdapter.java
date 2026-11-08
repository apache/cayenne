/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.cayenne.dba;

import org.apache.cayenne.CoreModule;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.jdbc.CSParameter;
import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.translator.procedure.DefaultProcedureTranslator;
import org.apache.cayenne.access.translator.ProcedureTranslator;
import org.apache.cayenne.access.translator.select.DefaultSelectTranslator;
import org.apache.cayenne.access.translator.SelectTranslator;
import org.apache.cayenne.access.translator.TranslatedBatch;
import org.apache.cayenne.access.translator.TranslatedProcedure;
import org.apache.cayenne.access.translator.TranslatedSQL;
import org.apache.cayenne.access.translator.TranslatedSelect;
import org.apache.cayenne.access.jdbc.DataNodeBatchAction;
import org.apache.cayenne.access.jdbc.DataNodeProcedureAction;
import org.apache.cayenne.access.jdbc.DataNodeSQLAction;
import org.apache.cayenne.access.jdbc.DataNodeSelectAction;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.ValueObjectTypeFactory;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.RuntimeProperties;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.compiled.CompiledColumnPair;
import org.apache.cayenne.map.compiled.CompiledDataMap;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.query.resolved.ResolvedProcedure;
import org.apache.cayenne.query.resolved.ResolvedSelect;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A generic DbAdapter implementation. Can be used as a default adapter or as a
 * superclass of a concrete adapter implementation.
 */
public class JdbcAdapter implements DbAdapter {

    protected QuotingStrategy quotingStrategy;

    protected int defaultCharColumnLength;

    protected Map<Integer, NativeColumnType[]> nativeColumnTypes;
    protected ExtendedTypeMap extendedTypes;
    protected boolean supportsBatchUpdates;
    protected boolean supportsUniqueConstraints;
    protected boolean supportsGeneratedKeys;

    protected boolean caseInsensitiveCollations;

    /**
     * Creates new JdbcAdapter with a set of default parameters.
     */
    public JdbcAdapter(@Inject RuntimeProperties runtimeProperties,
                       @Inject(CoreModule.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
                       @Inject(CoreModule.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
                       @Inject(CoreModule.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
                       @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {

        // init defaults
        this.defaultCharColumnLength = 255;
        this.setSupportsBatchUpdates(false);
        this.setSupportsUniqueConstraints(true);
        this.caseInsensitiveCollations = runtimeProperties.getBoolean(RuntimeProperties.CI_PROPERTY, false);

        this.quotingStrategy = createQuotingStrategy();

        this.nativeColumnTypes = indexBySqlType(createNativeTypes());
        this.extendedTypes = new ExtendedTypeMap();
        initExtendedTypes(defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, valueObjectTypeRegistry);
    }

    /**
     * Returns default separator - a semicolon.
     *
     * @since 1.0.4
     */
    @Override
    public String getBatchTerminator() {
        return ";";
    }

    /**
     * Returns the database-native types supported by this adapter.
     *
     * @since 5.0
     */
    protected NativeColumnType[] createNativeTypes() {
        return new NativeColumnType[]{
                NativeColumnType.of(Types.ARRAY, "ARRAY"),
                NativeColumnType.of(Types.BIGINT, "BIGINT"),
                NativeColumnType.of(Types.ROWID, "ROWID"),
                NativeColumnType.of(Types.BINARY, "BINARY"),
                NativeColumnType.of(Types.BIT, "BIT"),
                NativeColumnType.of(Types.BLOB, "BLOB"),
                NativeColumnType.of(Types.BOOLEAN, "BOOLEAN"),
                NativeColumnType.of(Types.CHAR, "CHAR"),
                NativeColumnType.of(Types.NCHAR, "NCHAR"),
                NativeColumnType.of(Types.CLOB, "CLOB"),
                NativeColumnType.of(Types.NCLOB, "NCLOB"),
                NativeColumnType.of(Types.DATALINK, "DATALINK"),
                NativeColumnType.of(Types.DATE, "DATE"),
                NativeColumnType.of(Types.DECIMAL, "DECIMAL"),
                NativeColumnType.of(Types.DOUBLE, "DOUBLE"),
                NativeColumnType.of(Types.FLOAT, "FLOAT"),
                NativeColumnType.of(Types.INTEGER, "INTEGER"),
                NativeColumnType.of(Types.JAVA_OBJECT, "JAVA_OBJECT"),
                NativeColumnType.of(Types.LONGVARBINARY, "LONGVARBINARY"),
                NativeColumnType.of(Types.LONGVARCHAR, "LONGVARCHAR"),
                NativeColumnType.of(Types.LONGNVARCHAR, "LONGNVARCHAR"),
                NativeColumnType.of(Types.NUMERIC, "NUMERIC"),
                NativeColumnType.of(Types.OTHER, "OTHER"),
                NativeColumnType.of(Types.REAL, "REAL"),
                NativeColumnType.of(Types.REF, "REF"),
                NativeColumnType.of(Types.SMALLINT, "SMALLINT"),
                NativeColumnType.of(Types.STRUCT, "STRUCT"),
                NativeColumnType.of(Types.TIME, "TIME"),
                NativeColumnType.of(Types.TIMESTAMP, "TIMESTAMP"),
                NativeColumnType.of(Types.TINYINT, "TINYINT"),
                NativeColumnType.of(Types.VARBINARY, "VARBINARY"),
                NativeColumnType.of(Types.VARCHAR, "VARCHAR"),
                NativeColumnType.of(Types.NVARCHAR, "NVARCHAR"),
                NativeColumnType.of(Types.SQLXML, "SQLXML"),
        };
    }

    private static Map<Integer, NativeColumnType[]> indexBySqlType(NativeColumnType[] types) {
        Map<Integer, List<NativeColumnType>> grouped = new HashMap<>();
        for (NativeColumnType type : types) {
            grouped.computeIfAbsent(type.jdbcType(), key -> new ArrayList<>()).add(type);
        }

        Map<Integer, NativeColumnType[]> indexed = new HashMap<>();
        grouped.forEach((sqlType, variants) -> indexed.put(sqlType, variants.toArray(new NativeColumnType[0])));
        return indexed;
    }

    /**
     * Called from {@link #initExtendedTypes(List, List, List, ValueObjectTypeRegistry)} to load
     * adapter-specific types into the ExtendedTypeMap right after the default
     * types are loaded, but before the DI overrides are. This method has
     * specific implementations in JdbcAdapter subclasses.
     */
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        // noop... subclasses may override to install custom types
    }

    /**
     * @since 3.1
     */
    protected void initExtendedTypes(List<ExtendedType> defaultExtendedTypes, List<ExtendedType> userExtendedTypes,
                                     List<ExtendedTypeFactory> extendedTypeFactories,
                                     ValueObjectTypeRegistry valueObjectTypeRegistry) {
        for (ExtendedType type : defaultExtendedTypes) {
            extendedTypes.registerType(type);
        }

        // loading adapter specific extended types
        configureExtendedTypes(extendedTypes);

        for (ExtendedType type : userExtendedTypes) {
            extendedTypes.registerType(type);
        }
        for (ExtendedTypeFactory typeFactory : extendedTypeFactories) {
            extendedTypes.addFactory(typeFactory);
        }
        extendedTypes.addFactory(new ValueObjectTypeFactory(extendedTypes, valueObjectTypeRegistry));
    }

    /**
     * Returns a generic PK generator based on the "AUTO_PK_SUPPORT" lookup table.
     */
    @Override
    public PkGenerator createPkGenerator() {
        return new JdbcPkGenerator();
    }

    /**
     * Returns true.
     *
     * @since 1.1
     */
    @Override
    public boolean supportsUniqueConstraints() {
        return supportsUniqueConstraints;
    }

    /**
     * Returns true.
     *
     * @since 4.0
     */
    @Override
    public boolean supportsCatalogsOnReverseEngineering() {
        return true;
    }

    /**
     * @since 1.1
     */
    public void setSupportsUniqueConstraints(boolean flag) {
        this.supportsUniqueConstraints = flag;
    }

    /**
     * Returns true if supplied type can have a length attribute as a part of
     * column definition
     *
     * @since 4.0
     */
    @Override
    public boolean typeSupportsLength(int type) {
        return type == Types.BINARY || type == Types.CHAR || type == Types.NCHAR || type == Types.NVARCHAR
                || type == Types.LONGNVARCHAR || type == Types.DECIMAL || type == Types.DOUBLE || type == Types.FLOAT
                || type == Types.NUMERIC || type == Types.REAL || type == Types.VARBINARY || type == Types.VARCHAR;
    }

    /**
     * Returns true if supplied type can have a scale attribute as a part of column definition.
     *
     * @param type sql type code
     * @return <code>true</code> if a given type supports scale
     * @since 5.0
     */
    @Override
    public boolean typeSupportsScale(int type) {
        return type == Types.DECIMAL || type == Types.DOUBLE || type == Types.REAL || type == Types.NUMERIC
                || type == Types.TIME || type == Types.TIMESTAMP;
    }

    /**
     * @since 5.0
     */
    @Override
    public int defaultCharColumnLength() {
        return defaultCharColumnLength;
    }

    /**
     * @since 3.0
     */
    @Override
    public Collection<String> dropTableStatements(CompiledTable table) {
        QuotingStrategy quotes = getQuotingStrategy(table);
        StringBuilder buf = new StringBuilder("DROP TABLE ");
        quotes.appendFQN(buf, table.catalog(), table.schema(), table.name());
        return Collections.singleton(buf.toString());
    }

    /**
     * Returns a SQL string that can be used to create database table
     * corresponding to <code>ent</code> parameter.
     */
    @Override
    public String createTable(CompiledTable table) {

        QuotingStrategy quotes = getQuotingStrategy(table);

        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append("CREATE TABLE ");
        quotes.appendFQN(sqlBuffer, table.catalog(), table.schema(), table.name());

        sqlBuffer.append(" (");
        // columns
        Iterator<CompiledColumn> it = table.columns().iterator();
        if (it.hasNext()) {
            boolean first = true;
            while (it.hasNext()) {
                if (first) {
                    first = false;
                } else {
                    sqlBuffer.append(", ");
                }

                CompiledColumn column = it.next();

                // attribute may not be fully valid, do a simple check
                if (column.jdbcType() == TypesMapping.NOT_DEFINED) {
                    throw new CayenneRuntimeException("Undefined type for attribute '%s.%s'."
                            , table.fullyQualifiedName(), column.name());
                }

                createTableAppendColumn(sqlBuffer, column);
            }

            createTableAppendPKClause(sqlBuffer, table);
        }

        sqlBuffer.append(')');
        return sqlBuffer.toString();
    }

    /**
     * @since 1.2
     */
    protected void createTableAppendPKClause(StringBuffer sqlBuffer, CompiledTable table) {

        QuotingStrategy quotes = getQuotingStrategy(table);

        Iterator<CompiledColumn> pkit = table.primaryKey().iterator();
        if (pkit.hasNext()) {
            sqlBuffer.append(", PRIMARY KEY (");
            boolean firstPk = true;

            while (pkit.hasNext()) {
                if (firstPk) {
                    firstPk = false;
                } else {
                    sqlBuffer.append(", ");
                }

                CompiledColumn column = pkit.next();

                quotes.appendStart(sqlBuffer);
                sqlBuffer.append(column.name());
                quotes.appendEnd(sqlBuffer);
            }
            sqlBuffer.append(')');
        }
    }

    /**
     * Appends SQL for column creation to CREATE TABLE buffer.
     *
     * @since 1.2
     */
    @Override
    public void createTableAppendColumn(StringBuffer sqlBuffer, CompiledColumn column) {
        QuotingStrategy quotes = getQuotingStrategy(column.table());
        quotes.appendStart(sqlBuffer);
        sqlBuffer.append(column.name());
        quotes.appendEnd(sqlBuffer);
        sqlBuffer.append(' ').append(preferredNativeColumnType(column).nativeType());

        sqlBuffer.append(sizeAndScale(this, column));
        sqlBuffer.append(column.isMandatory() ? " NOT NULL" : " NULL");
    }

    public static String sizeAndScale(DbAdapter adapter, CompiledColumn column) {
        return sizeAndScale(adapter, column, column.maxLength());
    }

    /**
     * Same as {@link #sizeAndScale(DbAdapter, CompiledColumn)}, but with an explicit length instead of the
     * column's own. Used by the adapters that need to adjust the length of a column in the DDL.
     *
     * @since 6.0
     */
    public static String sizeAndScale(DbAdapter adapter, CompiledColumn column, int maxLength) {
        int type = column.jdbcType();

        // an unconstrained character column either uses a length-free native type, or falls back to the
        // adapter's default length for databases that require one
        if (TypesMapping.isCharacterWithMaxLengthSupport(type) && maxLength <= 0) {
            return adapter.preferredNativeColumnType(column).unconstrained() ? "" : "(" + adapter.defaultCharColumnLength() + ")";
        }

        if (!adapter.typeSupportsLength(type) && !adapter.typeSupportsScale(type)) {
            return "";
        }

        int len = maxLength;
        int scale = TypesMapping.isDateTime(column.jdbcType())
                || TypesMapping.isDecimal(column.jdbcType()) && column.jdbcType() != Types.FLOAT
                ? column.scale() : -1;

        if (len > 0) {
            return "(" + len + (scale >= 0 && len > scale ? ", " + scale : "") + ")";
        }

        if (scale >= 0 && TypesMapping.isDateTime(column.jdbcType())) {
            return "(" + scale + ")";
        }

        return "";
    }

    /**
     * Returns a DDL string to create a unique constraint over a set of columns.
     *
     * @since 1.1
     */
    @Override
    public String createUniqueConstraint(CompiledTable source, Collection<CompiledColumn> columns) {

        if (columns == null || columns.isEmpty()) {
            throw new CayenneRuntimeException("Can't create UNIQUE constraint - no columns specified.");
        }

        QuotingStrategy quotes = getQuotingStrategy(source);

        StringBuilder buf = new StringBuilder();

        buf.append("ALTER TABLE ");
        quotes.appendFQN(buf, source.catalog(), source.schema(), source.name());
        buf.append(" ADD UNIQUE (");

        Iterator<CompiledColumn> it = columns.iterator();
        CompiledColumn first = it.next();
        quotes.appendStart(buf);
        buf.append(first.name());
        quotes.appendEnd(buf);

        while (it.hasNext()) {
            CompiledColumn next = it.next();
            buf.append(", ");
            quotes.appendStart(buf);
            buf.append(next.name());
            quotes.appendEnd(buf);
        }

        buf.append(")");

        return buf.toString();
    }

    /**
     * Returns a SQL string that can be used to create a foreign key constraint
     * for the relationship.
     */
    @Override
    public String createFkConstraint(CompiledJoin rel) {

        CompiledTable source = rel.source();
        QuotingStrategy quotes = getQuotingStrategy(source);
        StringBuilder buf = new StringBuilder();
        StringBuilder refBuf = new StringBuilder();

        buf.append("ALTER TABLE ");

        quotes.appendFQN(buf, source.catalog(), source.schema(), source.name());
        buf.append(" ADD FOREIGN KEY (");

        boolean first = true;

        // sort joins in the order PK are set in target, to avoid errors on some DBs
        List<CompiledColumnPair> joins = new ArrayList<>(rel.columnPairs());
        if (rel.isToPrimaryKey()) {
            List<CompiledColumn> pks = rel.target().primaryKey();
            joins.sort(Comparator.comparingInt(join -> pks.indexOf(join.target())));
        }

        for (CompiledColumnPair join : joins) {
            if (first) {
                first = false;
            } else {
                buf.append(", ");
                refBuf.append(", ");
            }

            quotes.appendStart(buf);
            buf.append(join.source().name());
            quotes.appendEnd(buf);
            quotes.appendStart(refBuf);
            refBuf.append(join.target().name());
            quotes.appendEnd(refBuf);
        }

        buf.append(") REFERENCES ");

        CompiledTable target = rel.target();
        quotes.appendFQN(buf, target.catalog(), target.schema(), target.name());

        buf.append(" (").append(refBuf).append(')');
        return buf.toString();
    }

    /**
     * @since 5.0
     */
    @Override
    public NativeColumnType[] nativeColumnTypes(int type) {
        return nativeColumnTypes.get(type);
    }

    @Override
    public ExtendedTypeMap getExtendedTypes() {
        return extendedTypes;
    }

    @Override
    public DbAttribute buildAttribute(String name, String typeName, int type, int maxLength, int scale, boolean allowNulls) {

        DbAttribute attr = new DbAttribute();
        attr.setName(name);
        attr.setType(type);
        attr.setMandatory(!allowNulls);

        if (maxLength >= 0) {
            attr.setMaxLength(maxLength);
        }

        if (scale >= 0) {
            attr.setScale(scale);
        }

        return attr;
    }

    @Override
    public String tableTypeForTable() {
        return "TABLE";
    }

    @Override
    public String tableTypeForView() {
        return "VIEW";
    }

    @Override
    public void executeSelect(TranslatedSelect select, Connection connection, StatementContext context,
            DataNode node) throws Exception {
        new DataNodeSelectAction(select, node).execute(connection, context);
    }

    @Override
    public void executeSQL(TranslatedSQL sql, Connection connection, StatementContext context,
            DataNode node) throws Exception {
        new DataNodeSQLAction(sql, node).execute(connection, context);
    }

    @Override
    public void executeProcedure(TranslatedProcedure procedure, Connection connection, StatementContext context,
            DataNode node) throws Exception {
        new DataNodeProcedureAction(procedure, node).execute(connection, context);
    }

    @Override
    public void executeBatch(TranslatedBatch batch, Connection connection, StatementContext context,
            DataNode node) throws Exception {
        new DataNodeBatchAction(batch, node, runsAsBatch(batch)).execute(connection, context);
    }

    /**
     * Returns whether a batch should run as a JDBC batch. Optimistic locking is not supported in batches due to JDBC
     * driver limitations, as the per-row update counts are needed.
     *
     * @since 6.0
     */
    protected boolean runsAsBatch(TranslatedBatch batch) {
        return !batch.query().usingOptimisticLocking() && supportsBatchUpdates();
    }

    @Override
    public SelectTranslator getSelectTranslator(ResolvedSelect select, CompiledMapping mapping) {
        return new DefaultSelectTranslator();
    }

    @Override
    public ProcedureTranslator getProcedureTranslator(ResolvedProcedure procedure) {
        return new DefaultProcedureTranslator();
    }

    @Override
    public SQLTreeProcessor getSqlTreeProcessor() {
        return node -> node;
    }

    @Override
    public void bindParameter(PreparedStatement statement, PSParameter<?> parameter) throws Exception {
        bind(statement, parameter.value(), parameter.psPosition(), parameter.psType(), parameter.psScale(),
                parameter.binder());
    }

    @Override
    public void bindParameter(CallableStatement statement, CSParameter<?> parameter) throws Exception {
        bind(statement, parameter.value(), parameter.psPosition(), parameter.psType(), parameter.psScale(),
                parameter.binder());
    }

    /**
     * Binds a raw value to a statement parameter. This is the single extension point shared by both
     * {@code bindParameter} overloads; adapters that need database-specific handling (e.g. of NULLs) should
     * override this method rather than the public overloads.
     *
     * @since 5.0
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void bind(PreparedStatement statement, Object value, int psPosition, int psType, int psScale,
                        ExtendedType binder) throws Exception {
        if (value == null) {
            statement.setNull(psPosition, psType);
        } else {
            binder.setJdbcObject(statement, value, psPosition, psType, psScale);
        }
    }

    @Override
    public boolean supportsBatchUpdates() {
        return this.supportsBatchUpdates;
    }

    public void setSupportsBatchUpdates(boolean flag) {
        this.supportsBatchUpdates = flag;
    }

    /**
     * @since 1.2
     */
    @Override
    public boolean supportsGeneratedKeys() {
        return supportsGeneratedKeys;
    }

    /**
     * @since 1.2
     */
    public void setSupportsGeneratedKeys(boolean flag) {
        this.supportsGeneratedKeys = flag;
    }

    @Override
    public List<String> getSystemCatalogs() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getSystemSchemas() {
        return Collections.emptyList();
    }

    protected QuotingStrategy createQuotingStrategy() {
        return new DefaultQuotingStrategy('"', '"');
    }

    @Override
    public QuotingStrategy getQuotingStrategy(CompiledTable table) {
        CompiledDataMap dataMap = table != null ? table.map() : null;
        return dataMap != null && dataMap.quotingSQLIdentifiers() ? quotingStrategy : QuotingStrategy.NONE;
    }

}
