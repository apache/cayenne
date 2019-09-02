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

package org.apache.cayenne.exp.property;

import java.time.LocalDateTime;

import org.apache.cayenne.EmbeddableObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.FunctionExpressionFactory;

/**
 *
 * Factory class that produces all property types.
 *
 * @see org.apache.cayenne.exp.property
 * @since 4.2
 */
public class PropertyFactory {

    /**
     * Property that can be used to select {@code COUNT(*)}
     * <p>
     * Usage:<pre>{@code
     * ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME, PropertyFactory.COUNT);
     * }</pre>
     * @see org.apache.cayenne.query.ObjectSelect#selectCount(ObjectContext)
     */
    public static final NumericProperty<Long> COUNT = createNumeric(FunctionExpressionFactory.countExp(), Long.class);

    /**
     * Property that corresponds to SQL function {@code NOW()}
     * <p>
     * Usage:<pre>{@code
     * ObjectSelect.query(Artist.class).where(Artist.DATE_OF_BIRTH.year().lt(PropertyFactory.NOW.year().sub(100)));
     * }</pre>
     */
    public static final DateProperty<LocalDateTime> NOW = createDate(FunctionExpressionFactory.currentTimestamp(), LocalDateTime.class);

    // BaseProperty

    /**
     * Create base property
     *
     * @param name of the property
     * @param expression that property will use
     * @param type type of represented attribute
     * @param <T> type of represented attribute
     * @return new property with custom expression
     */
    public static <T> BaseProperty<T> createBase(String name, Expression expression, Class<T> type) {
        return new BaseProperty<>(name, expression, type);
    }

    /**
     * Create base property
     *
     * @param name of the property, will be used as value for path expression
     * @param type type of represented attribute
     * @param <T> type of represented attribute
     * @return new path property
     */
    public static <T> BaseProperty<T> createBase(String name, Class<T> type) {
        return createBase(name, null, type);
    }

    /**
     * Create base property
     *
     * @param expression that property will use
     * @param type type of represented attribute
     * @param <T> type of represented attribute
     * @return new property with custom expression without name
     */
    public static <T> BaseProperty<T> createBase(Expression expression, Class<T> type) {
        return createBase(null, expression, type);
    }

    // StringProperty

    /**
     * Create string property
     *
     * @param name of the property
     * @param expression that property will use
     * @param type type of represented attribute
     * @param <T> type of represented attribute
     * @return new property with custom expression
     */
    public static <T extends CharSequence> StringProperty<T> createString(String name, Expression expression, Class<T> type) {
        return new StringProperty<>(name, expression, type);
    }

    /**
     * Create string property
     *
     * @param name of the property, will be used as value for path expression
     * @param type type of represented attribute
     * @param <T> type of represented attribute
     * @return new path property
     */
    public static <T extends CharSequence> StringProperty<T> createString(String name, Class<T> type) {
        return createString(name, null, type);
    }

    /**
     * Create string property
     *
     * @param expression that property will use
     * @param type type of represented attribute
     * @param <T> type of represented attribute
     * @return new property with custom expression without name
     */
    public static <T extends CharSequence> StringProperty<T> createString(Expression expression, Class<T> type) {
        return createString(null, expression, type);
    }

    // NumericProperty

    /**
     * Create numeric property
     *
     * @param name of the property
     * @param expression that property will use
     * @param type type of represented attribute
     * @param <T> type of represented attribute
     * @return new property with custom expression
     */
    public static <T extends Number> NumericProperty<T> createNumeric(String name, Expression expression, Class<T> type) {
        return new NumericProperty<>(name, expression, type);
    }

    /**
     * Create numeric property
     *
     * @param name of the property, will be used as value for path expression
     * @param type type of represented attribute
     * @param <T> type of represented attribute
     * @return new path property
     */
    public static <T extends Number> NumericProperty<T> createNumeric(String name, Class<T> type) {
        return createNumeric(name, null, type);
    }

