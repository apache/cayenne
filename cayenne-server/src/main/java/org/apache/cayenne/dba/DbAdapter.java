/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/
package org.apache.cayenne.dba;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslatorFactory;
import org.apache.cayenne.access.translator.select.SelectTranslator;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

/**
 * A Cayenne extension point that abstracts the differences between specifics of
 * JDBC interfaces to various databases. Cayenne already ships with a number of
 * built-in adapters for most common databases and users can provide their own
 * custom adapters.
 */
public interface DbAdapter {

	/**
	 * Returns a String used to terminate a batch in command-line tools. E.g.
	 * ";" on Oracle or "go" on Sybase.
	 *
	 * @since 1.0.4
	 */
	String getBatchTerminator();

	/**
	 * @since 4.2
	 */
	SelectTranslator getSelectTranslator(FluentSelect<?, ?> query, EntityResolver entityResolver);

	/**
	 * @since 4.2
	 * @return {@link SQLTreeProcessor} that can adjust SQL tree to specific database flavour
	 */
	SQLTreeProcessor getSqlTreeProcessor();

	/**
	 * Returns an instance of SQLAction that should handle the query.
	 *
	 * @since 1.2
	 */
	SQLAction getAction(Query query, DataNode node);

	/**
	 * Returns true if a target database supports UNIQUE constraints.
	 *
	 * @since 1.1
	 */
	boolean supportsUniqueConstraints();

	/**
	 * Returns true if a target database supports catalogs on reverse
	 * engineering.
	 *
	 * @since 4.0
	 */
	boolean supportsCatalogsOnReverseEngineering();

	/**
	 * Returns true if a target database supports key autogeneration. This
	 * feature also requires JDBC3-compliant driver.
	 *
	 * @since 1.2
	 */
	boolean supportsGeneratedKeys();

    /**
	 * Returns true if a target database supports key autogeneration in a batch insert.
	 * @see #supportsGeneratedKeys()
     * @since 4.2
     */
    default boolean supportsGeneratedKeysForBatchInserts() {
    	return supportsGeneratedKeys();
    }

	/**
	 * Returns <code>true</code> if the target database supports batch updates.
	 */
	boolean supportsBatchUpdates();

	boolean typeSupportsLength(int type);

	/**
	 * Returns a collection of SQL statements needed to drop a database table.
	 *
	 * @since 3.0
	 */
	Collection<String> dropTableStatements(DbEntity table);

	/**
	 * Returns a SQL string that can be used to create database table
	 * corresponding to <code>entity</code> parameter.
	 */
	String createTable(DbEntity entity);

	/**
	 * Returns a DDL string to create a unique constraint over a set of columns,
	 * or null if the unique constraints are not supported.
	 *
	 * @since 1.1
	 */
	String createUniqueConstraint(DbEntity source, Collection<DbAttribute> columns);

	/**
	 * Returns a SQL string that can be used to create a foreign key constraint
	 * for the relationship, or null if foreign keys are not supported.
	 */
	String createFkConstraint(DbRelationship rel);

	/**
	 * Returns an array of RDBMS types that can be used with JDBC
	 * <code>type</code>. Valid JDBC types are defined in java.sql.Types.
	 */
	String[] externalTypesForJdbcType(int type);

	/**
	 * Returns a map of ExtendedTypes that is used to translate values between
	 * Java and JDBC layer.
	 */
	ExtendedTypeMap getExtendedTypes();

	/**
	 * Returns primary key generator associated with this DbAdapter.
	 */
	PkGenerator getPkGenerator();

	/**
	 * Set custom PK generator  associated with this DbAdapter.
	 * @param pkGenerator to set
	 * @since 4.1
	 */
	void setPkGenerator(PkGenerator pkGenerator);

	/**
	 * Creates and returns a DbAttribute based on supplied parameters (usually
	 * obtained from database meta data).
	 *
	 * @param name
	 *            database column name
	 * @param typeName
	 *            database specific type name, may be used as a hint to
	 *            determine the right JDBC type.
	 * @param type
	 *            JDBC column type
	 * @param size
	 *            database column size (ignored if less than zero)
	 * @param scale
	 *            database column scale, i.e. the number of decimal digits
	 *            (ignored if less than zero)
	 * @param allowNulls
	 *            database column nullable parameter
	 */
	DbAttribute buildAttribute(String name, String typeName, int type, int size, int scale, boolean allowNulls);

	/**
	 * Binds an object value to PreparedStatement's parameter.
	 */
	void bindParameter(PreparedStatement statement, ParameterBinding parameterBinding) throws SQLException, Exception;

	/**
	 * Returns the name of the table type (as returned by
	 * <code>DatabaseMetaData.getTableTypes</code>) for a simple user table.
	 */
	String tableTypeForTable();

	/**
	 * Returns the name of the table type (as returned by
	 * <code>DatabaseMetaData.getTableTypes</code>) for a view table.
	 */
	String tableTypeForView();

	/**
	 * Append the column type part of a "create table" to the given
	 * {@link StringBuffer}
	 *
	 * @param sqlBuffer
	 *            the {@link StringBuffer} to append the column type to
	 * @param column
	 *            the {@link DbAttribute} defining the column to append type for
	 * @since 3.0
	 */
	void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column);

	/**
	 * Returns SQL identifier quoting strategy object
	 *
	 * @since 4.0
	 */
	QuotingStrategy getQuotingStrategy();

	/**
	 * Allows the users to get access to the adapter decorated by a given
	 * adapter.
	 *
	 * @since 4.0
	 */
	DbAdapter unwrap();

	/**
	 * Returns a translator factory for EJBQL to SQL translation.
	 *
	 * @since 4.0
	 */
	EJBQLTranslatorFactory getEjbqlTranslatorFactory();

	/**
	 * @since 4.1
	 * @return list of system catalogs
	 */
	List<String> getSystemCatalogs();

	/**
	 * @since 4.1
	 * @return list of system schemas
	 */
	List<String> getSystemSchemas();

}
