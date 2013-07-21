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

package org.apache.cayenne.access.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.apache.cayenne.util.Util;

/**
 * A test case checking Cayenne handling of 1.5 Enums.
 * 
 */
public class EnumTest extends TestCase {

    public void testSerializabilityWithHessianStandalone() throws Exception {
        MockEnum before = MockEnum.a;

        // test standalone
        Object after = HessianUtil.cloneViaClientServerSerialization(
                before,
                new EntityResolver());
        assertNotNull(after);
        assertSame(before, after);
    }

    public void testSerializabilityWithHessianInTheMap() throws Exception {
        // test in the Map
        Map<String, MockEnum> map = new HashMap<String, MockEnum>();
        map.put("a", MockEnum.b);

        Map after = (Map) HessianUtil.cloneViaClientServerSerialization(
                (Serializable) map,
                new EntityResolver());
        assertNotNull(map);
        assertSame(MockEnum.b, after.get("a"));

    }

    public void testSerializabilityWithHessianObjectProperty() throws Exception {
        // test object property
        MockEnumHolder object = new MockEnumHolder();
        object.setMockEnum(MockEnum.b);

        MockEnumHolder after = (MockEnumHolder) HessianUtil
                .cloneViaClientServerSerialization(object, new EntityResolver());
        assertNotNull(after);
        assertSame(MockEnum.b, after.getMockEnum());
    }

    public void testSerializabilityWithHessianObjectPropertyInAList() throws Exception {

        // test that Enum properties are serialized properly...

        MockEnumHolder o1 = new MockEnumHolder();
        o1.setMockEnum(MockEnum.b);

        MockEnumHolder o2 = new MockEnumHolder();
        o2.setMockEnum(MockEnum.c);

        ArrayList<MockEnumHolder> l = new ArrayList<MockEnumHolder>();
        l.add(o1);
        l.add(o2);

        ArrayList ld = (ArrayList) HessianUtil.cloneViaClientServerSerialization(
                l,
                new EntityResolver());
        assertEquals(2, ld.size());

        MockEnumHolder o1d = (MockEnumHolder) ld.get(0);
        MockEnumHolder o2d = (MockEnumHolder) ld.get(1);
        assertSame(MockEnum.b, o1d.getMockEnum());
        assertSame(MockEnum.c, o2d.getMockEnum());
    }

    public void testSerializability() throws Exception {
        MockEnum before = MockEnum.c;
        Object object = Util.cloneViaSerialization(before);
        assertNotNull(object);
        assertSame(before, object);
    }
}
