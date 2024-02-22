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

import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.ObjectSelect;

/**
 * Property that represents root entity
 * <br>
 * Usage example: <code><pre>
 * List<Object[]> result = Artist.SELF.columnQuery(Artist.SELF, Artist.PAINTING_ARRAY.count()).select(context);
 * </pre></code>
 *
 * @since 5.0
 * @param <E> type of the property
 */
public class SelfProperty<E extends Persistent> extends EntityProperty<E> {

    /**
     * Constructs a new property with the given name and expression
     *
     * @param path       of the property (will be used as alias for the expression)
     * @param expression expression for property
     * @param type       of the property
     * @see PropertyFactory#createBase(String, Expression, Class)
     */
    protected SelfProperty(CayennePath path, Expression expression, Class<E> type) {
        super(path, expression, type);
    }

    public Expression exists(Expression where) {
        return ExpressionFactory.exists(ObjectSelect.query(getType()).where(where));
    }

    public Expression notExists(Expression where) {
        return ExpressionFactory.notExists(ObjectSelect.query(getType()).where(where));
    }

    public ObjectSelect<E> query(Expression where) {
        return ObjectSelect.query(getType()).where(where);
    }

    public ObjectSelect<E> query() {
        return ObjectSelect.query(getType());
    }

    public <T> ColumnSelect<T> columnQuery(Property<T> property) {
        return ObjectSelect.columnQuery(getType(), property);
    }

    public ColumnSelect<Object[]> columnQuery(Property<?>... properties) {
        return ObjectSelect.columnQuery(getType(), properties);
    }
}
