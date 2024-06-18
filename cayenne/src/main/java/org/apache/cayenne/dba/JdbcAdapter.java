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

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslatorFactory;
import org.apache.cayenne.access.translator.ejbql.JdbcEJBQLTranslatorFactory;
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
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.cayenne.util.Util;

/**
 * A generic DbAdapter implementation. Can be used as a default adapter or as a
 * superclass of a concrete adapter implementation.
 */
public class JdbcAdapter implements DbAdapter {

    private PkGenerator pkGenerator;
    protected QuotingStrategy quotingStrategy;

    protected TypesHandler typesHandler;
    protected ExtendedTypeMap extendedTypes;
    protected boolean supportsBatchUpdates;
    protected boolean supportsUniqueConstraints;
    protected boolean supportsGeneratedKeys;
    protected EJBQLTranslatorFactory ejbqlTranslatorFactory;

    protected ResourceLocator resourceLocator;
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
                       @Inject(Constants.RESOURCE_LOCATOR) ResourceLocator resourceLocator,
                       @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {

        // init defaults
        this.setSupportsBatchUpdates(false);
        this.setSupportsUniqueConstraints(true);
        this.caseInsensitiveCollations = runtimeProperties.getBoolean(Constants.CI_PROPERTY, false);
        this.resourceLocator = resourceLocator;

        this.quotingStrategy = createQuotingStrategy();

        this.ejbqlTranslatorFactory = createEJBQLTranslatorFactory();
        this.typesHandler = TypesHandler.getHandler(findResource("/types.xml"));
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
     * Locates and returns a named adapter resource. A resource can be an XML
     * file, etc.
     * <p>
     * This implementation is based on the premise that each adapter is located
     * in its own Java package and all resources are in the same package as
     * well. Resource lookup is recursive, so that if DbAdapter is a subclass of
     * another adapter, parent adapter package is searched as a failover.
     * </p>
     *
     * @since 3.0
     */
    protected URL findResource(String name) {
        Class<?> adapterClass = getClass();

        while (adapterClass != null && JdbcAdapter.class.isAssignableFrom(adapterClass)) {

            String path = Util.getPackagePath(adapterClass.getName()) + name;
            Collection<Resource> resources = resourceLocator.findResources(path);

            if (!resources.isEmpty()) {
                return resources.iterator().next().getURL();
            }

            adapterClass = adapterClass.getSuperclass();
        }

        return null;
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
     * Creates and returns an {@link EJBQLTranslatorFactory} used to generate
     * visitors for EJBQL to SQL translations. This method should be overriden
     * by subclasses that need to customize EJBQL generation.
     *
     * @since 3.0
     */
    protected EJBQLTranslatorFactory createEJBQLTranslatorFactory() {
        JdbcEJBQLTranslatorFactory translatorFactory = new JdbcEJBQLTranslatorFactory();
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
     *
     * @since 5.0
     */
    @Override
    public boolean typeSupportsScale(int type) {
        return type == Types.DECIMAL || type == Types.DOUBLE || type == Types.REAL || type == Types.NUMERIC
                || type == Types.TIME || type == Types.TIMESTAMP;
    }

    /**
     * @since 3.0
     */
    @Override
    public Collection<String> dropTableStatements(DbEntity table) {
        return Collections.singleton("DROP TABLE " + quotingStrategy.quotedFullyQualifiedName(table));
    }

    /**
     * Returns a SQL string that can be used to create database table
     * corresponding to <code>ent</code> parameter.
     */
    @Override
    public String createTable(DbEntity entity) {

        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append("CREATE TABLE ");
        sqlBuffer.append(quotingStrategy.quotedFullyQualifiedName(entity));

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

                sqlBuffer.append(quotingStrategy.quotedName(at));
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
        sqlBuffer.append(quotingStrategy.quotedName(column));
        sqlBuffer.append(' ').append(getType(this, column));

        sqlBuffer.append(sizeAndPrecision(this, column));
        sqlBuffer.append(column.isMandatory() ? " NOT NULL" : " NULL");
    }

    public static String sizeAndPrecision(DbAdapter adapter, DbAttribute column) {
        if (!adapter.typeSupportsLength(column.getType()) && !adapter.typeSupportsScale(column.getType())) {
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

    public static String getType(DbAdapter adapter, DbAttribute column) {
        int columnType = column.getType();
        if(columnType == Types.OTHER) {
            // TODO: warn that this is unsupported yet
            return "OTHER";
        }

        String[] types = adapter.externalTypesForJdbcType(columnType);
        if (types == null || types.length == 0) {
            String entityName = column.getEntity() != null
                    ? column.getEntity().getFullyQualifiedName()
                    : "<null>";
            throw new CayenneRuntimeException("Undefined type for attribute '%s.%s': %s."
                    , entityName, column.getName(), column.getType());
        }
        return types[0];
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

        StringBuilder buf = new StringBuilder();

        buf.append("ALTER TABLE ");
        buf.append(quotingStrategy.quotedFullyQualifiedName(source));
        buf.append(" ADD UNIQUE (");

        Iterator<DbAttribute> it = columns.iterator();
        DbAttribute first = it.next();
        buf.append(quotingStrategy.quotedName(first));

        while (it.hasNext()) {
            DbAttribute next = it.next();
            buf.append(", ");
            buf.append(quotingStrategy.quotedName(next));
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
        StringBuilder buf = new StringBuilder();
        StringBuilder refBuf = new StringBuilder();

        buf.append("ALTER TABLE ");

        buf.append(quotingStrategy.quotedFullyQualifiedName(source));
        buf.append(" ADD FOREIGN KEY (");

        boolean first = true;

        // sort joins in the order PK are set in target, to avoid errors on some DBs
        List<DbJoin> joins = rel.getJoins();
        if(rel.isToPK()) {
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

            buf.append(quotingStrategy.quotedSourceName(join));
            refBuf.append(quotingStrategy.quotedTargetName(join));
        }

        buf.append(") REFERENCES ");

        buf.append(quotingStrategy.quotedFullyQualifiedName(rel.getTargetEntity()));

        buf.append(" (").append(refBuf.toString()).append(')');
        return buf.toString();
    }

    @Override
    public String[] externalTypesForJdbcType(int type) {
        return typesHandler.externalTypesForJdbcType(type);
    }

    @Override
    public ExtendedTypeMap getExtendedTypes() {
        return extendedTypes;
    }

    @Override
    public DbAttribute buildAttribute(String name, String typeName, int type, int size, int scale, boolean allowNulls) {

        DbAttribute attr = new DbAttribute();
        attr.setName(name);
        attr.setType(type);
        attr.setMandatory(!allowNulls);

        if (size >= 0) {
            attr.setMaxLength(size);
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
    public SelectTranslator getSelectTranslator(FluentSelect<?, ?> query, EntityResolver entityResolver) {
        return new DefaultSelectTranslator(query, this, entityResolver);
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
     * Returns a translator factory for EJBQL to SQL translation. The factory is
     * normally initialized in constructor by calling
     * {@link #createEJBQLTranslatorFactory()}, and can be changed later by
     * calling {@link #setEjbqlTranslatorFactory(EJBQLTranslatorFactory)}.
     *
     * @since 3.0
     */
    public EJBQLTranslatorFactory getEjbqlTranslatorFactory() {
        return ejbqlTranslatorFactory;
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
     * Sets a translator factory for EJBQL to SQL translation. This property is
     * normally initialized in constructor by calling
     * {@link #createEJBQLTranslatorFactory()}, so users would only override it
     * if they need to customize EJBQL translation.
     *
     * @since 3.0
     */
    public void setEjbqlTranslatorFactory(EJBQLTranslatorFactory ejbqlTranslatorFactory) {
        this.ejbqlTranslatorFactory = ejbqlTranslatorFactory;
    }

    /**
     * @since 4.0
     */
    protected QuotingStrategy createQuotingStrategy() {
        return new DefaultQuotingStrategy("\"", "\"");
    }

    /**
     * @since 4.0
     */
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
