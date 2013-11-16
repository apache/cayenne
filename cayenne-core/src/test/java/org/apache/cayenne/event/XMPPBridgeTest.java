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

package org.apache.cayenne.event;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 */
public class XMPPBridgeTest extends TestCase {

    public void testEventSerialization() throws Exception {
        Map info = new HashMap();
        info.put("a", "b");
        CayenneEvent e = new CayenneEvent(this, this, info);

        String string = XMPPBridge.serializeToString(e);
        assertNotNull(string);

        Object copy = XMPPBridge.deserializeFromString(string);
        assertNotNull(copy);
        assertTrue(copy instanceof CayenneEvent);

        CayenneEvent e2 = (CayenneEvent) copy;
        assertEquals(info, e2.getInfo());
        assertNull(e2.getPostedBy());
        assertNull(e2.getSource());
    }
}
