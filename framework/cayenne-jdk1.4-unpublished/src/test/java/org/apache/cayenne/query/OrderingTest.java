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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.art.Painting;
import org.apache.cayenne.unit.BasicCase;
import org.apache.cayenne.unit.util.TestBean;

public class OrderingTest extends BasicCase {

    public void testPathSpec1() throws Exception {
        String pathSpec = "a.b.c";
        Ordering ord = new Ordering();
        assertNull(ord.getSortSpec());

        ord.setSortSpecString(pathSpec);
        assertEquals(pathSpec, ord.getSortSpec().getOperand(0));
    }

    public void testPathSpec2() throws Exception {
        String pathSpec = "a.b.c";
        Ordering ord = new Ordering(pathSpec, false);
        assertEquals(pathSpec, ord.getSortSpec().getOperand(0));
    }

    public void testAsending1() throws Exception {
        Ordering ord = new Ordering();
        ord.setAscending(Ordering.DESC);
        assertEquals(Ordering.DESC, ord.isAscending());
    }

    public void testCaseInsensitive1() throws Exception {
        Ordering ord = new Ordering("M", Ordering.ASC, true);
        assertTrue(ord.isCaseInsensitive());
    }

    public void testCaseInsensitive2() throws Exception {
        Ordering ord = new Ordering("N", Ordering.ASC, false);
        assertFalse(ord.isCaseInsensitive());
    }

    public void testAsending2() throws Exception {
        Ordering ord = new Ordering("K", Ordering.DESC);
        assertEquals(Ordering.DESC, ord.isAscending());
    }

    public void testCompare1() throws Exception {
        Painting p1 = new Painting();
        p1.setEstimatedPrice(new BigDecimal(1000.00));

        Painting p2 = new Painting();
        p2.setEstimatedPrice(new BigDecimal(2000.00));

        Painting p3 = new Painting();
        p3.setEstimatedPrice(new BigDecimal(2000.00));

        Ordering ordering = new Ordering("estimatedPrice", Ordering.ASC);
        assertTrue(ordering.compare(p1, p2) < 0);
        assertTrue(ordering.compare(p2, p1) > 0);
        assertTrue(ordering.compare(p2, p3) == 0);
    }

    public void testCompare2() throws Exception {
        // compare on non-persistent property
        TestBean t1 = new TestBean(1000);
        TestBean t2 = new TestBean(2000);
        TestBean t3 = new TestBean(2000);

        Ordering ordering = new Ordering("integer", Ordering.ASC);
        assertTrue(ordering.compare(t1, t2) < 0);
        assertTrue(ordering.compare(t2, t1) > 0);
        assertTrue(ordering.compare(t2, t3) == 0);
    }

    public void testOrderList() throws Exception {
        // compare on non-persistent property
        List list = new ArrayList(3);

        list.add(new TestBean(5));
        list.add(new TestBean(2));
        list.add(new TestBean(3));

        new Ordering("integer", Ordering.ASC).orderList(list);
        assertEquals(2, ((TestBean) list.get(0)).getInteger().intValue());
        assertEquals(3, ((TestBean) list.get(1)).getInteger().intValue());
        assertEquals(5, ((TestBean) list.get(2)).getInteger().intValue());
    }

    public void testOrderListWithMultipleOrderings() throws Exception {
        // compare on non-persistent property
        List list = new ArrayList(6);

        list.add(new TestBean("c", 1));
        list.add(new TestBean("c", 30));
        list.add(new TestBean("a", 5));
        list.add(new TestBean("b", 1));
        list.add(new TestBean("b", 2));
        list.add(new TestBean("b", 5));

        List orderings = new ArrayList(2);
        orderings.add(new Ordering("string", Ordering.ASC));
        orderings.add(new Ordering("integer", Ordering.DESC));

        // clone list and then order
        List orderedList = new ArrayList(list);
        Ordering.orderList(orderedList, orderings);

        assertEquals(list.get(2), orderedList.get(0));
        assertEquals(list.get(5), orderedList.get(1));
        assertEquals(list.get(4), orderedList.get(2));
        assertEquals(list.get(3), orderedList.get(3));
        assertEquals(list.get(1), orderedList.get(4));
        assertEquals(list.get(0), orderedList.get(5));
    }
}
