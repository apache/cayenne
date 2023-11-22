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

package org.apache.cayenne.access.types;

import java.util.Objects;

/**
 * Descriptor and serialization helper for custom value objects that can be safely stored in the DB.
 * Lightweight alternative for the {@link ExtendedType}.
 *
 * @param <V> type of user's custom object.
 * @param <T> type that custom object will be serialized to/from
 *            should be backed by appropriate {@link ExtendedType}.
 *
 * @since 4.0
 */
public interface ValueObjectType<V, T> {

    /**
     * @return base type used to serialize <b>V</b> objects to.
     */
    Class<T> getTargetType();

    /**
     * @return type of Objects described by this ValueObjectType.
     */
    Class<V> getValueType();

    /**
     * @param value of type T
     * @return java object
     */
    V toJavaObject(T value);

    /**
     * @param object java object
     * @return value of type T
     */
    T fromJavaObject(V object);

    /**
     * Returned value should be same for objects that is logically equal.
     *
     * @return String representation usable for cache.
     */
    String toCacheKey(V object);

    /**
     * Allows to use special logic to compare values for equality
     * as in rare cases it is not suffice to use default equals() method.
     * Default implementation uses {@link Objects#equals(Object, Object)} method.
     *
     * @param value1 to compare
     * @param value2 to compare
     * @return true if given values are equal
     */
    default boolean equals(V value1, V value2) {
        return Objects.equals(value1, value2);
    }
}
