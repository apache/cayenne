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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * @deprecated since 3.0
 */
public class CayenneMapTest extends TestCase {

    protected CayenneMapEntry makeEntry() {
        return new CayenneMapEntry() {

            protected Object parent;

            public String getName() {
                return "abc";
            }

            public Object getParent() {
                return parent;
            }

            public void setParent(Object parent) {
                this.parent = parent;
            }
        };
    }

    public void testConstructor1() throws Exception {
        Object o1 = new Object();
        String k1 = "123";
        Map map = new HashMap();
        map.put(k1, o1);
        CayenneMap cm = new CayenneMap(null, map);
        assertSame(o1, cm.get(k1));
    }

    public void testConstructor2() throws Exception {
        Object parent = new Object();
        CayenneMapEntry o1 = makeEntry();
        String k1 = "123";
        Map map = new HashMap();
        map.put(k1, o1);
        CayenneMap cm = new CayenneMap(parent, map);
        assertSame(o1, cm.get(k1));
        assertSame(parent, o1.getParent());
    }

    public void testPut() throws Exception {
        Object parent = new Object();
        CayenneMapEntry o1 = makeEntry();
        String k1 = "123";
        CayenneMap cm = new CayenneMap(parent);
        cm.put(k1, o1);
        assertSame(o1, cm.get(k1));
        assertSame(parent, o1.getParent());
    }

    public void testParent() throws Exception {
        Object parent = new Object();
        CayenneMap cm = new CayenneMap(null);
        assertNull(cm.getParent());
        cm.setParent(parent);
        assertSame(parent, cm.getParent());
    }

    public void testSerializability() throws Exception {
        String parent = "abcde";
        CayenneMap cm = new CayenneMap(parent);

        CayenneMap d1 = (CayenneMap) Util.cloneViaSerialization(cm);
        assertEquals(cm, d1);
        assertEquals(parent, d1.getParent());

        cm.put("a", "b");
        cm.values();

        CayenneMap d2 = (CayenneMap) Util.cloneViaSerialization(cm);
        assertEquals(cm, d2);
        assertEquals(parent, d2.getParent());
    }
}
