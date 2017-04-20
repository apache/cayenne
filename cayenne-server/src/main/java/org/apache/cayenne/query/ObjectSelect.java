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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A selecting query providing chainable API. This is an alternative to
 * {@link SelectQuery} when you want to use a fluent API. For example, the following
 * is a convenient way to return a record:
 * <pre>
 * {@code
 * Artist a = ObjectSelect
 *      .query(Artist.class)
 *      .where(Artist.NAME.eq("Picasso"))
 *      .selectOne(context);
 * }
 * </pre>
 *
 * @since 4.0
 */
public class ObjectSelect<T> extends FluentSelect<T> {

    private static final long serialVersionUID = -156124021150949227L;

    protected boolean fetchingDataRows;

    /**
     * Creates a ObjectSelect that selects objects of a given persistent class.
     */
    public static <T> ObjectSelect<T> query(Class<T> entityType) {
        return new ObjectSelect<T>().entityType(entityType);
    }

    /**
     * Creates a ObjectSelect that selects objects of a given persistent class
     * and uses provided expression for its qualifier.
     */
    public static <T> ObjectSelect<T> query(Class<T> entityType, Expression expression) {
        return new ObjectSelect<T>().entityType(entityType).where(expression);
    }

    /**
     * Creates a ObjectSelect that selects objects of a given persistent class
     * and uses provided expression for its qualifier.
     */
    public static <T> ObjectSelect<T> query(Class<T> entityType, Expression expression, List<Ordering> orderings) {
        return new ObjectSelect<T>().entityType(entityType).where(expression).orderBy(orderings);
    }

    /**
     * Creates a ObjectSelect that fetches data for an {@link ObjEntity}
     * determined from a provided class.
     */
    public static ObjectSelect<DataRow> dataRowQuery(Class<?> entityType) {
        return query(entityType).fetchDataRows();
    }

    /**
     * Creates a ObjectSelect that fetches data for an {@link ObjEntity}
     * determined from a provided class and uses provided expression for its
     * qualifier.
     */
    public static ObjectSelect<DataRow> dataRowQuery(Class<?> entityType, Expression expression) {
        return query(entityType).fetchDataRows().where(expression);
    }

    /**
     * Creates a ObjectSelect that fetches data for {@link ObjEntity} determined
     * from provided "entityName", but fetches the result of a provided type.
     * This factory method is most often used with generic classes that by
     * themselves are not enough to resolve the entity to fetch.
     */
    public static <T> ObjectSelect<T> query(Class<T> resultType, String entityName) {
        return new ObjectSelect<T>().entityName(entityName);
    }

    /**
     * Creates a ObjectSelect that fetches DataRows for a {@link DbEntity}
     * determined from provided "dbEntityName".
     */
    public static ObjectSelect<DataRow> dbQuery(String dbEntityName) {
        return new ObjectSelect<DataRow>().fetchDataRows().dbEntityName(dbEntityName);
    }

    /**
     * Creates a ObjectSelect that fetches DataRows for a {@link DbEntity}
     * determined from provided "dbEntityName" and uses provided expression for
     * its qualifier.
     *
     * @return this object
     */
    public static ObjectSelect<DataRow> dbQuery(String dbEntityName, Expression expression) {
        return new ObjectSelect<DataRow>().fetchDataRows().dbEntityName(dbEntityName).where(expression);
    }

    /**
     * Creates a ColumnSelect that will fetch single property that can be resolved
     * against a given {@link ObjEntity} class.
     *
     * @param entityType base persistent class that will be used as a root for this query
     * @param column single column to select
     */
    public static <E> ColumnSelect<E> columnQuery(Class<?> entityType, Property<E> column) {
        return new ColumnSelect<>().entityType(entityType).column(column);
    }

