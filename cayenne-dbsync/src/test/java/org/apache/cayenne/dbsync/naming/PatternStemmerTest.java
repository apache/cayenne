/*
 *    Licensed to the Apache Software Foundation (ASF) under one
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
package org.apache.cayenne.dbsync.naming;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PatternStemmerTest {

    @Test
    public void testStemNoMatch() {
        assertEquals("xyzabc", new PatternStemmer("^pre", false).stem("xyzabc"));
    }

    @Test
    public void testStemCaseSensitive() {
        assertEquals("PREUPPERCASE", new PatternStemmer("^pre", true).stem("PREUPPERCASE"));
        assertEquals("UPPERCASE", new PatternStemmer("^pre", true).stem("preUPPERCASE"));
    }

    @Test
    public void testStemCaseInsensitive() {
        assertEquals("lowercase", new PatternStemmer("^pre", false).stem("prelowercase"));
        assertEquals("UPPERCASE", new PatternStemmer("^pre", false).stem("PREUPPERCASE"));
    }

    @Test
    public void testStemHead() {
        assertEquals("name", new PatternStemmer("^strip_", false).stem("strip_name"));
        assertEquals("strip_name", new PatternStemmer("^strip_", false).stem("strip_strip_name"));
    }

    @Test
    public void testStemTail() {
        assertEquals("name", new PatternStemmer("_strip$", false).stem("name_strip"));
        assertEquals("name_strip", new PatternStemmer("_strip$", false).stem("name_strip_strip"));
    }

    @Test
    public void testStemMiddle() {
        assertEquals("start_end", new PatternStemmer("_strip", false).stem("start_strip_end"));
        assertEquals("start_end", new PatternStemmer("_strip", false).stem("start_strip_strip_end"));
    }
}
