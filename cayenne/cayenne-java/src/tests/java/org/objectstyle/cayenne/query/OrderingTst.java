/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.query;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.unit.BasicTestCase;
import org.objectstyle.cayenne.unit.util.TestBean;

public class OrderingTst extends BasicTestCase {

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
