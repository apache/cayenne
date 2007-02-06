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

/**
 * Defines a callback operation.
 * 
 * @author Andrus Adamchik
 */
public interface LifecycleEventCallback {

    // these int constants correspond to indexes in array in EntityResolver, so they must
    // start with 0 and increment by 1.

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
            LifecycleEventCallback.PRE_PERSIST, LifecycleEventCallback.PRE_REMOVE,
            LifecycleEventCallback.PRE_UPDATE, LifecycleEventCallback.POST_PERSIST,
            LifecycleEventCallback.POST_REMOVE, LifecycleEventCallback.POST_UPDATE,
            LifecycleEventCallback.POST_LOAD
    };

    void performCallback(Object entity);
}
