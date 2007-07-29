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
package org.apache.cayenne.reflect;

import org.apache.cayenne.access.MockCallingBackListener;
import org.apache.cayenne.map.MockCallingBackEntity;
import org.apache.cayenne.reflect.CallbackOnListener;

import junit.framework.TestCase;

public class CallbackOnListenerTest extends TestCase {

    public void testPublicCallbackMethod() {

        MockCallingBackListener listener = new MockCallingBackListener();
        CallbackOnListener callback = new CallbackOnListener(
                listener,
                "publicCallback",
                Object.class);

        MockCallingBackEntity e = new MockCallingBackEntity();
        callback.performCallback(e);

        // entity itself should not be called back...
        assertFalse(e.publicCallbackInvoked);
        assertFalse(e.protectedCallbackInvoked);
        assertFalse(e.privateCallbackInvoked);
        assertFalse(e.defaultCallbackInvoked);

        assertSame(e, listener.getPublicCalledbackEntity());
    }
}
