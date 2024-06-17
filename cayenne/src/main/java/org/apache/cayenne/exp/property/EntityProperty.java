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
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.cayenne.exp.path.CayennePath;

/**
 * Property that represents to-one relationships.
 * <p>
 * Usage examples in where clause: <pre>{@code
 * ObjectSelect.query(Paintings.class)
 *      .where(Painting.TO_ARTIST.dot(Artist.ARTIST_NAME).eq("Pablo Picasso"));}</pre>
 * <p>
 * Usage examples in column select, in this case full Artist entity will be returned as the result:
 * <pre>{@code
 * ObjectSelect
 *      .columnQuery(Paintings.class, Painting.PAINTING_TITLE, Painting.TO_ARTIST);}
 * </pre>
 *
 * @see org.apache.cayenne.exp.property
 * @since 4.2
 */
public class EntityProperty<E extends Persistent> extends BaseProperty<E> implements RelationshipProperty<E> {

    /**
     * Constructs a new property with the given name and expression
     *
     * @param path       of the property (will be used as alias for the expression)
     * @param expression expression for property
     * @param type       of the property
     * @see PropertyFactory#createBase(String, Expression, Class)
     */
    protected EntityProperty(CayennePath path, Expression expression, Class<E> type) {
        super(path, expression, type);
    }

    public Expression eqId(Object id) {
        return ExpressionFactory.matchExp(getExpression(), id);
    }

    /**
     * @deprecated since 5.0 in favour of {@link #idsInCollection(Collection)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public Expression inId(Collection<Object> ids) {
        return ExpressionFactory.inExp(getExpression(), ids);
    }

    /**
     * @param ids to use for "IN" expression
     * @return {@code IN} expression comparing path represented by this property with provided ids
     *
     * @since 5.0
     */
    public Expression idsInCollection(Collection<?> ids) {
        return ExpressionFactory.inExp(getExpression(), ids);
    }

    /**
     * @deprecated since 5.0 in favour of {@link #idsIn(Object...)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public Expression inId(Object firstId, Object... moreIds) {
        Object[] ids = new Object[moreIds.length + 1];
        ids[0] = firstId;
        System.arraycopy(moreIds, 0, ids, 1, moreIds.length);
        return ExpressionFactory.inExp(getExpression(), ids);
    }

    /**
     * @param ids to use for "IN" expression
     * @return {@code IN} expression comparing path represented by this property with provided ids
     *
     * @since 5.0
     */
    public Expression idsIn(Object... ids) {
        return ExpressionFactory.inExp(getExpression(), ids);
    }

    public Expression neqId(Object id) {
        return ExpressionFactory.noMatchExp(getExpression(), id);
    }

    /**
     * @deprecated since 5.0 in favour of {@link #idsNotInCollection(Collection)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public Expression ninId(Collection<Object> ids) {
        return ExpressionFactory.notInExp(getExpression(), ids);
    }

    /**
     * @param ids collection of IDs to use for "{@code NOT IN}" expression
     * @return {@code NOT IN} expression comparing path represented by this property with provided IDs
     *
     * @since 5.0
     */
    public Expression idsNotInCollection(Collection<?> ids) {
        return ExpressionFactory.notInExp(getExpression(), ids);
    }

    /**
     * @deprecated since 5.0 in favour of {@link #idsNotIn(Object...)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public Expression ninId(Object firstId, Object... moreIds) {
        Object[] ids = new Object[moreIds.length + 1];
        ids[0] = firstId;
        System.arraycopy(moreIds, 0, ids, 1, moreIds.length);
        return ExpressionFactory.notInExp(getExpression(), ids);
    }

    /**
     * @param ids to use for "{@code NOT IN}" expression
     * @return {@code NOT IN} expression comparing path represented by this property with provided ids
     *
     * @since 5.0
     */
    public Expression idsNotIn(Object... ids) {
        return ExpressionFactory.notInExp(getExpression(), ids);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityProperty<E> alias(String alias) {
        ASTPath exp = PropertyUtils.createPathExp(this.getPath(), alias, getExpression().getPathAliases());
        return PropertyFactory.createEntity(exp.getPath(), exp, this.getType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityProperty<E> outer() {
        return getName().endsWith("+")
                ? this
                : PropertyFactory.createEntity(getName() + "+", getType());
    }

    /**
     * @return property that will be translated relative to parent query
     */
    public EntityProperty<E> enclosing() {
        return PropertyFactory.createEntity(ExpressionFactory.enclosingObjectExp(getExpression()), getType());
    }

}
