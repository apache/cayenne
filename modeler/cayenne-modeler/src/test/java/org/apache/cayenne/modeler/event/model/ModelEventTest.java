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

package org.apache.cayenne.modeler.event.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;


public class ModelEventTest {

    @Test
    public void testConstructor() {
        Object src = new Object();
        ModelEvent e = new TestModelEvent(src, ModelEvent.Type.CHANGE, "n", null);
        assertSame(src, e.getSource());
    }

    @Test
    public void testType() {
        ModelEvent change = new TestModelEvent(new Object(), ModelEvent.Type.CHANGE, "n", null);
        assertEquals(ModelEvent.Type.CHANGE, change.getType());

        ModelEvent add = new TestModelEvent(new Object(), ModelEvent.Type.ADD, "n", null);
        assertEquals(ModelEvent.Type.ADD, add.getType());
    }

    @Test
    public void testNoNameChange() {
        ModelEvent event = new TestModelEvent(new Object(), ModelEvent.Type.CHANGE, "someName", null);
        assertEquals("someName", event.getNewName());
        assertFalse(event.isNameChange());
        assertNull(event.getOldName());
    }

    @Test
    public void testNameChange() {
        ModelEvent event = new TestModelEvent(new Object(), ModelEvent.Type.CHANGE, "someName", "someOldName");
        assertEquals("someName", event.getNewName());
        assertEquals("someOldName", event.getOldName());
        assertTrue(event.isNameChange());
    }

    static final class TestModelEvent extends ModelEvent {

        private final String newName;

        TestModelEvent(Object source, Type type, String newName, String oldName) {
            super(source, type, oldName);
            this.newName = newName;
        }

        @Override
        public String getNewName() {
            return newName;
        }
    }
}
