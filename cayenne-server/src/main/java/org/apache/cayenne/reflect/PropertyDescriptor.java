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

package org.apache.cayenne.reflect;

import org.apache.cayenne.util.Util;

/**
 * Defines bean property API used by Cayenne to access object data, do faulting
 * and graph maintenance tasks.
 * 
 * @since 4.0
 */
public interface PropertyDescriptor {

    /**
     * Returns property name.
     */
    String getName();

    /**
     * Returns a property value of an object without disturbing the object fault
     * status.
     */
    Object readPropertyDirectly(Object object) throws PropertyException;

    /**
     * Returns a property value, inflating unresolved object if need.
     */
    Object readProperty(Object object) throws PropertyException;

    /**
     * Sets a property value of an object without disturbing the object fault
     * status. Old value of the property is specified as a hint and can be
     * ignored by the property implementor.
     */
    void writePropertyDirectly(Object object, Object oldValue, Object newValue)
            throws PropertyException;

    /**
     * Sets a property value, inflating unresolved object if need. Old value of
     * the property is specified as a hint and can be ignored by the property
     * implementor.
     */
    void writeProperty(Object object, Object oldValue, Object newValue)
            throws PropertyException;

    /**
     * A visitor accept method.
     * 
     * @return a status returned by the corresponding callback method of the
     *         visitor. It serves as an indication of whether peer properties
     *         processing is still needed.
     */
    boolean visit(PropertyVisitor visitor);

    /**
     * If a property is implemented as a ValueHolder, this operation would
     * create an unfaulted value holder and inject it into the object, if an
     * object doesn't have it set yet.
     */
    void injectValueHolder(Object object) throws PropertyException;

    /**
     * Allows to use special logic to compare values for equality
     * as in rare cases it is not sufficient to use the default equals() method.
     * Default implementation uses {@link Util#nullSafeEquals(Object, Object)} method.
     *
     * @param value1 to compare
     * @param value2 to compare
     * @return true if given values are equal
     *
     * @since 4.2
     */
    default boolean equals(Object value1, Object value2) {
        return Util.nullSafeEquals(value1, value2);
    }

}
