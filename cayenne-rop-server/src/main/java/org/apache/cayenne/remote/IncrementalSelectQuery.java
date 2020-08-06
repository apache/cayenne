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
import java.util.Map;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.ResultIteratorCallback;
import org.apache.cayenne.access.IncrementalFaultList;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.QueryMetadataProxy;
import org.apache.cayenne.query.QueryRouter;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLActionVisitor;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.util.XMLEncoder;

/**
 * A SelectQuery decorator that overrides the metadata to ensure that query
 * result is cached on the server, so that subranges could be retrieved at a
 * later time. Note that a special decorator that is a subclass of SelectQuery
 * is needed so that {@link IncrementalFaultList} on the server-side could apply
 * SelectQuery-specific optimizations.
 * 
 * @since 3.0
 */
class IncrementalSelectQuery<T> extends SelectQuery<T> {

	private SelectQuery<T> query;
	private String cacheKey;

	IncrementalSelectQuery(SelectQuery<T> delegate, String cacheKey) {
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
	public void addOrdering(Ordering ordering) {
		query.addOrdering(ordering);
	}

	@Override
	public void addOrdering(String sortPathSpec, SortOrder order) {
		query.addOrdering(sortPathSpec, order);
	}

	@Override
	public void addOrderings(Collection<? extends Ordering> orderings) {
		query.addOrderings(orderings);
	}

	@Override
	public PrefetchTreeNode addPrefetch(String prefetchPath) {
		return query.addPrefetch(prefetchPath);
	}

	@Override
	public void andQualifier(Expression e) {
		query.andQualifier(e);
	}

	@Override
	public void clearOrderings() {
		query.clearOrderings();
	}

	@Override
	public void clearPrefetches() {
		query.clearPrefetches();
	}

	@Override
	public SelectQuery<T> createQuery(Map<String, ?> parameters) {
		return query.createQuery(parameters);
	}

	@Override
	public SQLAction createSQLAction(SQLActionVisitor visitor) {
		return query.createSQLAction(visitor);
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
	public int getFetchLimit() {
		return query.getFetchLimit();
	}

	@Override
	public List<Ordering> getOrderings() {
		return query.getOrderings();
	}

	@Override
	public int getPageSize() {
		return query.getPageSize();
	}

	@Override
	public PrefetchTreeNode getPrefetchTree() {
		return query.getPrefetchTree();
	}

	@Override
	public Expression getQualifier() {
		return query.getQualifier();
	}

	@Override
	public Object getRoot() {
		return query.getRoot();
	}

	@Override
	public int hashCode() {
		return query.hashCode();
	}

	@Override
	public void initWithProperties(Map<String, ?> properties) {
		query.initWithProperties(properties);
	}

	@Override
	public boolean isDistinct() {
		return query.isDistinct();
	}

	@Override
	public boolean isFetchingDataRows() {
		return query.isFetchingDataRows();
	}

	@Override
	public void orQualifier(Expression e) {
		query.orQualifier(e);
	}

	@Override
	public SelectQuery<T> queryWithParameters(Map<String, ?> parameters, boolean pruneMissing) {
		return query.queryWithParameters(parameters, pruneMissing);
	}

	@Override
	public SelectQuery<T> queryWithParameters(Map<String, ?> parameters) {
		return query.queryWithParameters(parameters);
	}

	@Override
	public void removeOrdering(Ordering ordering) {
		query.removeOrdering(ordering);
	}

	@Override
	public void removePrefetch(String prefetchPath) {
		query.removePrefetch(prefetchPath);
	}

	@Override
	public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
		query.route(router, resolver, substitutedQuery);
	}

	/**
	 * @since 4.0
	 */
	@Override
	public void setCacheGroup(String cacheGroup) {
		query.setCacheGroup(cacheGroup);
	}

	@Override
	public void setDistinct(boolean distinct) {
		query.setDistinct(distinct);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void setFetchingDataRows(boolean flag) {
		query.setFetchingDataRows(flag);
	}

	@Override
	public void setFetchLimit(int fetchLimit) {
		query.setFetchLimit(fetchLimit);
	}

	@Override
	public void setPageSize(int pageSize) {
		query.setPageSize(pageSize);
	}

	@Override
	public void setPrefetchTree(PrefetchTreeNode prefetchTree) {
		query.setPrefetchTree(prefetchTree);
	}

	@Override
	public void setQualifier(Expression qualifier) {
		query.setQualifier(qualifier);
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
}
