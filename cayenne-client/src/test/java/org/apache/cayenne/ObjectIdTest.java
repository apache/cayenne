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
package org.apache.cayenne;

import java.util.Collections;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.service.HessianUtil;

public class ObjectIdTest extends TestCase {

    public void testHessianSerializabilityTemp() throws Exception {
        ObjectId temp1 = new ObjectId("e");

        // make sure hashcode is resolved
        int h = temp1.hashCode();
        assertEquals(h, temp1.hashCode);
        assertTrue(temp1.hashCode != 0);

        ObjectId temp2 = (ObjectId) HessianUtil.cloneViaClientServerSerialization(temp1, new EntityResolver());

        // make sure hashCode is reset to 0
        assertTrue(temp2.hashCode == 0);

        assertTrue(temp1.isTemporary());
        assertNotSame(temp1, temp2);
        assertEquals(temp1, temp2);
    }

    public void testHessianSerializabilityPerm() throws Exception {
        ObjectId perm1 = new ObjectId("e", "a", "b");

        // make sure hashcode is resolved
        int h = perm1.hashCode();
        assertEquals(h, perm1.hashCode);
        assertTrue(perm1.hashCode != 0);

        ObjectId perm2 = (ObjectId) HessianUtil.cloneViaClientServerSerialization(perm1, new EntityResolver());

        // make sure hashCode is reset to 0
        assertTrue(perm2.hashCode == 0);

        assertFalse(perm2.isTemporary());
        assertNotSame(perm1, perm2);
        assertEquals(perm1, perm2);
    }

    public void testHessianSerializabilityPerm1() throws Exception {
        // test serializing an id created with unmodifiable map

        Map id = Collections.unmodifiableMap(Collections.singletonMap("a", "b"));
        ObjectId perm1 = new ObjectId("e", id);
        ObjectId perm2 = (ObjectId) HessianUtil.cloneViaClientServerSerialization(perm1, new EntityResolver());

        assertFalse(perm2.isTemporary());
        assertNotSame(perm1, perm2);
        assertEquals(perm1, perm2);
    }
}
