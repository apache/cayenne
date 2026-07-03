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

package org.apache.cayenne.dba;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.CSParameter;
import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.translator.EJBQLTranslator;
import org.apache.cayenne.access.translator.ProcedureTranslator;
import org.apache.cayenne.access.translator.SelectTranslator;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.Select;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.List;

/**
 * A DbAdapter that automatically detects the kind of database it is running on
 * and instantiates an appropriate DB-specific adapter, delegating all
 * subsequent method calls to this adapter.
 *
 * @since 1.2
 */
public class AutoAdapter implements DbAdapter {

	protected Provider<DbAdapter> adapterProvider;
	protected PkGenerator pkGenerator;

	/**
	 * The actual adapter that is delegated methods execution.
	 */
	volatile DbAdapter adapter;

	/**
	 * Creates an {@link AutoAdapter} based on a delegate adapter obtained via
	 * "adapterProvider".
	 *
	 * @since 3.1
	 */
	public AutoAdapter(Provider<DbAdapter> adapterProvider) {

		if (adapterProvider == null) {
			throw new CayenneRuntimeException("Null adapterProvider");
		}

		this.adapterProvider = adapterProvider;
	}

	/**
	 * Returns a proxied DbAdapter, lazily creating it on first invocation.
	 */
	protected DbAdapter getAdapter() {
		if (adapter == null) {
			synchronized (this) {
				if (adapter == null) {
					this.adapter = loadAdapter();
				}
			}
		}

		return adapter;
	}

	/**
	 * Loads underlying DbAdapter delegate.
	 */
	protected DbAdapter loadAdapter() {
		return adapterProvider.get();
	}

	/**
	 * @since 4.2
	 */
	@Override
	public SelectTranslator getSelectTranslator(Select<?> query, EntityResolver entityResolver) {
		return getAdapter().getSelectTranslator(query, entityResolver);
	}

	/**
	 * @since 5.0
	 */
	@Override
	public ProcedureTranslator getProcedureTranslator(ProcedureQuery query, EntityResolver entityResolver) {
		return getAdapter().getProcedureTranslator(query, entityResolver);
	}

	@Override
	public String getBatchTerminator() {
		return getAdapter().getBatchTerminator();
	}

	@Override
	public SQLTreeProcessor getSqlTreeProcessor() {
		return getAdapter().getSqlTreeProcessor();
	}

	@Override
	public SQLAction getAction(Query query, DataNode node) {
		return getAdapter().getAction(query, node);
	}

	@Override
	public boolean supportsUniqueConstraints() {
		return getAdapter().supportsUniqueConstraints();
	}

	@Override
	public boolean supportsCatalogsOnReverseEngineering() {
		return getAdapter().supportsCatalogsOnReverseEngineering();
	}

	@Override
	public boolean supportsGeneratedKeys() {
		return getAdapter().supportsGeneratedKeys();
	}

	/**
	 * @since 4.2
	 */
	@Override
	public boolean supportsGeneratedKeysForBatchInserts() {
		return getAdapter().supportsGeneratedKeysForBatchInserts();
	}


	@Override
	public boolean supportsBatchUpdates() {
		return getAdapter().supportsBatchUpdates();
	}

	@Override
	public boolean typeSupportsLength(int type) {
		return getAdapter().typeSupportsLength(type);
	}

	/**
	 * @since 5.0
	 */
	@Override
	public boolean typeSupportsScale(int type) {
		return getAdapter().typeSupportsScale(type);
	}

	/**
	 * @since 5.0
	 */
	@Override
	public int defaultCharColumnLength() {
		return getAdapter().defaultCharColumnLength();
	}

	@Override
	public Collection<String> dropTableStatements(DbEntity table) {
		return getAdapter().dropTableStatements(table);
	}

	@Override
	public String createTable(DbEntity entity) {
		return getAdapter().createTable(entity);
	}

	@Override
	public String createUniqueConstraint(DbEntity source, Collection<DbAttribute> columns) {
		return getAdapter().createUniqueConstraint(source, columns);
	}

	@Override
	public String createFkConstraint(DbRelationship rel) {
		return getAdapter().createFkConstraint(rel);
	}

	@Deprecated(since = "5.0", forRemoval = true)
	@Override
	public String[] externalTypesForJdbcType(int type) {
		return getAdapter().externalTypesForJdbcType(type);
	}

	@Override
	public NativeColumnType[] nativeColumnTypes(int type) {
		return getAdapter().nativeColumnTypes(type);
	}

	@Override
	public ExtendedTypeMap getExtendedTypes() {
		return getAdapter().getExtendedTypes();
	}

	/**
	 * Returns a primary key generator.
	 */
	@Override
	public PkGenerator getPkGenerator() {
		return (pkGenerator != null) ? pkGenerator : getAdapter().getPkGenerator();
	}

	/**
	 * Sets a PK generator override. If set to non-null value, such PK generator
	 * will be used instead of the one provided by wrapped adapter.
	 */
	public void setPkGenerator(PkGenerator pkGenerator) {
		this.pkGenerator = pkGenerator;
	}

	@Override
	public DbAttribute buildAttribute(String name, String typeName, int type, int size, int precision,
			boolean allowNulls) {

		return getAdapter().buildAttribute(name, typeName, type, size, precision, allowNulls);
	}

	@Override
	public void bindParameter(PreparedStatement statement, PSParameter<?> parameter) throws Exception {
		getAdapter().bindParameter(statement, parameter);
	}

	@Override
	public void bindParameter(CallableStatement statement, CSParameter<?> parameter) throws Exception {
		getAdapter().bindParameter(statement, parameter);
	}

	/**
	 * @since 5.0
	 */
	@Override
	public int preferredBindingType(int jdbcType) {
		return getAdapter().preferredBindingType(jdbcType);
	}

	@Override
	public String tableTypeForTable() {
		return getAdapter().tableTypeForTable();
	}

	@Override
	public String tableTypeForView() {
		return getAdapter().tableTypeForView();
	}

	@Override
	public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {
		getAdapter().createTableAppendColumn(sqlBuffer, column);
	}

	@Deprecated
	@Override
	public QuotingStrategy getQuotingStrategy() {
		return getAdapter().getQuotingStrategy();
	}

	/**
	 * @since 5.0
	 */
	@Override
	public QuotingStrategy getQuotingStrategy(DbEntity entity) {
		return getAdapter().getQuotingStrategy(entity);
	}

	/**
	 * @since 4.0
	 */
	@Override
	public DbAdapter unwrap() {
		return getAdapter();
	}

	/**
	 * @since 5.0
	 */
	@Override
	public EJBQLTranslator getEjbqlTranslator() {
		return getAdapter().getEjbqlTranslator();
	}

	@Override
	public List<String> getSystemCatalogs() {
		return getAdapter().getSystemCatalogs();
	}

	@Override
	public List<String> getSystemSchemas() {
		return getAdapter().getSystemSchemas();
	}

}
