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

package org.apache.cayenne.remote;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RemoteSessionTest {

    @Test
    public void testConstructor1() {
        RemoteSession descriptor = new RemoteSession("abc");
        assertEquals("abc", descriptor.getSessionId());
        assertFalse(descriptor.isServerEventsEnabled());
    }

    @Test
    public void testConstructor2() {
        RemoteSession descriptor = new RemoteSession("abc", "factory", null);
        assertEquals("abc", descriptor.getSessionId());
        assertTrue(descriptor.isServerEventsEnabled());
    }

    @Test
    public void testHashCode() {
        RemoteSession d1 = new RemoteSession("1");
        RemoteSession d2 = new RemoteSession("1");

        assertEquals(d1.hashCode(), d1.hashCode());
        assertEquals(d1.hashCode(), d2.hashCode());

        d2.setName("some name");
        assertEquals(d1.hashCode(), d2.hashCode());

        RemoteSession d3 = new RemoteSession("2");
        assertFalse(d1.hashCode() == d3.hashCode());
    }
}
