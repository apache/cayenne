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
        TreeSet<IncludeTableFilter> includes = new TreeSet<>();
        includes.add(new IncludeTableFilter("aaa", false));
        includes.add(new IncludeTableFilter("bb", false));

        TableFilter filter = new TableFilter(includes, new TreeSet<>(PatternFilter.PATTERN_COMPARATOR));

        assertTrue(filter.isIncludeTable("aaa"));
        assertFalse(filter.isIncludeTable("aa"));
        assertFalse(filter.isIncludeTable("aaaa"));

        assertTrue(filter.isIncludeTable("bb"));
        assertFalse(filter.isIncludeTable(""));
        assertFalse(filter.isIncludeTable("bbbb"));
    }

    @Test
    public void testExclude() {
        TreeSet<Pattern> excludes = new TreeSet<>(PatternFilter.PATTERN_COMPARATOR);
        excludes.add(Pattern.compile("aaa"));
        excludes.add(Pattern.compile("bb"));

        TreeSet<IncludeTableFilter> includes = new TreeSet<>();
        includes.add(new IncludeTableFilter(null, PatternFilter.INCLUDE_EVERYTHING, false));

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
        TreeSet<Pattern> excludes = new TreeSet<>(PatternFilter.PATTERN_COMPARATOR);
        excludes.add(Pattern.compile("aaa"));
        excludes.add(Pattern.compile("bb"));

        TreeSet<IncludeTableFilter> includes = new TreeSet<>();
        includes.add(new IncludeTableFilter("aa.*", false));

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
        TreeSet<IncludeTableFilter> includes = new TreeSet<IncludeTableFilter>();
        includes.add(new IncludeTableFilter("aaa", false));
        includes.add(new IncludeTableFilter("bb", false));

        TreeSet<Pattern> excludes = new TreeSet<>();

        TableFilter filter = new TableFilter(includes, excludes);

        assertNotNull(filter.getIncludeTableColumnFilter("aaa"));
        assertNull(filter.getIncludeTableColumnFilter("aa"));
        assertNull(filter.getIncludeTableColumnFilter("aaaa"));

        assertNotNull(filter.getIncludeTableColumnFilter("bb"));
        assertNull(filter.getIncludeTableColumnFilter(""));
        assertNull(filter.getIncludeTableColumnFilter("bbbb"));
    }

    @Test
    public void testIncludeCaseSensitive() {
        TreeSet<IncludeTableFilter> includes = new TreeSet<>();
        includes.add(new IncludeTableFilter("aaa", true));
        includes.add(new IncludeTableFilter("bb", true));

        TableFilter filter = new TableFilter(includes, new TreeSet<>(PatternFilter.PATTERN_COMPARATOR));

        assertTrue(filter.isIncludeTable("aaa"));
        assertFalse(filter.isIncludeTable("aaA"));
        assertFalse(filter.isIncludeTable("AAA"));

        assertTrue(filter.isIncludeTable("bb"));
        assertFalse(filter.isIncludeTable("Bb"));
        assertFalse(filter.isIncludeTable("BB"));
    }

    @Test
    public void testExcludeCaseSensitive() {
        TreeSet<Pattern> excludes = new TreeSet<>(PatternFilter.PATTERN_COMPARATOR);
        excludes.add(Pattern.compile("aaa"));
        excludes.add(Pattern.compile("bb"));

        TreeSet<IncludeTableFilter> includes = new TreeSet<>();
        includes.add(new IncludeTableFilter(null, PatternFilter.INCLUDE_EVERYTHING, true));

        TableFilter filter = new TableFilter(includes, excludes);

        assertTrue(filter.isIncludeTable("aaA"));
        assertTrue(filter.isIncludeTable("AAA"));
        assertTrue(filter.isIncludeTable("aaaa"));

        assertTrue(filter.isIncludeTable("bB"));
        assertTrue(filter.isIncludeTable(""));
        assertTrue(filter.isIncludeTable("bbbb"));
    }

    @Test
    public void testIncludeExcludeCaseSensitive() {
        TreeSet<Pattern> excludes = new TreeSet<>(PatternFilter.PATTERN_COMPARATOR);
        excludes.add(Pattern.compile("aaa"));
        excludes.add(Pattern.compile("bb"));

        TreeSet<IncludeTableFilter> includes = new TreeSet<>();
        includes.add(new IncludeTableFilter("aa.*", true));

        TableFilter filter = new TableFilter(includes, excludes);

        assertFalse(filter.isIncludeTable("aaa"));
        assertTrue(filter.isIncludeTable("aa"));
        assertTrue(filter.isIncludeTable("aaA"));

        assertFalse(filter.isIncludeTable("bb"));
        assertFalse(filter.isIncludeTable(""));
        assertFalse(filter.isIncludeTable("bB"));
    }

    @Test
    public void testGetTableFilterCaseSensitive() {
        TreeSet<IncludeTableFilter> includes = new TreeSet<IncludeTableFilter>();
        includes.add(new IncludeTableFilter("aaa", true));
        includes.add(new IncludeTableFilter("bb", true));

        TreeSet<Pattern> excludes = new TreeSet<>();

        TableFilter filter = new TableFilter(includes, excludes);

        assertNotNull(filter.getIncludeTableColumnFilter("aaa"));
        assertNull(filter.getIncludeTableColumnFilter("aa"));
        assertNull(filter.getIncludeTableColumnFilter("aaA"));

        assertNotNull(filter.getIncludeTableColumnFilter("bb"));
        assertNull(filter.getIncludeTableColumnFilter(""));
        assertNull(filter.getIncludeTableColumnFilter("bB"));
    }
}