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

package org.apache.cayenne.property;

/**
 * Defines bean property API used by Cayenne to access object data, do faulting and graph
 * maintenance tasks.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public interface Property extends PropertyAccessor {

    /**
     * Returns a property value, resolving object fault if needed.
     */
    Object readProperty(Object object) throws PropertyAccessException;

    /**
     * Sets a property value,resolving object fault if needed. Old value of the property
     * is specified as a hint.
     */
    void writeProperty(Object object, Object oldValue, Object newValue)
            throws PropertyAccessException;

    boolean visit(PropertyVisitor visitor);

    /**
     * If a property is implemented as a ValueHolder, this operation would create an
     * unfaulted value holder and inject it into the object, if an object doesn't have it
     * set yet.
     */
    // TODO: andrus 5/25/2006 - maybe move this to ArcProperty as simple properties do not
    // support ValueHolders and ClassDescriptors are smart enough to avoid calling this
    // method on non-arc property.
    void injectValueHolder(Object object) throws PropertyAccessException;

    /**
     * Copies a property value from one object to another.
     */
    void shallowMerge(Object from, Object to) throws PropertyAccessException;
}
