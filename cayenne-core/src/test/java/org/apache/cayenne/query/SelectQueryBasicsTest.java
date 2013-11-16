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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.ExpressionParameter;
import org.apache.cayenne.testdo.testmap.Artist;

public class SelectQueryBasicsTest extends TestCase {

    public void testAddPrefetch() {
        SelectQuery q = new SelectQuery();

        assertNull(q.getPrefetchTree());
        q.addPrefetch("a.b.c");
        assertNotNull(q.getPrefetchTree());
        assertEquals(1, q.getPrefetchTree().nonPhantomNodes().size());
        assertNotNull(q.getPrefetchTree().getNode("a.b.c"));
    }

    public void testAddPrefetchDuplicates() {
        SelectQuery q = new SelectQuery();

        q.addPrefetch("a.b.c");
        q.addPrefetch("a.b.c");

        assertEquals(1, q.getPrefetchTree().nonPhantomNodes().size());
    }

    public void testClearPrefetches() {
        SelectQuery q = new SelectQuery();

        q.addPrefetch("abc");
        q.addPrefetch("xyz");
        assertNotNull(q.getPrefetchTree());

        q.clearPrefetches();
        assertNull(q.getPrefetchTree());
    }

    public void testPageSize() throws Exception {
        SelectQuery q = new SelectQuery();
        q.setPageSize(10);
        assertEquals(10, q.getPageSize());
    }

    public void testAddOrdering1() throws Exception {
        SelectQuery q = new SelectQuery();

        Ordering ord = new Ordering();
        q.addOrdering(ord);
        assertEquals(1, q.getOrderings().size());
        assertSame(ord, q.getOrderings().get(0));
    }

    public void testAddOrdering2() throws Exception {
        SelectQuery<Object> q = new SelectQuery<Object>();

        String path = "a.b.c";
        q.addOrdering(path, SortOrder.DESCENDING);
        assertEquals(1, q.getOrderings().size());

        Ordering ord = q.getOrderings().get(0);
        assertEquals(path, ord.getSortSpec().getOperand(0));
        assertEquals(false, ord.isAscending());
    }

    public void testDistinct() throws Exception {
        SelectQuery q = new SelectQuery();

        assertFalse(q.isDistinct());
        q.setDistinct(true);
        assertTrue(q.isDistinct());
    }

    public void testQueryWithParams1() {
        SelectQuery q = new SelectQuery();
        q.setRoot(Artist.class);
        q.setDistinct(true);

        SelectQuery q1 = q.queryWithParameters(new HashMap(), true);
        assertSame(q.getRoot(), q1.getRoot());
        assertEquals(q.isDistinct(), q1.isDistinct());
        assertNull(q1.getQualifier());
    }

    public void testQueryWithParams2() throws Exception {
        SelectQuery q = new SelectQuery();
        q.setRoot(Artist.class);

        List list = new ArrayList();
        list.add(ExpressionFactory.matchExp("k1", new ExpressionParameter("test1")));
        list.add(ExpressionFactory.matchExp("k2", new ExpressionParameter("test2")));
        list.add(ExpressionFactory.matchExp("k3", new ExpressionParameter("test3")));
        list.add(ExpressionFactory.matchExp("k4", new ExpressionParameter("test4")));
        q.setQualifier(ExpressionFactory.joinExp(Expression.OR, list));

        Map params = new HashMap();
        params.put("test2", "abc");
        params.put("test3", "xyz");
        SelectQuery q1 = q.queryWithParameters(params, true);
        assertSame(q.getRoot(), q1.getRoot());
        assertNotNull(q1.getQualifier());
        assertTrue(q1.getQualifier() != q.getQualifier());
    }

    public void testQueryWithParamsSkipName() {
        SelectQuery q = new SelectQuery();
        q.setRoot(Artist.class);
        q.setDistinct(true);
        q.setName("name");

        SelectQuery q1 = q.queryWithParameters(Collections.EMPTY_MAP);
        assertEquals("name", q.getName());
        assertNull(q1.getName());
    }
}
