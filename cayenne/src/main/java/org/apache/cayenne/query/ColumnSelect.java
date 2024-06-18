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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.property.ComparableProperty;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.map.EntityResolver;

/**
 * <p>A helper builder for queries selecting individual properties based on the root object.</p>
 * <p>
 *     It can be used to select properties of the object itself, properties of related entities
 *     or some function calls (including aggregate functions).
 * </p>
 * <p>
 * Usage examples: <pre>
 * {@code
 *      // select list of names:
 *      List<String> names = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME).select(context);
 *
 *      // select count:
 *      long count = ObjectSelect.columnQuery(Artist.class, PropertyFactory.COUNT).selectOne();
 *
 *      // select only required properties of an entity:
 *      List<Object[]> data = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH)
 *                                  .where(Artist.ARTIST_NAME.like("Picasso%))
 *                                  .select(context);
 * }
 * </pre>
 * </p>
 * <p><b>Note: this class can't be instantiated directly. Use {@link ObjectSelect}.</b></p>
 * @see ObjectSelect#columnQuery(Class, Property)
 *
 * @since 4.0
 */
public class ColumnSelect<T> extends FluentSelect<T, ColumnSelect<T>> {

    protected Collection<Property<?>> columns;
    protected boolean singleColumn = true;

    protected ColumnSelect() {
    }

    /**
     * Copy constructor to convert ObjectSelect to ColumnSelect
     */
    protected ColumnSelect(ObjectSelect<T> select) {
        this.entityType = select.entityType;
        this.entityName = select.entityName;
        this.dbEntityName = select.dbEntityName;
        this.where = select.where;
        this.having = select.having;
        this.orderings = select.orderings;
        this.metaData.copyFromInfo(select.metaData);
    }

    @Override
    protected ColumnSelectMetadata createMetadata() {
        return new ColumnSelectMetadata();
    }

    /**
     * <p>Add properties to select.</p>
     * <p>Can be any properties that can be resolved against root entity type
     * (root entity properties, function call expressions, properties of relationships, etc).</p>
     * <p>
     * <pre>
     * {@code
     * List<Object[]> columns = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME)
     *                                    .columns(Artist.ARTIST_SALARY, Artist.DATE_OF_BIRTH)
     *                                    .select(context);
     * }
     * </pre>
     *
     * @param properties array of properties to select
     * @see ColumnSelect#column(Property)
     * @see ColumnSelect#columns(Collection)
     */
    public ColumnSelect<Object[]> columns(Property<?>... properties) {
        if (properties.length == 0) {
            throw new IllegalArgumentException("properties must contain at least one element");
        }
        if (columns == null) {
            columns = new ArrayList<>(properties.length);
        }
        Collections.addAll(columns, properties);
        singleColumn = false;
        return castSelf();
    }

    /**
     * <p>Add properties to select.</p>
     * <p>Can be any properties that can be resolved against root entity type
     * (root entity properties, function call expressions, properties of relationships, etc).</p>
     * <p>
     * @param properties collection of properties, <b>must</b> contain at least one element
     * @see ColumnSelect#columns(Property[])
     */
    public ColumnSelect<Object[]> columns(Collection<Property<?>> properties) {
        if (properties == null){
            throw new NullPointerException("properties is null");
        }
        if (properties.isEmpty()) {
            throw new IllegalArgumentException("properties must contain at least one element");
        }

        if (this.columns == null) {
            this.columns = new ArrayList<>(properties.size());
        }

        columns.addAll(properties);
        singleColumn = false;
        return castSelf();
    }

    protected <E> ColumnSelect<E> column(Property<E> property) {
        if (this.columns == null) {
            this.columns = new ArrayList<>(1);
        } else {
            this.columns.clear(); // if we don't clear then return type will be incorrect
        }
        this.columns.add(property);
        return castSelf();
    }

