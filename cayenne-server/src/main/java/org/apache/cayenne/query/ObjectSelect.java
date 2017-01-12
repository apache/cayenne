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
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

import java.util.List;

/**
 * A selecting query providing chainable API. This is an alternative to
 * {@link SelectQuery} when you want to use a fluent API. For example, the following
 * is a convenient way to return a record:
 * <pre>
 * {@code
 * Artist a = ObjectSelect
 * .query(Artist.class)
 * .where(Artist.NAME.eq("Picasso"))
 * .selectOne(context);
 * }
 * </pre>
 *
 * @since 4.0
 */
public class ObjectSelect<T> extends FluentSelect<T, ObjectSelect<T>> {

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
     * (root entity properties, function call expressions, properties of relationships, etc).</p>
     * <p>
     * <pre>
     * List&lt;Object[]&gt; columns = ColumnSelect.query(Artist.class)
     *                                    .columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH)
     *                                    .select(context);
     * </pre>
     *
     * @param properties array of properties to select
     * @see ColumnSelect#column(Property)
     */
    @SuppressWarnings("unchecked")
    public ColumnSelect<Object[]> columns(Property<?>... properties) {
        return new ColumnSelect<>(this).columns(properties);
    }

    /**
     * <p>Select one specific property.</p>
     * <p>Can be any property that can be resolved against root entity type
     * (root entity property, function call expression, property of relationships, etc)</p>
     * <p>If you need several columns use {@link ColumnSelect#columns(Property[])} method as subsequent
     * call to this method will override previous columns set via this or
     * {@link ColumnSelect#columns(Property[])} method.</p>
     * <p>
     * <pre>
     * List&lt;String&gt; names = ObjectSelect.query(Artist.class).column(Artist.ARTIST_NAME).select(context);
     * </pre>
     *
     * @param property single property to select
     * @see ColumnSelect#columns(Property[])
     */
    @SuppressWarnings("unchecked")
    protected <E> ColumnSelect<E> column(Property<E> property) {
        return new ColumnSelect<>(this).column(property);
    }

    public boolean isFetchingDataRows() {
        return fetchingDataRows;
    }
}
