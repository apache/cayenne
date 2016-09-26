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
package org.apache.cayenne.query;

import static java.util.Collections.singletonMap;
import static org.apache.cayenne.exp.ExpressionFactory.matchAllDbExp;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.ResultIteratorCallback;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

/**
 * A query to select single objects by id.
 * 
 * @since 4.0
 */
public class SelectById<T> extends IndirectQuery implements Select<T> {

	private static final long serialVersionUID = -6589464349051607583L;

	// type is not same as T, as T maybe be DataRow or scalar
	// either type or entity name is specified, but not both
	Class<?> entityType;
	String entityName;

	// only one of the two id forms is provided, but not both
	Object singleId;
	Map<String, ?> mapId;

	boolean fetchingDataRows;
	QueryCacheStrategy cacheStrategy;
	String[] cacheGroups;
	PrefetchTreeNode prefetches;

	public static <T> SelectById<T> query(Class<T> entityType, Object id) {
		SelectById<T> q = new SelectById<T>();

		q.entityType = entityType;
		q.singleId = id;
		q.fetchingDataRows = false;

		return q;
	}

	public static <T> SelectById<T> query(Class<T> entityType, Map<String, ?> id) {
		SelectById<T> q = new SelectById<T>();

		q.entityType = entityType;
		q.mapId = id;
		q.fetchingDataRows = false;

		return q;
	}

	public static <T> SelectById<T> query(Class<T> entityType, ObjectId id) {
		checkObjectId(id);

		SelectById<T> q = new SelectById<T>();

		q.entityName = id.getEntityName();
		q.mapId = id.getIdSnapshot();
		q.fetchingDataRows = false;

		return q;
	}

	public static SelectById<DataRow> dataRowQuery(Class<?> entityType, Object id) {
		SelectById<DataRow> q = new SelectById<DataRow>();

		q.entityType = entityType;
		q.singleId = id;
		q.fetchingDataRows = true;

		return q;
	}

	public static SelectById<DataRow> dataRowQuery(Class<?> entityType, Map<String, Object> id) {
		SelectById<DataRow> q = new SelectById<DataRow>();

		q.entityType = entityType;
		q.mapId = id;
		q.fetchingDataRows = true;

		return q;
	}

	public static SelectById<DataRow> dataRowQuery(ObjectId id) {
		checkObjectId(id);

		SelectById<DataRow> q = new SelectById<DataRow>();

		q.entityName = id.getEntityName();
		q.mapId = id.getIdSnapshot();
		q.fetchingDataRows = true;

		return q;
	}

	private static void checkObjectId(ObjectId id) {
		if (id.isTemporary() && !id.isReplacementIdAttached()) {
			throw new CayenneRuntimeException("Can't build a query for temporary id: " + id);
		}
	}

	@Override
	public List<T> select(ObjectContext context) {
		return context.select(this);
	}

	@Override
	public T selectOne(ObjectContext context) {
		return context.selectOne(this);
	}

	@Override
	public T selectFirst(ObjectContext context) {
		return context.selectFirst(this);
	}

	@Override
	public void iterate(ObjectContext context, ResultIteratorCallback<T> callback) {
		context.iterate((Select<T>) this, callback);
	}

	@Override
	public ResultIterator<T> iterator(ObjectContext context) {
		return context.iterator(this);
	}

	@Override
	public ResultBatchIterator<T> batchIterator(ObjectContext context, int size) {
		return context.batchIterator(this, size);
	}

