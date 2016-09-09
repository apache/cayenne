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

package org.apache.cayenne.access.translator.select;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.dba.DbAdapter;
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
	protected List<ParameterBinding> bindings;

	/**
	 * @since 4.0
	 */
	public QueryAssembler(Query query, DbAdapter adapter, EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
		this.adapter = adapter;
		this.query = query;
		this.queryMetadata = query.getMetaData(entityResolver);
		this.bindings = new ArrayList<ParameterBinding>();
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
		ParameterBinding binding = new ParameterBinding(dbAttr);
		binding.setValue(anObject);
		binding.setStatementPosition(bindings.size() + 1);
		bindings.add(binding);
	}

    /**
     * Translates internal query into PreparedStatement.
     */
    public PreparedStatement createStatement() throws Exception {
        long t1 = System.currentTimeMillis();
        String sqlStr = createSqlString();
        logger.logQuery(sqlStr, attributes, values, System.currentTimeMillis() - t1);
        PreparedStatement stmt = connection.prepareStatement(sqlStr);
        initStatement(stmt);
        return stmt;
    }

    /**
     * Initializes prepared statements with collected parameters. Called
     * internally from "createStatement". Cayenne users shouldn't normally call
     * it directly.
     */
    protected void initStatement(PreparedStatement stmt) throws Exception {
        if (values != null && values.size() > 0) {
            int len = values.size();
            for (int i = 0; i < len; i++) {
                Object val = values.get(i);

                DbAttribute attr = attributes.get(i);

                // null DbAttributes are a result of inferior qualifier processing
                // (qualifier can't map parameters to DbAttributes and therefore
                // only supports standard java types now) hence, a special moronic case here:
                if (attr == null) {
                    stmt.setObject(i + 1, val);
                } else {
                    adapter.bindParameter(stmt, val, i + 1, attr.getType(), attr.getScale());
                }
            }
        }

        if (queryMetadata.getStatementFetchSize() != 0) {
            stmt.setFetchSize(queryMetadata.getStatementFetchSize());
        }
    }

	/**
	 * @since 4.0
	 */
	public ParameterBinding[] getBindings() {
		return bindings.toArray(new ParameterBinding[bindings.size()]);
	}
}
