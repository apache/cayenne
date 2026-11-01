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
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.DbPathExp;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.ObjPathExp;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.property.ComparableProperty;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.ql.QLParser;
import org.apache.cayenne.query.resolved.ResolvedQuery;
import org.apache.cayenne.query.resolved.ResolvedSelect;

import java.io.IOException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A select query with a chainable API. It is rooted in an entity and fetches either the root objects (as persistent
 * objects or DataRows), or a set of columns: properties of the root, properties of related objects, function calls
 * and aggregates. The type parameter is the type of a result row, and the methods that change what the query selects
 * change it accordingly:
 * <pre>
 * {@code
 * ObjectSelect<Artist>   artists = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.like("P%"));
 * ObjectSelect<DataRow>  rows    = ObjectSelect.query(Artist.class).fetchDataRows();
 * ObjectSelect<String>   names   = ObjectSelect.query(Artist.class).column(Artist.ARTIST_NAME);
 * ObjectSelect<Object[]> columns = ObjectSelect.query(Artist.class).columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH);
 * ObjectSelect<Long>     count   = ObjectSelect.query(Artist.class).count();
 * }
 * </pre>
 * All the methods modify and return the same query, retyping it where needed.
 *
 * @since 4.0
 */
public class ObjectSelect<T> implements Query<T> {

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
    protected boolean suppressingDistinct;

    // null for a query of the root objects
    protected Collection<Property<?>> columns;
    protected boolean singleColumn = true;

    protected int fetchLimit = ResolvedQuery.FETCH_LIMIT_DEFAULT;
    protected int fetchOffset = ResolvedQuery.FETCH_OFFSET_DEFAULT;
    protected int pageSize = ResolvedQuery.PAGE_SIZE_DEFAULT;
    protected int statementFetchSize = ResolvedQuery.STATEMENT_FETCH_SIZE_DEFAULT;
    protected int queryTimeout = ResolvedQuery.QUERY_TIMEOUT_DEFAULT;
    protected QueryCacheStrategy cacheStrategy = QueryCacheStrategy.getDefaultStrategy();
    protected String cacheGroup;
    protected boolean fetchingDataRows = ResolvedQuery.FETCHING_DATA_ROWS_DEFAULT;
    protected PrefetchTreeNode prefetches;

    /**
     * Creates a query that selects the objects of a given persistent class.
     */
    public static <T> ObjectSelect<T> query(Class<T> entityType) {
        return new ObjectSelect<T>().entityType(entityType);
    }

    /**
     * Creates a query that fetches DataRows of an {@link ObjEntity} determined from a provided class.
     */
    public static ObjectSelect<DataRow> dataRowQuery(Class<?> entityType) {
        return query(entityType).fetchDataRows();
    }

    /**
     * Creates a query that fetches the objects of an {@link ObjEntity} determined from provided "entityName", but
     * typed as the provided result type. This factory method is most often used with generic classes that by
     * themselves are not enough to resolve the entity to fetch.
     */
    public static <T> ObjectSelect<T> query(Class<T> resultType, String entityName) {
        return new ObjectSelect<T>().entityName(entityName);
    }

    /**
     * Creates a query that fetches DataRows of a {@link DbEntity} determined from provided "dbEntityName".
     */
    public static ObjectSelect<DataRow> dbQuery(String dbEntityName) {
        return new ObjectSelect<DataRow>().fetchDataRows().dbEntityName(dbEntityName);
    }

    /**
     * Creates a query that fetches a single column resolved against a given {@link ObjEntity} class. A shortcut for
     * {@code query(entityType).column(column)}.
     *
     * @param entityType base persistent class that will be used as a root for this query
     * @param column     single column to select
     */
    public static <E> ObjectSelect<E> columnQuery(Class<?> entityType, Property<E> column) {
        return new ObjectSelect<E>().entityType(entityType).column(column);
    }

    /**
     * Creates a query that fetches multiple columns of a given {@link ObjEntity}. A shortcut for
     * {@code query(entityType).columns(columns)}.
     *
     * @param entityType base persistent class that will be used as a root for this query
     * @param columns    columns to select
     */
    public static ObjectSelect<Object[]> columnQuery(Class<?> entityType, Property<?>... columns) {
        return new ObjectSelect<Object[]>().entityType(entityType).columns(columns);
    }

