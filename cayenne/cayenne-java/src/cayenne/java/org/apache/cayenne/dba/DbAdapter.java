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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.QueryTranslator;
import org.apache.cayenne.access.trans.QualifierTranslator;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

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
     * @see org.apache.cayenne.access.types.ExtendedType
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
