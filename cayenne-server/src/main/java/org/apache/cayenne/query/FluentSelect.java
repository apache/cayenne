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

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.ResultIteratorCallback;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

/**
 * Base class for {@link ObjectSelect} and {@link ColumnSelect}
 *
 * @since 4.0
 */
public abstract class FluentSelect<T, S extends FluentSelect<T, S>> extends AbstractQuery implements Select<T> {

    // root
    protected Class<?> entityType;
    protected String entityName;
    protected String dbEntityName;

    protected Expression where;
    protected Expression having;
    protected boolean havingExpressionIsActive = false;

    protected Collection<Ordering> orderings;
    protected boolean distinct;

    protected ObjectSelectMetadata metaData;

    protected FluentSelect() {
        metaData = createMetadata();
    }

    protected abstract ObjectSelectMetadata createMetadata();

    protected Object resolveRoot(EntityResolver resolver) {
        Object root;
        if (entityType != null) {
            root = entityType;
        } else if (entityName != null) {

            ObjEntity entity = resolver.getObjEntity(entityName);
            if (entity == null) {
                throw new CayenneRuntimeException("Unrecognized ObjEntity name: %s", entityName);
            }

            root = entity;
        } else if (dbEntityName != null) {

            DbEntity entity = resolver.getDbEntity(dbEntityName);
            if (entity == null) {
                throw new CayenneRuntimeException("Unrecognized DbEntity name: %s", dbEntityName);
            }

            root = entity;
        } else {
            throw new CayenneRuntimeException("Undefined root entity of the query");
        }
        return root;
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
        getBaseMetaData().mergePrefetch(prefetch);
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
        getBaseMetaData().addPrefetch(path, semantics);
        return castSelf();
    }

    /**
     * Resets query fetch limit - a parameter that defines max number of objects
     * that should be ever be fetched from the database.
     */
    public S limit(int fetchLimit) {
        this.getBaseMetaData().setFetchLimit(fetchLimit);
        return castSelf();
    }

    /**
     * Resets query fetch offset - a parameter that defines how many objects
     * should be skipped when reading data from the database.
     */
    public S offset(int fetchOffset) {
        this.getBaseMetaData().setFetchOffset(fetchOffset);
        return castSelf();
    }

    /**
     * Resets query page size. A non-negative page size enables query result
     * pagination that saves memory and processing time for large lists if only
     * parts of the result are ever going to be accessed.
     */
    public S pageSize(int pageSize) {
        this.getBaseMetaData().setPageSize(pageSize);
        return castSelf();
    }

    /**
     * Sets fetch size of the PreparedStatement generated for this query. Only
     * non-negative values would change the default size.
     *
     * @see Statement#setFetchSize(int)
     */
    public S statementFetchSize(int size) {
        this.getBaseMetaData().setStatementFetchSize(size);
        return castSelf();
    }

    /**
     * Sets query timeout of PreparedStatement generated for this query.
     * @see Statement#setQueryTimeout(int)
     */
    public S queryTimeout(int timeout) {
        this.getBaseMetaData().setQueryTimeout(timeout);
        return castSelf();
    }

    public S cacheStrategy(QueryCacheStrategy strategy) {
        setCacheStrategy(strategy);
        setCacheGroup(null);
        return castSelf();
    }

    public S cacheStrategy(QueryCacheStrategy strategy, String cacheGroup) {
        return cacheStrategy(strategy).cacheGroup(cacheGroup);
    }

    public S cacheGroup(String cacheGroup) {
        setCacheGroup(cacheGroup);
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

    public int getStatementFetchSize() {
        return getBaseMetaData().getStatementFetchSize();
    }

    /**
     * @since 4.2
     */
    public int getQueryTimeout() {
        return getBaseMetaData().getQueryTimeout();
    }

    public int getPageSize() {
        return getBaseMetaData().getPageSize();
    }

    public int getLimit() {
        return getBaseMetaData().getFetchLimit();
    }

    public int getOffset() {
        return getBaseMetaData().getFetchOffset();
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
        return getBaseMetaData().getPrefetchTree();
    }

    protected void setActiveExpression(Expression exp) {
        if(havingExpressionIsActive) {
            having = exp;
        } else {
            where = exp;
        }
    }

    protected Expression getActiveExpression() {
        if(havingExpressionIsActive) {
            return having;
        } else {
            return where;
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

    @Override
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return visitor.objectSelectAction(this);
    }

    @Override
    public void route(QueryRouter router, EntityResolver resolver, Query substitutedQuery) {
        super.route(router, resolver, substitutedQuery);

        // suppress prefetches for paginated queries.. instead prefetches will be resolved per row...
        if (getPageSize() <= 0) {
            routePrefetches(router, resolver);
        }
    }

    public boolean isFetchingDataRows() {
        return getBaseMetaData().isFetchingDataRows();
    }

    protected void routePrefetches(QueryRouter router, EntityResolver resolver) {
        new FluentSelectPrefetchRouterAction().route(this, router, resolver);
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
     * @since 4.2
     */
    public void initWithProperties(Map<String, String> properties) {
        getBaseMetaData().initWithProperties(properties);
    }

    /**
     * Utility method to perform (re)cast this type, doesn't perform any checks, so use with caution.
     *
     * @return <code>this</code> casted to the type E
     * @param <E> type to cast to
     * @since 5.0
     */
    @SuppressWarnings("unchecked")
    protected <E> E castSelf() {
        return (E)this;
    }
}
