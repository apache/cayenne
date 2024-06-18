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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistentObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LifecycleCallbackEventHandlerTest {

    @Test
    public void testDefaultListeners() {

        LifecycleCallbackEventHandler map = new LifecycleCallbackEventHandler();
        L1 l1 = new L1();
        map.addDefaultListener(l1, "callback");

        C1 c1 = new C1();
        c1.setObjectId(ObjectId.of("bogus"));

        assertEquals(0, l1.entities.size());
        map.performCallbacks(c1);
        assertEquals(1, l1.entities.size());
        assertTrue(l1.entities.contains(c1));
    }

    @Test
    public void testDefaultListenersCallbackOrder() {

        LifecycleCallbackEventHandler map = new LifecycleCallbackEventHandler();
        L2 l1 = new L2();
        map.addListener(C1.class, l1, "callback");

        L2 l2 = new L2();
        map.addDefaultListener(l2, "callback");

        C1 c1 = new C1();
        c1.setObjectId(ObjectId.of("bogus"));

        map.performCallbacks(c1);
        assertEquals(1, l1.callbackTimes.size());
        assertEquals(1, l2.callbackTimes.size());

        Long t1 = (Long) l1.callbackTimes.get(0);
        Long t2 = (Long) l2.callbackTimes.get(0);
        assertTrue(t2.compareTo(t1) < 0);
    }

    @Test
    public void testCallbackOnSuperclass() {

        LifecycleCallbackEventHandler map = new LifecycleCallbackEventHandler();
        map.addListener(C1.class, "c1Callback");

        C3 subclass = new C3();
        subclass.setObjectId(ObjectId.of("bogusSubclass"));

        assertEquals(0, subclass.callbacks.size());
        map.performCallbacks(subclass);
        assertEquals(1, subclass.callbacks.size());
    }

    @Test
    public void testCallbackOnSuperclassWithSublcassOverrides() {

        LifecycleCallbackEventHandler map = new LifecycleCallbackEventHandler();
        map.addListener(C1.class, "c1Callback");

        C4 subclass = new C4();
        subclass.setObjectId(ObjectId.of("bogus"));

        assertEquals(0, subclass.callbacks.size());
        map.performCallbacks(subclass);
        assertEquals(1, subclass.callbacks.size());
        assertEquals("c4Callback", subclass.callbacks.get(0));
    }

    @Test
    public void testCallbackOrderInInheritanceHierarchy() {

        LifecycleCallbackEventHandler map = new LifecycleCallbackEventHandler();
        map.addListener(C2.class, "c2Callback");
        map.addListener(C1.class, "c1Callback");

        C2 c = new C2();
        c.setObjectId(ObjectId.of("bogus"));

        assertTrue(c.callbacks.isEmpty());
        map.performCallbacks(c);
        assertEquals(2, c.callbacks.size());

        // superclass callbacks should be invoked first
        assertEquals("c1Callback", c.callbacks.get(0));
        assertEquals("c2Callback", c.callbacks.get(1));
    }

    static class C1 extends PersistentObject {

        protected List callbacks = new ArrayList();

        void c1Callback() {
            callbacks.add("c1Callback");
        }
    }

    static class C2 extends C1 {

        void c2Callback() {
            callbacks.add("c2Callback");
        }
    }

    static class C3 extends C1 {

    }

    static class C4 extends C1 {

        @Override
        void c1Callback() {
            callbacks.add("c4Callback");
        }
    }

    static class L1 {

        protected List entities = new ArrayList();

        void callback(Object entity) {
            entities.add(entity);
        }
    }

    static class L2 {

        protected List callbackTimes = new ArrayList();

        void callback(Object entity) {
            callbackTimes.add(System.currentTimeMillis());
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