    /**
     * Create numeric property
     *
     * @param expression that property will use
     * @param type type of represented attribute
     * @param <T> type of represented attribute
     * @return new property with custom expression without name
     */
    public static <T extends Number> NumericProperty<T> createNumeric(Expression expression, Class<T> type) {
        return createNumeric(null, expression, type);
    }

    // DateProperty

    /**
     * Create date property
     *
     * @param name of the property
     * @param expression that property will use
     * @param type type of represented attribute
     * @param <T> type of represented attribute
     * @return new property with custom expression
     */
    public static <T> DateProperty<T> createDate(String name, Expression expression, Class<T> type) {
        return new DateProperty<>(name, expression, type);
    }

    /**
     * Create date property
     *
     * @param name of the property, will be used as value for path expression
     * @param type type of represented attribute
     * @param <T> type of represented attribute
     * @return new path property
     */
    public static <T> DateProperty<T> createDate(String name, Class<T> type) {
        return createDate(name, null, type);
    }

    /**
     * Create date property
     *
     * @param expression that property will use
     * @param type type of represented attribute
     * @param <T> type of represented attribute
     * @return new property with custom expression without name
     */
    public static <T> DateProperty<T> createDate(Expression expression, Class<T> type) {
        return createDate(null, expression, type);
    }

    // ToOne relationship property

    /**
     * Create entity property
     *
     * @param name of the property
     * @param expression that property will use
     * @param entityType type of represented relationship entity
     * @param <T> type of represented relationship entity
     * @return new property with custom expression
     */
    public static <T extends Persistent> EntityProperty<T> createEntity(String name, Expression expression, Class<T> entityType) {
        return new EntityProperty<>(name, expression, entityType);
    }

    /**
     * Create entity property
     *
     * @param name of the property, will be used as value for path expression
     * @param type type of represented relationship entity
     * @param <T> type of represented relationship entity
     * @return new path property
     */
    public static <T extends Persistent> EntityProperty<T> createEntity(String name, Class<T> type) {
        return createEntity(name, null, type);
    }

    /**
     * Create entity property
     *
     * @param expression that property will use
     * @param type type of represented relationship entity
     * @param <T> type of represented relationship entity
     * @return new property with custom expression without name
     */
    public static <T extends Persistent> EntityProperty<T> createEntity(Expression expression, Class<T> type) {
        return createEntity(null, expression, type);
    }

    // Self properties

    /**
     * <b>Self</b> property allows to create column queries that return
     * full objects along with custom column set.
     *  <p>
     *  Usage example, query will return object with dependent objects count:<pre>{@code
     *  List<Object[]> result = ObjectSelect.columnQuery(Artist.class,
     *          PropertyFactory.createSelf(Artist.class),
     *          Artist.PAINTING_ARRAY.count())
     *      .select(context); }</pre>
     *
     * @param type of represented entity
     * @param <T> type of represented entity
     * @return new 'self' property
     */
    public static <T extends Persistent> EntityProperty<T> createSelf(Class<T> type) {
        return createEntity(ExpressionFactory.fullObjectExp(), type);
    }

    /**
     * <b>Self</b> property allows to create column queries that return
     * full objects along with custom column set.
     *  <p>
     *  This method is not much useful, as to-one property can be used as is in this case,
     *  example is purely for demonstration purpose only. See {@link EntityProperty} usage examples.
     *  <p>
     *  Usage example, query will return object with dependent objects count:<pre>{@code
     *  List<Object[]> result = ObjectSelect.columnQuery(Painting.class,
     *          Painting.PAINTING_TITLE,
     *          PropertyFactory.createSelf(Painting.TO_ARTIST.getExpression(), Painting.class))
     *      .select(context); }</pre>
     *
     * @param expression expression to be used for this property (usually it will be path from other property)
     * @param type of represented entity
     * @param <T> type of represented entity
     * @return new 'self' property
     */
    public static <T extends Persistent> EntityProperty<T> createSelf(Expression expression, Class<T> type) {
        return createEntity(ExpressionFactory.fullObjectExp(expression), type);
    }

    // ToMany relationship properties

