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
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.resolved.ResolvedQuery;
import org.apache.cayenne.query.resolved.ResolvedSelect;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Base class for {@link ObjectSelect} and {@link ColumnSelect}
 *
 * @since 4.0
 */
public abstract class FluentSelect<T, S extends FluentSelect<T, S>> implements Query<T> {

    // an untyped "self" of the query root, used to build "by id" qualifiers; paths in the resulting expressions
    // are relative to the root and get resolved against the actual entity at translation time
    private static final SelfProperty<Persistent> ROOT = PropertyFactory.createSelf(Persistent.class);

    // root
    protected Class<?> entityType;
    protected String entityName;
    protected String dbEntityName;

    protected Expression where;
    protected Expression having;
    protected boolean havingExpressionIsActive = false;

    protected Collection<Ordering> orderings;
    protected boolean distinct;

    protected int fetchLimit = ResolvedQuery.FETCH_LIMIT_DEFAULT;
    protected int fetchOffset = ResolvedQuery.FETCH_OFFSET_DEFAULT;
    protected int pageSize = ResolvedQuery.PAGE_SIZE_DEFAULT;
    protected int statementFetchSize = ResolvedQuery.STATEMENT_FETCH_SIZE_DEFAULT;
    protected int queryTimeout = ResolvedQuery.QUERY_TIMEOUT_DEFAULT;
    protected QueryCacheStrategy cacheStrategy = QueryCacheStrategy.getDefaultStrategy();
    protected String cacheGroup;
    protected boolean fetchingDataRows = ResolvedQuery.FETCHING_DATA_ROWS_DEFAULT;
    protected PrefetchTreeNode prefetches;

    protected FluentSelect() {
    }

    /**
     * Copies the state of another select into this one. Used to turn an {@link ObjectSelect} into a
     * {@link ColumnSelect}.
     */
    protected void copyFrom(FluentSelect<?, ?> other) {
        this.entityType = other.entityType;
        this.entityName = other.entityName;
        this.dbEntityName = other.dbEntityName;
        this.where = other.where;
        this.having = other.having;
        this.orderings = other.orderings;
        this.fetchLimit = other.fetchLimit;
        this.fetchOffset = other.fetchOffset;
        this.pageSize = other.pageSize;
        this.statementFetchSize = other.statementFetchSize;
        this.queryTimeout = other.queryTimeout;
        this.cacheStrategy = other.cacheStrategy;
        this.cacheGroup = other.cacheGroup;
        this.fetchingDataRows = other.fetchingDataRows;
        this.prefetches = other.prefetches != null ? other.prefetches.deepClone() : null;
    }

    /**
     * Sets the type of the entity to fetch without changing the return type of
     * the query.
     *
     * @return this object
     */
    public S entityType(Class<?> entityType) {
        return resetEntity(entityType, null, null);
    }

    /**
     * Sets the {@link ObjEntity} name to fetch without changing the return type
     * of the query. This form is most often used for generic entities that
     * don't map to a distinct class.
     *
     * @return this object
     */
    public S entityName(String entityName) {
        return resetEntity(null, entityName, null);
    }

    /**
     * Sets the {@link DbEntity} name to fetch without changing the return type
     * of the query. This form is most often used for generic entities that
     * don't map to a distinct class.
     *
     * @return this object
     */
    public S dbEntityName(String dbEntityName) {
        return resetEntity(null, null, dbEntityName);
    }

    private S resetEntity(Class<?> entityType, String entityName, String dbEntityName) {
        this.entityType = entityType;
        this.entityName = entityName;
        this.dbEntityName = dbEntityName;
        return castSelf();
    }

    /**
     * Appends a qualifier expression of this query. An equivalent to
     * {@link #and(Expression...)} that can be used a syntactic sugar.
     *
     * @return this object
     */
    public S where(Expression expression) {
        return and(expression);
    }

    /**
     * Appends a qualifier expression of this query, using provided expression
     * String and an array of position parameters. This is an equivalent to
     * calling "and".
     *
     * @return this object
     */
    public S where(String expressionString, Object... parameters) {
        return and(ExpressionFactory.exp(expressionString, parameters));
    }

