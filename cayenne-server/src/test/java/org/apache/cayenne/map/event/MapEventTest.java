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

package org.apache.cayenne.map.event;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class MapEventTest {

    @Test
    public void testNoNameChange() throws Exception {
        MapEvent event = new MapEventFixture(new Object(), "someName");
        assertEquals("someName", event.getNewName());
        assertFalse(event.isNameChange());
    }

    @Test
    public void testNameChange() throws Exception {
        MapEvent event = new MapEventFixture(new Object(), "someName", "someOldName");
        assertEquals("someName", event.getNewName());
        assertTrue(event.isNameChange());
    }

    @Test
    public void testOldName() throws Exception {
        MapEvent event = new MapEventFixture(new Object(), "someName");
        assertNull(event.getOldName());

        event.setOldName("oldName");
        assertEquals("oldName", event.getOldName());
    }

    final class MapEventFixture extends MapEvent {

        String newName;

        public MapEventFixture(Object source, String newName) {
            super(source);
            this.newName = newName;
        }

        public MapEventFixture(Object source, String newName, String oldName) {
            super(source, oldName);
            this.newName = newName;
        }

        @Override
        public String getNewName() {
            return newName;
        }
    }
}