    /**
     * Creates a ColumnSelect that will fetch multiple columns of a given {@link ObjEntity}
     *
     * @param entityType base persistent class that will be used as a root for this query
     * @param firstColumn column to select
     * @param otherColumns columns to select
     */
    public static ColumnSelect<Object[]> columnQuery(Class<?> entityType, Property<?> firstColumn, Property<?>... otherColumns) {
        return new ColumnSelect<Object[]>().entityType(entityType).columns(firstColumn, otherColumns);
    }

    protected ObjectSelect() {
    }

    /**
     * Translates self to a SelectQuery.
     */
    @SuppressWarnings({"deprecation", "unchecked"})
    @Override
    protected Query createReplacementQuery(EntityResolver resolver) {
        SelectQuery<?> replacement = (SelectQuery<?>) super.createReplacementQuery(resolver);
        replacement.setFetchingDataRows(fetchingDataRows);
        return replacement;
    }

    /**
     * Sets the type of the entity to fetch without changing the return type of
     * the query.
     *
     * @return this object
     */
    public ObjectSelect<T> entityType(Class<?> entityType) {
        return resetEntity(entityType, null, null);
    }

    /**
     * Sets the {@link ObjEntity} name to fetch without changing the return type
     * of the query. This form is most often used for generic entities that
     * don't map to a distinct class.
     *
     * @return this object
     */
    public ObjectSelect<T> entityName(String entityName) {
        return resetEntity(null, entityName, null);
    }

    /**
     * Sets the {@link DbEntity} name to fetch without changing the return type
     * of the query. This form is most often used for generic entities that
     * don't map to a distinct class.
     *
     * @return this object
     */
    public ObjectSelect<T> dbEntityName(String dbEntityName) {
        return resetEntity(null, null, dbEntityName);
    }

    private ObjectSelect<T> resetEntity(Class<?> entityType, String entityName, String dbEntityName) {
        this.entityType = entityType;
        this.entityName = entityName;
        this.dbEntityName = dbEntityName;
        return this;
    }

    /**
     * Appends a qualifier expression of this query. An equivalent to
     * {@link #and(Expression...)} that can be used a syntactic sugar.
     *
     * @return this object
     */
    public ObjectSelect<T> where(Expression expression) {
        return and(expression);
    }

    /**
     * Appends a qualifier expression of this query, using provided expression
     * String and an array of position parameters. This is an equivalent to
     * calling "and".
     *
     * @return this object
     */
    public ObjectSelect<T> where(String expressionString, Object... parameters) {
        return and(ExpressionFactory.exp(expressionString, parameters));
    }

    /**
     * AND's provided expressions to the existing WHERE clause expression.
     *
     * @return this object
     */
    public ObjectSelect<T> and(Expression... expressions) {
        if (expressions == null || expressions.length == 0) {
            return this;
        }

        return and(Arrays.asList(expressions));
    }

    /**
     * AND's provided expressions to the existing WHERE clause expression.
     *
     * @return this object
     */
    public ObjectSelect<T> and(Collection<Expression> expressions) {

        if (expressions == null || expressions.isEmpty()) {
            return this;
        }

        Collection<Expression> all;

        if (where != null) {
            all = new ArrayList<>(expressions.size() + 1);
            all.add(where);
            all.addAll(expressions);
        } else {
            all = expressions;
        }

        where = ExpressionFactory.and(all);
        return this;
    }

    /**
     * OR's provided expressions to the existing WHERE clause expression.
     *
     * @return this object
     */
    public ObjectSelect<T> or(Expression... expressions) {
        if (expressions == null || expressions.length == 0) {
            return this;
        }

        return or(Arrays.asList(expressions));
    }

    /**
     * OR's provided expressions to the existing WHERE clause expression.
     *
     * @return this object
     */
    public ObjectSelect<T> or(Collection<Expression> expressions) {
        if (expressions == null || expressions.isEmpty()) {
            return this;
        }

        Collection<Expression> all;

        if (where != null) {
            all = new ArrayList<>(expressions.size() + 1);
            all.add(where);
            all.addAll(expressions);
        } else {
            all = expressions;
        }

        where = ExpressionFactory.or(all);
        return this;
    }