    /**
     * Appends a qualifier matching a single object of the root entity by its id. The id can be one of:
     * <ul>
     *     <li>a scalar, for entities with a single-column PK, e.g. {@code byId(1)};</li>
     *     <li>a {@link Map} of PK column names to values, e.g. {@code byId(Map.of("KEY1", "x", "KEY2", "y"))},
     *     which is the form to use for a compound PK;</li>
     *     <li>an {@link ObjectId} of the root entity, matched by its id snapshot.</li>
     * </ul>
     * This is a shorthand for {@link #where(Expression)} with one of the "id" expressions of the root entity
     * "self" property, so it combines with any other query settings. Like {@code where(..)}, it is AND'ed to
     * any qualifier set previously.
     *
     * @param id a scalar, a {@code Map} or an {@code ObjectId}
     * @since 5.0
     */
    public S byId(Object id) {
        return and(idQualifier(id));
    }

    /**
     * Appends a qualifier matching any of the objects of the root entity with the given ids. Each id can be a
     * scalar, a {@link Map} of PK column names to values or an {@link ObjectId}, and the forms can be mixed.
     * Scalars are combined into a single {@code IN}, the rest into an {@code OR} of per-id matches. An empty
     * array matches nothing.
     *
     * @param ids scalars, {@code Map}s or {@code ObjectId}s
     * @return this object
     * @since 5.0
     */
    public S byIds(Object... ids) {
        if (ids == null) {
            throw new CayenneRuntimeException("Null ids");
        }
        return byIds(Arrays.asList(ids));
    }

    /**
     * Appends a qualifier matching any of the objects of the root entity with the given ids. Each id can be a
     * scalar, a {@link Map} of PK column names to values or an {@link ObjectId}, and the forms can be mixed.
     * Scalars are combined into a single {@code IN}, the rest into an {@code OR} of per-id matches. An empty
     * collection matches nothing.
     *
     * @param ids scalars, {@code Map}s or {@code ObjectId}s
     * @return this object
     * @since 5.0
     */
    public S byIds(Collection<?> ids) {
        if (ids == null) {
            throw new CayenneRuntimeException("Null ids");
        }

        List<Object> scalars = new ArrayList<>();
        List<Map<String, ?>> maps = new ArrayList<>();
        for (Object id : ids) {
            if (id instanceof ObjectId oid) {
                maps.add(idSnapshot(oid));
            } else if (id instanceof Map<?, ?> map) {
                maps.add(idMap(map));
            } else {
                scalars.add(id);
            }
        }

        if (scalars.isEmpty() && maps.isEmpty()) {
            return and(ExpressionFactory.expFalse());
        }

        Expression qualifier = null;
        if (!scalars.isEmpty()) {
            qualifier = ROOT.idsInCollection(scalars);
        }
        if (!maps.isEmpty()) {
            Expression mapsQualifier = ROOT.idMapsInCollection(maps);
            qualifier = qualifier == null ? mapsQualifier : qualifier.orExp(mapsQualifier);
        }
        return and(qualifier);
    }

    private static Expression idQualifier(Object id) {
        if (id instanceof ObjectId oid) {
            return ROOT.eqIdMap(idSnapshot(oid));
        } else if (id instanceof Map<?, ?> map) {
            return ROOT.eqIdMap(idMap(map));
        } else {
            return ROOT.eqId(id);
        }
    }

