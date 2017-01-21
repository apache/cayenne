/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.access.translator.select;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;

/**
 * Abstract superclass of Query translators.
 */
public abstract class QueryAssembler {

	protected Query query;
	protected QueryMetadata queryMetadata;
	protected boolean translated;
	protected String sql;
	protected DbAdapter adapter;
	protected EntityResolver entityResolver;
	protected List<DbAttributeBinding> bindings;
	/**
	 * @since 4.0
	 */
	protected AddBindingListener addBindingListener;

	/**
	 * @since 4.0
	 */
	public QueryAssembler(Query query, DbAdapter adapter, EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
		this.adapter = adapter;
		this.query = query;
		this.queryMetadata = query.getMetaData(entityResolver);
		this.bindings = new ArrayList<>();
	}

	/**
	 * Returns aliases for the path splits defined in the query.
	 *
	 * @since 3.0
	 */
	protected Map<String, String> getPathAliases() {
		return queryMetadata.getPathSplitAliases();
	}

	public EntityResolver getEntityResolver() {
		return entityResolver;
	}

	public DbAdapter getAdapter() {
		return adapter;
	}

	/**
	 * Returns query object being processed.
	 */
	public Query getQuery() {
		return query;
	}

	public QueryMetadata getQueryMetadata() {
		return queryMetadata;
	}

	/**
	 * A callback invoked by a child qualifier or ordering processor allowing
	 * query assembler to reset its join stack.
	 *
	 * @since 3.0
	 */
	public abstract void resetJoinStack();

	/**
	 * Returns an alias of the table which is currently at the top of the join
	 * stack.
	 *
	 * @since 3.0
	 */
	public abstract String getCurrentAlias();

	/**
	 * Appends a join with given semantics to the query.
	 *
	 * @since 3.0
	 */
	public abstract void dbRelationshipAdded(DbRelationship relationship, JoinType joinType, String joinSplitAlias);

	/**
	 * Translates query into an SQL string formatted to use in a
	 * PreparedStatement.
	 */
	public String getSql() {
		ensureTranslated();
		return sql;
	}

	/**
	 * @since 4.0
	 */
	protected void ensureTranslated() {
		if (!translated) {
			doTranslate();
			translated = true;
		}
	}

	/**
	 * @since 4.0
	 */
	protected abstract void doTranslate();

	/**
	 * Returns <code>true</code> if table aliases are supported. Default
	 * implementation returns false.
	 */
	public boolean supportsTableAliases() {
		return false;
	}

	/**
	 * Registers <code>anObject</code> as a PreparedStatement parameter.
	 *
	 * @param anObject
	 *            object that represents a value of DbAttribute
	 * @param dbAttr
	 *            DbAttribute being processed.
	 */
	public void addToParamList(DbAttribute dbAttr, Object anObject) {
		ExtendedType extendedType = anObject != null
				? adapter.getExtendedTypes().getRegisteredType(anObject.getClass())
				: adapter.getExtendedTypes().getDefaultType();

		DbAttributeBinding binding = new DbAttributeBinding(dbAttr);
		binding.setStatementPosition(bindings.size() + 1);
		binding.setValue(anObject);
		binding.setExtendedType(extendedType);

		bindings.add(binding);
		if(addBindingListener != null) {
			addBindingListener.onAdd(binding);
		}
	}

	/**
	 * @since 4.0
	 */
	public DbAttributeBinding[] getBindings() {
		return bindings.toArray(new DbAttributeBinding[bindings.size()]);
	}

    /**
     * @since 4.0
     */
	public abstract String getAliasForExpression(Expression exp);

	/**
	 * @since 4.0
	 */
	public void setAddBindingListener(AddBindingListener addBindingListener) {
		this.addBindingListener = addBindingListener;
	}

	/**
	 * @since 4.0
	 */
	protected interface AddBindingListener {
		void onAdd(DbAttributeBinding binding);
	}
}
