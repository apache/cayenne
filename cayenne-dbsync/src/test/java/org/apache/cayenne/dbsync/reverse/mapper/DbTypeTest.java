/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cayenne.dbsync.reverse.mapper;

import org.junit.Test;

import java.util.Iterator;
import java.util.TreeSet;

import static org.junit.Assert.*;

public class DbTypeTest {

    @Test
    public void testCompareTo() throws Exception {
        TreeSet<DbType> set = new TreeSet<DbType>();
        set.add(new DbType("type-01", null, null, null, null));
        set.add(new DbType("type-02", null, null, null, null));
        set.add(new DbType("type-02", 1, null, null, null));
        set.add(new DbType("type-02", 2, null, null, null));
        set.add(new DbType("type-02", 2, null, null, true));
        set.add(new DbType("type-02", 2, null, null, false));
        set.add(new DbType("type-02", 2, null, 5, null));
        set.add(new DbType("type-02", 2, null, 5, false));
        set.add(new DbType("type-02", 2, null, 5, true));
        set.add(new DbType("type-02", null, 8, 5, true));
        set.add(new DbType("type-02", null, 9, 5, true));

        Iterator<DbType> iterator = set.iterator();
        assertEquals(new DbType("type-02", 2, null, 5, true), iterator.next());
        assertEquals(new DbType("type-02", 2, null, 5, false), iterator.next());
        assertEquals(new DbType("type-02", null, 9, 5, true), iterator.next());
        assertEquals(new DbType("type-02", null, 8, 5, true), iterator.next());
        assertEquals(new DbType("type-02", 2, null, 5, null), iterator.next());
        assertEquals(new DbType("type-02", 2, null, null, true), iterator.next());
        assertEquals(new DbType("type-02", 2, null, null, false), iterator.next());
        assertEquals(new DbType("type-02", 2, null, null, null), iterator.next());
        assertEquals(new DbType("type-02", 1, null, null, null), iterator.next());
        assertEquals(new DbType("type-02", null, null, null, null), iterator.next());
        assertEquals(new DbType("type-01", null, null, null, null), iterator.next());
    }

    @Test
    public void testCover() throws Exception {
        DbType typeJava = new DbType("java");
        assertTrue(typeJava.isCover(typeJava));
        assertTrue(typeJava.isCover(new DbType("java", 1, 1, 1, null)));
        assertTrue(typeJava.isCover(new DbType("java", 1, null, null, null)));
        assertTrue(typeJava.isCover(new DbType("java", null, 1, null, null)));
        assertTrue(typeJava.isCover(new DbType("java", null, null, 1, null)));
        assertTrue(typeJava.isCover(new DbType("java", null, null, null, true)));
        assertTrue(typeJava.isCover(new DbType("java", null, null, null, false)));
        assertFalse(typeJava.isCover(new DbType("java1", null, null, null, null)));

        DbType typeWithLength = new DbType("java", 1, null, null, null);
        assertTrue(typeWithLength.isCover(typeWithLength));
        assertTrue(typeWithLength.isCover(new DbType("java", 1, null, 1, null)));
        assertTrue(typeWithLength.isCover(new DbType("java", 1, null, 1, true)));
        assertTrue(typeWithLength.isCover(new DbType("java", 1, null, null, true)));
        assertTrue(typeWithLength.isCover(new DbType("java", 1, 1, null, true)));
        assertFalse(typeWithLength.isCover(new DbType("java", 2, null, null, null)));
        assertFalse(typeWithLength.isCover(new DbType("java", null, null, null, true)));
        assertFalse(typeWithLength.isCover(new DbType("java1", 2, null, null, null)));

        DbType typeWithLengthAndNotNull = new DbType("java", 1, null, null, true);
        assertTrue(typeWithLength.isCover(typeWithLengthAndNotNull));
        assertTrue(typeWithLength.isCover(new DbType("java", 1, null, 1, true)));
        assertTrue(typeWithLength.isCover(new DbType("java", 1, 1, 1, true)));
    }
}