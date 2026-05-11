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
package org.apache.cayenne.query;

import org.apache.cayenne.MockPersistentObject;
import org.apache.cayenne.Persistent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RefreshQueryTest {

    @Test
    public void refreshAllConstructor() {

        RefreshQuery q = new RefreshQuery();
        assertNull(q.getObjects());
        assertNull(q.getQuery());
        assertNull(q.getGroupKeys());
        assertTrue(q.isRefreshAll());
    }

    @Test
    public void collectionConstructor() {
        Collection c = new ArrayList();
        c.add(new Object());
        c.add(new Object());

        RefreshQuery q = new RefreshQuery(c);
        assertSame(c, q.getObjects());
        assertNull(q.getQuery());
        assertNull(q.getGroupKeys());
        assertFalse(q.isRefreshAll());
    }

    @Test
    public void objectConstructor() {
        Persistent p = new MockPersistentObject();

        RefreshQuery q = new RefreshQuery(p);
        assertNotNull(q.getObjects());
        assertEquals(1, q.getObjects().size());
        assertSame(p, q.getObjects().iterator().next());
        assertNull(q.getQuery());
        assertNull(q.getGroupKeys());
        assertFalse(q.isRefreshAll());
    }

    @Test
    public void queryConstructor() {
        Query query = new MockQuery();

        RefreshQuery q = new RefreshQuery(query);
        assertNull(q.getObjects());
        assertNotNull(q.getQuery());
        assertNotSame(query, q.getQuery(), "query must be wrapped");
        assertNull(q.getGroupKeys());
        assertFalse(q.isRefreshAll());
    }

    @Test
    public void groupKeysConstructor() {
        String[] groupKeys = new String[] { "a", "b" };

        RefreshQuery q = new RefreshQuery(groupKeys);
        assertNull(q.getObjects());
        assertNull(q.getQuery());
        assertSame(groupKeys, q.getGroupKeys());
    }

}
