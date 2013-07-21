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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A mapping descriptor of a single callback event.
 * 
 * @since 3.0
 */
public class CallbackDescriptor implements Serializable {

    protected LifecycleEvent callbackType;
    protected Set<String> callbackMethods;

    public CallbackDescriptor(LifecycleEvent callbackType) {
        setCallbackType(callbackType);
        this.callbackMethods = new LinkedHashSet<String>(3);
    }

    /**
     * Removes all callback methods.
     */
    public void clear() {
        callbackMethods.clear();
    }

    /**
     * Returns all callback methods for this callback event.
     * 
     * @return Returns all callback methods
     */
    public Collection<String> getCallbackMethods() {
        return Collections.unmodifiableCollection(callbackMethods);
    }

    public void addCallbackMethod(String methodName) {
        callbackMethods.add(methodName);
    }

    public void removeCallbackMethod(String methodName) {
        callbackMethods.remove(methodName);
    }

    public LifecycleEvent getCallbackType() {
        return callbackType;
    }

    void setCallbackType(LifecycleEvent callbackType) {
        this.callbackType = callbackType;
    }

    /**
     * moves specified callback method to the specified position
     * 
     * @param callbackMethod callbacm method name (should exist)
     * @param destinationIndex destinationi index (should be valid)
     * @return true if any changes were made
     */
    public boolean moveMethod(String callbackMethod, int destinationIndex) {
        List<String> callbackMethodsList = new ArrayList<String>(callbackMethods);
        int currentIndex = callbackMethodsList.indexOf(callbackMethod);
        if (currentIndex < 0)
            throw new IllegalArgumentException("Unknown callback method: "
                    + callbackMethod);

        boolean changed = false;

        if (destinationIndex > currentIndex) {
            callbackMethodsList.add(destinationIndex + 1, callbackMethod);
            callbackMethodsList.remove(currentIndex);
            changed = true;
        }
        else if (destinationIndex < currentIndex) {
            callbackMethodsList.add(destinationIndex, callbackMethod);
            callbackMethodsList.remove(currentIndex + 1);
            changed = true;
        }

        if (changed) {
            callbackMethods.clear();
            callbackMethods.addAll(callbackMethodsList);
        }

        return changed;
    }

    /**
     * Replaces a callback method at the specified position
     * 
     * @param index callback method index
     * @param method new callback method
     */
    public void setCallbackMethodAt(int index, String method) {
        List<String> callbackMethodsList = new ArrayList<String>(callbackMethods);
        callbackMethodsList.set(index, method);
        callbackMethods.clear();
        callbackMethods.addAll(callbackMethodsList);
    }
}
