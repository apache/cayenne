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

import java.util.Collection;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.path.CayennePath;

/**
 * Base class for {@link ListProperty} and {@link SetProperty}
 * @since 4.2
 */
public abstract class CollectionProperty<V extends Persistent, E extends Collection<V>>
        extends BaseProperty<E> implements RelationshipProperty<E> {

    protected Class<V> entityType;

    /**
     * Constructs a new property with the given name and expression
     *
     * @param path       of the property (will be used as alias for the expression)
     * @param expression expression for property
     * @param collectionType type of the collection
     * @param entityType type of related entity
     */
    protected CollectionProperty(CayennePath path, Expression expression, Class<? super E> collectionType, Class<V> entityType) {
        super(path, expression, collectionType);
        this.entityType = entityType;
    }

    /**
     * <p>Create new "flat" property for toMany relationship.</p>
     * <p>
     * Example:
     * <pre>{@code
     * List<Object[]> result = ObjectSelect
     *      .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.flat())
     *      .select(context);
     * }</pre>
     * </p>
     */
    public EntityProperty<V> flat() {
        return PropertyFactory.createEntity(ExpressionFactory.fullObjectExp(getExpression()), getEntityType());
    }

    /**
     * @return An expression representing equality to a value.
     * @deprecated since 5.0 in favour of {@link #containsValue(V)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public Expression contains(V value) {
        return containsValue(value);
    }

    /**
     * @return An expression representing equality to a value.
     * @deprecated since 5.0 in favour of {@link #notContainsValue(V)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public Expression notContains(V value) {
        return notContainsValue(value);
    }

    /**
     * @return An expression for finding objects with values in the given set.
     * @deprecated since 5.0 in favour of {@link #containsValues(V...)}
     */
    @SuppressWarnings("unchecked")
    @Deprecated(since = "5.0", forRemoval = true)
    public Expression contains(V firstValue, V... moreValues) {
        int moreValuesLength = moreValues != null ? moreValues.length : 0;
        Object[] values = new Object[moreValuesLength + 1];
        values[0] = firstValue;

        if (moreValuesLength > 0) {
            System.arraycopy(moreValues, 0, values, 1, moreValuesLength);
        }
        return ExpressionFactory.inExp(getExpression(), values);
    }

    /**
     * @return An expression for finding objects with values in the given set.
     * @deprecated since 5.0 in favour of {@link #containsValuesCollection(Collection)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public Expression contains(Collection<V> values) {
        return ExpressionFactory.inExp(getExpression(), values);
    }

    /**
     * @param id object id
     * @return An expression for finding object with given id.
     */
    public Expression containsId(Object id) {
        return ExpressionFactory.matchExp(getExpression(), id);
    }

    /**
     * @return An expression for finding objects with given id set
     * @deprecated since 5.0 in favour of  {@link #containsIds(Object...)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public Expression containsId(Object firstId, Object... moreId) {

        int moreValuesLength = moreId != null ? moreId.length : 0;

        Object[] values = new Object[moreValuesLength + 1];
        values[0] = firstId;

        if (moreValuesLength > 0) {
            System.arraycopy(moreId, 0, values, 1, moreValuesLength);
        }

        return ExpressionFactory.inExp(getExpression(), values);
    }

    /**
     * @return An expression for finding objects with given id set.
     * @deprecated since 5.0 in favour of {@link #containsIdsCollection(Collection)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public Expression containsId(Collection<Object> ids) {
        return ExpressionFactory.inExp(getExpression(), ids);
    }

    /**
     * @param id object id
     * @return An expression for finding object without given id.
     */
    public Expression notContainsId(Object id) {
        return ExpressionFactory.noMatchExp(getExpression(), id);
    }

    /**
     * @return An expression for finding objects without given id set.
     * @deprecated since 5.0 in favour of {@link #notContainsIds(Object...)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public Expression notContainsId(Object firstId, Object... moreId) {

        int moreValuesLength = moreId != null ? moreId.length : 0;

        Object[] values = new Object[moreValuesLength + 1];
        values[0] = firstId;

        if (moreValuesLength > 0) {
            System.arraycopy(moreId, 0, values, 1, moreValuesLength);
        }

        return ExpressionFactory.notInExp(getExpression(), values);
    }

    /**
     * @return An expression for finding objects without given id set.
     * @deprecated since 5.0 in favour of {@link #notContainsIdsCollection(Collection)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public Expression notContainsId(Collection<Object> ids) {
        return ExpressionFactory.notInExp(getExpression(), ids);
    }

    /**
     * @return An expression for finding objects with values not in the given set.
     * @deprecated since 5.0 in favour of {@link #notContainsValuesCollection(Collection)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public Expression notContains(Collection<V> values) {
        return ExpressionFactory.notInExp(getExpression(), values);
    }

    /**
     * @return An expression for finding objects with values not in the given set.
     * @deprecated since 5.0 in favour of {@link #notContainsValues(V...)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    @SafeVarargs
    public final Expression notContains(V firstValue, V... moreValues) {

        int moreValuesLength = moreValues != null ? moreValues.length : 0;

        Object[] values = new Object[moreValuesLength + 1];
        values[0] = firstValue;

        if (moreValuesLength > 0) {
            System.arraycopy(moreValues, 0, values, 1, moreValuesLength);
        }

        return ExpressionFactory.notInExp(getExpression(), values);
    }

    /**
     * @return An expression representing equality to a value.
     * @since 5.0
     */
    public Expression containsValue(V value) {
        return ExpressionFactory.matchExp(getExpression(), value);
    }

    /**
     * @return An expression representing inequality to a value.
     * @since 5.0
     */
    public Expression notContainsValue(V value) {
        return ExpressionFactory.noMatchExp(getExpression(), value);
    }

    /**
     * @return An expression for finding objects with values in the given set.
     * @since 5.0
     */
    @SafeVarargs
    public final Expression containsValues(V... values) {
        if(values == null || values.length == 0) {
            throw new IllegalArgumentException("At least one value is expected.");
        }
        return ExpressionFactory.inExp(getExpression(), (Object[])values);
    }

    /**
     * @return An expression for finding objects with values in the given set.
     * @since 5.0
     */
    public Expression containsValuesCollection(Collection<V> values) {
        return ExpressionFactory.inExp(getExpression(), values);
    }

    /**
     * @return An expression for finding objects with given id set
     * @since 5.0
     */
    public Expression containsIds(Object... ids) {
        return ExpressionFactory.inExp(getExpression(), ids);
    }

    /**
     * @return An expression for finding objects with given id set.
     * @since 5.0
     */
    public Expression containsIdsCollection(Collection<?> ids) {
        return ExpressionFactory.inExp(getExpression(), ids);
    }

    /**
     * @return An expression for finding objects with given id set
     * @since 5.0
     */
    public Expression notContainsIds(Object... ids) {
        return ExpressionFactory.notInExp(getExpression(), ids);
    }

    /**
     * @return An expression for finding objects without given id set.
     * @since 5.0
     */
    public Expression notContainsIdsCollection(Collection<?> ids) {
        return ExpressionFactory.notInExp(getExpression(), ids);
    }

    /**
     * @return An expression for finding objects with values not in the given set.
     * @since 5.0
     */
    public Expression notContainsValuesCollection(Collection<V> values) {
        return ExpressionFactory.notInExp(getExpression(), values);
    }

    /**
     * @return An expression for finding objects with values not in the given set.
     * @since 5.0
     */
    @SafeVarargs
    public final Expression notContainsValues(V... values) {
        return ExpressionFactory.notInExp(getExpression(), (Object[])values);
    }

    /**
     * @return object entity type represented by this property
     */
    protected Class<V> getEntityType() {
        return entityType;
    }
}
