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

import junit.framework.TestCase;

public class PatternFilterTest extends TestCase {

    public void testInclude() throws Exception {
        PatternFilter filter = new PatternFilter(false)
                .include("aaa")
                .include("bbb");

        assertTrue(filter.isIncluded("aaa"));
        assertTrue(filter.isIncluded("bbb"));
        assertFalse(filter.isIncluded("aaaa"));
        assertFalse(filter.isIncluded("aa"));
        assertFalse(filter.isIncluded("abb"));

        filter = new PatternFilter(false).include("^v_.*$");
        assertTrue(filter.isIncluded("v_new_view"));
        assertFalse(filter.isIncluded("new_view"));
        assertFalse(filter.isIncluded("view"));
        assertFalse(filter.isIncluded("girl"));
    }

    public void testExclude() throws Exception {
        PatternFilter filter = new PatternFilter(false)
                .exclude("aaa")
                .exclude("bbb");

        assertFalse(filter.isIncluded("aaa"));
        assertFalse(filter.isIncluded("bbb"));
        assertTrue(filter.isIncluded("aaaa"));
        assertTrue(filter.isIncluded("aa"));
        assertTrue(filter.isIncluded("abb"));
    }

    public void testIncludeExclude() throws Exception {
        PatternFilter filter = new PatternFilter(false)
                .include("aa.*")
                .exclude("aaa");

        assertFalse(filter.isIncluded("aaa"));
        assertFalse(filter.isIncluded("bbb"));
        assertTrue(filter.isIncluded("aaaa"));
        assertTrue(filter.isIncluded("aa"));
        assertFalse(filter.isIncluded("abb"));
    }

    public void testIncludeAllFilter() {
        assertTrue(PatternFilter.INCLUDE_EVERYTHING.isIncluded("qwe"));
        assertTrue(PatternFilter.INCLUDE_EVERYTHING.isIncluded(""));
        assertTrue(PatternFilter.INCLUDE_EVERYTHING.isIncluded(null));
    }

    public void testIncludeNoneFilter() {
        assertFalse(PatternFilter.INCLUDE_NOTHING.isIncluded("qwe"));
        assertFalse(PatternFilter.INCLUDE_NOTHING.isIncluded(""));
        assertFalse(PatternFilter.INCLUDE_NOTHING.isIncluded(null));
    }

    public void testIncludeCaseSensitive() throws Exception {
        PatternFilter filter = new PatternFilter(true)
                .include("aaa")
                .include("bbb");

        assertTrue(filter.isIncluded("aaa"));
        assertTrue(filter.isIncluded("bbb"));
        assertFalse(filter.isIncluded("aaA"));
        assertFalse(filter.isIncluded("AAA"));
        assertFalse(filter.isIncluded("Bbb"));

        filter = new PatternFilter(true).include("^v_.*$");
        assertTrue(filter.isIncluded("v_new_view"));
        assertFalse(filter.isIncluded("V_new_view"));
    }

    public void testExcludeCaseSensitive() throws Exception {
        PatternFilter filter = new PatternFilter(true)
                .exclude("aaa")
                .exclude("bbb");

        assertTrue(filter.isIncluded("Aaa"));
        assertTrue(filter.isIncluded("bbB"));
        assertTrue(filter.isIncluded("AAA"));
        assertTrue(filter.isIncluded("Bbb"));
    }

    public void testIncludeExcludeCaseSensitive() throws Exception {
        PatternFilter filter = new PatternFilter(true)
                .include("aa.*")
                .exclude("aaa");

        assertFalse(filter.isIncluded("aaa"));
        assertTrue(filter.isIncluded("aaA"));
        assertFalse(filter.isIncluded("bbb"));
        assertFalse(filter.isIncluded("Aaaa"));
        assertTrue(filter.isIncluded("aaAA"));
        assertFalse(filter.isIncluded("abb"));
    }
}