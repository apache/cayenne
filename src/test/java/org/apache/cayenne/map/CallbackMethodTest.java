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

import junit.framework.TestCase;

public class CallbackMethodTest extends TestCase {

    public void testAddCallbackEvent() {
        CallbackMethod m = new CallbackMethod();
        assertFalse(m.supportsCallbackEvent(LifecycleEventCallback.POST_LOAD));
        m.addCallbackEvent(LifecycleEventCallback.POST_LOAD);
        assertTrue(m.supportsCallbackEvent(LifecycleEventCallback.POST_LOAD));
        assertFalse(m.supportsCallbackEvent(LifecycleEventCallback.PRE_PERSIST));
    }

    public void testAddInvalidCallbackEvent() {
        CallbackMethod m = new CallbackMethod();

        try {
            m.addCallbackEvent(100);
        }
        catch (IllegalArgumentException e) {
            // expected
        }

        try {
            m.addCallbackEvent(-1);
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testAddDuplicateCallbackEvent() {
        CallbackMethod m = new CallbackMethod();

        m.addCallbackEvent(LifecycleEventCallback.POST_LOAD);

        try {
            m.addCallbackEvent(LifecycleEventCallback.POST_LOAD);
        }
        catch (IllegalArgumentException e) {
            // expected
        }

        m.addCallbackEvent(LifecycleEventCallback.PRE_REMOVE);
    }
}