	/**
	 * Instructs Cayenne to look for query results in the "local" cache when
	 * running the query. This is a short-hand notation for:
	 *
	 * <pre>
	 * query.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, cacheGroups);
	 * </pre>
	 *
	 * @since 4.0.M3
	 */
	public SelectById<T> localCache(String... cacheGroups) {
		return cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, cacheGroups);
	}

	/**
	 * Instructs Cayenne to look for query results in the "shared" cache when
	 * running the query. This is a short-hand notation for:
	 *
	 * <pre>
	 * query.cacheStrategy(QueryCacheStrategy.SHARED_CACHE, cacheGroups);
	 * </pre>
	 *
	 * @since 4.0.M3
	 */
	public SelectById<T> sharedCache(String... cacheGroups) {
		return cacheStrategy(QueryCacheStrategy.SHARED_CACHE, cacheGroups);
	}

	/**
	 * Instructs Cayenne to look for query results in the "local" cache when
	 * running the query. This is a short-hand notation for:
	 *
	 * @deprecated since 4.0.M3 use {@link #localCache(String...)}
	 */
	@Deprecated
	public SelectById<T> useLocalCache(String... cacheGroups) {
		return localCache(cacheGroups);
	}

	/**
	 * Instructs Cayenne to look for query results in the "shared" cache when
	 * running the query. This is a short-hand notation for:
	 *
	 * @deprecated since 4.0.M3 use {@link #sharedCache(String...)}
	 */
	@Deprecated
	public SelectById<T> useSharedCache(String... cacheGroups) {
		return sharedCache(cacheGroups);
	}

	public QueryCacheStrategy getCacheStrategy() {
		return cacheStrategy;
	}

	public SelectById<T> cacheStrategy(QueryCacheStrategy strategy, String... cacheGroups) {
		if (this.cacheStrategy != strategy) {
			this.cacheStrategy = strategy;
			this.replacementQuery = null;
		}

		return cacheGroups(cacheGroups);
	}

	public String[] getCacheGroups() {
		return cacheGroups;
	}

	public SelectById<T> cacheGroups(String... cacheGroups) {
		this.cacheGroups = cacheGroups != null && cacheGroups.length > 0 ? cacheGroups : null;
		this.replacementQuery = null;
		return this;
	}

	public SelectById<T> cacheGroups(Collection<String> cacheGroups) {

		if (cacheGroups == null) {
			return cacheGroups((String) null);
		}

		String[] array = new String[cacheGroups.size()];
		return cacheGroups(cacheGroups.toArray(array));
	}

	public boolean isFetchingDataRows() {
		return fetchingDataRows;
	}

	/**
	 * Merges prefetch into the query prefetch tree.
	 * 
	 * @return this object
	 */
	public SelectById<T> prefetch(PrefetchTreeNode prefetch) {

		if (prefetch == null) {
			return this;
		}

		if (prefetches == null) {
			prefetches = new PrefetchTreeNode();
		}

		prefetches.merge(prefetch);
		return this;
	}

	/**
	 * Merges a prefetch path with specified semantics into the query prefetch
	 * tree.
	 * 
	 * @return this object
	 */
	public SelectById<T> prefetch(String path, int semantics) {

		if (path == null) {
			return this;
		}

		if (prefetches == null) {
			prefetches = new PrefetchTreeNode();
		}

		prefetches.addPath(path).setSemantics(semantics);
		return this;
	}

	public PrefetchTreeNode getPrefetches() {
		return prefetches;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Query createReplacementQuery(EntityResolver resolver) {

		ObjEntity entity = resolveEntity(resolver);
		Map<String, ?> id = resolveId(entity);

		SelectQuery<Object> query = new SelectQuery<Object>();
		query.setRoot(entity);
		query.setFetchingDataRows(fetchingDataRows);
		query.setQualifier(matchAllDbExp(id, Expression.EQUAL_TO));

		// note on caching... this hits query cache instead of object cache...
		// until we merge the two this may result in not using the cache
		// optimally - object cache may have an object, but query cache will not
		query.setCacheGroups(cacheGroups);
		query.setCacheStrategy(cacheStrategy);
		query.setPrefetchTree(prefetches);

		return query;
	}

	protected Map<String, ?> resolveId(ObjEntity entity) {

		if (singleId == null && mapId == null) {
			throw new CayenneRuntimeException("Misconfigured query. Either singleId or mapId must be set");
		}

		if (mapId != null) {
			return mapId;
		}

		Collection<String> pkAttributes = entity.getPrimaryKeyNames();
		if (pkAttributes.size() != 1) {
			throw new CayenneRuntimeException("PK contains " + pkAttributes.size() + " columns, expected 1.");
		}

		String pk = pkAttributes.iterator().next();
		return singletonMap(pk, singleId);
	}

	protected ObjEntity resolveEntity(EntityResolver resolver) {

		if (entityName == null && entityType == null) {
			throw new CayenneRuntimeException("Misconfigured query. Either entityName or entityType must be set");
		}

		return entityName != null ? resolver.getObjEntity(entityName) : resolver.getObjEntity(entityType);
	}
}
