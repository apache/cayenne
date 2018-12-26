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

import java.util.List;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;

/**
 * Property that represents to-many relationship mapped on {@link List}.
 * <pre>{@code
 * ObjectSelect.query(Artist.class)
 *      .where(Artist.PAINTING_ARRAY.contains(painting));
 * }</pre>
 *
 * @see org.apache.cayenne.exp.property
 * @since 4.2
 */
public class ListProperty<V extends Persistent> extends CollectionProperty<V, List<V>> {

    /**
     * Constructs a new property with the given name and expression
     *
     * @param name           of the property (will be used as alias for the expression)
     * @param expression     expression for property
     * @param entityType     type of related entity
     */
    protected ListProperty(String name, Expression expression, Class<V> entityType) {
        super(name, expression, List.class, entityType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListProperty<V> alias(String alias) {
        return PropertyFactory.createList(alias, this.getExpression(), this.getEntityType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListProperty<V> outer() {
        return getName().endsWith("+")
                ? this
                : PropertyFactory.createList(getName() + "+", getEntityType());
    }
}