    /**
     * Add an ascending ordering on the given property. If there is already an ordering
     * on this query then add this ordering with a lower priority.
     *
     * @param property the property to sort on
     * @return this object
     */
    public ObjectSelect<T> orderBy(String property) {
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
    public ObjectSelect<T> orderBy(String property, SortOrder sortOrder) {
        return orderBy(new Ordering(property, sortOrder));
    }

    /**
     * Add one or more orderings to this query.
     *
     * @return this object
     */
    public ObjectSelect<T> orderBy(Ordering... orderings) {

        if (orderings == null) {
            return this;
        }

        if (this.orderings == null) {
            this.orderings = new ArrayList<>(orderings.length);
        }

        Collections.addAll(this.orderings, orderings);

        return this;
    }

    /**
     * Adds a list of orderings to this query.
     *
     * @return this object
     */
    public ObjectSelect<T> orderBy(Collection<Ordering> orderings) {

        if (orderings == null) {
            return this;
        }

        if (this.orderings == null) {
            this.orderings = new ArrayList<>(orderings.size());
        }

        this.orderings.addAll(orderings);

        return this;
    }

    /**
     * Merges prefetch into the query prefetch tree.
     *
     * @return this object
     */
    public ObjectSelect<T> prefetch(PrefetchTreeNode prefetch) {

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
    public ObjectSelect<T> prefetch(String path, int semantics) {

        if (path == null) {
            return this;
        }

        if (prefetches == null) {
            prefetches = new PrefetchTreeNode();
        }

        prefetches.addPath(path).setSemantics(semantics);
        return this;
    }

    /**
     * Resets query fetch limit - a parameter that defines max number of objects
     * that should be ever be fetched from the database.
     */
    @SuppressWarnings("unchecked")
    public ObjectSelect<T> limit(int fetchLimit) {
        if (this.limit != fetchLimit) {
            this.limit = fetchLimit;
            this.replacementQuery = null;
        }

        return this;
    }

    /**
     * Resets query fetch offset - a parameter that defines how many objects
     * should be skipped when reading data from the database.
     */
    public ObjectSelect<T> offset(int fetchOffset) {
        if (this.offset != fetchOffset) {
            this.offset = fetchOffset;
            this.replacementQuery = null;
        }

        return this;
    }

    /**
     * Resets query page size. A non-negative page size enables query result
     * pagination that saves memory and processing time for large lists if only
     * parts of the result are ever going to be accessed.
     */
    public ObjectSelect<T> pageSize(int pageSize) {
        if (this.pageSize != pageSize) {
            this.pageSize = pageSize;
            this.replacementQuery = null;
        }

        return this;
    }

    /**
     * Sets fetch size of the PreparedStatement generated for this query. Only
     * non-negative values would change the default size.
     *
     * @see Statement#setFetchSize(int)
     */
    public ObjectSelect<T> statementFetchSize(int size) {
        if (this.statementFetchSize != size) {
            this.statementFetchSize = size;
            this.replacementQuery = null;
        }

        return this;
    }

    public ObjectSelect<T> cacheStrategy(QueryCacheStrategy strategy) {
        if (this.cacheStrategy != strategy) {
            this.cacheStrategy = strategy;
            this.replacementQuery = null;
        }

        if(this.cacheGroup != null) {
            this.cacheGroup = null;
            this.replacementQuery = null;
        }

        return this;
    }

    public ObjectSelect<T> cacheStrategy(QueryCacheStrategy strategy, String cacheGroup) {
        return cacheStrategy(strategy).cacheGroup(cacheGroup);
    }

    public ObjectSelect<T> cacheGroup(String cacheGroup) {
        this.cacheGroup = cacheGroup;
        this.replacementQuery = null;
        return this;
    }

    /**
     * Instructs Cayenne to look for query results in the "local" cache when
     * running the query. This is a short-hand notation for:
     * <p>
     * <pre>
     * query.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, cacheGroup);
     * </pre>
     */
    public ObjectSelect<T> localCache(String cacheGroup) {
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
    public ObjectSelect<T> localCache() {
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
    public ObjectSelect<T> sharedCache(String cacheGroup) {
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
    public ObjectSelect<T> sharedCache() {
        return cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
    }

    /**
     * Forces query to fetch DataRows. This automatically changes whatever
     * result type was set previously to "DataRow".
     *
     * @return this object
     */
    @SuppressWarnings("unchecked")
    public ObjectSelect<DataRow> fetchDataRows() {
        this.fetchingDataRows = true;
        return (ObjectSelect<DataRow>) this;
    }

    /**
     * <p>Select only specific properties.</p>
     * <p>Can be any properties that can be resolved against root entity type
     * (root entity's properties, function call expressions, properties of relationships, etc).</p>
     * <p>
     * <pre>
     * {@code
     * List<Object[]> columns = ObjectSelect.query(Artist.class)
     *                                    .columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH)
     *                                    .select(context);
     * }
     * </pre>
     *
     * @param properties array of properties to select
     * @see ObjectSelect#column(Property)
     */
    public ColumnSelect<Object[]> columns(Property<?> firstProperty, Property<?>... properties) {
        return new ColumnSelect<>(this).columns(firstProperty, properties);
    }

    /**
     * <p>Select one specific property.</p>
     * <p>Can be any property that can be resolved against root entity type
     * (root entity's property, function call expression, property of relationships, etc)</p>
     * <p>If you need several columns use {@link ObjectSelect#columns(Property, Property[])} method.</p>
     * <p>
     * <pre>
     * {@code
     * List<String> names = ObjectSelect.query(Artist.class)
     *                                  .column(Artist.ARTIST_NAME)
     *                                  .select(context);
     * }
     * </pre>
     * </p>
     * @param property single property to select
     * @see ObjectSelect#columns(Property, Property[])
     */
    public <E> ColumnSelect<E> column(Property<E> property) {
        return new ColumnSelect<>(this).column(property);
    }

    /**
     * Select COUNT(*)
     * @see ObjectSelect#column(Property)
     */
    public ColumnSelect<Long> count() {
        return column(Property.COUNT);
    }

    /**
     * <p>Select COUNT(property)</p>
     * <p>Can return different result than COUNT(*) as it will count only non null values</p>
     * @see ObjectSelect#count()
     * @see ObjectSelect#column(Property)
     */
    public ColumnSelect<Long> count(Property<?> property) {
        return column(property.count());
    }

    /**
     * <p>Select minimum value of property</p>
     * @see ObjectSelect#column(Property)
     */
    public <E> ColumnSelect<E> min(Property<E> property) {
        return column(property.min());
    }

    /**
     * <p>Select maximum value of property</p>
     * @see ObjectSelect#column(Property)
     */
    public <E> ColumnSelect<E> max(Property<E> property) {
        return column(property.max());
    }

    /**
     * <p>Select average value of property</p>
     * @see ObjectSelect#column(Property)
     */
    public <E> ColumnSelect<E> avg(Property<E> property) {
        return column(property.avg());
    }

    /**
     * <p>Select sum of values</p>
     * @see ObjectSelect#column(Property)
     */
    public <E extends Number> ColumnSelect<E> sum(Property<E> property) {
        return column(property.sum());
    }

    /**
     * <p>Quick way to select count of records</p>
     * <p>Usage:
     * <pre>
     * {@code
     *     long count = ObjectSelect.query(Artist.class)
     *                      .where(Artist.ARTIST_NAME.like("a%"))
     *                      .selectCount(context);
     * }
     * </pre>
     * </p>
     * @param context to perform query
     * @return count of rows
     */
    public long selectCount(ObjectContext context) {
        return count().selectOne(context);
    }

    @Override
    public T selectFirst(ObjectContext context) {
        return context.selectFirst(limit(1));
    }

    public boolean isFetchingDataRows() {
        return fetchingDataRows;
    }
}
