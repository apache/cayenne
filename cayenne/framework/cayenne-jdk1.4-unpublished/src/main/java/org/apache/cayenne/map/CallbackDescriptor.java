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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A mapping descriptor of a single callback event.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class CallbackDescriptor implements Serializable {
    
   

    protected int callbackType;
    protected Set callbackMethods;

    public CallbackDescriptor(int callbackType) {
        setCallbackType(callbackType);
        this.callbackMethods = new HashSet(3);
    }

    /**
     * Removes all callback methods.
     */
    public void clear() {
        callbackMethods.clear();
    }

    /**
     * Returns all callback methods for this callback event.
     */
    public Collection getCallbackMethods() {
        return Collections.unmodifiableCollection(callbackMethods);
    }

    public void addCallbackMethod(String methodName) {
        callbackMethods.add(methodName);
    }

    public void removeCallbackMethod(String methodName) {
        callbackMethods.remove(methodName);
    }

    public int getCallbackType() {
        return callbackType;
    }

    void setCallbackType(int callbackType) {
        if (Arrays.binarySearch(CallbackMap.CALLBACKS, callbackType) != callbackType) {
            throw new IllegalArgumentException("Invalid callback: " + callbackType);
        }

        this.callbackType = callbackType;
    }
}
