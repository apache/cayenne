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
package org.objectstyle.cayenne.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class IndexPropertyListTst extends TestCase {

    public void testSort() {

        IndexedObject o1 = new IndexedObject(1);
        IndexedObject o2 = new IndexedObject(2);
        IndexedObject o3 = new IndexedObject(3);
        IndexedObject o4 = new IndexedObject(4);

        List list1 = Arrays.asList(new Object[] {
                o2, o4, o3, o1
        });

        IndexPropertyList indexedList = new IndexPropertyList("order", list1, true);
        // sort should be done implictly on get...
        assertEquals(o1, indexedList.get(0));
        assertEquals(o2, indexedList.get(1));
        assertEquals(o3, indexedList.get(2));
        assertEquals(o4, indexedList.get(3));

        List list2 = Arrays.asList(new Object[] {
                o2, o4, o3, o1
        });
        IndexPropertyList indexedUnsortedList = new IndexPropertyList(
                "order",
                list2,
                false);
        // sort should be done implictly on get...
        assertEquals(o2, indexedUnsortedList.get(0));
        assertEquals(o4, indexedUnsortedList.get(1));
        assertEquals(o3, indexedUnsortedList.get(2));
        assertEquals(o1, indexedUnsortedList.get(3));
    }

    public void testAppend() {
        IndexedObject o1 = new IndexedObject(1);
        IndexedObject o2 = new IndexedObject(2);
        IndexedObject o3 = new IndexedObject(3);
        IndexedObject o4 = new IndexedObject(4);

        List list1 = new ArrayList(Arrays.asList(new Object[] {
                o2, o4, o3, o1
        }));

        IndexPropertyList indexedList = new IndexPropertyList("order", list1, true);

        IndexedObject o5 = new IndexedObject(-1);
        indexedList.add(o5);

        assertEquals(4, o4.getOrder());
        assertTrue(o4.getOrder() < o5.getOrder());
    }

    public void testInsert() {
        IndexedObject o1 = new IndexedObject(1);
        IndexedObject o2 = new IndexedObject(2);
        IndexedObject o3 = new IndexedObject(3);
        IndexedObject o4 = new IndexedObject(4);

        List list1 = new ArrayList(Arrays.asList(new Object[] {
                o2, o4, o3, o1
        }));

        IndexPropertyList indexedList = new IndexPropertyList("order", list1, true);

        IndexedObject o5 = new IndexedObject(-1);
        indexedList.add(1, o5);

        assertEquals(1, o1.getOrder());
        assertTrue(o1.getOrder() < o5.getOrder());
        assertTrue(o5.getOrder() < o2.getOrder());
        assertTrue(o2.getOrder() < o3.getOrder());
        assertTrue(o3.getOrder() < o4.getOrder());
    }
}