    /**
     * Create to-many relationship mapped on list property
     *
     * @param name of the property
     * @param expression that property will use
     * @param entityType type of represented relationship entity
     * @param <T> type of represented relationship entity
     * @return new property with custom expression
     */
    public static <T extends Persistent> ListProperty<T> createList(String name, Expression expression, Class<T> entityType) {
        return new ListProperty<>(name, expression, entityType);
    }

    /**
     * Create to-many relationship mapped on list property
     *
     * @param name of the property, will be used as value for path expression
     * @param entityType type of represented relationship entity
     * @param <T> type of represented relationship entity
     * @return new path property
     */
    public static <T extends Persistent> ListProperty<T> createList(String name, Class<T> entityType) {
        return createList(name, null, entityType);
    }

    /**
     * Create to-many relationship mapped on set property
     *
     * @param name of the property
     * @param expression that property will use
     * @param entityType type of represented attribute
     * @param <T> type of represented attribute
     * @return new property with custom expression
     */
    public static <T extends Persistent> SetProperty<T> createSet(String name, Expression expression, Class<T> entityType) {
        return new SetProperty<>(name, expression, entityType);
    }

    /**
     * Create to-many relationship mapped on set property
     *
     * @param name of the property, will be used as value for path expression
     * @param entityType type of represented relationship entity
     * @param <T> type of represented relationship entity
     * @return new path property
     */
    public static <T extends Persistent> SetProperty<T> createSet(String name, Class<T> entityType) {
        return createSet(name, null, entityType);
    }

    /**
     * Create to-many relationship mapped on map property
     *
     * @param name of the property
     * @param expression that property will use
     * @param keyType type of represented relationship keys
     * @param entityType type of represented relationship values
     * @param <K> type of represented relationship keys
     * @param <V> type of represented relationship values
     * @return new property with custom expression
     */
    public static <K, V extends Persistent> MapProperty<K, V> createMap(String name, Expression expression, Class<K> keyType, Class<V> entityType) {
        return new MapProperty<>(name, expression, keyType, entityType);
    }

    /**
     * Create to-many relationship mapped on map property
     *
     * @param name of the property, will be used as value for path expression
     * @param keyType type of represented relationship keys
     * @param entityType type of represented relationship values
     * @param <K> type of represented relationship keys
     * @param <V> type of represented relationship values
     * @return new path property
     */
    public static <K, V extends Persistent> MapProperty<K, V> createMap(String name, Class<K> keyType, Class<V> entityType) {
        return createMap(name, null, keyType, entityType);
    }

    /**
     * Create property that represents embeddable
     *
     * @param name of the property, will be used as value for path expression
     * @param embeddableType type of represented embeddable entity
     * @param <T> type of represented embeddable entity
     * @return new path property
     */
    public static <T extends EmbeddableObject> EmbeddableProperty<T> createEmbeddable(String name, Class<T> embeddableType) {
        return new EmbeddableProperty<>(name, null, embeddableType);
    }

    /**
     * Create property that represents embeddable
     *
     * @param name of the property, will be used as value for path expression
     * @param exp that property will use
     * @param embeddableType type of represented embeddable entity
     * @param <T> type of represented embeddable entity
     * @return new path property
     */
    public static <T extends EmbeddableObject> EmbeddableProperty<T> createEmbeddable(String name, Expression exp, Class<T> embeddableType) {
        return new EmbeddableProperty<>(name, exp, embeddableType);
    }

    public static <T> BaseIdProperty<T> createBaseId(String attribute, String entityName, Class<T> propertyType) {
        return createBaseId(attribute, null, entityName, propertyType);
    }

    public static <T> BaseIdProperty<T> createBaseId(String attribute, String path, String entityName, Class<T> propertyType) {
        return new BaseIdProperty<>(attribute, path, entityName, propertyType);
    }

    public static <T extends Number> NumericIdProperty<T> createNumericId(String attribute, String entityName, Class<T> propertyType) {
        return createNumericId(attribute, null, entityName, propertyType);
    }

    public static <T extends Number> NumericIdProperty<T> createNumericId(String attribute, String path, String entityName, Class<T> propertyType) {
        return new NumericIdProperty<>(attribute, path, entityName, propertyType);
    }
}
