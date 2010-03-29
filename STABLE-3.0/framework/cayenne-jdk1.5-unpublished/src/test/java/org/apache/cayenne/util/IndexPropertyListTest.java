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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class IndexPropertyListTest extends TestCase {

    public void testSort() {

        IndexedObject o1 = new IndexedObject(1);
        IndexedObject o2 = new IndexedObject(2);
        IndexedObject o3 = new IndexedObject(3);
        IndexedObject o4 = new IndexedObject(4);

        List list1 = Arrays.asList(o2, o4, o3, o1);

        IndexPropertyList indexedList = new IndexPropertyList("order", list1, true);
        // sort should be done implictly on get...
        assertEquals(o1, indexedList.get(0));
        assertEquals(o2, indexedList.get(1));
        assertEquals(o3, indexedList.get(2));
        assertEquals(o4, indexedList.get(3));

        List list2 = Arrays.asList(o2, o4, o3, o1);
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

        List list1 = new ArrayList(Arrays.asList(o2, o4, o3, o1));

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

        List list1 = new ArrayList(Arrays.asList(o2, o4, o3, o1));

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
