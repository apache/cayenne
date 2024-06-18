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
package org.apache.cayenne.query;

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.apache.cayenne.exp.ExpressionFactory.*;

/**
 * A query to select objects by id.
 * 
 * @since 4.0
 */
public class SelectById<T> extends IndirectQuery implements Select<T> {

	private static final long serialVersionUID = -6589464349051607583L;

	final QueryRoot root;
	final IdSpec idSpec;
	final boolean fetchingDataRows;

	QueryCacheStrategy cacheStrategy;
	String cacheGroup;
	PrefetchTreeNode prefetches;

	public static <T> SelectById<T> query(Class<T> entityType, Object id) {
		QueryRoot root = new ByEntityTypeResolver(entityType);
		IdSpec idSpec = new SingleScalarIdSpec(id);
		return new SelectById<>(root, idSpec);
	}

	public static <T> SelectById<T> query(Class<T> entityType, Map<String, ?> id) {
		QueryRoot root = new ByEntityTypeResolver(entityType);
		IdSpec idSpec = new SingleMapIdSpec(id);
		return new SelectById<>(root, idSpec);
	}

	public static <T> SelectById<T> query(Class<T> entityType, ObjectId id) {
		checkObjectId(id);
		QueryRoot root = new ByEntityNameResolver(id.getEntityName());
		IdSpec idSpec = new SingleMapIdSpec(id.getIdSnapshot());
		return new SelectById<>(root, idSpec);
	}

	/**
	 * @since 4.2
	 */
	public static <T> SelectById<T> query(Class<T> entityType, Object firstId, Object... otherIds) {
		QueryRoot root = new ByEntityTypeResolver(entityType);
		IdSpec idSpec = new MultiScalarIdSpec(firstId, otherIds);
		return new SelectById<>(root, idSpec);
	}

	/**
	 * @since 4.2
	 */
	public static <T> SelectById<T> query(Class<T> entityType, Collection<Object> ids) {
		QueryRoot root = new ByEntityTypeResolver(entityType);
		IdSpec idSpec = new MultiScalarIdSpec(ids);
		return new SelectById<>(root, idSpec);
	}

	/**
	 * @since 4.2
	 */
	@SafeVarargs
	public static <T> SelectById<T> query(Class<T> entityType, Map<String, ?> firstId, Map<String, ?>... otherIds) {
		QueryRoot root = new ByEntityTypeResolver(entityType);
		IdSpec idSpec = new MultiMapIdSpec(firstId, otherIds);
		return new SelectById<>(root, idSpec);
	}

	/**
	 * @since 4.2
	 */
	public static <T> SelectById<T> query(Class<T> entityType, ObjectId firstId, ObjectId... otherIds) {
		checkObjectId(firstId);
		for(ObjectId id : otherIds) {
			checkObjectId(id, firstId.getEntityName());
		}

		QueryRoot root = new ByEntityNameResolver(firstId.getEntityName());
		IdSpec idSpec = new MultiMapIdSpec(firstId, otherIds);
		return new SelectById<>(root, idSpec);
	}

	public static SelectById<DataRow> dataRowQuery(Class<?> entityType, Object id) {
		QueryRoot root = new ByEntityTypeResolver(entityType);
		IdSpec idSpec = new SingleScalarIdSpec(id);
		return new SelectById<>(root, idSpec, true);
	}

	public static SelectById<DataRow> dataRowQuery(Class<?> entityType, Map<String, ?> id) {
		QueryRoot root = new ByEntityTypeResolver(entityType);
		IdSpec idSpec = new SingleMapIdSpec(id);
		return new SelectById<>(root, idSpec, true);
	}

	public static SelectById<DataRow> dataRowQuery(ObjectId id) {
		checkObjectId(id);
		QueryRoot root = new ByEntityNameResolver(id.getEntityName());
		IdSpec idSpec = new SingleMapIdSpec(id.getIdSnapshot());
		return new SelectById<>(root, idSpec, true);
	}

