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
package org.apache.cayenne.access.loader.filters;

import junit.framework.TestCase;

public class PatternFilterTest extends TestCase {

    public void testInclude() throws Exception {
        PatternFilter filter = new PatternFilter()
                .include("aaa")
                .include("bbb");

        assertTrue(filter.isInclude("aaa"));
        assertTrue(filter.isInclude("bbb"));
        assertFalse(filter.isInclude("aaaa"));
        assertFalse(filter.isInclude("aa"));
        assertFalse(filter.isInclude("abb"));

        filter = new PatternFilter().include("^v_.*$");
        assertTrue(filter.isInclude("v_new_view"));
        assertFalse(filter.isInclude("new_view"));
        assertFalse(filter.isInclude("view"));
        assertFalse(filter.isInclude("girl"));
    }

    public void testExclude() throws Exception {
        PatternFilter filter = new PatternFilter()
                .exclude("aaa")
                .exclude("bbb");

        assertFalse(filter.isInclude("aaa"));
        assertFalse(filter.isInclude("bbb"));
        assertTrue(filter.isInclude("aaaa"));
        assertTrue(filter.isInclude("aa"));
        assertTrue(filter.isInclude("abb"));
    }

    public void testIncludeExclude() throws Exception {
        PatternFilter filter = new PatternFilter()
                .include("aa.*")
                .exclude("aaa");

        assertFalse(filter.isInclude("aaa"));
        assertFalse(filter.isInclude("bbb"));
        assertTrue(filter.isInclude("aaaa"));
        assertTrue(filter.isInclude("aa"));
        assertFalse(filter.isInclude("abb"));
    }

    public void testIncludeAllFilter() {
        assertTrue(PatternFilter.INCLUDE_EVERYTHING.isInclude("qwe"));
        assertTrue(PatternFilter.INCLUDE_EVERYTHING.isInclude(""));
        assertTrue(PatternFilter.INCLUDE_EVERYTHING.isInclude(null));
    }

    public void testIncludeNoneFilter() {
        assertFalse(PatternFilter.INCLUDE_NOTHING.isInclude("qwe"));
        assertFalse(PatternFilter.INCLUDE_NOTHING.isInclude(""));
        assertFalse(PatternFilter.INCLUDE_NOTHING.isInclude(null));
    }
}