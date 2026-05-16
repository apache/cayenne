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
import org.apache.cayenne.unit.util.TestObject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class OrderingTest {

    @Test
    public void pathSpec1() {
        String pathSpec = "a.b.c";
        Ordering ord = new Ordering();
        assertNull(ord.getSortSpec());

        ord.setSortSpecString(pathSpec);
        assertEquals(pathSpec, ord.getSortSpec().getOperand(0).toString());
    }

    @Test
    public void pathSpec3() {
        String pathSpec = "a.b.c";
        Ordering ord = new Ordering(pathSpec, SortOrder.DESCENDING);
        assertEquals(pathSpec, ord.getSortSpec().getOperand(0).toString());
    }

    @Test
    public void ascending1() {
        Ordering ord = new Ordering();
        ord.setAscending();
        assertTrue(ord.isAscending());
        assertFalse(ord.isDescending());
    }

    @Test
    public void ascending2() {
        Ordering ord = new Ordering();
        ord.setSortOrder(SortOrder.ASCENDING);
        assertTrue(ord.isAscending());
        assertFalse(ord.isDescending());
    }

    @Test
    public void ascending3() {
        Ordering ord = new Ordering();
        ord.setSortOrder(SortOrder.ASCENDING_INSENSITIVE);
        assertTrue(ord.isAscending());
        assertFalse(ord.isDescending());
    }

    @Test
    public void descending1() {
        Ordering ord = new Ordering();
        ord.setDescending();
        assertFalse(ord.isAscending());
        assertTrue(ord.isDescending());
    }

    @Test
    public void descending2() {
        Ordering ord = new Ordering();
        ord.setSortOrder(SortOrder.DESCENDING);
        assertFalse(ord.isAscending());
        assertTrue(ord.isDescending());
    }

    @Test
    public void descending3() {
        Ordering ord = new Ordering();
        ord.setSortOrder(SortOrder.DESCENDING_INSENSITIVE);
        assertFalse(ord.isAscending());
        assertTrue(ord.isDescending());
    }

    @Test
    public void caseInsensitive3() {
        Ordering ord = new Ordering("M", SortOrder.ASCENDING_INSENSITIVE);
        assertTrue(ord.isCaseInsensitive());
    }

    @Test
    public void caseInsensitive4() {
        Ordering ord = new Ordering("N", SortOrder.ASCENDING);
        assertFalse(ord.isCaseInsensitive());
    }

    @Test
    public void caseInsensitive5() {
        Ordering ord = new Ordering("M", SortOrder.DESCENDING_INSENSITIVE);
        assertTrue(ord.isCaseInsensitive());
    }

    @Test
    public void caseInsensitive6() {
        Ordering ord = new Ordering("N", SortOrder.DESCENDING);
        assertFalse(ord.isCaseInsensitive());
    }

    @Test
    public void orderingWithExpression() {
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
    public void compare3() {
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
    public void compare4() {
        // compare on non-persistent property
        TestObject t1 = new TestObject(1000);
        TestObject t2 = new TestObject(2000);
        TestObject t3 = new TestObject(2000);

        Ordering ordering = new Ordering("integer", SortOrder.ASCENDING);
        assertTrue(ordering.compare(t1, t2) < 0);
        assertTrue(ordering.compare(t2, t1) > 0);
        assertTrue(ordering.compare(t2, t3) == 0);
    }

    @Test
    public void orderList() {
        List<TestObject> list = new ArrayList<>(3);

        list.add(new TestObject(5));
        list.add(new TestObject(2));
        list.add(new TestObject(3));

        new Ordering("integer", SortOrder.ASCENDING).orderList(list);
        assertEquals(2, list.get(0).getInteger().intValue());
        assertEquals(3, list.get(1).getInteger().intValue());
        assertEquals(5, list.get(2).getInteger().intValue());
    }

    @Test
    public void orderList_Related() {
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
    public void orderList_OuterRelated() {
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
    public void orderList_Static() {
        List<TestObject> list = new ArrayList<>(6);

        list.add(new TestObject("c", 1));
        list.add(new TestObject("c", 30));
        list.add(new TestObject("a", 5));
        list.add(new TestObject("b", 1));
        list.add(new TestObject("b", 2));
        list.add(new TestObject("b", 5));

        List<Ordering> orderings = asList(
                new Ordering("string", SortOrder.ASCENDING),
                new Ordering("integer", SortOrder.DESCENDING));

        // clone list and then order
        List<TestObject> orderedList = new ArrayList<>(list);
        Ordering.orderList(orderedList, orderings);

        assertEquals(list.get(2), orderedList.get(0));
        assertEquals(list.get(5), orderedList.get(1));
        assertEquals(list.get(4), orderedList.get(2));
        assertEquals(list.get(3), orderedList.get(3));
        assertEquals(list.get(1), orderedList.get(4));
        assertEquals(list.get(0), orderedList.get(5));
    }

    @Test
    public void orderedList() {
        Collection<TestObject> set = new HashSet<>(6);

        TestObject shouldBe0 = new TestObject("a", 0);
        TestObject shouldBe1 = new TestObject("b", 0);
        TestObject shouldBe2 = new TestObject("c", 0);
        TestObject shouldBe3 = new TestObject("d", 0);
        TestObject shouldBe4 = new TestObject("f", 0);
        TestObject shouldBe5 = new TestObject("r", 0);

        set.add(shouldBe1);
        set.add(shouldBe0);
        set.add(shouldBe5);
        set.add(shouldBe3);
        set.add(shouldBe2);
        set.add(shouldBe4);

        List<TestObject> orderedList = new Ordering("string", SortOrder.ASCENDING).orderedList(set);

        assertEquals(shouldBe0, orderedList.get(0));
        assertEquals(shouldBe1, orderedList.get(1));
        assertEquals(shouldBe2, orderedList.get(2));
        assertEquals(shouldBe3, orderedList.get(3));
        assertEquals(shouldBe4, orderedList.get(4));
        assertEquals(shouldBe5, orderedList.get(5));
    }

    @Test
    public void orderedList_Static() {
        Collection<TestObject> set = new HashSet<>(6);

        TestObject shouldBe0 = new TestObject("a", 5);
        TestObject shouldBe1 = new TestObject("b", 5);
        TestObject shouldBe2 = new TestObject("b", 2);
        TestObject shouldBe3 = new TestObject("b", 1);
        TestObject shouldBe4 = new TestObject("c", 30);
        TestObject shouldBe5 = new TestObject("c", 1);

        set.add(shouldBe0);
        set.add(shouldBe5);
        set.add(shouldBe3);
        set.add(shouldBe1);
        set.add(shouldBe4);
        set.add(shouldBe2);

        List<Ordering> orderings = asList(
                new Ordering("string", SortOrder.ASCENDING),
                new Ordering("integer", SortOrder.DESCENDING));

        List<TestObject> orderedList = Ordering.orderedList(set, orderings);

        assertEquals(shouldBe0, orderedList.get(0));
        assertEquals(shouldBe1, orderedList.get(1));
        assertEquals(shouldBe2, orderedList.get(2));
        assertEquals(shouldBe3, orderedList.get(3));
        assertEquals(shouldBe4, orderedList.get(4));
        assertEquals(shouldBe5, orderedList.get(5));
    }

    @Test
    public void orderListWithFunction() {
        Collection<TestObject> set = new HashSet<>(6);

        TestObject shouldBe0 = new TestObject("", 0);
        TestObject shouldBe1 = new TestObject("", -1);
        TestObject shouldBe2 = new TestObject("", -2);
        TestObject shouldBe3 = new TestObject("", 5);
        TestObject shouldBe4 = new TestObject("", -6);
        TestObject shouldBe5 = new TestObject("", -30);

        set.add(shouldBe4);
        set.add(shouldBe2);
        set.add(shouldBe1);
        set.add(shouldBe5);
        set.add(shouldBe0);
        set.add(shouldBe3);

        List<TestObject> orderedList = new Ordering(FunctionExpressionFactory.absExp("integer"), SortOrder.ASCENDING).orderedList(set);

        assertEquals(shouldBe0, orderedList.get(0));
        assertEquals(shouldBe1, orderedList.get(1));
        assertEquals(shouldBe2, orderedList.get(2));
        assertEquals(shouldBe3, orderedList.get(3));
        assertEquals(shouldBe4, orderedList.get(4));
        assertEquals(shouldBe5, orderedList.get(5));
    }

    @Test
    public void orderListWithFunction_Static() {
        Collection<TestObject> set = new HashSet<>(6);

        TestObject shouldBe0 = new TestObject("cx", -2);
        TestObject shouldBe1 = new TestObject("cf", -1);
        TestObject shouldBe2 = new TestObject("basa", 2);
        TestObject shouldBe3 = new TestObject("abcd", -1);
        TestObject shouldBe4 = new TestObject("bdsasd", -2);
        TestObject shouldBe5 = new TestObject("bdsadf", 1);

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

        List<TestObject> orderedList = Ordering.orderedList(set, orderings);

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
