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

package org.apache.cayenne.wocompat.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class PropertyListParserTest extends TestCase {

    private static Parser parser(String plistText) {
        return new Parser(new StringReader(plistText));
    }

    public void testListPlist() throws Exception {
        List list = new ArrayList();
        list.add("str");
        list.add(new Integer(5));

        Object plist = parser("(str, 5)").object("");
        assertTrue(list.equals(plist));
    }

    public void testMapPlist() throws Exception {
        Map map = new HashMap();
        map.put("key1", "val");
        map.put("key2", new Integer(5));

        Object plist = parser("{key1 = val; key2 = 5}").object("");
        assertTrue(map.equals(plist));
    }

    public void testStringWithQuotes() throws Exception {
        List list = new ArrayList();
        list.add("s\"tr");
        list.add(new Integer(5));

        Object plist = parser("(\"s\\\"tr\", 5)").object("");
        assertTrue(list.equals(plist));
    }

    public void testNestedPlist() throws Exception {
        Map map = new HashMap();
        map.put("key1", "val");
        map.put("key2", new Integer(5));

        List list = new ArrayList();
        list.add("str");
        list.add(new Integer(5));
        map.put("key3", list);

        assertEquals(map, parser("{key1 = val; key2 = 5; key3 = (str, 5)}").object(""));
    }

    public void testStringWithSpaces() throws Exception {
        List list = new ArrayList();
        list.add("s tr");
        list.add(new Integer(5));

        Object plist = parser("(\"s tr\", 5)").object("");
        assertTrue(list.equals(plist));
    }

    public void testStringWithBraces() throws Exception {
        List list = new ArrayList();
        list.add("s{t)r");
        list.add(new Integer(5));

        assertEquals(list, parser("(\"s{t)r\", 5)").object(""));
    }

    public void testStringWithSlashes() throws Exception {
        List list = new ArrayList();
        list.add("s/t\\r");
        list.add(new Integer(5));

        assertEquals(list, parser("(\"s/t\\\\r\", 5)").object(""));
    }

    public void testMapWithLastSemicolon() throws Exception {
        Map map = new HashMap();
        map.put("key1", "val");
        map.put("key2", new Integer(5));

        // last semicolon is optional
        assertEquals(map, parser("{key1 = val; key2 = 5; }").object(""));
        assertEquals(map, parser("{key1 = val; key2 = 5 }").object(""));
    }

    public void testEmptyMap() throws Exception {
        assertEquals(Collections.EMPTY_MAP, parser("{}").object(""));
    }

    public void testEmptyList() throws Exception {
        assertEquals(Collections.EMPTY_LIST, parser("()").object(""));
    }

    public void testOutsideComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("// comment\n ( str)").object(""));
    }

    public void testInsideComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("(\n // comment\n str )").object(""));
    }

    public void testInsideKVComments() throws Exception {
        Map map = Collections.singletonMap("str", new Integer(5));
        assertEquals(map, parser("{\n str = // comment\n 5; }").object(""));
    }

    public void testTrailingComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("(// comment\n str)").object(""));
    }

    public void testDoubleslashInsideLiteral() throws Exception {
        List list = Collections.singletonList("s//tr");
        assertEquals(list, parser("( \"s//tr\" )").object(""));
    }

    public void testWindowsComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("// comment\r\n ( str)").object(""));
    }

    public void testMacComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("// comment\r ( str)").object(""));
    }

    public void testUNIXComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("// comment\n ( str)").object(""));
    }
}
