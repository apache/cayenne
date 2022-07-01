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
package org.apache.cayenne.remote;

import java.util.Collection;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.ResultIteratorCallback;
import org.apache.cayenne.access.IncrementalFaultList;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.QueryMetadataProxy;
import org.apache.cayenne.query.QueryRouter;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLActionVisitor;

/**
 * A SelectQuery decorator that overrides the metadata to ensure that query
 * result is cached on the server, so that subranges could be retrieved at a
 * later time. Note that a special decorator that is a subclass of SelectQuery
 * is needed so that {@link IncrementalFaultList} on the server-side could apply
 * SelectQuery-specific optimizations.
 * 
 * @since 3.0
 * @since 4.3 this query extends ObjectSelect
 */
class IncrementalSelectQuery<T> extends ObjectSelect<T> {

	private ObjectSelect<T> query;
	private String cacheKey;

	IncrementalSelectQuery(ObjectSelect<T> delegate, String cacheKey) {
		this.query = delegate;
		this.cacheKey = cacheKey;
	}

	@Override
	public QueryMetadata getMetaData(EntityResolver resolver) {
		final QueryMetadata metadata = query.getMetaData(resolver);

		// the way paginated queries work on the server is that they are never
		// cached
		// (IncrementalFaultList interception happens before cache
		// interception). So
		// overriding caching settings in the metadata will only affect
		// ClientServerChannel behavior
		return new QueryMetadataProxy(metadata) {
			public Query getOriginatingQuery() {
				return null;
			}

			public String getCacheKey() {
				return cacheKey;
			}
		};
	}

	@Override
	public SQLAction createSQLAction(SQLActionVisitor visitor) {
		return query.createSQLAction(visitor);
	}

	@Override
	public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
		query.route(router, resolver, substitutedQuery);
	}

	@Override
	public boolean equals(Object obj) {
		return query.equals(obj);
	}

	/**
	 * @since 4.0
	 */
	@Override
	public String getCacheGroup() {
		return super.getCacheGroup();
	}

	@Override
	public Object getRoot() {
		return query.getRoot();
	}

	@Override
	public int hashCode() {
		return query.hashCode();
	}

	/**
	 * @since 4.0
	 */
	@Override
	public void setCacheGroup(String cacheGroup) {
		query.setCacheGroup(cacheGroup);
	}

	@Override
	public void setRoot(Object value) {
		query.setRoot(value);
	}

	@Override
	public String toString() {
		return query.toString();
	}

	@Override
	public List<T> select(ObjectContext context) {
		return query.select(context);
	}

	@Override
	public T selectOne(ObjectContext context) {
		return query.selectOne(context);
	}

	@Override
	public T selectFirst(ObjectContext context) {
		return query.selectFirst(context);
	}

	@Override
	public void iterate(ObjectContext context, ResultIteratorCallback<T> callback) {
		query.iterate(context, callback);
	}

	@Override
	public ResultIterator<T> iterator(ObjectContext context) {
		return query.iterator(context);
	}

	@Override
	public ResultBatchIterator<T> batchIterator(ObjectContext context, int size) {
		return query.batchIterator(context, size);
	}

	@Override
	public Expression getWhere() {
		return query.getWhere();
	}

	/**
	 * Returns a HAVING clause Expression of this query.
	 */
	@Override
	public Expression getHaving() {
		return query.getHaving();
	}

	@Override
	public Collection<Ordering> getOrderings() {
		return query.getOrderings();
	}

	public boolean isDistinct() {
		return query.isDistinct();
	}

	@Override
	public boolean isFetchingDataRows() {
		return query.isFetchingDataRows();
	}

	@Override
	public int getStatementFetchSize() {
		return query.getStatementFetchSize();
	}

	@Override
	public int getQueryTimeout() {
		return query.getQueryTimeout();
	}

	@Override
	public int getPageSize() {
		return query.getPageSize();
	}

	@Override
	public int getLimit() {
		return query.getLimit();
	}

	@Override
	public int getOffset() {
		return query.getOffset();
	}

	@Override
	public Class<?> getEntityType() {
		return query.getEntityType();
	}

	@Override
	public String getEntityName() {
		return query.getEntityName();
	}

	@Override
	public String getDbEntityName() {
		return query.getDbEntityName();
	}

	@Override
	public PrefetchTreeNode getPrefetches() {
		return query.getPrefetches();
	}
}
