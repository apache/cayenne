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

import java.util.Objects;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.path.CayennePath;

/**
 * Property that represents numeric PK
 *
 * @since 4.2
 */
public class NumericIdProperty<E extends Number> extends NumericProperty<E> implements IdProperty<E> {

    private final String entityName;

    private final String attributeName;

    /**
     * Constructs a new property with the given name and expression
     *
     * @param attribute  PK attribute name (optional, can be omitted for single PK entity)
     * @param path       cayenne path (optional, can be omitted for  ID of the root)
     * @param entityName name of the entity (mandatory)
     * @param type       of the property (mandatory)
     * @see PropertyFactory#createNumericId(String, String, String, Class)
     */
    protected NumericIdProperty(String attribute, CayennePath path, String entityName, Class<E> type) {
        super(CayennePath.EMPTY_PATH, ExpressionFactory.dbIdPathExp(path.dot(attribute)), type);
        this.entityName = Objects.requireNonNull(entityName);
        this.attributeName = Objects.requireNonNull(attribute);
    }

    @Override
    public String getEntityName() {
        return entityName;
    }

    @Override
    public String getAttributeName() {
        return attributeName;
    }
}
