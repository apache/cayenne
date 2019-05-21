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
package org.apache.cayenne;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class ObjectIdTest {

    @Test
    public void testHessianSerializabilityTemp() throws Exception {
        ObjectId temp1 = ObjectId.of("e");

        // make sure hashcode is resolved
        int h = temp1.hashCode();
        assertEquals(h, temp1.hashCode());
        assertTrue(temp1.hashCode() != 0);

        ObjectId temp2 = (ObjectId) HessianUtil.cloneViaClientServerSerialization(temp1, new EntityResolver());

        // make sure hashCode is reset to 0
        assertEquals(h, temp2.hashCode());

        assertTrue(temp1.isTemporary());
        assertNotSame(temp1, temp2);
        assertEquals(temp1, temp2);
    }

    @Test
    public void testHessianSerializabilityPerm() throws Exception {
        ObjectId perm1 = ObjectId.of("e", "a", "b");

        // make sure hashcode is resolved
        int h = perm1.hashCode();
        assertEquals(h, perm1.hashCode());
        assertTrue(perm1.hashCode() != 0);

        ObjectId perm2 = (ObjectId) HessianUtil.cloneViaClientServerSerialization(perm1, new EntityResolver());

        assertEquals(h, perm2.hashCode());

        assertFalse(perm2.isTemporary());
        assertNotSame(perm1, perm2);
        assertEquals(perm1, perm2);
    }

    @Test
    public void testHessianSerializabilityPerm1() throws Exception {
        // test serializing an id created with unmodifiable map

        Map<String, Object> id = Collections.unmodifiableMap(Collections.singletonMap("a", "b"));
        ObjectId perm1 = ObjectId.of("e", id);
        ObjectId perm2 = (ObjectId) HessianUtil.cloneViaClientServerSerialization(perm1, new EntityResolver());

        assertFalse(perm2.isTemporary());
        assertNotSame(perm1, perm2);
        assertEquals(perm1, perm2);
    }
}