    /**
     * <p>Shortcut for {@link #columns(Property[])} columns}(Property.COUNT)</p>
     */
    public ColumnSelect<Object[]> count() {
        return columns(PropertyFactory.COUNT);
    }

    /**
     * <p>Select COUNT(property)</p>
     * <p>Can return different result than COUNT(*) as it will count only non null values</p>
     * @see ColumnSelect#count()
     */
    public ColumnSelect<Object[]> count(BaseProperty<?> property) {
        return columns(property.count());
    }

    /**
     * <p>Select minimum value of property</p>
     * @see ColumnSelect#columns(Property[])
     */
    public ColumnSelect<Object[]> min(ComparableProperty<?> property) {
        return columns(property.min());
    }

    /**
     * <p>Select maximum value of property</p>
     * @see ColumnSelect#columns(Property[])
     */
    public ColumnSelect<Object[]> max(ComparableProperty<?> property) {
        return columns(property.max());
    }

    /**
     * <p>Select average value of property</p>
     * @see ColumnSelect#columns(Property[])
     */
    public ColumnSelect<Object[]> avg(NumericProperty<?> property) {
        return columns(property.avg());
    }

    /**
     * <p>Select sum of values</p>
     * @see ColumnSelect#columns(Property[])
     */
    public <E extends Number> ColumnSelect<Object[]> sum(NumericProperty<E> property) {
        return columns(property.sum());
    }

    /**
     * <p>Select result of some function, that aggregates values.</p>
     * @see ColumnSelect#columns(Property[])
     *
     * @since 5.0
     */
    public <E> ColumnSelect<Object[]> aggregate(BaseProperty<E> property, String function, Class<E> type) {
        return columns(property.aggregate(function, type));
    }

    /**
     * Appends a having qualifier expression of this query. An equivalent to
     * {@link #and(Expression...)} that can be used a syntactic sugar.
     *
     * @return this object
     */
    public ColumnSelect<T> having(Expression expression) {
        havingExpressionIsActive = true;
        return and(expression);
    }

    /**
     * Appends a having qualifier expression of this query, using provided expression
     * String and an array of position parameters. This is an equivalent to
     * calling "and".
     *
     * @return this object
     */
    public ColumnSelect<T> having(String expressionString, Object... parameters) {
        havingExpressionIsActive = true;
        return and(ExpressionFactory.exp(expressionString, parameters));
    }

    /**
     * Explicitly request distinct in query.
     */
    public ColumnSelect<T> distinct() {
        getBaseMetaData().setSuppressingDistinct(false);
        this.distinct = true;
        return this;
    }

    /**
     * Explicitly suppress distinct in query.
     */
    public ColumnSelect<T> suppressDistinct() {
        getBaseMetaData().setSuppressingDistinct(true);
        this.distinct = false;
        return this;
    }

    @Override
    public Collection<Property<?>> getColumns() {
        return columns;
    }

    @Override
    public T selectFirst(ObjectContext context) {
        return context.selectFirst(limit(1));
    }

    boolean isSingleColumn() {
        return singleColumn;
    }

    @Override
    public QueryMetadata getMetaData(EntityResolver resolver) {
        Object root = resolveRoot(resolver);
        getBaseMetaData().resolve(root, resolver, this);
        return metaData;
    }

    @Override
    protected ColumnSelectMetadata getBaseMetaData() {
        return (ColumnSelectMetadata) metaData;
    }

    /**
     * Maps result of this query by processing with a given function.
     * <br/>
     * Could be used to map plain Object[] to some domain-specific object.
     * <br/>
     * <b>Note:</b> this method could be called multiple time, result will be mapped by all functions in the call order.
     * @param mapper function that maps result to the required type.
     * @return this query with changed result type
     * @param <E> new result type
     *
     * @since 4.2
     */
    public <E> ColumnSelect<E> map(Function<T, E> mapper) {
        getBaseMetaData().setResultMapper(mapper);
        return castSelf();
    }
}
