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
package org.apache.cayenne.dbsync.reverse.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.*;

public class TableFilterTest {

    @Test
    public void testIncludeEverything() {
        TableFilter filter = TableFilter.everything();

        assertNotNull(filter.isIncludeTable("table"));
        assertNotNull(filter.isIncludeTable("aaaa"));
        assertNotNull(filter.isIncludeTable(""));
        assertNotNull(filter.isIncludeTable("alex"));
    }

    @Test
    public void testInclude() {
        List<IncludeTableFilter> includes = new ArrayList<>();
        includes.add(new IncludeTableFilter("aaa"));
        includes.add(new IncludeTableFilter("bb"));

        TableFilter filter = new TableFilter(includes, new ArrayList<>());

        assertTrue(filter.isIncludeTable("aaa"));
        assertFalse(filter.isIncludeTable("aa"));
        assertFalse(filter.isIncludeTable("aaaa"));

        assertTrue(filter.isIncludeTable("bb"));
        assertFalse(filter.isIncludeTable(""));
        assertFalse(filter.isIncludeTable("bbbb"));
    }

    @Test
    public void testExclude() {
        List<Pattern> excludes = new ArrayList<>();
        excludes.add(Pattern.compile("aaa"));
        excludes.add(Pattern.compile("bb"));

        List<IncludeTableFilter> includes = new ArrayList<>();
        includes.add(new IncludeTableFilter(null, PatternFilter.INCLUDE_EVERYTHING));

        TableFilter filter = new TableFilter(includes, excludes);

        assertFalse(filter.isIncludeTable("aaa"));
        assertTrue(filter.isIncludeTable("aa"));
        assertTrue(filter.isIncludeTable("aaaa"));

        assertFalse(filter.isIncludeTable("bb"));
        assertTrue(filter.isIncludeTable(""));
        assertTrue(filter.isIncludeTable("bbbb"));
    }

    @Test
    public void testIncludeExclude() {
        List<Pattern> excludes = new ArrayList<>();
        excludes.add(Pattern.compile("aaa"));
        excludes.add(Pattern.compile("bb"));

        List<IncludeTableFilter> includes = new ArrayList<>();
        includes.add(new IncludeTableFilter("aa.*"));

        TableFilter filter = new TableFilter(includes, excludes);

        assertFalse(filter.isIncludeTable("aaa"));
        assertTrue(filter.isIncludeTable("aa"));
        assertTrue(filter.isIncludeTable("aaaa"));

        assertFalse(filter.isIncludeTable("bb"));
        assertFalse(filter.isIncludeTable(""));
        assertFalse(filter.isIncludeTable("bbbb"));
    }

    @Test
    public void testGetTableFilter() {
        List<IncludeTableFilter> includes = new ArrayList<>();
        includes.add(new IncludeTableFilter("aaa"));
        includes.add(new IncludeTableFilter("bb"));

        List<Pattern> excludes = new ArrayList<>();

        TableFilter filter = new TableFilter(includes, excludes);

        assertNotNull(filter.getIncludeTableColumnFilter("aaa"));
        assertNull(filter.getIncludeTableColumnFilter("aa"));
        assertNull(filter.getIncludeTableColumnFilter("aaaa"));

        assertNotNull(filter.getIncludeTableColumnFilter("bb"));
        assertNull(filter.getIncludeTableColumnFilter(""));
        assertNull(filter.getIncludeTableColumnFilter("bbbb"));
    }

    @Test
    public void testExcludePriority(){
        List<IncludeTableFilter> includes = new ArrayList<>();
        includes.add(new IncludeTableFilter("a"));

        List<Pattern> excludes = new ArrayList<>();
        excludes.add(Pattern.compile("a"));

        TableFilter tableFilter = new TableFilter(includes, excludes);

        assertNull( tableFilter.getIncludeTableColumnFilter("a"));
    }

    @Test
    public void testPatternsOrder(){
        List<IncludeTableFilter> includes = new ArrayList<>();
        includes.add(new IncludeTableFilter("b"));
        includes.add(new IncludeTableFilter("a"));

        List<Pattern> excludes = new ArrayList<>();
        excludes.add(Pattern.compile("b"));
        excludes.add(Pattern.compile("a"));

        TableFilter tableFilter = new TableFilter(includes, excludes);

        assertEquals("b",tableFilter.getIncludes().get(0).pattern.pattern());
        assertEquals("b",tableFilter.getExcludes().get(0).pattern());
    }
    @Test
    public void testNullArguments(){
        TableFilter tableFilter = new TableFilter(null, null);
        assertNotNull(tableFilter);
        assertThrows(NullPointerException.class, () -> tableFilter.isIncludeTable(null)  );

    }

}