	/**
	 * @since 4.2
	 */
	public static SelectById<DataRow> dataRowQuery(Class<?> entityType, Object firstId, Object... otherIds) {
		QueryRoot root = new ByEntityTypeResolver(entityType);
		IdSpec idSpec = new MultiScalarIdSpec(firstId, otherIds);
		return new SelectById<>(root, idSpec, true);
	}

	/**
	 * @since 4.2
	 */
	@SafeVarargs
	public static SelectById<DataRow> dataRowQuery(Class<?> entityType, Map<String, ?> firstId, Map<String, ?>... otherIds) {
		QueryRoot root = new ByEntityTypeResolver(entityType);
		IdSpec idSpec = new MultiMapIdSpec(firstId, otherIds);
		return new SelectById<>(root, idSpec, true);
	}

	/**
	 * @since 4.2
	 */
	public static SelectById<DataRow> dataRowQuery(ObjectId firstId, ObjectId... otherIds) {
		checkObjectId(firstId);
		for(ObjectId id : otherIds) {
			checkObjectId(id, firstId.getEntityName());
		}

		QueryRoot root = new ByEntityNameResolver(firstId.getEntityName());
		IdSpec idSpec = new MultiMapIdSpec(firstId, otherIds);
		return new SelectById<>(root, idSpec, true);
	}

	protected SelectById(QueryRoot root, IdSpec idSpec, boolean fetchingDataRows) {
		this.root = root;
		this.idSpec = idSpec;
		this.fetchingDataRows = fetchingDataRows;
	}