    private static Map<String, ?> idSnapshot(ObjectId id) {
        if (id.isTemporary() && !id.isReplacementIdAttached()) {
            throw new CayenneRuntimeException("Can't build a query for a temporary id: %s", id);
        }
        return id.getIdSnapshot();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> idMap(Map<?, ?> id) {
        if (id.isEmpty()) {
            throw new CayenneRuntimeException("Empty id map");
        }
        return (Map<String, ?>) id;
    }

    /**
     * AND's provided expressions to the existing WHERE clause expression.
     *
     * @return this object
     */
    public S and(Expression... expressions) {
        if (expressions == null || expressions.length == 0) {
            return castSelf();
        }
        return and(Arrays.asList(expressions));
    }

    /**
     * AND's provided expressions to the existing WHERE clause expression.
     *
     * @return this object
     */

    public S and(Collection<Expression> expressions) {
        return joinExpression(expressions, ExpressionFactory::and);
    }

    protected S joinExpression(Collection<Expression> expressions, Function<Collection<Expression>, Expression> joiner) {
        if (expressions == null || expressions.isEmpty()) {
            return castSelf();
        }

        Collection<Expression> all;
        Expression activeExpression = getActiveExpression();
        if (activeExpression != null) {
            all = new ArrayList<>(expressions.size() + 1);
            all.add(activeExpression);
            all.addAll(expressions);
        } else {
            all = expressions;
        }

        setActiveExpression(joiner.apply(all));
        return castSelf();
    }

    /**
     * OR's provided expressions to the existing WHERE clause expression.
     *
     * @return this object
     */
    public S or(Expression... expressions) {
        if (expressions == null || expressions.length == 0) {
            return castSelf();
        }
        return or(Arrays.asList(expressions));
    }

    /**
     * OR's provided expressions to the existing WHERE clause expression.
     *
     * @return this object
     */
    public S or(Collection<Expression> expressions) {
        return joinExpression(expressions, ExpressionFactory::or);
    }

    /**
     * Add an ascending ordering on the given property. If there is already an ordering
     * on this query then add this ordering with a lower priority.
     *
     * @param property the property to sort on
     * @return this object
     */
    public S orderBy(String property) {
        return orderBy(new Ordering(property));
    }

    /**
     * Add an ordering on the given property. If there is already an ordering
     * on this query then add this ordering with a lower priority.
     *
     * @param property  the property to sort on
     * @param sortOrder the direction of the ordering
     * @return this object
     */
    public S orderBy(String property, SortOrder sortOrder) {
        return orderBy(new Ordering(property, sortOrder));
    }

    /**
     * Add one or more orderings to this query.
     *
     * @return this object
     */
    public S orderBy(Ordering... orderings) {

        if (orderings == null) {
            return castSelf();
        }

        if (this.orderings == null) {
            this.orderings = new ArrayList<>(orderings.length);
        }

        Collections.addAll(this.orderings, orderings);

        return castSelf();
    }

    /**
     * Adds a list of orderings to this query.
     *
     * @return this object
     */
    public S orderBy(Collection<Ordering> orderings) {

        if (orderings == null) {
            return castSelf();
        }

        if (this.orderings == null) {
            this.orderings = new ArrayList<>(orderings.size());
        }

        this.orderings.addAll(orderings);

        return castSelf();
    }

    /**
     * Merges prefetch into the query prefetch tree.
     *
     * @return this object
     */
    public S prefetch(PrefetchTreeNode prefetch) {
        if (prefetches == null) {
            prefetches = new PrefetchTreeNode();
        }
        prefetches.merge(prefetch);
        return castSelf();
    }

    /**
     * Merges a prefetch path with specified semantics into the query prefetch tree.
     *
     * @return this object
     */
    public S prefetch(String path, int semantics) {
        if (path == null) {
            return castSelf();
        }
        if (prefetches == null) {
            prefetches = new PrefetchTreeNode();
        }
        PrefetchTreeNode node = prefetches.addPath(path);
        node.setSemantics(semantics);
        node.setPhantom(false);
        return castSelf();
    }

    /**
     * Resets query fetch limit - a parameter that defines max number of objects
     * that should be ever be fetched from the database.
     */
    public S limit(int fetchLimit) {
        this.fetchLimit = fetchLimit;
        return castSelf();
    }

    /**
     * Resets query fetch offset - a parameter that defines how many objects
     * should be skipped when reading data from the database.
     */
    public S offset(int fetchOffset) {
        this.fetchOffset = fetchOffset;
        return castSelf();
    }

    /**
     * Resets query page size. A non-negative page size enables query result
     * pagination that saves memory and processing time for large lists if only
     * parts of the result are ever going to be accessed.
     */
    public S pageSize(int pageSize) {
        this.pageSize = pageSize;
        return castSelf();
    }

    /**
     * Sets fetch size of the PreparedStatement generated for this query. Any non-zero value is passed to the driver
     * as is; 0 (the default) leaves the fetch size to the adapter and the driver. A negative value is only meaningful
     * for drivers that give it a special meaning, such as {@link Integer#MIN_VALUE} for streaming on MySQL.
     *
     * @see Statement#setFetchSize(int)
     */
    public S statementFetchSize(int size) {
        this.statementFetchSize = size;
        return castSelf();
    }

    /**
     * Sets query timeout of PreparedStatement generated for this query.
     *
     * @see Statement#setQueryTimeout(int)
     */
    public S queryTimeout(int timeout) {
        this.queryTimeout = timeout;
        return castSelf();
    }

    public S cacheStrategy(QueryCacheStrategy strategy) {
        this.cacheStrategy = strategy != null ? strategy : QueryCacheStrategy.getDefaultStrategy();
        this.cacheGroup = null;
        return castSelf();
    }

    public S cacheStrategy(QueryCacheStrategy strategy, String cacheGroup) {
        return cacheStrategy(strategy).cacheGroup(cacheGroup);
    }

    public S cacheGroup(String cacheGroup) {
        this.cacheGroup = cacheGroup;
        return castSelf();
    }

    /**
     * Instructs Cayenne to look for query results in the "local" cache when
     * running the query. This is a short-hand notation for:
     * <p>
     * <pre>
     * query.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, cacheGroup);
     * </pre>
     */
    public S localCache(String cacheGroup) {
        return cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, cacheGroup);
    }

