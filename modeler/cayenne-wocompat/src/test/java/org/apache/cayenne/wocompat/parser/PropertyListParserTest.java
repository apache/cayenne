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

package org.apache.cayenne.wocompat.parser;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PropertyListParserTest {

    private static Parser parser(String plistText) {
        return new Parser(new StringReader(plistText));
    }

    @Test
    public void listPlist() throws Exception {
        List list = new ArrayList();
        list.add("str");
        list.add(5);

        Object plist = parser("(str, 5)").object("");
        assertTrue(list.equals(plist));
    }

    @Test
    public void mapPlist() throws Exception {
        Map map = new HashMap();
        map.put("key1", "val");
        map.put("key2", 5);

        Object plist = parser("{key1 = val; key2 = 5}").object("");
        assertTrue(map.equals(plist));
    }

    @Test
    public void stringWithQuotes() throws Exception {
        List list = new ArrayList();
        list.add("s\"tr");
        list.add(5);

        Object plist = parser("(\"s\\\"tr\", 5)").object("");
        assertTrue(list.equals(plist));
    }

    @Test
    public void nestedPlist() throws Exception {
        Map map = new HashMap();
        map.put("key1", "val");
        map.put("key2", 5);

        List list = new ArrayList();
        list.add("str");
        list.add(5);
        map.put("key3", list);

        assertEquals(map, parser("{key1 = val; key2 = 5; key3 = (str, 5)}").object(""));
    }

    @Test
    public void stringWithSpaces() throws Exception {
        List list = new ArrayList();
        list.add("s tr");
        list.add(5);

        Object plist = parser("(\"s tr\", 5)").object("");
        assertTrue(list.equals(plist));
    }

    @Test
    public void stringWithBraces() throws Exception {
        List list = new ArrayList();
        list.add("s{t)r");
        list.add(5);

        assertEquals(list, parser("(\"s{t)r\", 5)").object(""));
    }

    @Test
    public void stringWithSlashes() throws Exception {
        List list = new ArrayList();
        list.add("s/t\\r");
        list.add(5);

        assertEquals(list, parser("(\"s/t\\\\r\", 5)").object(""));
    }

    @Test
    public void mapWithLastSemicolon() throws Exception {
        Map map = new HashMap();
        map.put("key1", "val");
        map.put("key2", 5);

        // last semicolon is optional
        assertEquals(map, parser("{key1 = val; key2 = 5; }").object(""));
        assertEquals(map, parser("{key1 = val; key2 = 5 }").object(""));
    }

    @Test
    public void emptyMap() throws Exception {
        assertEquals(Collections.EMPTY_MAP, parser("{}").object(""));
    }

    @Test
    public void emptyList() throws Exception {
        assertEquals(Collections.EMPTY_LIST, parser("()").object(""));
    }

    @Test
    public void outsideComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("// comment\n ( str)").object(""));
    }

    @Test
    public void insideComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("(\n // comment\n str )").object(""));
    }

    @Test
    public void insideKVComments() throws Exception {
        Map map = Collections.singletonMap("str", 5);
        assertEquals(map, parser("{\n str = // comment\n 5; }").object(""));
    }

    @Test
    public void trailingComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("(// comment\n str)").object(""));
    }

    @Test
    public void doubleslashInsideLiteral() throws Exception {
        List list = Collections.singletonList("s//tr");
        assertEquals(list, parser("( \"s//tr\" )").object(""));
    }

    @Test
    public void windowsComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("// comment\r\n ( str)").object(""));
    }

    @Test
    public void macComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("// comment\r ( str)").object(""));
    }

    @Test
    public void uNIXComments() throws Exception {
        List list = Collections.singletonList("str");
        assertEquals(list, parser("// comment\n ( str)").object(""));
    }
}
