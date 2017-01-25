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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.Property;
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
 *      long count = ObjectSelect.columnQuery(Artist.class, Property.COUNT).selectOne();
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

    private Collection<Property<?>> columns;
    private boolean havingExpressionIsActive = false;
    // package private for tests
    boolean singleColumn = true;
    private Expression having;

    protected ColumnSelect() {
        super();
    }

    /**
     * Copy constructor to convert ObjectSelect to ColumnSelect
     */
    protected ColumnSelect(ObjectSelect<T> select) {
        super();
        this.entityType = select.entityType;
        this.entityName = select.entityName;
        this.dbEntityName = select.dbEntityName;
        this.where = select.where;
        this.orderings = select.orderings;
        this.prefetches = select.prefetches;
        this.limit = select.limit;
        this.offset = select.offset;
        this.pageSize = select.pageSize;
        this.statementFetchSize = select.statementFetchSize;
        this.cacheStrategy = select.cacheStrategy;
        this.cacheGroups = select.cacheGroups;
    }

    @Override
    protected Query createReplacementQuery(EntityResolver resolver) {
        SelectQuery<?> replacement = (SelectQuery)super.createReplacementQuery(resolver);
        replacement.setColumns(columns);
        replacement.setHavingQualifier(having);
        replacement.setCanReturnScalarValue(singleColumn);
        return replacement;
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
     * @param firstProperty first property
     * @param otherProperties array of properties to select
     * @see ColumnSelect#column(Property)
     * @see ColumnSelect#columns(Collection)
     */
    @SuppressWarnings("unchecked")
    public ColumnSelect<Object[]> columns(Property<?> firstProperty, Property<?>... otherProperties) {
        if (columns == null) {
            columns = new ArrayList<>(otherProperties.length + 1);
        }
        columns.add(firstProperty);
        Collections.addAll(columns, otherProperties);
        singleColumn = false;
        return (ColumnSelect<Object[]>)this;
    }

    /**
     * <p>Add properties to select.</p>
     * <p>Can be any properties that can be resolved against root entity type
     * (root entity properties, function call expressions, properties of relationships, etc).</p>
     * <p>
     * @param properties collection of properties, <b>must</b> contain at least one element
     * @see ColumnSelect#columns(Property, Property[])
     */
    @SuppressWarnings("unchecked")
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
        return (ColumnSelect<Object[]>)this;
    }

    @SuppressWarnings("unchecked")
    protected  <E> ColumnSelect<E> column(Property<E> property) {
        if (this.columns == null) {
            this.columns = new ArrayList<>(1);
        } else {
            this.columns.clear(); // if we don't clear then return type will be incorrect
        }
        this.columns.add(property);
        return (ColumnSelect<E>) this;
    }

    /**
     * <p>Shortcut for {@link #columns(Property, Property[])} columns}(Property.COUNT)</p>
     */
    public ColumnSelect<Object[]> count() {
        return columns(Property.COUNT);
    }

    /**
     * <p>Select COUNT(property)</p>
     * <p>Can return different result than COUNT(*) as it will count only non null values</p>
     * @see ColumnSelect#count()
     */
    public ColumnSelect<Object[]> count(Property<?> property) {
        return columns(property.count());
    }

    /**
     * <p>Select minimum value of property</p>
     * @see ColumnSelect#columns(Property, Property[])
     */
    public ColumnSelect<Object[]> min(Property<?> property) {
        return columns(property.min());
    }

    /**
     * <p>Select maximum value of property</p>
     * @see ColumnSelect#columns(Property, Property[])
     */
    public ColumnSelect<Object[]> max(Property<?> property) {
        return columns(property.max());
    }

    /**
     * <p>Select average value of property</p>
     * @see ColumnSelect#columns(Property, Property[])
     */
    public ColumnSelect<Object[]> avg(Property<?> property) {
        return columns(property.avg());
    }

    /**
     * <p>Select sum of values</p>
     * @see ColumnSelect#columns(Property, Property[])
     */
    public <E extends Number> ColumnSelect<Object[]> sum(Property<E> property) {
        return columns(property.sum());
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
     * AND's provided expressions to the existing WHERE or HAVING clause expression.
     *
     * @return this object
     */
    @SuppressWarnings("unchecked")
    public ColumnSelect<T> and(Collection<Expression> expressions) {

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

        setActiveExpression(ExpressionFactory.and(all));
        return this;
    }

    /**
     * OR's provided expressions to the existing WHERE or HAVING clause expression.
     *
     * @return this object
     */
    @SuppressWarnings("unchecked")
    public ColumnSelect<T> or(Collection<Expression> expressions) {
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

        setActiveExpression(ExpressionFactory.or(all));
        return this;
    }

    private void setActiveExpression(Expression exp) {
        if(havingExpressionIsActive) {
            having = exp;
        } else {
            where = exp;
        }
    }

    private Expression getActiveExpression() {
        if(havingExpressionIsActive) {
            return having;
        } else {
            return where;
        }
    }

    public Collection<Property<?>> getColumns() {
        return columns;
    }

    /**
     * Returns a HAVING clause Expression of this query.
     */
    public Expression getHaving() {
        return having;
    }
}
