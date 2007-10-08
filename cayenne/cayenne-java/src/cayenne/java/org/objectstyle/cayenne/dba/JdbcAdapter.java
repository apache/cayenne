/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.dba;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.QueryTranslator;
import org.objectstyle.cayenne.access.trans.DeleteTranslator;
import org.objectstyle.cayenne.access.trans.ProcedureTranslator;
import org.objectstyle.cayenne.access.trans.QualifierTranslator;
import org.objectstyle.cayenne.access.trans.QueryAssembler;
import org.objectstyle.cayenne.access.trans.SelectTranslator;
import org.objectstyle.cayenne.access.trans.UpdateTranslator;
import org.objectstyle.cayenne.access.types.BooleanType;
import org.objectstyle.cayenne.access.types.ByteArrayType;
import org.objectstyle.cayenne.access.types.CharType;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.access.types.UtilDateType;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.DeleteQuery;
import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SQLAction;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.query.UpdateQuery;
import org.objectstyle.cayenne.util.ResourceLocator;
import org.objectstyle.cayenne.util.Util;

/**
 * A generic DbAdapter implementation. Can be used as a default adapter or as a superclass
 * of a concrete adapter implementation.
 * 
 * @author Andrei Adamchik
 */
public class JdbcAdapter implements DbAdapter {

    protected PkGenerator pkGenerator;
    protected TypesHandler typesHandler;
    protected ExtendedTypeMap extendedTypes;
    protected boolean supportsBatchUpdates;
    protected boolean supportsFkConstraints;
    protected boolean supportsUniqueConstraints;
    protected boolean supportsGeneratedKeys;

    /**
     * Creates new JdbcAdapter with a set of default parameters.
     */
    public JdbcAdapter() {
        // init defaults
        this.setSupportsBatchUpdates(false);
        this.setSupportsUniqueConstraints(true);
        this.setSupportsFkConstraints(true);

        this.pkGenerator = this.createPkGenerator();
        this.typesHandler = TypesHandler.getHandler(findAdapterResource("/types.xml"));
        this.extendedTypes = new ExtendedTypeMap();
        this.configureExtendedTypes(extendedTypes);
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
     * @deprecated since 1.2 this method is unneeded as customizations are done via custom
     *             SQLActions.
     */
    public QueryTranslator getQueryTranslator(Query query) throws Exception {
        Class queryClass = queryTranslatorClass(query);

        try {
            QueryTranslator t = (QueryTranslator) queryClass.newInstance();
            t.setQuery(query);
            t.setAdapter(this);
            return t;
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Can't load translator class: "
                    + queryClass);
        }
    }

    /**
     * Returns a class of the query translator that should be used to translate the query
     * <code>q</code> to SQL. Exists mainly for the benefit of subclasses that can
     * override this method providing their own translator.
     * 
     * @deprecated since 1.2 this method is unneeded as customizations are done via custom
     *             SQLActions.
     */
    protected Class queryTranslatorClass(Query q) {
        if (q == null) {
            throw new NullPointerException("Null query.");
        }
        else if (q instanceof SelectQuery) {
            return SelectTranslator.class;
        }
        else if (q instanceof UpdateQuery) {
            return UpdateTranslator.class;
        }
        else if (q instanceof org.objectstyle.cayenne.query.InsertQuery) {
            return org.objectstyle.cayenne.access.trans.InsertTranslator.class;
        }
        else if (q instanceof DeleteQuery) {
            return DeleteTranslator.class;
        }
        else if (q instanceof ProcedureQuery) {
            return ProcedureTranslator.class;
        }
        else {
            throw new CayenneRuntimeException("Unrecognized query class..."
                    + q.getClass().getName());
        }
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
        if (entity instanceof DerivedDbEntity) {
            throw new CayenneRuntimeException("Can't create table for derived DbEntity '"
                    + entity.getName()
                    + "'.");
        }

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
        Iterator pkit = entity.getPrimaryKey().iterator();
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
            int prec = TypesMapping.isDecimal(column.getType())
                    ? column.getPrecision()
                    : -1;

            // sanity check
            if (prec > len) {
                prec = -1;
            }

            if (len > 0) {
                sqlBuffer.append('(').append(len);

                if (prec >= 0) {
                    sqlBuffer.append(", ").append(prec);
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
            int precision,
            boolean allowNulls) {

        DbAttribute attr = new DbAttribute();
        attr.setName(name);
        attr.setType(type);
        attr.setMandatory(!allowNulls);

        if (size >= 0) {
            attr.setMaxLength(size);
        }

        if (precision >= 0) {
            attr.setPrecision(precision);
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
     * Creates an instance of DataNode class.
     * 
     * @deprecated since 1.2 this method is not used as node behavior customization is
     *             done via SQLActionVisitor.
     */
    public DataNode createDataNode(String name) {
        DataNode node = new DataNode(name);
        node.setAdapter(this);
        return node;
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
            int precision) throws SQLException, Exception {

        if (object == null) {
            statement.setNull(pos, sqlType);
        }
        else {
            ExtendedType typeProcessor = getExtendedTypes().getRegisteredType(
                    object.getClass());
            typeProcessor.setJdbcObject(statement, object, pos, sqlType, precision);
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
     * Always returns <code>true</code>, letting DataNode to handle the query.
     * 
     * @deprecated Since 1.2 this method is obsolete and is ignored across Cayenne.
     */
    public boolean shouldRunBatchQuery(
            DataNode node,
            Connection con,
            BatchQuery query,
            OperationObserver delegate) throws SQLException, Exception {
        return true;
    }
}