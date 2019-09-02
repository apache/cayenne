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

import org.apache.cayenne.EmbeddableObject;
import org.apache.cayenne.Persistent;

/**
 * Property that represents path segment (relationship or embeddable).
 * Basically it provides {@code dot()} operator.
 *
 * @since 4.2
 */
public interface PathProperty<E> extends Property<E> {

    /**
     * Constructs a property path by appending the argument to the existing property separated by a dot.
     *
     * @return a newly created Property object.
     */
    default BaseProperty<Object> dot(String property) {
        String path = getName() + "." + property;
        return PropertyFactory.createBase(path,
                PropertyUtils.buildExp(path, getExpression().getPathAliases()),
                null);
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T> BaseProperty<T> dot(BaseProperty<T> property) {
        String path = getName() + "." + property.getName();
        return PropertyFactory.createBase(path,
                PropertyUtils.buildExp(path, getExpression().getPathAliases()),
                property.getType());
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T extends Number> NumericProperty<T> dot(NumericProperty<T> property) {
        String path = getName() + "." + property.getName();
        return PropertyFactory.createNumeric(path,
                PropertyUtils.buildExp(path, getExpression().getPathAliases()),
                property.getType());
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T extends CharSequence> StringProperty<T> dot(StringProperty<T> property) {
        String path = getName() + "." + property.getName();
        return PropertyFactory.createString(path,
                PropertyUtils.buildExp(path, getExpression().getPathAliases()),
                property.getType());
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T> DateProperty<T> dot(DateProperty<T> property) {
        String path = getName() + "." + property.getName();
        return PropertyFactory.createDate(path,
                PropertyUtils.buildExp(path, getExpression().getPathAliases()),
                property.getType());
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T extends Persistent > EntityProperty<T> dot(EntityProperty<T> property) {
        String path = getName() + "." + property.getName();
        return PropertyFactory.createEntity(path,
                PropertyUtils.buildExp(path, getExpression().getPathAliases()),
                property.getType());
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T extends Persistent> ListProperty<T> dot(ListProperty<T> property) {
        String path = getName() + "." + property.getName();
        return PropertyFactory.createList(path,
                PropertyUtils.buildExp(path, getExpression().getPathAliases()),
                property.getEntityType());
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T extends Persistent> SetProperty<T> dot(SetProperty<T> property) {
        String path = getName() + "." + property.getName();
        return PropertyFactory.createSet(path,
                PropertyUtils.buildExp(path, getExpression().getPathAliases()),
                property.getEntityType());
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <K, V extends Persistent> MapProperty<K, V> dot(MapProperty<K, V> property) {
        String path = getName() + "." + property.getName();
        return PropertyFactory.createMap(path,
                PropertyUtils.buildExp(path, getExpression().getPathAliases()),
                property.getKeyType(),
                property.getEntityType());
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T extends EmbeddableObject> EmbeddableProperty<T> dot(EmbeddableProperty<T> property) {
        String path = getName() + "." + property.getName();
        return PropertyFactory.createEmbeddable(path,
                PropertyUtils.buildExp(path, getExpression().getPathAliases()),
                property.getType());
    }
}
