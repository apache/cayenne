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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.QueryTranslator;
import org.objectstyle.cayenne.access.trans.QualifierTranslator;
import org.objectstyle.cayenne.access.trans.QueryAssembler;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SQLAction;

/**
 * Defines API needed to handle differences between various databases accessed via JDBC.
 * Implementing classed are intended to be pluggable database-specific adapters.
 * DbAdapter-based architecture is introduced to solve the following problems:
 * <ul>
 * <li>Make Cayenne code independent from SQL syntax differences between different RDBMS.
 * <li>Allow for vendor-specific tuning of JDBC access.
 * </ul>
 * 
 * @author Andrei Adamchik
 */
public interface DbAdapter {

    /**
     * Returns a String used to terminate a batch in command-line tools. E.g. ";" on
     * Oracle or "go" on Sybase.
     * 
     * @since 1.0.4
     */
    public String getBatchTerminator();

    /**
     * Creates an returns a named instance of a DataNode.
     * 
     * @deprecated since 1.2 this method is not used as node behavior customization is
     *             done via SQLActionVisitor.
     */
    public DataNode createDataNode(String name);

    /**
     * Creates and returns a QueryTranslator appropriate for the specified
     * <code>query</code> parameter. Sets translator "query" and "adapter" property.
     * <p>
     * This factory method allows subclasses to specify their own translators that
     * implement vendor-specific optimizations.
     * </p>
     * 
     * @deprecated since 1.2 this method is unneeded as customizations are done via custom
     *             SQLActions.
     */
    public QueryTranslator getQueryTranslator(Query query) throws Exception;

    // TODO: deprecate and move into SQLAction implementation
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler);

    /**
     * Returns an instance of SQLAction that should handle the query.
     * 
     * @since 1.2
     */
    public SQLAction getAction(Query query, DataNode node);

    /**
     * Returns true if a target database supports FK constraints.
     */
    public boolean supportsFkConstraints();

    /**
     * Returns true if a target database supports UNIQUE constraints.
     * 
     * @since 1.1
     */
    public boolean supportsUniqueConstraints();

    /**
     * Returns true if a target database supports key autogeneration. This feature also
     * requires JDBC3-compliant driver.
     * 
     * @since 1.2
     */
    public boolean supportsGeneratedKeys();

    /**
     * Returns <code>true</code> if the target database supports batch updates.
     */
    public boolean supportsBatchUpdates();

    /**
     * Returns a SQL string that can be used to drop a database table corresponding to
     * <code>ent</code> parameter.
     */
    public String dropTable(DbEntity entity);

    /**
     * Returns a SQL string that can be used to create database table corresponding to
     * <code>ent</code> parameter.
     */
    public String createTable(DbEntity entity);

    /**
     * Returns a DDL string to create a unique constraint over a set of columns.
     * 
     * @since 1.1
     */
    public String createUniqueConstraint(DbEntity source, Collection columns);

    /**
     * Returns a SQL string that can be used to create a foreign key constraint for the
     * relationship.
     */
    public String createFkConstraint(DbRelationship rel);

    /**
     * Returns an array of RDBMS types that can be used with JDBC <code>type</code>.
     * Valid JDBC types are defined in java.sql.Types.
     */
    public String[] externalTypesForJdbcType(int type);

    /**
     * Returns a map of ExtendedTypes that is used to translate values between Java and
     * JDBC layer.
     * 
     * @see org.objectstyle.cayenne.access.types.ExtendedType
     */
    public ExtendedTypeMap getExtendedTypes();

    /**
     * Returns primary key generator associated with this DbAdapter.
     */
    public PkGenerator getPkGenerator();

    /**
     * Creates and returns a DbAttribute based on supplied parameters (usually obtained
     * from database meta data).
     * 
     * @param name database column name
     * @param typeName database specific type name, may be used as a hint to determine the
     *            right JDBC type.
     * @param type JDBC column type
     * @param size database column size (ignored if less than zero)
     * @param precision database column precision (ignored if less than zero)
     * @param allowNulls database column nullable parameter
     */
    public DbAttribute buildAttribute(
            String name,
            String typeName,
            int type,
            int size,
            int precision,
            boolean allowNulls);

    /**
     * Binds an object value to PreparedStatement's numbered parameter.
     */
    public void bindParameter(
            PreparedStatement statement,
            Object object,
            int pos,
            int sqlType,
            int precision) throws SQLException, Exception;

    /**
     * Returns the name of the table type (as returned by
     * <code>DatabaseMetaData.getTableTypes</code>) for a simple user table.
     */
    public String tableTypeForTable();

    /**
     * Returns the name of the table type (as returned by
     * <code>DatabaseMetaData.getTableTypes</code>) for a view table.
     */
    public String tableTypeForView();

    /**
     * @deprecated Since 1.2 this method is obsolete and is ignored across Cayenne.
     */
    public boolean shouldRunBatchQuery(
            DataNode node,
            Connection con,
            BatchQuery query,
            OperationObserver delegate) throws SQLException, Exception;
}