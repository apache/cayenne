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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
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
     * @deprecated in favour of {@link #idsInCollection(Collection)}
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
     * @deprecated in favour of {@link #idsIn(Object...)}
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
     * @deprecated in favour of {@link #idsNotInCollection(Collection)}
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
     * @deprecated in favour of {@link #idsNotIn(Object...)}
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
     * Matches an object by a possibly compound id, expressed as a map of DB attribute names to values.
     * Unlike {@link #eqId(Object)} this form supports entities with a compound PK.
     *
     * @param id a map of the target entity PK attribute names to values
     * @return an expression matching an object with the given id
     * @since 5.0
     */
    public Expression eqIdMap(Map<String, ?> id) {
        return idExp(id);
    }

    /**
     * @param id a map of the target entity PK attribute names to values
     * @return an expression excluding an object with the given id
     * @since 5.0
     * @see #eqIdMap(Map)
     */
    public Expression neqIdMap(Map<String, ?> id) {
        return idExp(id).notExp();
    }

    /**
     * Matches any of the objects identified by the provided ids. Unlike {@link #idsIn(Object...)} this form
     * supports entities with a compound PK, expanding to an {@code OR} of per-id matches rather than an
     * {@code IN}, as SQL {@code IN} can't be applied to a multi-column key.
     *
     * @param ids maps of the target entity PK attribute names to values
     * @since 5.0
     * @see #eqIdMap(Map)
     */
    @SafeVarargs
    public final Expression idMapsIn(Map<String, ?>... ids) {
        return idMapsInCollection(Arrays.asList(ids));
    }

    /**
     * @param ids maps of the target entity PK attribute names to values
     * @since 5.0
     * @see #idMapsIn(Map[])
     */
    public Expression idMapsInCollection(Collection<Map<String, ?>> ids) {
        List<Expression> expressions = new ArrayList<>(ids.size());
        for (Map<String, ?> id : ids) {
            expressions.add(idExp(id));
        }
        return expressions.isEmpty() ? ExpressionFactory.expFalse() : ExpressionFactory.joinExp(Expression.OR, expressions);
    }

    /**
     * Matches any of the objects identified by the provided {@link ObjectId}s. Unlike
     * {@link #idsIn(Object...)} this form supports entities with a compound PK.
     *
     * @since 5.0
     * @see #idMapsIn(Map[])
     */
    public Expression objectIdsIn(ObjectId... ids) {
        return objectIdsInCollection(Arrays.asList(ids));
    }

    /**
     * @since 5.0
     * @see #objectIdsIn(ObjectId...)
     */
    public Expression objectIdsInCollection(Collection<ObjectId> ids) {
        List<Map<String, ?>> snapshots = new ArrayList<>(ids.size());
        for (ObjectId id : ids) {
            snapshots.add(id.getIdSnapshot());
        }
        return idMapsInCollection(snapshots);
    }

    /**
     * Builds an {@code AND} of per-PK-attribute matches. Paths are built as "dbid:" paths relative to this
     * property, so they resolve through the Obj layer and work for both the query root (an empty path, see
     * {@link SelfProperty}) and a to-one relationship.
     */
    private Expression idExp(Map<String, ?> id) {
        if (id == null || id.isEmpty()) {
            throw new CayenneRuntimeException("Null or empty id map");
        }

        Expression result = null;
        for (Map.Entry<String, ?> entry : id.entrySet()) {
            Expression next = ExpressionFactory
                    .matchExp(ExpressionFactory.dbIdPathExp(getPath().dot(entry.getKey())), entry.getValue());
            result = result == null ? next : result.andExp(next);
        }
        return result;
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
