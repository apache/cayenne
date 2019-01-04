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

package org.apache.cayenne.exp.property;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTPath;

/**
 * Property that represents to-one relationships.
 * <p>
 * Usage examples in where clause: <pre>{@code
 * ObjectSelect.query(Paintings.class)
 *      .where(Painting.TO_ARTIST.dot(Artist.ARTIST_NAME).eq("Pablo Picasso"));}</pre>
 * <p>
 * Usage examples in column select, in this case full Artist entity will be
 * returned in the result: <pre>{@code
 * ObjectSelect
 *      .columnQuery(Paintings.class, Painting.PAINTING_TITLE, Painting.TO_ARTIST);}</pre>
 *
 * @see org.apache.cayenne.exp.property
 * @since 4.2
 */
public class EntityProperty<E extends Persistent> extends BaseProperty<E> implements RelationshipProperty<E> {

    /**
     * Constructs a new property with the given name and expression
     *
     * @param name       of the property (will be used as alias for the expression)
     * @param expression expression for property
     * @param type       of the property
     * @see PropertyFactory#createBase(String, Expression, Class)
     */
    protected EntityProperty(String name, Expression expression, Class<E> type) {
        super(name, expression, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityProperty<E> alias(String alias) {
        ASTPath exp = PropertyUtils.createPathExp(this.getName(), alias, getExpression().getPathAliases());
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
}
