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

package org.apache.cayenne.dba;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.EJBQLTranslatorFactory;
import org.apache.cayenne.access.jdbc.JdbcEJBQLTranslatorFactory;
import org.apache.cayenne.access.trans.QualifierTranslator;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.access.types.BigIntegerType;
import org.apache.cayenne.access.types.BooleanType;
import org.apache.cayenne.access.types.ByteArrayType;
import org.apache.cayenne.access.types.CalendarType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.UtilDateType;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.util.ResourceLocator;
import org.apache.cayenne.util.Util;

/**
 * A generic DbAdapter implementation. Can be used as a default adapter or as a superclass
 * of a concrete adapter implementation.
 * 
 * @author Andrus Adamchik
 */
public class JdbcAdapter implements DbAdapter {

    protected PkGenerator pkGenerator;
    protected TypesHandler typesHandler;
    protected ExtendedTypeMap extendedTypes;
    protected boolean supportsBatchUpdates;
    protected boolean supportsFkConstraints;
    protected boolean supportsUniqueConstraints;
    protected boolean supportsGeneratedKeys;
    protected EJBQLTranslatorFactory ejbqlTranslatorFactory;

    /**
     * Creates new JdbcAdapter with a set of default parameters.
     */
    public JdbcAdapter() {
        // init defaults
        this.setSupportsBatchUpdates(false);
        this.setSupportsUniqueConstraints(true);
        this.setSupportsFkConstraints(true);

        this.pkGenerator = createPkGenerator();
        this.typesHandler = TypesHandler.getHandler(findAdapterResource("/types.xml"));
        this.extendedTypes = new ExtendedTypeMap();
        this.configureExtendedTypes(extendedTypes);
        this.ejbqlTranslatorFactory = createEJBQLTranslatorFactory();
    }

    /**
     * Returns default separator - a semicolon.
     * 
     * @since 1.0.4
     */
    public String getBatchTerminator() {
        return ";";
    }

    /**
     * Locates and returns a named adapter resource. A resource can be an XML file, etc.
     * <p>
     * This implementation is based on the premise that each adapter is located in its own
     * Java package and all resources are in the same package as well. Resource lookup is
     * recursive, so that if DbAdapter is a subclass of another adapter, parent adapter
     * package is searched as a failover.
     * </p>
     * 
     * @since 1.1
     */
    public URL findAdapterResource(String name) {
        Class adapterClass = this.getClass();

        while (adapterClass != null && JdbcAdapter.class.isAssignableFrom(adapterClass)) {

            String path = Util.getPackagePath(adapterClass.getName()) + name;
            URL url = ResourceLocator.findURLInClasspath(path);
            if (url != null) {
                return url;
            }

            adapterClass = adapterClass.getSuperclass();
        }

        return null;
    }

    /**
     * Installs appropriate ExtendedTypes as converters for passing values between JDBC
     * and Java layers. Called from default constructor.
     */
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        // use BooleanType to ensure that returned booleans are an enum of Boolean.TRUE
        // and Boolean.FALSE
        map.registerType(new BooleanType());

        // Create a default CHAR handler with some generic settings.
        // Subclasses may need to install their own CharType or reconfigure
        // this one to work better with the target database.
        map.registerType(new CharType(false, true));

        // enable java.util.Dates as "persistent" values
        map.registerType(new UtilDateType());

        // enable "small" BLOBs
        map.registerType(new ByteArrayType(false, true));

        // enable Calendar
        // TODO: andrus 9/1/2006 - maybe use ExtendedTypeFactory to handle all calendar
        // subclasses at once
        map.registerType(new CalendarType(GregorianCalendar.class));
        map.registerType(new CalendarType(Calendar.class));