    /**
     * Parses a query String. The general form of the query is
     * <pre>
     * [select [distinct] columns] from Entity [where exp] [having exp] [order by orderings]
     * [limit n] [offset m] [prefetch paths]</pre>
     * where all the expressions follow the syntax of {@link ExpressionFactory#exp(String, Object...)}. A query with
     * no "select" clause, or with "select self" fetches the root objects, any other "select" clause fetches the
     * listed columns. E.g.:
     * <pre>
     * {@code
     * ObjectSelect.parse("from Artist where artistName like $name order by dateOfBirth desc limit 10", "A%");
     * ObjectSelect.parse("select artistName, count(paintingArray) from Artist");
     * }
     * </pre>
     *
     * @param query  a query String
     * @param params values of the "$name" parameters in the String, bound in the order of their first occurrence.
     *               With no values, the parameters are preserved in the query, to be bound later, e.g. with
     *               {@link Expression#params(Map)} on its "where" clause
     * @see #parse(String, Map)
     * @see #parse(Class, String, Object...)
     * @since 5.0
     */
    public static ObjectSelect<?> parse(String query, Object... params) {
        return QLParser.parse(query, parser -> {
            if (params != null && params.length > 0) {
                parser.setParameters(params);
            }
            ObjectSelect<?> select = parser.query();
            parser.checkParametersBound();
            return select;
        });
    }

    /**
     * Parses a query String, binding the values of its "$name" parameters by name. A parameter that has no value in
     * the map is preserved, and the smallest condition of a "where" or "having" clause that contains it is dropped
     * from the query, in the subqueries alike. So the same String can be used with and without its optional
     * parameters. A null value in the map is bound as is.
     *
     * @param query  a query String
     * @param params values of the "$name" parameters in the String by name, or null to preserve the parameters in
     *               the query, like {@link #parse(String, Object...)} with no values
     * @see #parse(String, Object...)
     * @since 5.0
     */
    public static ObjectSelect<?> parse(String query, Map<String, ?> params) {
        return QLParser.parse(query, parser -> {
            if (params != null) {
                parser.setParameters(params);
            }
            return parser.query();
        });
    }

    /**
     * Parses a query String, typing the result rows. The type must match the "select" clause of the String:
     * <ul>
     *     <li>a persistent class, for a query of the root objects, i.e. with no "select" clause or with
     *     "select self";</li>
     *     <li>{@link DataRow}, for a query of the root objects that should fetch DataRows;</li>
     *     <li>the type of the column, for a query with a single column in its "select" clause;</li>
     *     <li>{@code Object[]}, for a query with multiple columns in its "select" clause.</li>
     * </ul>
     *
     * @param type result row type of the query
     * @see #parse(String, Object...)
     * @since 5.0
     */
    public static <T> ObjectSelect<T> parse(Class<T> type, String query, Object... params) {
        return typed(parse(query, params), type, query);
    }

