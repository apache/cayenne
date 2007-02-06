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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A callback method of an {@link EntityListener}. CallbackMethod can be associated with
 * one or more callback events.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class CallbackMethod {

    protected String name;
    protected Set callbackEvents;

    public CallbackMethod() {
        callbackEvents = new HashSet(3);
    }

    public CallbackMethod(String name) {
        this.name = name;
    }

    /**
     * Returns callback method name.
     */
    public String getName() {
        return name;
    }

    public void setName(String methodName) {
        this.name = methodName;
    }

    /**
     * Returns all supported callback events. The collection contains java.lang.Integer
     * instances corresponding to callback events defined in
     * {@link LifecycleEventCallback#CALLBACKS}.
     */
    public Collection getCallbackEvents() {
        return Collections.unmodifiableCollection(callbackEvents);
    }

    public void addCallbackEvent(int eventType) {

        if (Arrays.binarySearch(LifecycleEventCallback.CALLBACKS, eventType) != eventType) {
            throw new IllegalArgumentException("Invalid callback: " + eventType);
        }

        if (!callbackEvents.add(new Integer(eventType))) {
            throw new IllegalArgumentException("Duplicate callback: " + eventType);
        }
    }

    public void removeCallbackEvent(int eventType) {
        callbackEvents.remove(new Integer(eventType));
    }

    public boolean supportsCallbackEvent(int eventType) {
        return callbackEvents.contains(new Integer(eventType));
    }
}