	protected SelectById(QueryRoot root, IdSpec idSpec) {
		this(root, idSpec, false);
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
		context.iterate(this, callback);
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
	 * query.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, cacheGroup);
	 * </pre>
	 */
	public SelectById<T> localCache(String cacheGroup) {
		return cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, cacheGroup);
	}

	/**
	 * Instructs Cayenne to look for query results in the "local" cache when
	 * running the query. This is a short-hand notation for:
	 *
	 * <pre>
	 * query.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
	 * </pre>
	 */
	public SelectById<T> localCache() {
		return cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
	}

	/**
	 * Instructs Cayenne to look for query results in the "shared" cache when
	 * running the query. This is a short-hand notation for:
	 *
	 * <pre>
	 * query.cacheStrategy(QueryCacheStrategy.SHARED_CACHE, cacheGroup);
	 * </pre>
	 */
	public SelectById<T> sharedCache(String cacheGroup) {
		return cacheStrategy(QueryCacheStrategy.SHARED_CACHE, cacheGroup);
	}

	/**
	 * Instructs Cayenne to look for query results in the "shared" cache when
	 * running the query. This is a short-hand notation for:
	 *
	 * <pre>
	 * query.cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
	 * </pre>
	 */
	public SelectById<T> sharedCache() {
		return cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
	}

	public QueryCacheStrategy getCacheStrategy() {
		return cacheStrategy;
	}

	public SelectById<T> cacheStrategy(QueryCacheStrategy strategy) {
		if (this.cacheStrategy != strategy) {
			this.cacheStrategy = strategy;
			this.replacementQuery = null;
		}

		return this;
	}

	public SelectById<T> cacheStrategy(QueryCacheStrategy strategy, String cacheGroup) {
		return cacheStrategy(strategy).cacheGroup(cacheGroup);
	}

	public String getCacheGroup() {
		return cacheGroup;
	}

	public SelectById<T> cacheGroup(String cacheGroup) {
		this.cacheGroup = cacheGroup;
		this.replacementQuery = null;
		return this;
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

	@Override
	protected Query createReplacementQuery(EntityResolver resolver) {
		ObjEntity entity = root.resolve(resolver);

		ObjectSelect<?> query = new ObjectSelect<>()
				.entityName(entity.getName())
				.where(idSpec.getQualifier(entity))
				.cacheStrategy(cacheStrategy, cacheGroup);
		if(prefetches != null) {
			query.prefetch(prefetches);
		}
		if(fetchingDataRows) {
			query.fetchDataRows();
		}
		return query;
	}

	private static String resolveSinglePkName(ObjEntity entity) {
		Collection<String> pkAttributes = entity.getPrimaryKeyNames();
		if(pkAttributes.size() == 1) {
			return pkAttributes.iterator().next();
		}
		throw new CayenneRuntimeException("PK contains %d columns, expected 1.",  pkAttributes.size());
	}

	private static void checkObjectId(ObjectId id) {
		if (id.isTemporary() && !id.isReplacementIdAttached()) {
			throw new CayenneRuntimeException("Can't build a query for a temporary id: %s", id);
		}
	}

	private static void checkObjectId(ObjectId id, String entityName) {
		checkObjectId(id);
		if(!entityName.equals(id.getEntityName())) {
			throw new CayenneRuntimeException("Can't build a query with mixed object types for given ObjectIds");
		}
	}

	@SafeVarargs
	private static <E, R> Collection<R> foldArguments(Function<E, R> mapper, E first, E... other) {
		List<R> result = new ArrayList<>(1 + other.length);
		result.add(mapper.apply(first));
		for(E next : other) {
			result.add(mapper.apply(next));
		}
		return result;
	}

	protected interface QueryRoot extends Serializable {
		ObjEntity resolve(EntityResolver resolver);
	}

	protected interface IdSpec extends Serializable{
		Expression getQualifier(ObjEntity entity);
	}

	protected static class SingleScalarIdSpec implements IdSpec {

		private final Object id;

		protected SingleScalarIdSpec(Object id) {
			this.id = id;
		}

		@Override
		public Expression getQualifier(ObjEntity entity) {
			return matchDbExp(resolveSinglePkName(entity), id);
		}
	}

	protected static class MultiScalarIdSpec implements IdSpec {

		private final Collection<Object> ids;

		protected MultiScalarIdSpec(Object firstId, Object... otherIds) {
			this.ids = foldArguments(Function.identity(), firstId, otherIds);
		}

		protected MultiScalarIdSpec(Collection<Object> ids) {
			this.ids = ids;
		}

		@Override
		public Expression getQualifier(ObjEntity entity) {
			return inDbExp(resolveSinglePkName(entity), ids);
		}
	}

	protected static class SingleMapIdSpec implements IdSpec {

		private final Map<String, ?> id;

		protected SingleMapIdSpec(Map<String, ?> id) {
			this.id = id;
		}

		@Override
		public Expression getQualifier(ObjEntity entity) {
			return matchAllDbExp(id, Expression.EQUAL_TO);
		}
	}

	protected static class MultiMapIdSpec implements IdSpec {

		private final Collection<Map<String, ?>> ids;

		@SafeVarargs
		protected MultiMapIdSpec(Map<String, ?> firstId, Map<String, ?>... otherIds) {
			this.ids = foldArguments(Function.identity(), firstId, otherIds);
		}

		protected MultiMapIdSpec(ObjectId firstId, ObjectId... otherIds) {
			this.ids = foldArguments(ObjectId::getIdSnapshot, firstId, otherIds);
		}

		@Override
		public Expression getQualifier(ObjEntity entity) {
			List<Expression> expressions = new ArrayList<>();
			for(Map<String, ?> id : ids) {
				expressions.add(matchAllDbExp(id, Expression.EQUAL_TO));
			}

			return or(expressions);
		}
	}

	private static class ByEntityTypeResolver implements QueryRoot {
		private final Class<?> entityType;

		public ByEntityTypeResolver(Class<?> entityType) {
			this.entityType = entityType;
		}

		@Override
		public ObjEntity resolve(EntityResolver resolver) {
			return resolver.getObjEntity(entityType);
		}
	}

	private static class ByEntityNameResolver implements QueryRoot {
		private final String entityName;

		public ByEntityNameResolver(String entityName) {
			this.entityName = entityName;
		}

		@Override
		public ObjEntity resolve(EntityResolver resolver) {
			return resolver.getObjEntity(entityName);
		}
	}
}
