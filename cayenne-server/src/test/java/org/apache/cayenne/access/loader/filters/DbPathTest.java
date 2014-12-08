/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.cayenne.access.loader.filters;

import org.junit.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.apache.cayenne.access.loader.filters.FiltersFactory.path;
import static org.junit.Assert.*;

public class DbPathTest {

    @Test
    public void testIsCover() throws Exception {
        assertTrue(path(null, null).isCover(path("Hello", "World")));
        assertTrue(path(null, null).isCover(path("Hello", null)));
        assertTrue(path(null, null).isCover(path(null, null)));
        assertTrue(path(null, "Yo").isCover(path("Yo!", "Yo")));
        assertTrue(path(null, "Yo").isCover(path(null, "Yo")));

        assertFalse(path(null, "Yo!").isCover(path(null, "Yo!!")));
        assertFalse(path("aa", "Yo!").isCover(path(null, "Yo!!")));
        assertFalse(path("aaa", "Yo!").isCover(path("aa", "Yo!!")));

        assertTrue(path("aa", null).isCover(path("aa", null)));
        assertTrue(path("aa", null).isCover(path("aa", "bb")));
        assertTrue(path("aa", "Yo!").isCover(path("aa", "Yo!")));
        assertFalse(path("aa", "Yo!").isCover(path("aa", "Yo!!")));

        assertFalse(path("", "APP").isCover(path(null, null)));

        assertTrue(path(null, "schema_01").isCover(path("", "schema_01")));
        assertTrue(path(null, "schema_01").isCover(path(null, "schema_01")));
        assertFalse(path(null, "schema_01").isCover(path("", "schema_02")));
        assertFalse(path(null, "schema_02").isCover(path("", "schema_01")));
    }

    @Test
    public void testToString() throws Exception {
        assertEquals("%", path(null, null).toString());
        assertEquals("/schema", path("", "schema").toString());
        assertEquals("%/schema", path(null, "schema").toString());
        assertEquals("catalog/schema", path("catalog", "schema").toString());
        assertEquals("catalog//table", path("catalog", "", "table").toString());
        assertEquals("catalog/%/table", path("catalog", null, "table").toString());
        assertEquals("//table", path("", "", "table").toString());
        assertEquals("%/%/table", path(null, null, "table").toString());
        assertEquals("%", path(null, null, null).toString());
        assertEquals("c/%/", path("c", null, "").toString());
    }

    @Test
    public void testMerge() throws Exception {
        DbPath path1 = path(null, null);
        DbPath path2 = path("", "APP");
        assertEquals(path1, path1.merge(path2));
        assertEquals(path1, path2.merge(path1));
    }

    @Test
    public void testEquals() throws Exception {
        Set<DbPath> pathes = new TreeSet<DbPath>();
        pathes.add(path("q", "w"));
        pathes.add(path("q", null));
        pathes.add(path("q", ""));
        pathes.add(path("", ""));
        pathes.add(path("q", "w"));
        pathes.add(path("q", "w"));
        pathes.add(path("q", "w"));
        pathes.add(path("q", "w"));

        assertEquals(4, pathes.size());
    }
}