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
package org.apache.cayenne.map;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A mapping descriptor of an entity listener class that declares one or more callback
 * methods to be notified of the entity events.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class EntityListener {

    protected String className;
    protected SortedMap callbackMethods;

    public EntityListener() {
        this(null);
    }

    public EntityListener(String className) {
        this.callbackMethods = new TreeMap();
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Returns an unmodifiable sorted map of listener methods.
     */
    public SortedMap getCallbackMethodsMap() {
        // create a new instance ... Caching unmodifiable map causes
        // serialization issues (esp. with Hessian).
        return Collections.unmodifiableSortedMap(callbackMethods);
    }

    /**
     * Returns an unmodifiable collection of listener methods.
     */
    public Collection getCallbackMethods() {
        // create a new instance. Caching unmodifiable collection causes
        // serialization issues (esp. with Hessian).
        return Collections.unmodifiableCollection(callbackMethods.values());
    }

    /**
     * Adds new listener method. If a method has no name, IllegalArgumentException is thrown.
     */
    public void addCallbackMethod(CallbackMethod method) {
        
        if (method.getName() == null) {
            throw new IllegalArgumentException("Attempt to insert unnamed method.");
        }

        Object existingMethod = callbackMethods.get(method.getName());
        if (existingMethod != null) {
            if (existingMethod == method) {
                return;
            }
            else {
                throw new IllegalArgumentException(
                        "An attempt to override method '"
                                + method.getName()
                                + "'");
            }
        }

        callbackMethods.put(method.getName(), method);
    }

    public CallbackMethod getCallbackMethod(String name) {
        return (CallbackMethod) callbackMethods.get(name);
    }

    public void removeCallbackMethod(String name) {
        callbackMethods.remove(name);
    }
}
