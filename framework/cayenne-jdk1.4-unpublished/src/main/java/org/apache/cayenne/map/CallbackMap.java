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
import java.util.Iterator;

import org.apache.cayenne.LifecycleListener;
import org.apache.cayenne.util.XMLEncoder;

/**
 * A generic descriptor of a set of standard lifecycle callbacks.
 * 
 * @author Andrus Adamchik
 * @since 3.0
 */
public class CallbackMap implements Serializable {

    // these int constants correspond to indexes in array in LifecycleCallbackRegistry, so
    // they must start with 0 and increment by 1.

    /**
     * An array containing all valid callbacks with each callback int value corresponding
     * to its index in the array.
     */
    public static final int[] CALLBACKS = new int[] {
            LifecycleListener.PRE_PERSIST, LifecycleListener.PRE_REMOVE,
            LifecycleListener.PRE_UPDATE, LifecycleListener.POST_PERSIST,
            LifecycleListener.POST_REMOVE, LifecycleListener.POST_UPDATE,
            LifecycleListener.POST_LOAD
    };

    protected CallbackDescriptor prePersist;
    protected CallbackDescriptor postPersist;
    protected CallbackDescriptor preUpdate;
    protected CallbackDescriptor postUpdate;
    protected CallbackDescriptor preRemove;
    protected CallbackDescriptor postRemove;
    protected CallbackDescriptor postLoad;

    public CallbackMap() {
        this.prePersist = new CallbackDescriptor(LifecycleListener.PRE_PERSIST);
        this.postPersist = new CallbackDescriptor(LifecycleListener.POST_PERSIST);
        this.preUpdate = new CallbackDescriptor(LifecycleListener.PRE_UPDATE);
        this.postUpdate = new CallbackDescriptor(LifecycleListener.POST_UPDATE);
        this.preRemove = new CallbackDescriptor(LifecycleListener.PRE_REMOVE);
        this.postRemove = new CallbackDescriptor(LifecycleListener.POST_REMOVE);
        this.postLoad = new CallbackDescriptor(LifecycleListener.POST_LOAD);
    }

    /**
     * Returns all event callbacks in a single array ordered by event type, following the
     * order in {@link CallbackMap#CALLBACKS} array.
     */
    public CallbackDescriptor[] getCallbacks() {
        return new CallbackDescriptor[] {
                prePersist, preRemove, preUpdate, postPersist, postRemove, postUpdate,
                postLoad
        };
    }

    public CallbackDescriptor getPostLoad() {
        return postLoad;
    }

    public CallbackDescriptor getPostPersist() {
        return postPersist;
    }

    public CallbackDescriptor getPostRemove() {
        return postRemove;
    }

    public CallbackDescriptor getPostUpdate() {
        return postUpdate;
    }

    public CallbackDescriptor getPrePersist() {
        return prePersist;
    }

    public CallbackDescriptor getPreRemove() {
        return preRemove;
    }

    public CallbackDescriptor getPreUpdate() {
        return preUpdate;
    }

    public void encodeCallbacksAsXML(XMLEncoder encoder) {
        printMethods(prePersist, "pre-persist", encoder);
        printMethods(postPersist, "post-persist", encoder);
        printMethods(preUpdate, "pre-update", encoder);
        printMethods(postUpdate, "post-update", encoder);
        printMethods(preRemove, "pre-remove", encoder);
        printMethods(postRemove, "post-remove", encoder);
        printMethods(postLoad, "post-load", encoder);
    }

    private static void printMethods(CallbackDescriptor descriptor, String stringCallbackName, XMLEncoder encoder) {
        for (Iterator i = descriptor.getCallbackMethods().iterator(); i.hasNext();) {
            encoder.print("<");
            encoder.print(stringCallbackName);
            encoder.print(" method-name=\"");
            encoder.print((String)i.next());
            encoder.println("\"/>");
        }
    }
}
