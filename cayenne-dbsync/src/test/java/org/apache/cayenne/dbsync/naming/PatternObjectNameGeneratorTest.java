/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.dbsync.naming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PatternObjectNameGeneratorTest {


    @Test
    public void dbEntityBaseName_NoMatch() {
        assertEquals("xyzabc", new PatternObjectNameGenerator("^pre").dbEntityBaseName("xyzabc"));
    }

    @Test
    public void dbEntityBaseName() {
        assertEquals("lowercase", new PatternObjectNameGenerator("^pre").dbEntityBaseName("prelowercase"));
        assertEquals("UPPERCASE", new PatternObjectNameGenerator("^pre").dbEntityBaseName("PREUPPERCASE"));
    }

    @Test
    public void stripHead() {
        assertEquals("name", new PatternObjectNameGenerator("^strip_").dbEntityBaseName("strip_name"));
        assertEquals("strip_name", new PatternObjectNameGenerator("^strip_").dbEntityBaseName("strip_strip_name"));
    }

    @Test
    public void stripTail() {
        assertEquals("name", new PatternObjectNameGenerator("_strip$").dbEntityBaseName("name_strip"));
        assertEquals("name_strip", new PatternObjectNameGenerator("_strip$").dbEntityBaseName("name_strip_strip"));
    }

    @Test
    public void stripiddle() {
        assertEquals("start_end", new PatternObjectNameGenerator("_strip").dbEntityBaseName("start_strip_end"));
        assertEquals("start_end", new PatternObjectNameGenerator("_strip").dbEntityBaseName("start_strip_strip_end"));
    }
}
