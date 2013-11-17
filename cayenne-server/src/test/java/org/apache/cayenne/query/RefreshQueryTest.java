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

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

import org.apache.cayenne.MockPersistentObject;
import org.apache.cayenne.Persistent;

public class RefreshQueryTest extends TestCase {

    public void testRefreshAllConstructor() {

        RefreshQuery q = new RefreshQuery();
        assertNull(q.getObjects());
        assertNull(q.getQuery());
        assertNull(q.getGroupKeys());
        assertTrue(q.isRefreshAll());
    }

    public void testCollectionConstructor() {
        Collection c = new ArrayList();
        c.add(new Object());
        c.add(new Object());

        RefreshQuery q = new RefreshQuery(c);
        assertSame(c, q.getObjects());
        assertNull(q.getQuery());
        assertNull(q.getGroupKeys());
        assertFalse(q.isRefreshAll());
    }

    public void testObjectConstructor() {
        Persistent p = new MockPersistentObject();

        RefreshQuery q = new RefreshQuery(p);
        assertNotNull(q.getObjects());
        assertEquals(1, q.getObjects().size());
        assertSame(p, q.getObjects().iterator().next());
        assertNull(q.getQuery());
        assertNull(q.getGroupKeys());
        assertFalse(q.isRefreshAll());
    }

    public void testQueryConstructor() {
        Query query = new MockQuery();

        RefreshQuery q = new RefreshQuery(query);
        assertNull(q.getObjects());
        assertNotNull(q.getQuery());
        assertNotSame("query must be wrapped", query, q.getQuery());
        assertNull(q.getGroupKeys());
        assertFalse(q.isRefreshAll());
    }

    public void testGroupKeysConstructor() {
        String[] groupKeys = new String[] { "a", "b" };

        RefreshQuery q = new RefreshQuery(groupKeys);
        assertNull(q.getObjects());
        assertNull(q.getQuery());
        assertSame(groupKeys, q.getGroupKeys());
    }

}
