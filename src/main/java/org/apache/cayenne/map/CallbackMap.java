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

/**
 * A generic descriptor of a set of standard lifecycle callbacks.
 * 
 * @author Andrus Adamchik
 * @since 3.0
 */
public class CallbackMap implements Serializable {

    // these int constants correspond to indexes in array in LifecycleCallbackRegistry, so
    // they must start with 0 and increment by 1.

    public static final int PRE_PERSIST = 0;
    public static final int PRE_REMOVE = 1;
    public static final int PRE_UPDATE = 2;
    public static final int POST_PERSIST = 3;
    public static final int POST_REMOVE = 4;
    public static final int POST_UPDATE = 5;
    public static final int POST_LOAD = 6;

    /**
     * An array containing all valid callbacks with each callback int value corresponding
     * to its index in the array.
     */
    public static final int[] CALLBACKS = new int[] {
            CallbackMap.PRE_PERSIST, CallbackMap.PRE_REMOVE, CallbackMap.PRE_UPDATE,
            CallbackMap.POST_PERSIST, CallbackMap.POST_REMOVE, CallbackMap.POST_UPDATE,
            CallbackMap.POST_LOAD
    };

    protected CallbackDescriptor prePersist;
    protected CallbackDescriptor postPersist;
    protected CallbackDescriptor preUpdate;
    protected CallbackDescriptor postUpdate;
    protected CallbackDescriptor preRemove;
    protected CallbackDescriptor postRemove;
    protected CallbackDescriptor postLoad;

    public CallbackMap() {
        this.prePersist = new CallbackDescriptor(CallbackMap.PRE_PERSIST);
        this.postPersist = new CallbackDescriptor(CallbackMap.POST_PERSIST);
        this.preUpdate = new CallbackDescriptor(CallbackMap.PRE_UPDATE);
        this.postUpdate = new CallbackDescriptor(CallbackMap.POST_UPDATE);
        this.preRemove = new CallbackDescriptor(CallbackMap.PRE_REMOVE);
        this.postRemove = new CallbackDescriptor(CallbackMap.POST_REMOVE);
        this.postLoad = new CallbackDescriptor(CallbackMap.POST_LOAD);
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
}
