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

import org.apache.cayenne.util.XMLEncoder;

/**
 * A generic descriptor of a set of standard lifecycle callbacks.
 * 
 * @since 3.0
 */
public class CallbackMap implements Serializable {

    protected CallbackDescriptor[] callbacks = new CallbackDescriptor[LifecycleEvent
            .values().length];

    public CallbackMap() {

        LifecycleEvent[] events = LifecycleEvent.values();
        callbacks = new CallbackDescriptor[events.length];

        for (int i = 0; i < events.length; i++) {
            callbacks[i] = new CallbackDescriptor(events[i]);
        }
    }

    /**
     * Returns all event callbacks as an array ordered by event type.
     */
    public CallbackDescriptor[] getCallbacks() {
        return callbacks;
    }

    /**
     * @param callbackType callback type id
     * @return CallbackDescriptor for the specified callback type id
     */
    public CallbackDescriptor getCallbackDescriptor(LifecycleEvent callbackType) {
        return callbacks[callbackType.ordinal()];
    }

    public CallbackDescriptor getPostLoad() {
        return callbacks[LifecycleEvent.POST_LOAD.ordinal()];
    }

    public CallbackDescriptor getPostPersist() {
        return callbacks[LifecycleEvent.POST_PERSIST.ordinal()];
    }

    public CallbackDescriptor getPostRemove() {
        return callbacks[LifecycleEvent.POST_REMOVE.ordinal()];
    }

    public CallbackDescriptor getPostUpdate() {
        return callbacks[LifecycleEvent.POST_UPDATE.ordinal()];
    }

    public CallbackDescriptor getPostAdd() {
        return callbacks[LifecycleEvent.POST_ADD.ordinal()];
    }
    
    public CallbackDescriptor getPrePersist() {
        return callbacks[LifecycleEvent.PRE_PERSIST.ordinal()];
    }

    public CallbackDescriptor getPreRemove() {
        return callbacks[LifecycleEvent.PRE_REMOVE.ordinal()];
    }

    public CallbackDescriptor getPreUpdate() {
        return callbacks[LifecycleEvent.PRE_UPDATE.ordinal()];
    }

    public void encodeCallbacksAsXML(XMLEncoder encoder) {
        printMethods(getPostAdd(), "post-add", encoder);
        printMethods(getPrePersist(), "pre-persist", encoder);
        printMethods(getPostPersist(), "post-persist", encoder);
        printMethods(getPreUpdate(), "pre-update", encoder);
        printMethods(getPostUpdate(), "post-update", encoder);
        printMethods(getPreRemove(), "pre-remove", encoder);
        printMethods(getPostRemove(), "post-remove", encoder);
        printMethods(getPostLoad(), "post-load", encoder);
    }

    private static void printMethods(
            CallbackDescriptor descriptor,
            String stringCallbackName,
            XMLEncoder encoder) {

        for (String methodName : descriptor.getCallbackMethods()) {
            encoder.print("<");
            encoder.print(stringCallbackName);
            encoder.print(" method-name=\"");
            encoder.print(methodName);
            encoder.println("\"/>");
        }
    }
}
