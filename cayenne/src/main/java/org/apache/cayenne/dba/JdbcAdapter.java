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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslator;
import org.apache.cayenne.access.translator.ejbql.JdbcEJBQLTranslator;
import org.apache.cayenne.access.translator.procedure.DefaultProcedureTranslator;
import org.apache.cayenne.access.translator.procedure.ProcedureTranslator;
import org.apache.cayenne.access.translator.select.DefaultSelectTranslator;
import org.apache.cayenne.access.translator.select.SelectTranslator;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.ValueObjectTypeFactory;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.Select;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

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

    private PkGenerator pkGenerator;
    protected QuotingStrategy quotingStrategy;

    protected int defaultCharColumnLength;

    protected Map<Integer, NativeColumnType[]> nativeColumnTypes;
    protected ExtendedTypeMap extendedTypes;
    protected boolean supportsBatchUpdates;
    protected boolean supportsUniqueConstraints;
    protected boolean supportsGeneratedKeys;
    protected EJBQLTranslator ejbqlTranslator;

    protected boolean caseInsensitiveCollations;

    @Inject
    protected JdbcEventLogger logger;

    /**
     * Creates new JdbcAdapter with a set of default parameters.
     */
    public JdbcAdapter(@Inject RuntimeProperties runtimeProperties,
                       @Inject(Constants.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
                       @Inject(Constants.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
                       @Inject(Constants.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
                       @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {

        // init defaults
        this.defaultCharColumnLength = 255;
        this.setSupportsBatchUpdates(false);
        this.setSupportsUniqueConstraints(true);
        this.caseInsensitiveCollations = runtimeProperties.getBoolean(Constants.CI_PROPERTY, false);

        this.quotingStrategy = createQuotingStrategy();

        this.ejbqlTranslator = createEJBQLTranslator();
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
     * @since 3.1
     */
    public JdbcEventLogger getJdbcEventLogger() {
        return this.logger;
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
     * Creates and returns an {@link EJBQLTranslator} used to generate
     * visitors for EJBQL to SQL translations. This method should be overriden
     * by subclasses that need to customize EJBQL generation.
     *
     * @since 3.0
     */
    protected EJBQLTranslator createEJBQLTranslator() {
        JdbcEJBQLTranslator translatorFactory = new JdbcEJBQLTranslator();
        translatorFactory.setCaseInsensitive(caseInsensitiveCollations);
        return translatorFactory;
    }

    /**
     * Returns primary key generator associated with this DbAdapter.
     */
    @Override
    public PkGenerator getPkGenerator() {
        return pkGenerator;
    }

    /**
     * Sets new primary key generator.
     *
     * @since 1.1
     */
    @Override
    public void setPkGenerator(PkGenerator pkGenerator) {
        this.pkGenerator = pkGenerator;
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
    public Collection<String> dropTableStatements(DbEntity table) {
        QuotingStrategy quotes = getQuotingStrategy(table);
        StringBuilder buf = new StringBuilder("DROP TABLE ");
        quotes.appendFQN(buf, table.getCatalog(), table.getSchema(), table.getName());
        return Collections.singleton(buf.toString());
    }

    /**
     * Resolves the {@link QuotingStrategy} instance to use for the given entity, based on whether
     * its DataMap enables SQL identifier quoting.
     */
    protected QuotingStrategy getQuotingStrategy(DbEntity entity) {
        DataMap dataMap = entity.getDataMap();
        return dataMap != null && dataMap.isQuotingSQLIdentifiers() ? getQuotingStrategy() : QuotingStrategy.NONE;
    }

    /**
     * Returns a SQL string that can be used to create database table
     * corresponding to <code>ent</code> parameter.
     */
    @Override
    public String createTable(DbEntity entity) {

        QuotingStrategy quotes = getQuotingStrategy(entity);

        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append("CREATE TABLE ");
        quotes.appendFQN(sqlBuffer, entity.getCatalog(), entity.getSchema(), entity.getName());

        sqlBuffer.append(" (");
        // columns
        Iterator<DbAttribute> it = entity.getAttributes().iterator();
        if (it.hasNext()) {
            boolean first = true;
            while (it.hasNext()) {
                if (first) {
                    first = false;
                } else {
                    sqlBuffer.append(", ");
                }

                DbAttribute column = it.next();

                // attribute may not be fully valid, do a simple check
                if (column.getType() == TypesMapping.NOT_DEFINED) {
                    throw new CayenneRuntimeException("Undefined type for attribute '%s.%s'."
                            , entity.getFullyQualifiedName(), column.getName());
                }

                createTableAppendColumn(sqlBuffer, column);
            }

            createTableAppendPKClause(sqlBuffer, entity);
        }

        sqlBuffer.append(')');
        return sqlBuffer.toString();
    }

    /**
     * @since 1.2
     */
    protected void createTableAppendPKClause(StringBuffer sqlBuffer, DbEntity entity) {

        QuotingStrategy quotes = getQuotingStrategy(entity);

        Iterator<DbAttribute> pkit = entity.getPrimaryKeys().iterator();
        if (pkit.hasNext()) {
            sqlBuffer.append(", PRIMARY KEY (");
            boolean firstPk = true;

            while (pkit.hasNext()) {
                if (firstPk) {
                    firstPk = false;
                } else {
                    sqlBuffer.append(", ");
                }

                DbAttribute at = pkit.next();

                quotes.appendStart(sqlBuffer);
                sqlBuffer.append(at.getName());
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
    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {
        QuotingStrategy quotes = getQuotingStrategy(column.getEntity());
        quotes.appendStart(sqlBuffer);
        sqlBuffer.append(column.getName());
        quotes.appendEnd(sqlBuffer);
        sqlBuffer.append(' ').append(preferredNativeColumnType(column).nativeType());

        sqlBuffer.append(sizeAndScale(this, column));
        sqlBuffer.append(column.isMandatory() ? " NOT NULL" : " NULL");
    }

    /**
     * @deprecated in favor of {@link #sizeAndScale(DbAdapter, DbAttribute)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public static String sizeAndPrecision(DbAdapter adapter, DbAttribute column) {
        return sizeAndScale(adapter, column);
    }

    public static String sizeAndScale(DbAdapter adapter, DbAttribute column) {
        int type = column.getType();

        // an unconstrained character column either uses a length-free native type, or falls back to the
        // adapter's default length for databases that require one
        if (TypesMapping.isCharacterWithMaxLengthSupport(type) && column.getMaxLength() <= 0) {
            return adapter.preferredNativeColumnType(column).unconstrained() ? "" : "(" + adapter.defaultCharColumnLength() + ")";
        }

        if (!adapter.typeSupportsLength(type) && !adapter.typeSupportsScale(type)) {
            return "";
        }

        int len = column.getMaxLength();
        int scale = TypesMapping.isDateTime(column.getType())
                || TypesMapping.isDecimal(column.getType()) && column.getType() != Types.FLOAT
                ? column.getScale() : -1;

        if (len > 0) {
            return "(" + len + (scale >= 0 && len > scale ? ", " + scale : "") + ")";
        }

        if (scale >= 0 && TypesMapping.isDateTime(column.getType())) {
            return "(" + scale + ")";
        }

        return "";
    }

    /**
     * @deprecated in favor of {@link #preferredNativeColumnType(DbAttribute)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public static String getType(DbAdapter adapter, DbAttribute column) {
        return adapter.preferredNativeColumnType(column).nativeType();
    }

    /**
     * Returns a DDL string to create a unique constraint over a set of columns.
     *
     * @since 1.1
     */
    @Override
    public String createUniqueConstraint(DbEntity source, Collection<DbAttribute> columns) {

        if (columns == null || columns.isEmpty()) {
            throw new CayenneRuntimeException("Can't create UNIQUE constraint - no columns specified.");
        }

        QuotingStrategy quotes = getQuotingStrategy(source);

        StringBuilder buf = new StringBuilder();

        buf.append("ALTER TABLE ");
        quotes.appendFQN(buf, source.getCatalog(), source.getSchema(), source.getName());
        buf.append(" ADD UNIQUE (");

        Iterator<DbAttribute> it = columns.iterator();
        DbAttribute first = it.next();
        quotes.appendStart(buf);
        buf.append(first.getName());
        quotes.appendEnd(buf);

        while (it.hasNext()) {
            DbAttribute next = it.next();
            buf.append(", ");
            quotes.appendStart(buf);
            buf.append(next.getName());
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
    public String createFkConstraint(DbRelationship rel) {

        DbEntity source = rel.getSourceEntity();
        QuotingStrategy quotes = getQuotingStrategy(source);
        StringBuilder buf = new StringBuilder();
        StringBuilder refBuf = new StringBuilder();

        buf.append("ALTER TABLE ");

        quotes.appendFQN(buf, source.getCatalog(), source.getSchema(), source.getName());
        buf.append(" ADD FOREIGN KEY (");

        boolean first = true;

        // sort joins in the order PK are set in target, to avoid errors on some DBs
        List<DbJoin> joins = rel.getJoins();
        if (rel.isToPK()) {
            List<DbAttribute> pks = rel.getTargetEntity().getPrimaryKeys();
            joins.sort(Comparator.comparingInt(join -> pks.indexOf(join.getTarget())));
        }

        for (DbJoin join : joins) {
            if (first) {
                first = false;
            } else {
                buf.append(", ");
                refBuf.append(", ");
            }

            quotes.appendStart(buf);
            buf.append(join.getSourceName());
            quotes.appendEnd(buf);
            quotes.appendStart(refBuf);
            refBuf.append(join.getTargetName());
            quotes.appendEnd(refBuf);
        }

        buf.append(") REFERENCES ");

        DbEntity target = rel.getTargetEntity();
        quotes.appendFQN(buf, target.getCatalog(), target.getSchema(), target.getName());

        buf.append(" (").append(refBuf).append(')');
        return buf.toString();
    }

    /**
     * @deprecated use {@link #nativeColumnTypes(int)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    @Override
    public String[] externalTypesForJdbcType(int type) {
        NativeColumnType[] variants = nativeColumnTypes.get(type);
        if (variants == null) {
            return null;
        }
        String[] names = new String[variants.length];
        for (int i = 0; i < variants.length; i++) {
            names[i] = variants[i].nativeType();
        }
        return names;
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

    /**
     * Uses JdbcActionBuilder to create the right action.
     *
     * @since 1.2
     */
    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new JdbcActionBuilder(node));
    }

    @Override
    public SelectTranslator getSelectTranslator(Select<?> query, EntityResolver entityResolver) {
        return new DefaultSelectTranslator();
    }

    @Override
    public ProcedureTranslator getProcedureTranslator(ProcedureQuery query, EntityResolver entityResolver) {
        return new DefaultProcedureTranslator();
    }

    @Override
    public SQLTreeProcessor getSqlTreeProcessor() {
        return node -> node;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void bindParameter(PreparedStatement statement, ParameterBinding binding) throws Exception {

        if (binding.getValue() == null) {
            statement.setNull(binding.getStatementPosition(), binding.getJdbcType());
        } else {
            binding.getExtendedType().setJdbcObject(statement,
                    binding.getValue(),
                    binding.getStatementPosition(),
                    binding.getJdbcType(),
                    binding.getScale());
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

    /**
     * Returns the {@link EJBQLTranslator} for EJBQL to SQL translation. It is normally initialized in the
     * constructor by calling {@link #createEJBQLTranslator()}, and can be changed later by calling
     * {@link #setEjbqlTranslator(EJBQLTranslator)}.
     *
     * @since 5.0
     */
    @Override
    public EJBQLTranslator getEjbqlTranslator() {
        return ejbqlTranslator;
    }

    @Override
    public List<String> getSystemCatalogs() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getSystemSchemas() {
        return Collections.emptyList();
    }

    /**
     * Sets the {@link EJBQLTranslator} for EJBQL to SQL translation. This property is normally initialized in the
     * constructor by calling {@link #createEJBQLTranslator()}, so users would only override it if they need to
     * customize EJBQL translation.
     *
     * @since 5.0
     */
    public void setEjbqlTranslator(EJBQLTranslator ejbqlTranslator) {
        this.ejbqlTranslator = ejbqlTranslator;
    }

    /**
     * @deprecated in favor of {@link #setEjbqlTranslator(EJBQLTranslator)}.
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public void setEjbqlTranslatorFactory(EJBQLTranslator ejbqlTranslator) {
        setEjbqlTranslator(ejbqlTranslator);
    }

    protected QuotingStrategy createQuotingStrategy() {
        return new DefaultQuotingStrategy('"', '"');
    }

    @Override
    public QuotingStrategy getQuotingStrategy() {
        return quotingStrategy;
    }

    /**
     * Simply returns this, as JdbcAdapter is not a wrapper.
     *
     * @since 4.0
     */
    @Override
    public DbAdapter unwrap() {
        return this;
    }

}
