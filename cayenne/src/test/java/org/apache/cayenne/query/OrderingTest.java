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

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.FunctionExpressionFactory;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.util.TstBean;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class OrderingTest {

    @Test
    public void testPathSpec1() {
        String pathSpec = "a.b.c";
        Ordering ord = new Ordering();
        assertNull(ord.getSortSpec());

        ord.setSortSpecString(pathSpec);
        assertEquals(pathSpec, ord.getSortSpec().getOperand(0).toString());
    }

    @Test
    public void testPathSpec3() {
        String pathSpec = "a.b.c";
        Ordering ord = new Ordering(pathSpec, SortOrder.DESCENDING);
        assertEquals(pathSpec, ord.getSortSpec().getOperand(0).toString());
    }

    @Test
    public void testAscending1() {
        Ordering ord = new Ordering();
        ord.setAscending();
        assertTrue(ord.isAscending());
        assertFalse(ord.isDescending());
    }

    @Test
    public void testAscending2() {
        Ordering ord = new Ordering();
        ord.setSortOrder(SortOrder.ASCENDING);
        assertTrue(ord.isAscending());
        assertFalse(ord.isDescending());
    }

    @Test
    public void testAscending3() {
        Ordering ord = new Ordering();
        ord.setSortOrder(SortOrder.ASCENDING_INSENSITIVE);
        assertTrue(ord.isAscending());
        assertFalse(ord.isDescending());
    }

    @Test
    public void testDescending1() {
        Ordering ord = new Ordering();
        ord.setDescending();
        assertFalse(ord.isAscending());
        assertTrue(ord.isDescending());
    }

    @Test
    public void testDescending2() {
        Ordering ord = new Ordering();
        ord.setSortOrder(SortOrder.DESCENDING);
        assertFalse(ord.isAscending());
        assertTrue(ord.isDescending());
    }

    @Test
    public void testDescending3() {
        Ordering ord = new Ordering();
        ord.setSortOrder(SortOrder.DESCENDING_INSENSITIVE);
        assertFalse(ord.isAscending());
        assertTrue(ord.isDescending());
    }

    @Test
    public void testCaseInsensitive3() {
        Ordering ord = new Ordering("M", SortOrder.ASCENDING_INSENSITIVE);
        assertTrue(ord.isCaseInsensitive());
    }

    @Test
    public void testCaseInsensitive4() {
        Ordering ord = new Ordering("N", SortOrder.ASCENDING);
        assertFalse(ord.isCaseInsensitive());
    }

    @Test
    public void testCaseInsensitive5() {
        Ordering ord = new Ordering("M", SortOrder.DESCENDING_INSENSITIVE);
        assertTrue(ord.isCaseInsensitive());
    }

    @Test
    public void testCaseInsensitive6() {
        Ordering ord = new Ordering("N", SortOrder.DESCENDING);
        assertFalse(ord.isCaseInsensitive());
    }

    @Test
    public void testOrderingWithExpression() {
        Expression exp = FunctionExpressionFactory.absExp("x");
        Ordering ord = new Ordering();
        ord.setSortSpec(exp);
        ord.setSortOrder(SortOrder.ASCENDING);

        Ordering ord2 = new Ordering(exp);
        assertEquals(ord, ord2);
        assertEquals(exp, ord2.getSortSpec());
        assertEquals(SortOrder.ASCENDING, ord2.getSortOrder());
    }

    @Test
    public void testCompare3() {
        Painting p1 = new Painting();
        p1.setEstimatedPrice(new BigDecimal(1000.00));

        Painting p2 = new Painting();
        p2.setEstimatedPrice(new BigDecimal(2000.00));

        Painting p3 = new Painting();
        p3.setEstimatedPrice(new BigDecimal(2000.00));

        Ordering ordering = new Ordering("estimatedPrice", SortOrder.ASCENDING);
        assertTrue(ordering.compare(p1, p2) < 0);
        assertTrue(ordering.compare(p2, p1) > 0);
        assertTrue(ordering.compare(p2, p3) == 0);
    }

    @Test
    public void testCompare4() {
        // compare on non-persistent property
        TstBean t1 = new TstBean(1000);
        TstBean t2 = new TstBean(2000);
        TstBean t3 = new TstBean(2000);

        Ordering ordering = new Ordering("integer", SortOrder.ASCENDING);
        assertTrue(ordering.compare(t1, t2) < 0);
        assertTrue(ordering.compare(t2, t1) > 0);
        assertTrue(ordering.compare(t2, t3) == 0);
    }

    @Test
    public void testOrderList() {
        List<TstBean> list = new ArrayList<>(3);

        list.add(new TstBean(5));
        list.add(new TstBean(2));
        list.add(new TstBean(3));

        new Ordering("integer", SortOrder.ASCENDING).orderList(list);
        assertEquals(2, list.get(0).getInteger().intValue());
        assertEquals(3, list.get(1).getInteger().intValue());
        assertEquals(5, list.get(2).getInteger().intValue());
    }

    @Test
    public void testOrderList_Related() {
        List<B1> unordered = asList(
                new B1().setName("three").setB2(new B2().setName("Z")),
                new B1().setName("one").setB2(new B2().setName("A")),
                new B1().setName("two").setB2(new B2().setName("M"))
        );

        List<B1> ordered = new Ordering("b2.name", SortOrder.ASCENDING).orderedList(unordered);
        assertEquals("one", ordered.get(0).getName());
        assertEquals("two", ordered.get(1).getName());
        assertEquals("three", ordered.get(2).getName());
    }

    /**
     * CAY-1551
     */
    @Test
    public void testOrderList_OuterRelated() {
        List<B1> unordered = asList(
                new B1().setName("three").setB2(new B2().setName("Z")),
                new B1().setName("one").setB2(new B2().setName("A")),
                new B1().setName("two").setB2(new B2().setName("M"))
        );

        List<B1> ordered = new Ordering("b2+.name", SortOrder.ASCENDING).orderedList(unordered);
        assertEquals("one", ordered.get(0).getName());
        assertEquals("two", ordered.get(1).getName());
        assertEquals("three", ordered.get(2).getName());
    }

    @Test
    public void testOrderList_Static() {
        List<TstBean> list = new ArrayList<>(6);

        list.add(new TstBean("c", 1));
        list.add(new TstBean("c", 30));
        list.add(new TstBean("a", 5));
        list.add(new TstBean("b", 1));
        list.add(new TstBean("b", 2));
        list.add(new TstBean("b", 5));

        List<Ordering> orderings = asList(
                new Ordering("string", SortOrder.ASCENDING),
                new Ordering("integer", SortOrder.DESCENDING));

        // clone list and then order
        List<TstBean> orderedList = new ArrayList<>(list);
        Ordering.orderList(orderedList, orderings);

        assertEquals(list.get(2), orderedList.get(0));
        assertEquals(list.get(5), orderedList.get(1));
        assertEquals(list.get(4), orderedList.get(2));
        assertEquals(list.get(3), orderedList.get(3));
        assertEquals(list.get(1), orderedList.get(4));
        assertEquals(list.get(0), orderedList.get(5));
    }

    @Test
    public void testOrderedList() {
        Collection<TstBean> set = new HashSet<>(6);

        TstBean shouldBe0 = new TstBean("a", 0);
        TstBean shouldBe1 = new TstBean("b", 0);
        TstBean shouldBe2 = new TstBean("c", 0);
        TstBean shouldBe3 = new TstBean("d", 0);
        TstBean shouldBe4 = new TstBean("f", 0);
        TstBean shouldBe5 = new TstBean("r", 0);

        set.add(shouldBe1);
        set.add(shouldBe0);
        set.add(shouldBe5);
        set.add(shouldBe3);
        set.add(shouldBe2);
        set.add(shouldBe4);

        List<TstBean> orderedList = new Ordering("string", SortOrder.ASCENDING).orderedList(set);

        assertEquals(shouldBe0, orderedList.get(0));
        assertEquals(shouldBe1, orderedList.get(1));
        assertEquals(shouldBe2, orderedList.get(2));
        assertEquals(shouldBe3, orderedList.get(3));
        assertEquals(shouldBe4, orderedList.get(4));
        assertEquals(shouldBe5, orderedList.get(5));
    }

    @Test
    public void testOrderedList_Static() {
        Collection<TstBean> set = new HashSet<>(6);

        TstBean shouldBe0 = new TstBean("a", 5);
        TstBean shouldBe1 = new TstBean("b", 5);
        TstBean shouldBe2 = new TstBean("b", 2);
        TstBean shouldBe3 = new TstBean("b", 1);
        TstBean shouldBe4 = new TstBean("c", 30);
        TstBean shouldBe5 = new TstBean("c", 1);

        set.add(shouldBe0);
        set.add(shouldBe5);
        set.add(shouldBe3);
        set.add(shouldBe1);
        set.add(shouldBe4);
        set.add(shouldBe2);

        List<Ordering> orderings = asList(
                new Ordering("string", SortOrder.ASCENDING),
                new Ordering("integer", SortOrder.DESCENDING));

        List<TstBean> orderedList = Ordering.orderedList(set, orderings);

        assertEquals(shouldBe0, orderedList.get(0));
        assertEquals(shouldBe1, orderedList.get(1));
        assertEquals(shouldBe2, orderedList.get(2));
        assertEquals(shouldBe3, orderedList.get(3));
        assertEquals(shouldBe4, orderedList.get(4));
        assertEquals(shouldBe5, orderedList.get(5));
    }

    @Test
    public void testOrderListWithFunction() {
        Collection<TstBean> set = new HashSet<>(6);

        TstBean shouldBe0 = new TstBean("", 0);
        TstBean shouldBe1 = new TstBean("", -1);
        TstBean shouldBe2 = new TstBean("", -2);
        TstBean shouldBe3 = new TstBean("", 5);
        TstBean shouldBe4 = new TstBean("", -6);
        TstBean shouldBe5 = new TstBean("", -30);

        set.add(shouldBe4);
        set.add(shouldBe2);
        set.add(shouldBe1);
        set.add(shouldBe5);
        set.add(shouldBe0);
        set.add(shouldBe3);

        List<TstBean> orderedList = new Ordering(FunctionExpressionFactory.absExp("integer"), SortOrder.ASCENDING).orderedList(set);

        assertEquals(shouldBe0, orderedList.get(0));
        assertEquals(shouldBe1, orderedList.get(1));
        assertEquals(shouldBe2, orderedList.get(2));
        assertEquals(shouldBe3, orderedList.get(3));
        assertEquals(shouldBe4, orderedList.get(4));
        assertEquals(shouldBe5, orderedList.get(5));
    }

    @Test
    public void testOrderListWithFunction_Static() {
        Collection<TstBean> set = new HashSet<>(6);

        TstBean shouldBe0 = new TstBean("cx", -2);
        TstBean shouldBe1 = new TstBean("cf", -1);
        TstBean shouldBe2 = new TstBean("basa", 2);
        TstBean shouldBe3 = new TstBean("abcd", -1);
        TstBean shouldBe4 = new TstBean("bdsasd", -2);
        TstBean shouldBe5 = new TstBean("bdsadf", 1);

        set.add(shouldBe4);
        set.add(shouldBe2);
        set.add(shouldBe1);
        set.add(shouldBe5);
        set.add(shouldBe0);
        set.add(shouldBe3);

        List<Ordering> orderings = asList(
                new Ordering(FunctionExpressionFactory.lengthExp("string"), SortOrder.ASCENDING),
                new Ordering(FunctionExpressionFactory.absExp("integer"), SortOrder.DESCENDING)
        );

        List<TstBean> orderedList = Ordering.orderedList(set, orderings);

        assertEquals(shouldBe0, orderedList.get(0));
        assertEquals(shouldBe1, orderedList.get(1));
        assertEquals(shouldBe2, orderedList.get(2));
        assertEquals(shouldBe3, orderedList.get(3));
        assertEquals(shouldBe4, orderedList.get(4));
        assertEquals(shouldBe5, orderedList.get(5));
    }

    public static class B1 {

        private B2 b2;
        private String name;

        public B2 getB2() {
            return b2;
        }

        public B1 setB2(B2 b2) {
            this.b2 = b2;
            return this;
        }

        public String getName() {
            return name;
        }

        public B1 setName(String name) {
            this.name = name;
            return this;
        }
    }

    public static class B2 {

        private String name;

        public String getName() {
            return name;
        }

        public B2 setName(String name) {
            this.name = name;
            return this;
        }
    }
}
