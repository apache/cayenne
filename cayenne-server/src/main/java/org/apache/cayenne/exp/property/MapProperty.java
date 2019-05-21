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
import java.util.Map;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTPath;

/**
 * Property that represents to-many relationship mapped on {@link Map}.
 *
 * @see org.apache.cayenne.exp.property
 * @since 4.2
 */
public class MapProperty<K, V extends Persistent> extends BaseProperty<Map<K, V>> implements RelationshipProperty<Map<K, V>> {

    protected Class<K> keyType;

    protected Class<V> entityType;

    /**
     * Constructs a new property with the given name and expression
     *
     * @param name       of the property (will be used as alias for the expression)
     * @param expression expression for property
     * @param keyType    type of keys of the property
     * @param entityType type of related entities
     * @see PropertyFactory#createMap(String, Expression, Class, Class)
     */
    protected MapProperty(String name, Expression expression, Class<K> keyType, Class<V> entityType) {
        super(name, expression, Map.class);
        this.keyType = keyType;
        this.entityType = entityType;
    }

    /**
     * <p>Create new "flat" property for toMany relationship.</p>
     * <p>
     *     Example:
     *     <pre>{@code
     *     List<Object[]> result = ObjectSelect
     *          .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.flat(Painting.class))
     *          .select(context);
     *     }</pre>
     * </p>
     */
    public EntityProperty<V> flat() {
        return PropertyFactory.createEntity(ExpressionFactory.fullObjectExp(getExpression()), getEntityType());
    }

    // TODO: move all *contains* methods to RelationshipProperty once Property class is removed

    /**
     * @return An expression representing equality to a value.
     */
    public Expression contains(V value) {
        return ExpressionFactory.matchExp(getExpression(), value);
    }


    /**
     * @return An expression representing inequality to a value.
     */
    public Expression notContains(V value) {
        return ExpressionFactory.noMatchExp(getExpression(), value);
    }

    /**
     * @return An expression for finding objects with values in the given set.
     */
    @SafeVarargs
    public final Expression contains(V firstValue, V... moreValues) {

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
     */
    public Expression contains(Collection<V> values) {
        return ExpressionFactory.inExp(getExpression(), values);
    }

    /**
     * @return An expression for finding objects with values not in the given set.
     */
    public Expression notContains(Collection<V> values) {
        return ExpressionFactory.notInExp(getExpression(), values);
    }

    /**
     * @return An expression for finding objects with values not in the given set.
     */
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
     * @param id object id
     * @return An expression for finding object with given id.
     */
    public Expression containsId(Object id) {
        return ExpressionFactory.matchExp(getExpression(), id);
    }

    /**
     * @return An expression for finding objects with given id set
     */
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
     */
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
     */
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
     */
    public Expression notContainsId(Collection<Object> ids) {
        return ExpressionFactory.notInExp(getExpression(), ids);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MapProperty<K, V> alias(String alias) {
        ASTPath exp = PropertyUtils.createPathExp(this.getName(), alias, getExpression().getPathAliases());
        return PropertyFactory.createMap(exp.getPath(), exp, this.getKeyType(), this.getEntityType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MapProperty<K, V> outer() {
        return getName().endsWith("+")
                ? this
                : PropertyFactory.createMap(getName() + "+", getKeyType(), getEntityType());
    }

    /**
     * @return type of keys in represented attribute
     */
    protected Class<K> getKeyType() {
        return keyType;
    }

    /**
     * @return type of object entity in represented attribute
     */
    protected Class<V> getEntityType() {
        return entityType;
    }

    /**
     * @return property that will be translated relative to parent query
     */
    public MapProperty<K, V> enclosing() {
        return PropertyFactory.createMap(null, ExpressionFactory.enclosingObjectExp(getExpression()),  getKeyType(), getEntityType());
    }
}
