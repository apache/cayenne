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

import org.apache.cayenne.exp.path.CayennePath;

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
        CayennePath path = getPath().dot(property);
        return PropertyFactory.createBase(
                path,
                PropertyUtils.buildExp(path, getExpression().getPathAliases()),
                null
        );
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T> BaseProperty<T> dot(BaseProperty<T> property) {
        CayennePath path = getPath().dot(property.getPath());
        return PropertyFactory.createBase(
                path,
                PropertyUtils.buildExp(path, getExpression().getPathAliases()),
                property.getType()
        );
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T extends Number> NumericProperty<T> dot(NumericProperty<T> property) {
        CayennePath path = getPath().dot(property.getPath());
        return PropertyFactory.createNumeric(
                path,
                PropertyUtils.buildExp(path, getExpression().getPathAliases()),
                property.getType()
        );
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T extends CharSequence> StringProperty<T> dot(StringProperty<T> property) {
        CayennePath path = getPath().dot(property.getPath());
        return PropertyFactory.createString(
                path,
                PropertyUtils.buildExp(path, getExpression().getPathAliases()),
                property.getType()
        );
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T> DateProperty<T> dot(DateProperty<T> property) {
        CayennePath path = getPath().dot(property.getPath());
        return PropertyFactory.createDate(
                path,
                PropertyUtils.buildExp(path, getExpression().getPathAliases()),
                property.getType()
        );
    }

}
