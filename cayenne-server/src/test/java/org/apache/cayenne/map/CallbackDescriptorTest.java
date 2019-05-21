/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CallbackDescriptorTest {

    @Test
    public void testConstructor() {
        CallbackDescriptor m = new CallbackDescriptor(LifecycleEvent.POST_LOAD);
        assertEquals(LifecycleEvent.POST_LOAD, m.getCallbackType());
    }

    @Test
    public void testAddCallbackMethod() {
        CallbackDescriptor m = new CallbackDescriptor(LifecycleEvent.POST_ADD);
        assertEquals(0, m.getCallbackMethods().size());
        m.addCallbackMethod("a");
        assertEquals(1, m.getCallbackMethods().size());
        m.addCallbackMethod("b");
        assertEquals(2, m.getCallbackMethods().size());
        m.addCallbackMethod("a");
        assertEquals(2, m.getCallbackMethods().size());
    }
}
