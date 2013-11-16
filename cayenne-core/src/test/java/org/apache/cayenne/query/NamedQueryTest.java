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

package org.apache.cayenne.query;

import junit.framework.TestCase;

import org.apache.cayenne.util.Util;

public class NamedQueryTest extends TestCase {

    public void testName() {
        NamedQuery query = new NamedQuery("abc");

        assertEquals("abc", query.getName());
        query.setName("123");
        assertEquals("123", query.getName());
    }

    public void testQueryName() {
        NamedQuery query = new NamedQuery("abc");
        assertEquals("abc", query.getName());
    }

    public void testSerializability() throws Exception {
        NamedQuery o = new NamedQuery("abc");
        Object clone = Util.cloneViaSerialization(o);

        assertTrue(clone instanceof NamedQuery);
        NamedQuery c1 = (NamedQuery) clone;

        assertNotSame(o, c1);
        assertEquals(o.getName(), c1.getName());
    }

    /**
     * Proper 'equals' and 'hashCode' implementations are important when mapping
     * results obtained in a QueryChain back to the query.
     */
    public void testEquals() throws Exception {
        NamedQuery q1 = new NamedQuery("abc", new String[] { "a", "b" }, new Object[] { "1", "2" });

        NamedQuery q2 = new NamedQuery("abc", new String[] { "a", "b" }, new Object[] { "1", "2" });

        NamedQuery q3 = new NamedQuery("abc", new String[] { "a", "b" }, new Object[] { "1", "3" });

        NamedQuery q4 = new NamedQuery("123", new String[] { "a", "b" }, new Object[] { "1", "2" });

        assertTrue(q1.equals(q2));
        assertEquals(q1.hashCode(), q2.hashCode());

        assertFalse(q1.equals(q3));
        assertFalse(q1.hashCode() == q3.hashCode());

        assertFalse(q1.equals(q4));
        assertFalse(q1.hashCode() == q4.hashCode());
    }
}
