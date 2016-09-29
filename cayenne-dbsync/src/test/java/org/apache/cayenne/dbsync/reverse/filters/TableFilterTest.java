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
package org.apache.cayenne.dbsync.reverse.filters;

import junit.framework.TestCase;

import java.util.TreeSet;
import java.util.regex.Pattern;

public class TableFilterTest extends TestCase {

    public void testIncludeEverything() {
        TableFilter filter = TableFilter.everything();

        assertNotNull(filter.isIncludeTable("table"));
        assertNotNull(filter.isIncludeTable("aaaa"));
        assertNotNull(filter.isIncludeTable(""));
        assertNotNull(filter.isIncludeTable("alex"));
    }

    public void testInclude() {
        TreeSet<IncludeTableFilter> includes = new TreeSet<IncludeTableFilter>();
        includes.add(new IncludeTableFilter("aaa"));
        includes.add(new IncludeTableFilter("bb"));

        TableFilter filter = new TableFilter(includes, new TreeSet<Pattern>(PatternFilter.PATTERN_COMPARATOR));

        assertNotNull(filter.isIncludeTable("aaa"));
        assertNull(filter.isIncludeTable("aa"));
        assertNull(filter.isIncludeTable("aaaa"));

        assertNotNull(filter.isIncludeTable("bb"));
        assertNull(filter.isIncludeTable(""));
        assertNull(filter.isIncludeTable("bbbb"));
    }


    public void testExclude() {
        TreeSet<Pattern> excludes = new TreeSet<Pattern>(PatternFilter.PATTERN_COMPARATOR);
        excludes.add(Pattern.compile("aaa"));
        excludes.add(Pattern.compile("bb"));

        TreeSet<IncludeTableFilter> includes = new TreeSet<IncludeTableFilter>();
        includes.add(new IncludeTableFilter(null, PatternFilter.INCLUDE_EVERYTHING));

        TableFilter filter = new TableFilter(includes, excludes);

        assertNull(filter.isIncludeTable("aaa"));
        assertNotNull(filter.isIncludeTable("aa"));
        assertNotNull(filter.isIncludeTable("aaaa"));

        assertNull(filter.isIncludeTable("bb"));
        assertNotNull(filter.isIncludeTable(""));
        assertNotNull(filter.isIncludeTable("bbbb"));
    }

    public void testIncludeExclude() {
        TreeSet<Pattern> excludes = new TreeSet<Pattern>(PatternFilter.PATTERN_COMPARATOR);
        excludes.add(Pattern.compile("aaa"));
        excludes.add(Pattern.compile("bb"));

        TreeSet<IncludeTableFilter> includes = new TreeSet<IncludeTableFilter>();
        includes.add(new IncludeTableFilter("aa.*"));

        TableFilter filter = new TableFilter(includes, excludes);

        assertNull(filter.isIncludeTable("aaa"));
        assertNotNull(filter.isIncludeTable("aa"));
        assertNotNull(filter.isIncludeTable("aaaa"));

        assertNull(filter.isIncludeTable("bb"));
        assertNull(filter.isIncludeTable(""));
        assertNull(filter.isIncludeTable("bbbb"));
    }
}