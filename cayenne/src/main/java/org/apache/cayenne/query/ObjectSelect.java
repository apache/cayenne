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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.ql.QLParser;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.property.ComparableProperty;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

import java.util.List;
import java.util.Map;

/**
 * A selecting query providing chainable API.
 * <p>
 * For example, the following is a convenient way to return a record:
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
public class ObjectSelect<T> extends FluentSelect<T, ObjectSelect<T>> {

    /**
     * Creates a ObjectSelect that selects objects of a given persistent class.
     */
    public static <T> ObjectSelect<T> query(Class<T> entityType) {
        return new ObjectSelect<T>().entityType(entityType);
    }

    /**
     * Creates a ObjectSelect that fetches data for an {@link ObjEntity}
     * determined from a provided class.
     */
    public static ObjectSelect<DataRow> dataRowQuery(Class<?> entityType) {
        return query(entityType).fetchDataRows();
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
     * Creates a ColumnSelect that will fetch single property that can be resolved
     * against a given {@link ObjEntity} class.
     *
     * @param entityType base persistent class that will be used as a root for this query
     * @param column     single column to select
     */
    public static <E> ColumnSelect<E> columnQuery(Class<?> entityType, Property<E> column) {
        return new ColumnSelect<>().entityType(entityType).column(column);
    }

    /**
     * Creates a ColumnSelect that will fetch multiple columns of a given {@link ObjEntity}
     *
     * @param entityType base persistent class that will be used as a root for this query
     * @param columns    columns to select
     */
    public static ColumnSelect<Object[]> columnQuery(Class<?> entityType, Property<?>... columns) {
        return new ColumnSelect<Object[]>().entityType(entityType).columns(columns);
    }

    /**
     * Parses a query String, creating either an ObjectSelect or a ColumnSelect, depending on the "select" clause
     * of the String. The general form of the query is
     * <pre>
     * [select [distinct] columns] from Entity [where exp] [having exp] [order by orderings]
     * [limit n] [offset m] [prefetch paths]</pre>
     * where all the expressions follow the syntax of {@link ExpressionFactory#exp(String, Object...)}. A query with
     * no "select" clause, or with "select self" results in an ObjectSelect. E.g.:
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
     * @since 5.0
     */
    public static FluentSelect<?, ?> parse(String query, Object... params) {
        return QLParser.parse(query, parser -> {
            if (params != null && params.length > 0) {
                parser.setParameters(params);
            }
            FluentSelect<?, ?> select = parser.query();
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
    public static FluentSelect<?, ?> parse(String query, Map<String, ?> params) {
        return QLParser.parse(query, parser -> {
            if (params != null) {
                parser.setParameters(params);
            }
            return parser.query();
        });
    }

    /**
     * Parses a String of a query that fetches root objects, i.e. a query with no "select" clause, or with
     * "select self". If the type is DataRow, the query will fetch DataRows.
     *
     * @param type result type of the query
     * @see #parse(String, Object...)
     * @since 5.0
     */
    public static <T> ObjectSelect<T> parse(Class<T> type, String query, Object... params) {
        return asObjectSelect(parse(query, params), type, query);
    }

    /**
     * Parses a String of a query that fetches root objects, binding its parameters by name.
     *
     * @param type result type of the query
     * @see #parse(String, Map)
     * @see #parse(Class, String, Object...)
     * @since 5.0
     */
    public static <T> ObjectSelect<T> parse(Class<T> type, String query, Map<String, ?> params) {
        return asObjectSelect(parse(query, params), type, query);
    }

    /**
     * Parses a String of a query that fetches a single column.
     *
     * @param type result type of the query
     * @see #parse(String, Object...)
     * @since 5.0
     */
    public static <T> ColumnSelect<T> parseColumn(Class<T> type, String query, Object... params) {
        return asColumnSelect(parse(query, params), query);
    }

    /**
     * Parses a String of a query that fetches a single column, binding its parameters by name.
     *
     * @param type result type of the query
     * @see #parse(String, Map)
     * @since 5.0
     */
    public static <T> ColumnSelect<T> parseColumn(Class<T> type, String query, Map<String, ?> params) {
        return asColumnSelect(parse(query, params), query);
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectSelect<T> asObjectSelect(FluentSelect<?, ?> parsed, Class<T> type, String query) {
        if (!(parsed instanceof ObjectSelect<?> select)) {
            throw new ExpressionException("Expected a query that selects root objects, got a column query: %s", query);
        }
        return type == DataRow.class ? (ObjectSelect<T>) select.fetchDataRows() : (ObjectSelect<T>) select;
    }

    @SuppressWarnings("unchecked")
    private static <T> ColumnSelect<T> asColumnSelect(FluentSelect<?, ?> parsed, String query) {
        if (!(parsed instanceof ColumnSelect<?> select) || !select.isSingleColumn()) {
            throw new ExpressionException("Expected a query that selects a single column: %s", query);
        }
        return (ColumnSelect<T>) select;
    }

    /**
     * Parses a String of a query that fetches multiple columns.
     *
     * @see #parse(String, Object...)
     * @since 5.0
     */
    @SuppressWarnings("unchecked")
    public static ColumnSelect<Object[]> parseColumns(String query, Object... params) {
        if (!(parse(query, params) instanceof ColumnSelect<?> select) || select.isSingleColumn()) {
            throw new ExpressionException("Expected a query that selects multiple columns: %s", query);
        }
        return (ColumnSelect<Object[]>) select;
    }

    protected ObjectSelect() {
    }

    @Override
    protected ObjectSelectMetadata createMetadata() {
        return new ObjectSelectMetadata();
    }

    /**
     * Appends a having qualifier expression of this query. An equivalent to
     * {@link #and(Expression...)} that can be used a syntactic sugar.
     *
     * @return this object
     * @since 4.2
     */
    public ObjectSelect<T> having(Expression expression) {
        havingExpressionIsActive = true;
        return and(expression);
    }

    /**
     * Appends a having qualifier expression of this query, using provided expression
     * String and an array of position parameters. This is an equivalent to
     * calling "and".
     *
     * @return this object
     * @since 4.2
     */
    public ObjectSelect<T> having(String expressionString, Object... parameters) {
        havingExpressionIsActive = true;
        return and(ExpressionFactory.exp(expressionString, parameters));
    }

    /**
     * Forces query to fetch DataRows. This automatically changes whatever result type was set previously to "DataRow".
     *
     * @return this object
     */
    public ObjectSelect<DataRow> fetchDataRows() {
        metaData.setFetchingDataRows(true);
        return castSelf();
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
    public ColumnSelect<Object[]> columns(Property<?>... properties) {
        return new ColumnSelect<>(this).columns(properties);
    }

    /**
     * <p>Select one specific property.</p>
     * <p>Can be any property that can be resolved against root entity type
     * (root entity's property, function call expression, property of relationships, etc)</p>
     * <p>If you need several columns use {@link ObjectSelect#columns(Property[])} method.</p>
     * <p>
     * <pre>
     * {@code
     * List<String> names = ObjectSelect.query(Artist.class)
     *                                  .column(Artist.ARTIST_NAME)
     *                                  .select(context);
     * }
     * </pre>
     * </p>
     *
     * @param property single property to select
     * @see ObjectSelect#columns(Property[])
     */
    public <E> ColumnSelect<E> column(Property<E> property) {
        return new ColumnSelect<>(this).column(property);
    }

    /**
     * Select COUNT(*)
     *
     * @see ObjectSelect#column(Property)
     */
    public ColumnSelect<Long> count() {
        return column(PropertyFactory.COUNT);
    }

    /**
     * <p>Select COUNT(property)</p>
     * <p>Can return different result than COUNT(*) as it will count only non null values</p>
     *
     * @see ObjectSelect#count()
     * @see ObjectSelect#column(Property)
     */
    public ColumnSelect<Long> count(BaseProperty<?> property) {
        return column(property.count());
    }

    /**
     * <p>Select minimum value of property</p>
     *
     * @see ObjectSelect#column(Property)
     */
    public <E> ColumnSelect<E> min(ComparableProperty<E> property) {
        return column(property.min());
    }

    /**
     * <p>Select minimum value of property</p>
     *
     * @see ObjectSelect#column(Property)
     */
    public <E extends Number> ColumnSelect<E> min(NumericProperty<E> property) {
        return column(property.min());
    }

    /**
     * <p>Select maximum value of property</p>
     *
     * @see ObjectSelect#column(Property)
     */
    public <E> ColumnSelect<E> max(ComparableProperty<E> property) {
        return column(property.max());
    }

    /**
     * <p>Select maximum value of property</p>
     *
     * @see ObjectSelect#column(Property)
     */
    public <E extends Number> ColumnSelect<E> max(NumericProperty<E> property) {
        return column(property.max());
    }

    /**
     * <p>Select average value of property</p>
     *
     * @see ObjectSelect#column(Property)
     */
    public <E extends Number> ColumnSelect<E> avg(NumericProperty<E> property) {
        return column(property.avg());
    }

    /**
     * <p>Select sum of values</p>
     *
     * @see ObjectSelect#column(Property)
     */
    public <E extends Number> ColumnSelect<E> sum(NumericProperty<E> property) {
        return column(property.sum());
    }

    /**
     * <p>Select result of some function, that aggregates values.</p>
     *
     * @see ObjectSelect#column(Property)
     * @since 5.0
     */
    public <E> ColumnSelect<E> aggregate(BaseProperty<E> property, String function, Class<E> type) {
        return column(property.aggregate(function, type));
    }

    /**
     * @return this
     * @since 4.2
     */
    public ObjectSelect<T> distinct() {
        this.distinct = true;
        return this;
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
     *
     * @param context to perform query
     * @return count of rows
     */
    public long selectCount(ObjectContext context) {
        ColumnSelect<Long> count = count();
        // reset orderings, see CAY-2836
        count.orderings = List.of();
        return count.selectOne(context);
    }

    @Override
    public T selectFirst(ObjectContext context) {
        return context.selectFirst(limit(1));
    }

    @Override
    public QueryMetadata getMetaData(EntityResolver resolver) {
        Object root = resolveRoot(resolver);
        metaData.resolve(root, resolver, this);
        return metaData;
    }

    @Override
    protected ObjectSelectMetadata getBaseMetaData() {
        return metaData;
    }
}
