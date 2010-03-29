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

package org.apache.cayenne;

import java.io.Serializable;

/**
 * Provides a level of indirection for property value access, most often used for deferred
 * faulting of to-one relationships. A ValueHolder abstracts how a property value is
 * obtained (fetched from DB, etc.), thus simplifying design of an object that uses it.
 * <p>
 * Here is an example of a bean property implemented using ValueHolder:
 * </p>
 * 
 * <pre>
 * protected ValueHolder someProperty;
 * 
 * public SomeClass getSomeProperty() {
 *     return (SomeClass) somePropertyHolder.getValue(SomeClass.class);
 * }
 * 
 * public void setSomeProperty(SomeClass newValue) {
 *     somePropertyHolder.setValue(SomeClass.class, newValue);
 * }
 * </pre>
 * 
 * @since 1.2
 */
public interface ValueHolder extends Serializable {

    /**
     * Returns an object stored by this ValueHolder.
     */
    Object getValue() throws CayenneRuntimeException;

    /**
     * Retrieves ValueHolder value without triggering fault resolution.
     */
    Object getValueDirectly() throws CayenneRuntimeException;

    /**
     * Sets an object stored by this ValueHolder.
     * 
     * @param value a new value of the ValueHolder.
     * @return a previous value saved in the ValueHolder.
     */
    Object setValue(Object value) throws CayenneRuntimeException;

    /**
     * Sets ValueHolder vaue without triggering fault resolution.
     */
    Object setValueDirectly(Object value) throws CayenneRuntimeException;

    /**
     * Returns true if the internal value is not yet resolved.
     */
    boolean isFault();

    /**
     * Turns a ValueHolder into a fault.
     */
    void invalidate();
}
