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

package org.apache.cayenne.map;

import junit.framework.TestCase;

import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.apache.cayenne.util.Util;

public class ClientObjectRelationshipTest extends TestCase {

    public void testSerializability() throws Exception {

        ClientObjRelationship r1 = new ClientObjRelationship("r1", "rr1", true, true);
        ClientObjRelationship r2 = (ClientObjRelationship) Util.cloneViaSerialization(r1);
        assertEquals(r1.getName(), r2.getName());
        assertEquals(r1.getReverseRelationship(), r2.getReverseRelationship());
        assertEquals(r1.isToMany(), r2.isToMany());
        assertEquals(r1.isReadOnly(), r2.isReadOnly());

        ClientObjRelationship r3 = new ClientObjRelationship("r3", null, false, false);
        ClientObjRelationship r4 = (ClientObjRelationship) Util.cloneViaSerialization(r3);
        assertEquals(r3.getName(), r4.getName());
        assertNull(r4.getReverseRelationship());
        assertEquals(r3.isToMany(), r4.isToMany());
        assertEquals(r3.isReadOnly(), r4.isReadOnly());
    }

    public void testSerializabilityViaHessian() throws Exception {

        ClientObjRelationship r1 = new ClientObjRelationship("r1", "rr1", true, true);
        ClientObjRelationship r2 = (ClientObjRelationship) HessianUtil
                .cloneViaClientServerSerialization(r1, new EntityResolver());
        assertEquals(r1.getName(), r2.getName());
        assertEquals(r1.getReverseRelationship(), r2.getReverseRelationship());
        assertEquals(r1.isToMany(), r2.isToMany());
        assertEquals(r1.isReadOnly(), r2.isReadOnly());

        ClientObjRelationship r3 = new ClientObjRelationship("r3", null, false, false);
        ClientObjRelationship r4 = (ClientObjRelationship) HessianUtil
                .cloneViaClientServerSerialization(r3, new EntityResolver());
        assertEquals(r3.getName(), r4.getName());
        assertNull(r4.getReverseRelationship());
        assertEquals(r3.isToMany(), r4.isToMany());
        assertEquals(r3.isReadOnly(), r4.isReadOnly());
    }
}
