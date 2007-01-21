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

package org.apache.cayenne.util;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.apache.cayenne.testdo.mt.ClientMtTable1;

public class PersistentObjectListTest extends TestCase {

    public void testFaultHessianSerialization() throws Exception {

        ClientMtTable1 owner = new ClientMtTable1();
        owner.setPersistenceState(PersistenceState.COMMITTED);
        owner.setGlobalAttribute1("a");
        PersistentObjectList list = new PersistentObjectList(owner, "x");

        assertTrue(list.isFault());

        Object deserialized = HessianUtil.cloneViaServerClientSerialization(
                list,
                new EntityResolver());

        // faults are writtens as nulls
        assertNull(deserialized);
    }

    public void testResolvedHessianSerialization() throws Exception {

        ClientMtTable1 owner = new ClientMtTable1();
        owner.setPersistenceState(PersistenceState.COMMITTED);
        owner.setGlobalAttribute1("a");
        PersistentObjectList list = new PersistentObjectList(owner, "x");

        List objects = new ArrayList();
        objects.add("a");
        objects.add("b");
        list.setObjectList(objects);
        assertFalse(list.isFault());

        Object deserialized = HessianUtil.cloneViaServerClientSerialization(
                list,
                new EntityResolver());

        assertNotNull(deserialized);
        assertTrue(
                "Invalid deserialized: " + deserialized.getClass().getName(),
                deserialized instanceof PersistentObjectList);
        PersistentObjectList dlist = (PersistentObjectList) deserialized;
        assertFalse(dlist.isFault());
        assertNotNull(dlist.getRelationshipOwner());
        assertEquals("x", dlist.getRelationshipName());
        assertEquals(objects, dlist.objectList);
    }
}
