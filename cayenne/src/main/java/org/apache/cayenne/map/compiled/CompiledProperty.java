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
package org.apache.cayenne.map.compiled;

import org.apache.cayenne.reflect.PropertyException;

/**
 * A property of a compiled entity, an attribute or a relationship, as read and written on the objects of the entity.
 *
 * @since 6.0
 */
public sealed interface CompiledProperty permits CompiledAttribute, CompiledRelationship {

    String name();

    /**
     * Returns the entity this property belongs to.
     */
    CompiledEntity entity();

    /**
     * Returns the property value of an object without disturbing the object fault status.
     */
    Object readPropertyDirectly(Object object) throws PropertyException;

    /**
     * Returns the property value, resolving the object and the value if they are faults.
     */
    Object readProperty(Object object) throws PropertyException;

    /**
     * Sets the property value of an object without disturbing the object fault status.
     */
    void writePropertyDirectly(Object object, Object oldValue, Object newValue) throws PropertyException;

    /**
     * Sets the property value, resolving the object if it is a fault.
     */
    void writeProperty(Object object, Object oldValue, Object newValue) throws PropertyException;

    /**
     * Compares two values of the property for equality.
     */
    boolean equals(Object value1, Object value2);
}
