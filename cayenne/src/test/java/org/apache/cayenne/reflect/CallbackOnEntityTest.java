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
package org.apache.cayenne.reflect;

import org.apache.cayenne.map.MockCallingBackEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CallbackOnEntityTest {

    @Test
    public void publicCallbackMethod() {
        CallbackOnEntity callback = new CallbackOnEntity(
                MockCallingBackEntity.class,
                "publicCallback");

        MockCallingBackEntity e = new MockCallingBackEntity();
        callback.performCallback(e);
        assertTrue(e.publicCallbackInvoked);
        assertFalse(e.protectedCallbackInvoked);
        assertFalse(e.privateCallbackInvoked);
        assertFalse(e.defaultCallbackInvoked);
    }

    @Test
    public void protectedCallbackMethod() {
        CallbackOnEntity callback = new CallbackOnEntity(
                MockCallingBackEntity.class,
                "protectedCallback");

        MockCallingBackEntity e = new MockCallingBackEntity();
        callback.performCallback(e);
        assertFalse(e.publicCallbackInvoked);
        assertTrue(e.protectedCallbackInvoked);
        assertFalse(e.privateCallbackInvoked);
        assertFalse(e.defaultCallbackInvoked);
    }

    @Test
    public void privateCallbackMethod() {
        CallbackOnEntity callback = new CallbackOnEntity(
                MockCallingBackEntity.class,
                "privateCallback");

        MockCallingBackEntity e = new MockCallingBackEntity();
        callback.performCallback(e);
        assertFalse(e.publicCallbackInvoked);
        assertFalse(e.protectedCallbackInvoked);
        assertTrue(e.privateCallbackInvoked);
        assertFalse(e.defaultCallbackInvoked);
    }

    @Test
    public void defaultCallbackMethod() {
        CallbackOnEntity callback = new CallbackOnEntity(
                MockCallingBackEntity.class,
                "defaultCallback");

        MockCallingBackEntity e = new MockCallingBackEntity();
        callback.performCallback(e);
        assertFalse(e.publicCallbackInvoked);
        assertFalse(e.protectedCallbackInvoked);
        assertFalse(e.privateCallbackInvoked);
        assertTrue(e.defaultCallbackInvoked);
    }

    @Test
    public void staticCallbackMethod() {
        assertThrows(IllegalArgumentException.class,
                () -> new CallbackOnEntity(MockCallingBackEntity.class, "staticCallback"));
    }
}