    /**
     * Parses a query String, typing the result rows and binding its parameters by name.
     *
     * @param type result row type of the query
     * @see #parse(String, Map)
     * @see #parse(Class, String, Object...)
     * @since 5.0
     */
    public static <T> ObjectSelect<T> parse(Class<T> type, String query, Map<String, ?> params) {
        return typed(parse(query, params), type, query);
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectSelect<T> typed(ObjectSelect<?> parsed, Class<T> type, String query) {
        boolean columnQuery = parsed.columns != null;
        boolean multiColumn = columnQuery && !parsed.singleColumn;

        if (type == Object[].class) {
            if (!multiColumn) {
                throw new ExpressionException("Expected a query that selects multiple columns: %s", query);
            }
        } else if (type == DataRow.class) {
            if (columnQuery) {
                throw new ExpressionException(
                        "Expected a query that selects root objects, got a column query: %s", query);
            }
            parsed.fetchDataRows();
        } else if (multiColumn) {
            throw new ExpressionException(
                    "Expected a query that selects root objects or a single column, got multiple columns: %s", query);
        }

        return (ObjectSelect<T>) parsed;
    }

    protected ObjectSelect() {
    }

    /**
     * Copies the state of another select into this one, to produce a modified copy of a query.
     */
    protected void copyFrom(ObjectSelect<?> other) {
        this.entityType = other.entityType;
        this.entityName = other.entityName;
        this.dbEntityName = other.dbEntityName;
        this.where = other.where;
        this.having = other.having;
        this.havingExpressionIsActive = other.havingExpressionIsActive;
        this.orderings = other.orderings != null ? new ArrayList<>(other.orderings) : null;
        this.distinct = other.distinct;
        this.suppressingDistinct = other.suppressingDistinct;
        this.columns = other.columns != null ? new ArrayList<>(other.columns) : null;
        this.singleColumn = other.singleColumn;
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
     * Sets the type of the entity to fetch without changing the return type of the query.
     *
     * @return this object
     */
    public ObjectSelect<T> entityType(Class<?> entityType) {
        return resetEntity(entityType, null, null);
    }

    /**
     * Sets the {@link ObjEntity} name to fetch without changing the return type of the query. This form is most often
     * used for generic entities that don't map to a distinct class.
     *
     * @return this object
     */
    public ObjectSelect<T> entityName(String entityName) {
        return resetEntity(null, entityName, null);
    }

    /**
     * Sets the {@link DbEntity} name to fetch without changing the return type of the query. This form is most often
     * used for generic entities that don't map to a distinct class.
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
     * Appends a qualifier expression of this query. An equivalent to {@link #and(Expression...)} that can be used
     * as syntactic sugar.
     *
     * @return this object
     */
    public ObjectSelect<T> where(Expression expression) {
        return and(expression);
    }

    /**
     * Appends a qualifier expression of this query, using provided expression String and an array of position
     * parameters. This is an equivalent to calling "and".
     *
     * @return this object
     */
    public ObjectSelect<T> where(String expressionString, Object... parameters) {
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
    public ObjectSelect<T> byId(Object id) {
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
    public ObjectSelect<T> byIds(Object... ids) {
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
    public ObjectSelect<T> byIds(Collection<?> ids) {
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
        return joinExpression(expressions, ExpressionFactory::and);
    }

    protected ObjectSelect<T> joinExpression(
            Collection<Expression> expressions,
            Function<Collection<Expression>, Expression> joiner) {

        if (expressions == null || expressions.isEmpty()) {
            return this;
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

        if (havingExpressionIsActive) {
            having = joiner.apply(all);
        } else {
            where = joiner.apply(all);
        }

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
        return joinExpression(expressions, ExpressionFactory::or);
    }

    /**
     * Appends a having qualifier expression of this query. An equivalent to {@link #and(Expression...)} that can be
     * used as syntactic sugar. Once a "having" is set, {@code and(..)} and {@code or(..)} append to it rather than to
     * the "where" clause.
     *
     * @return this object
     * @since 4.2
     */
    public ObjectSelect<T> having(Expression expression) {
        havingExpressionIsActive = true;
        return and(expression);
    }

    /**
     * Appends a having qualifier expression of this query, using provided expression String and an array of
     * position parameters. This is an equivalent to calling "and".
     *
     * @return this object
     * @since 4.2
     */
    public ObjectSelect<T> having(String expressionString, Object... parameters) {
        havingExpressionIsActive = true;
        return and(ExpressionFactory.exp(expressionString, parameters));
    }

    /**
     * Add an ascending ordering on the given property. If there is already an ordering on this query then add this
     * ordering with a lower priority.
     *
     * @param property the property to sort on
     * @return this object
     */
    public ObjectSelect<T> orderBy(String property) {
        return orderBy(new Ordering(property));
    }

    /**
     * Add an ordering on the given property. If there is already an ordering on this query then add this ordering
     * with a lower priority.
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
        if (prefetches == null) {
            prefetches = new PrefetchTreeNode();
        }
        prefetches.merge(prefetch);
        return this;
    }

    /**
     * Merges a prefetch path with specified semantics into the query prefetch tree.
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
        PrefetchTreeNode node = prefetches.addPath(path);
        node.setSemantics(semantics);
        node.setPhantom(false);
        return this;
    }

    /**
     * Resets query fetch limit - a parameter that defines max number of objects that should be ever be fetched from
     * the database.
     */
    public ObjectSelect<T> limit(int fetchLimit) {
        this.fetchLimit = fetchLimit;
        return this;
    }

    /**
     * Resets query fetch offset - a parameter that defines how many objects should be skipped when reading data
     * from the database.
     */
    public ObjectSelect<T> offset(int fetchOffset) {
        this.fetchOffset = fetchOffset;
        return this;
    }

    /**
     * Resets query page size. A non-negative page size enables query result pagination that saves memory and
     * processing time for large lists if only parts of the result are ever going to be accessed.
     */
    public ObjectSelect<T> pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    /**
     * Sets fetch size of the PreparedStatement generated for this query. Any non-zero value is passed to the driver
     * as is; 0 (the default) leaves the fetch size to the adapter and the driver. A negative value is only meaningful
     * for drivers that give it a special meaning, such as {@link Integer#MIN_VALUE} for streaming on MySQL.
     *
     * @see Statement#setFetchSize(int)
     */
    public ObjectSelect<T> statementFetchSize(int size) {
        this.statementFetchSize = size;
        return this;
    }

    /**
     * Sets query timeout of PreparedStatement generated for this query.
     *
     * @see Statement#setQueryTimeout(int)
     */
    public ObjectSelect<T> queryTimeout(int timeout) {
        this.queryTimeout = timeout;
        return this;
    }

    public ObjectSelect<T> cacheStrategy(QueryCacheStrategy strategy) {
        this.cacheStrategy = strategy != null ? strategy : QueryCacheStrategy.getDefaultStrategy();
        this.cacheGroup = null;
        return this;
    }

    public ObjectSelect<T> cacheStrategy(QueryCacheStrategy strategy, String cacheGroup) {
        return cacheStrategy(strategy).cacheGroup(cacheGroup);
    }

    public ObjectSelect<T> cacheGroup(String cacheGroup) {
        this.cacheGroup = cacheGroup;
        return this;
    }

    /**
     * Instructs Cayenne to look for query results in the "local" cache when running the query. This is a short-hand
     * notation for:
     * <pre>
     * query.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, cacheGroup);
     * </pre>
     */
    public ObjectSelect<T> localCache(String cacheGroup) {
        return cacheStrategy(QueryCacheStrategy.LOCAL_CACHE, cacheGroup);
    }

    /**
     * Instructs Cayenne to look for query results in the "local" cache when running the query. This is a short-hand
     * notation for:
     * <pre>
     * query.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
     * </pre>
     */
    public ObjectSelect<T> localCache() {
        return cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
    }

    /**
     * Instructs Cayenne to look for query results in the "shared" cache when running the query. This is a short-hand
     * notation for:
     * <pre>
     * query.cacheStrategy(QueryCacheStrategy.SHARED_CACHE, cacheGroup);
     * </pre>
     */
    public ObjectSelect<T> sharedCache(String cacheGroup) {
        return cacheStrategy(QueryCacheStrategy.SHARED_CACHE, cacheGroup);
    }

    /**
     * Instructs Cayenne to look for query results in the "shared" cache when running the query. This is a short-hand
     * notation for:
     * <pre>
     * query.cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
     * </pre>
     */
    public ObjectSelect<T> sharedCache() {
        return cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
    }

    /**
     * Forces query to fetch DataRows. This automatically changes whatever result type was set previously to "DataRow".
     *
     * @return this object
     */
    public ObjectSelect<DataRow> fetchDataRows() {
        this.fetchingDataRows = true;
        return castSelf();
    }

    /**
     * Switches the query to selecting the given properties instead of the root objects, or adds them to the
     * columns selected already. The properties can be anything resolvable against the root entity: its own
     * properties, properties of related objects, function calls, aggregates, etc. The result row becomes an
     * {@code Object[]} with a value per column.
     * <pre>
     * {@code
     * List<Object[]> columns = ObjectSelect.query(Artist.class)
     *                                    .columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH)
     *                                    .select(context);
     * }
     * </pre>
     *
     * @param properties array of properties to select, must contain at least one element
     * @return this object
     * @see #column(Property)
     */
    public ObjectSelect<Object[]> columns(Property<?>... properties) {
        if (properties == null || properties.length == 0) {
            throw new IllegalArgumentException("properties must contain at least one element");
        }
        return columns(Arrays.asList(properties));
    }

    /**
     * Switches the query to selecting the given properties instead of the root objects, or adds them to the
     * columns selected already.
     *
     * @param properties collection of properties, <b>must</b> contain at least one element
     * @return this object
     * @see #columns(Property[])
     */
    public ObjectSelect<Object[]> columns(Collection<Property<?>> properties) {
        if (properties == null) {
            throw new NullPointerException("properties is null");
        }
        if (properties.isEmpty()) {
            throw new IllegalArgumentException("properties must contain at least one element");
        }

        if (this.columns == null) {
            this.columns = new ArrayList<>(properties.size());
        }

        this.columns.addAll(properties);
        this.singleColumn = false;
        return castSelf();
    }

    /**
     * Switches the query to selecting a single property instead of the root objects, replacing any columns selected
     * already. The property can be anything resolvable against the root entity: its own property, a property of a
     * related object, a function call, an aggregate, etc. The result row becomes the value of the column.
     * <pre>
     * {@code
     * List<String> names = ObjectSelect.query(Artist.class)
     *                                  .column(Artist.ARTIST_NAME)
     *                                  .select(context);
     * }
     * </pre>
     *
     * @param property single property to select
     * @return this object
     * @see #columns(Property[])
     */
    public <E> ObjectSelect<E> column(Property<E> property) {
        checkSingleColumnType(property);

        if (this.columns == null) {
            this.columns = new ArrayList<>(1);
        } else {
            // a single column defines the result type, so it can't be combined with the columns selected before
            this.columns.clear();
        }
        this.columns.add(property);
        this.singleColumn = true;
        return castSelf();
    }

    /**
     * A to-many relationship column always results in a single related object per row. With multiple columns that
     * is not a problem, as the row is an Object[]. But a single column defines the type of the query result, and
     * here a to-many property declared as a List or a Map would promise a result that the query can't deliver.
     */
    private static void checkSingleColumnType(Property<?> property) {
        Class<?> type = property.getType();
        Expression exp = property.getExpression();

        if (type != null
                && exp != null
                && (exp instanceof ObjPathExp || exp instanceof DbPathExp)
                && (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))) {
            throw new CayenneRuntimeException("""
                    Can't select a to-many relationship '%s' as a single column, as its type wouldn't match \
                    the result type. Either select related objects with flat(), or use an aggregate function \
                    like count().""", exp);
        }
    }

    /**
     * Selects COUNT(*), a shortcut for {@code column(PropertyFactory.COUNT)}.
     *
     * @see #column(Property)
     */
    public ObjectSelect<Long> count() {
        return column(PropertyFactory.COUNT);
    }

    /**
     * Selects COUNT(property). Can return a different result than COUNT(*), as it will count only non null values.
     *
     * @see #count()
     * @see #column(Property)
     */
    public ObjectSelect<Long> count(BaseProperty<?> property) {
        return column(property.count());
    }

    /**
     * Selects the minimum value of a property.
     *
     * @see #column(Property)
     */
    public <E> ObjectSelect<E> min(ComparableProperty<E> property) {
        return column(property.min());
    }

    /**
     * Selects the minimum value of a property.
     *
     * @see #column(Property)
     */
    public <E extends Number> ObjectSelect<E> min(NumericProperty<E> property) {
        return column(property.min());
    }

    /**
     * Selects the maximum value of a property.
     *
     * @see #column(Property)
     */
    public <E> ObjectSelect<E> max(ComparableProperty<E> property) {
        return column(property.max());
    }

    /**
     * Selects the maximum value of a property.
     *
     * @see #column(Property)
     */
    public <E extends Number> ObjectSelect<E> max(NumericProperty<E> property) {
        return column(property.max());
    }

    /**
     * Selects the average value of a property.
     *
     * @see #column(Property)
     */
    public <E extends Number> ObjectSelect<E> avg(NumericProperty<E> property) {
        return column(property.avg());
    }

    /**
     * Selects the sum of the values of a property.
     *
     * @see #column(Property)
     */
    public <E extends Number> ObjectSelect<E> sum(NumericProperty<E> property) {
        return column(property.sum());
    }

    /**
     * Selects the result of an aggregate function over a property.
     *
     * @see #column(Property)
     * @since 5.0
     */
    public <E> ObjectSelect<E> aggregate(BaseProperty<E> property, String function, Class<E> type) {
        return column(property.aggregate(function, type));
    }

    /**
     * Explicitly requests distinct in the query.
     *
     * @return this object
     * @since 4.2
     */
    public ObjectSelect<T> distinct() {
        this.suppressingDistinct = false;
        this.distinct = true;
        return this;
    }

    /**
     * Explicitly suppresses distinct in the query, including the one that Cayenne would add on its own to a column
     * query with joins.
     *
     * @return this object
     */
    public ObjectSelect<T> suppressDistinct() {
        this.suppressingDistinct = true;
        this.distinct = false;
        return this;
    }

    /**
     * Selects the count of the root rows matching the query, leaving the query itself unchanged:
     * <pre>
     * {@code
     *     long count = ObjectSelect.query(Artist.class)
     *                      .where(Artist.ARTIST_NAME.like("a%"))
     *                      .selectCount(context);
     * }
     * </pre>
     *
     * @param context to perform query
     * @return count of rows
     */
    public long selectCount(ObjectContext context) {
        ObjectSelect<Long> count = new ObjectSelect<>();
        count.copyFrom(this);
        // orderings make no sense for a count, and some DBs reject them, see CAY-2836
        count.orderings = null;
        return count.count().selectOne(context);
    }

    @Override
    public T selectFirst(ObjectContext context) {
        return context.selectFirst(limit(1));
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

    /**
     * Returns the columns selected by this query, or null if it selects the root objects.
     *
     * @since 4.2
     */
    public Collection<Property<?>> getColumns() {
        return columns;
    }

    /**
     * Returns whether this query selects columns rather than the root objects.
     *
     * @since 6.0
     */
    public boolean isColumnQuery() {
        return columns != null;
    }

    /**
     * Returns whether this query selects a single column, so that each result row is the value of that column
     * rather than an {@code Object[]}. Always false for a query of the root objects.
     *
     * @since 6.0
     */
    public boolean isSingleColumn() {
        return columns != null && singleColumn;
    }

    protected Expression getActiveExpression() {
        return havingExpressionIsActive ? having : where;
    }

    @Override
    public ResolvedSelect resolve(EntityResolver resolver) {
        ResolvedSelect.Builder builder = ResolvedSelect.builder()
                .source(this)
                .where(where)
                .having(having)
                .orderings(orderings)
                .distinct(distinct)
                .suppressingDistinct(suppressingDistinct)
                .columns(columns, singleColumn)
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

        return builder.build(resolver);
    }

    /**
     * Returns this query in the query language of {@link #parse(String, Object...)}, so that parsing the result
     * produces an equivalent query. Just like the expressions that it consists of, the printed query may not be
     * parseable if it contains values that have no String representation in the grammar (dates, persistent objects,
     * etc.) Also there is no access to the mapping here, so a root defined as a Java class is printed as the simple
     * name of that class, which is the name of the entity by convention, but doesn't have to be.
     *
     * @see #appendQL(Appendable)
     * @since 6.0
     */
    public String toQL() {
        StringBuilder out = new StringBuilder();
        try {
            appendQL(out);
        } catch (IOException e) {
            // StringBuilder doesn't throw
            throw new IllegalStateException(e);
        }
        return out.toString();
    }

    /**
     * Appends this query in the query language to the given Appendable, see {@link #toQL()}.
     *
     * @since 6.0
     */
    public void appendQL(Appendable out) throws IOException {
        ObjectSelectPrinter.append(this, out);
    }

    /**
     * Returns {@link #toQL()}.
     */
    @Override
    public String toString() {
        return toQL();
    }

    /**
     * Utility method to (re)cast this query to another row type. Doesn't perform any checks, so use with caution.
     *
     * @param <E> type to cast to
     * @return <code>this</code> cast to the type E
     * @since 5.0
     */
    @SuppressWarnings("unchecked")
    protected <E> E castSelf() {
        return (E) this;
    }
}
