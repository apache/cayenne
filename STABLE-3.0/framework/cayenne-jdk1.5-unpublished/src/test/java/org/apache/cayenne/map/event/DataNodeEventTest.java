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

package org.apache.cayenne.map.event;

import junit.framework.TestCase;

import org.apache.cayenne.access.DataNode;

/**
 */
public class DataNodeEventTest extends TestCase {

    public void testNewName() throws Exception {
        MapEvent event = new DataNodeEvent(new Object(), new DataNode("someName"));
        assertEquals("someName", event.getNewName());
    }

    public void testNoNameChange() throws Exception {
        MapEvent event = new DataNodeEvent(new Object(), new DataNode("someName"));
        assertFalse(event.isNameChange());
        
        event.setOldName("someOldName");
        assertTrue(event.isNameChange());
    }

    public void testNameChange() throws Exception {
        MapEvent event = new DataNodeEvent(
                new Object(),
                new DataNode("someName"),
                "someOldName");
        assertEquals("someName", event.getNewName());
        assertTrue(event.isNameChange());
    }

}