        map.registerType(new BigIntegerType());
    }

    /**
     * Creates and returns a primary key generator. This factory method should be
     * overriden by JdbcAdapter subclasses to provide custom implementations of
     * PKGenerator.
     */
    protected PkGenerator createPkGenerator() {
        return new JdbcPkGenerator();
    }

    /**
     * Creates and returns an {@link EJBQLTranslatorFactory} used to generate visitors for
     * EJBQL to SQL translations. This method should be overriden by subclasses that need
     * to customize EJBQL generation.
     * 
     * @since 3.0
     */
    protected EJBQLTranslatorFactory createEJBQLTranslatorFactory() {
        return new JdbcEJBQLTranslatorFactory();
    }

    /**
     * Returns primary key generator associated with this DbAdapter.
     */
    public PkGenerator getPkGenerator() {
        return pkGenerator;
    }

    /**
     * Sets new primary key generator.
     * 
     * @since 1.1
     */
    public void setPkGenerator(PkGenerator pkGenerator) {
        this.pkGenerator = pkGenerator;
    }

    /**
     * Returns true.
     */
    public boolean supportsFkConstraints() {
        return supportsFkConstraints;
    }

    /**
     * @since 1.1
     */
    public void setSupportsFkConstraints(boolean flag) {
        this.supportsFkConstraints = flag;
    }

    /**
     * Returns true.
     * 
     * @since 1.1
     */
    public boolean supportsUniqueConstraints() {
        return supportsUniqueConstraints;
    }

    /**
     * @since 1.1
     */
    public void setSupportsUniqueConstraints(boolean flag) {
        this.supportsUniqueConstraints = flag;
    }

    /**
     * Returns a SQL string to drop a table corresponding to <code>ent</code> DbEntity.
     */
    public String dropTable(DbEntity ent) {
        return "DROP TABLE " + ent.getFullyQualifiedName();
    }

    /**
     * Returns a SQL string that can be used to create database table corresponding to
     * <code>ent</code> parameter.
     */
    public String createTable(DbEntity entity) {
 
        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append("CREATE TABLE ").append(entity.getFullyQualifiedName()).append(
                " (");

        // columns
        Iterator it = entity.getAttributes().iterator();
        if (it.hasNext()) {
            boolean first = true;
            while (it.hasNext()) {
                if (first) {
                    first = false;
                }
                else {
                    sqlBuffer.append(", ");
                }

                DbAttribute column = (DbAttribute) it.next();

                // attribute may not be fully valid, do a simple check
                if (column.getType() == TypesMapping.NOT_DEFINED) {
                    throw new CayenneRuntimeException("Undefined type for attribute '"
                            + entity.getFullyQualifiedName()
                            + "."
                            + column.getName()
                            + "'.");
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
        Iterator pkit = entity.getPrimaryKeys().iterator();
        if (pkit.hasNext()) {
            sqlBuffer.append(", PRIMARY KEY (");
            boolean firstPk = true;
            while (pkit.hasNext()) {
                if (firstPk)
                    firstPk = false;
                else
                    sqlBuffer.append(", ");

                DbAttribute at = (DbAttribute) pkit.next();
                sqlBuffer.append(at.getName());
            }
            sqlBuffer.append(')');
        }
    }

    /**
     * Appends SQL for column creation to CREATE TABLE buffer.
     * 
     * @since 1.2
     */
    protected void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {
        String[] types = externalTypesForJdbcType(column.getType());
        if (types == null || types.length == 0) {
            String entityName = column.getEntity() != null ? ((DbEntity) column
                    .getEntity()).getFullyQualifiedName() : "<null>";
            throw new CayenneRuntimeException("Undefined type for attribute '"
                    + entityName
                    + "."
                    + column.getName()
                    + "': "
                    + column.getType());
        }

        String type = types[0];
        sqlBuffer.append(column.getName()).append(' ').append(type);

        // append size and precision (if applicable)
        if (TypesMapping.supportsLength(column.getType())) {
            int len = column.getMaxLength();
            int scale = TypesMapping.isDecimal(column.getType()) ? column.getScale() : -1;

            // sanity check
            if (scale > len) {
                scale = -1;
            }

            if (len > 0) {
                sqlBuffer.append('(').append(len);

                if (scale >= 0) {
                    sqlBuffer.append(", ").append(scale);
                }

                sqlBuffer.append(')');
            }
        }

        sqlBuffer.append(column.isMandatory() ? " NOT NULL" : " NULL");
    }

    /**
     * Returns a DDL string to create a unique constraint over a set of columns.
     * 
     * @since 1.1
     */
    public String createUniqueConstraint(DbEntity source, Collection columns) {
        if (columns == null || columns.isEmpty()) {
            throw new CayenneRuntimeException(
                    "Can't create UNIQUE constraint - no columns specified.");
        }

        StringBuffer buf = new StringBuffer();

        buf.append("ALTER TABLE ").append(source.getFullyQualifiedName()).append(
                " ADD UNIQUE (");

        Iterator it = columns.iterator();
        DbAttribute first = (DbAttribute) it.next();
        buf.append(first.getName());

        while (it.hasNext()) {
            DbAttribute next = (DbAttribute) it.next();
            buf.append(", ");
            buf.append(next.getName());
        }

        buf.append(")");

        return buf.toString();
    }

    /**
     * Returns a SQL string that can be used to create a foreign key constraint for the
     * relationship.
     */
    public String createFkConstraint(DbRelationship rel) {
        StringBuffer buf = new StringBuffer();
        StringBuffer refBuf = new StringBuffer();

        buf.append("ALTER TABLE ").append(
                ((DbEntity) rel.getSourceEntity()).getFullyQualifiedName()).append(
                " ADD FOREIGN KEY (");

        Iterator jit = rel.getJoins().iterator();
        boolean first = true;
        while (jit.hasNext()) {
            DbJoin join = (DbJoin) jit.next();
            if (!first) {
                buf.append(", ");
                refBuf.append(", ");
            }
            else
                first = false;

            buf.append(join.getSourceName());
            refBuf.append(join.getTargetName());
        }

        buf
                .append(") REFERENCES ")
                .append(((DbEntity) rel.getTargetEntity()).getFullyQualifiedName())
                .append(" (")
                .append(refBuf.toString())
                .append(')');
        return buf.toString();
    }

    public String[] externalTypesForJdbcType(int type) {
        return typesHandler.externalTypesForJdbcType(type);
    }

    public ExtendedTypeMap getExtendedTypes() {
        return extendedTypes;
    }

    public DbAttribute buildAttribute(
            String name,
            String typeName,
            int type,
            int size,
            int scale,
            boolean allowNulls) {

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

    public String tableTypeForTable() {
        return "TABLE";
    }

    public String tableTypeForView() {
        return "VIEW";
    }

    /**
     * Creates and returns a default implementation of a qualifier translator.
     */
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        return new QualifierTranslator(queryAssembler);
    }

    /**
     * Uses JdbcActionBuilder to create the right action.
     * 
     * @since 1.2
     */
    public SQLAction getAction(Query query, DataNode node) {
        return query
                .createSQLAction(new JdbcActionBuilder(this, node.getEntityResolver()));
    }

    public void bindParameter(
            PreparedStatement statement,
            Object object,
            int pos,
            int sqlType,
            int scale) throws SQLException, Exception {

        if (object == null) {
            statement.setNull(pos, sqlType);
        }
        else {
            ExtendedType typeProcessor = getExtendedTypes().getRegisteredType(
                    object.getClass());
            typeProcessor.setJdbcObject(statement, object, pos, sqlType, scale);
        }
    }

    public boolean supportsBatchUpdates() {
        return this.supportsBatchUpdates;
    }

    public void setSupportsBatchUpdates(boolean flag) {
        this.supportsBatchUpdates = flag;
    }

    /**
     * @since 1.2
     */
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
     * Returns a translator factory for EJBQL to SQL translation. This property is
     * normally initialized in constructor by calling
     * {@link #createEJBQLTranslatorFactory()}, and can be overriden by calling
     * {@link #setEjbqlTranslatorFactory(EJBQLTranslatorFactory)}.
     * 
     * @since 3.0
     */
    public EJBQLTranslatorFactory getEjbqlTranslatorFactory() {
        return ejbqlTranslatorFactory;
    }

    /**
     * Sets a translator factory for EJBQL to SQL translation. This property is normally
     * initialized in constructor by calling {@link #createEJBQLTranslatorFactory()}, so
     * users would only override it if they need to customize EJBQL translation.
     * 
     * @since 3.0
     */
    public void setEjbqlTranslatorFactory(EJBQLTranslatorFactory ejbqlTranslatorFactory) {
        this.ejbqlTranslatorFactory = ejbqlTranslatorFactory;
    }

    public MergerFactory mergerFactory() {
        return new MergerFactory();
    }
}
