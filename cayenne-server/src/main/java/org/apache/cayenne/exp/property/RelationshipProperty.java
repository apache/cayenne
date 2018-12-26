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
import org.apache.cayenne.query.PrefetchTreeNode;

/**
 * Interface (or "Trait") that provides basic functionality for all types of relationships.
 * <p>
 * Provides "dot", prefetch and "outer" functionality.
 *
 * @see org.apache.cayenne.exp.property
 * @since 4.2
 */
public interface RelationshipProperty<E> extends Property<E> {

    /**
     * Constructs a property path by appending the argument to the existing property separated by a dot.
     *
     * @return a newly created Property object.
     */
    default BaseProperty<Object> dot(String property) {
        return PropertyFactory.createBase(getName() + "." + property, null);
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T> BaseProperty<T> dot(BaseProperty<T> property) {
        return PropertyFactory.createBase(getName() + "." + property.getName(), property.getType());
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T extends Number> NumericProperty<T> dot(NumericProperty<T> property) {
        return PropertyFactory.createNumeric(getName() + "." + property.getName(), property.getType());
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T extends CharSequence> StringProperty<T> dot(StringProperty<T> property) {
        return PropertyFactory.createString(getName() + "." + property.getName(), property.getType());
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T> DateProperty<T> dot(DateProperty<T> property) {
        return PropertyFactory.createDate(getName() + "." + property.getName(), property.getType());
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T extends Persistent> EntityProperty<T> dot(EntityProperty<T> property) {
        return PropertyFactory.createEntity(getName() + "." + property.getName(), property.getType());
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T extends Persistent> ListProperty<T> dot(ListProperty<T> property) {
        return PropertyFactory.createList(getName() + "." + property.getName(), property.getEntityType());
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <T extends Persistent> SetProperty<T> dot(SetProperty<T> property) {
        return PropertyFactory.createSet(getName() + "." + property.getName(), property.getEntityType());
    }

    /**
     * Constructs a new property path by appending the argument to the existing property separated by a dot.
     *
     * @param property to append to path
     * @return a newly created Property object.
     */
    default <K, V extends Persistent> MapProperty<K, V> dot(MapProperty<K, V> property) {
        return PropertyFactory.createMap(getName() + "." + property.getName(), property.getKeyType(), property.getEntityType());
    }

    /**
     * Returns a version of this property that represents an OUTER join. It is
     * up to caller to ensure that the property corresponds to a relationship,
     * as "outer" attributes make no sense.
     */
    BaseProperty<E> outer();

    /**
     * Returns a prefetch tree that follows this property path, potentially
     * spanning a number of phantom nodes, and having a single leaf with "joint"
     * prefetch semantics.
     */
    default PrefetchTreeNode joint() {
        return PrefetchTreeNode.withPath(getName(), PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
    }

    /**
     * Returns a prefetch tree that follows this property path, potentially
     * spanning a number of phantom nodes, and having a single leaf with
     * "disjoint" prefetch semantics.
     */
    default PrefetchTreeNode disjoint() {
        return PrefetchTreeNode.withPath(getName(), PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
    }

    /**
     * Returns a prefetch tree that follows this property path, potentially
     * spanning a number of phantom nodes, and having a single leaf with
     * "disjoint by id" prefetch semantics.
     */
    default PrefetchTreeNode disjointById() {
        return PrefetchTreeNode.withPath(getName(), PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);
    }

}
