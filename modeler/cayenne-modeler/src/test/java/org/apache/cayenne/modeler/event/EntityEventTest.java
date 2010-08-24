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

package org.apache.cayenne.modeler.event;

import junit.framework.TestCase;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.event.EntityEvent;

/**
 */
public class EntityEventTest extends TestCase {

    public void testConstructor1() throws Exception {
        Object src = new Object();
        Entity d = new DbEntity("abc");
        EntityEvent e = new EntityEvent(src, d);

        assertSame(src, e.getSource());
        assertSame(d, e.getEntity());
    }

    public void testConstructor2() throws Exception {
        Object src = new Object();
        Entity d = new DbEntity("abc");
        EntityEvent e = new EntityEvent(src, d, "oldname");

        assertSame(src, e.getSource());
        assertSame(d, e.getEntity());
        assertEquals("oldname", e.getOldName());
    }

    public void testEntity() throws Exception {
        Object src = new Object();
        Entity d = new DbEntity("abc");
        EntityEvent e = new EntityEvent(src, null);

        e.setEntity(d);
        assertSame(d, e.getEntity());
    }

    public void testNameChange1() throws Exception {
        Entity d = new DbEntity("abc");
        EntityEvent e = new EntityEvent(new Object(), d, "xyz");
        assertEquals(d.getName(), e.getNewName());
        assertTrue(e.isNameChange());
    }
    
	public void testNameChange2() throws Exception {
		Entity d = new DbEntity("abc");
		EntityEvent e = new EntityEvent(new Object(), d, "abc");
		assertEquals(d.getName(), e.getNewName());
		assertFalse(e.isNameChange());
	}
}