    /**
     * Instructs Cayenne to look for query results in the "local" cache when
     * running the query. This is a short-hand notation for:
     * <p>
     * <pre>
     * query.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
     * </pre>
     */
    public S localCache() {
        return cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
    }

    /**
     * Instructs Cayenne to look for query results in the "shared" cache when
     * running the query. This is a short-hand notation for:
     * <p>
     * <pre>
     * query.cacheStrategy(QueryCacheStrategy.SHARED_CACHE, cacheGroup);
     * </pre>
     */
    public S sharedCache(String cacheGroup) {
        return cacheStrategy(QueryCacheStrategy.SHARED_CACHE, cacheGroup);
    }

    /**
     * Instructs Cayenne to look for query results in the "shared" cache when
     * running the query. This is a short-hand notation for:
     * <p>
     * <pre>
     * query.cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
     * </pre>
     */
    public S sharedCache() {
        return cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
    }

    /**
     * @since 3.0
     */
    public QueryCacheStrategy getCacheStrategy() {
        return cacheStrategy;
    }

    /**
     * @since 4.0
     */
    public String getCacheGroup() {
        return cacheGroup;
    }

    public int getStatementFetchSize() {
        return statementFetchSize;
    }

    /**
     * @since 4.2
     */
    public int getQueryTimeout() {
        return queryTimeout;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getLimit() {
        return fetchLimit;
    }

    public int getOffset() {
        return fetchOffset;
    }

    public Class<?> getEntityType() {
        return entityType;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getDbEntityName() {
        return dbEntityName;
    }

    /**
     * Returns a WHERE clause Expression of this query.
     */
    public Expression getWhere() {
        return where;
    }

    /**
     * Returns a HAVING clause Expression of this query.
     */
    public Expression getHaving() {
        return having;
    }

    public Collection<Ordering> getOrderings() {
        return orderings;
    }

    public PrefetchTreeNode getPrefetches() {
        return prefetches;
    }

    protected void setActiveExpression(Expression exp) {
        if (havingExpressionIsActive) {
            having = exp;
        } else {
            where = exp;
        }
    }

    protected Expression getActiveExpression() {
        if (havingExpressionIsActive) {
            return having;
        } else {
            return where;
        }
    }

    @Override
    public ResolvedSelect resolve(EntityResolver resolver) {
        ResolvedSelect.Builder builder = ResolvedSelect.builder()
                .source(this)
                .where(where)
                .having(having)
                .orderings(orderings)
                .distinct(distinct)
                .fetchingDataRows(fetchingDataRows)
                .fetchLimit(fetchLimit)
                .fetchOffset(fetchOffset)
                .pageSize(pageSize)
                .statementFetchSize(statementFetchSize)
                .queryTimeout(queryTimeout)
                .cacheStrategy(cacheStrategy)
                .cacheGroup(cacheGroup)
                .prefetchTree(prefetches);

        if (entityType != null) {
            builder.entityType(entityType);
        } else if (entityName != null) {
            builder.entityName(entityName);
        } else if (dbEntityName != null) {
            builder.dbEntityName(dbEntityName);
        }

        configure(builder);
        return builder.build(resolver);
    }

    /**
     * A hook for subclasses to add their own state to the resolved select. The base implementation does nothing.
     */
    protected void configure(ResolvedSelect.Builder builder) {
    }

    public boolean isFetchingDataRows() {
        return fetchingDataRows;
    }

    /**
     * @since 4.2
     */
    public Collection<Property<?>> getColumns() {
        return null;
    }

    /**
     * @since 4.2
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * Utility method to perform (re)cast this type, doesn't perform any checks, so use with caution.
     *
     * @param <E> type to cast to
     * @return <code>this</code> casted to the type E
     * @since 5.0
     */
    @SuppressWarnings("unchecked")
    protected <E> E castSelf() {
        return (E) this;
    }
}
